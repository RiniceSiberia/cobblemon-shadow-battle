package xiaocaoawa.minecraft.mod.cobblebattle.battle

import com.cobblemon.mod.common.CobblemonNetwork.sendPacket
import com.cobblemon.mod.common.battles.BattleRegistry
import com.cobblemon.mod.common.battles.BattleSide
import com.cobblemon.mod.common.net.messages.client.battle.BattleInitializePacket
import com.cobblemon.mod.common.net.messages.client.battle.BattleMessagePacket
import com.google.gson.JsonObject
import io.github.rinicesiberia.shadowbattle.battle.BattleConstructionAttempt
import io.github.rinicesiberia.shadowbattle.battle.BattleFormatResolver
import io.github.rinicesiberia.shadowbattle.battle.SpectatorTeamLayout
import io.github.rinicesiberia.shadowbattle.battle.SpectatorTeams
import io.github.rinicesiberia.shadowbattle.transport.BattleControlMessages
import net.minecraft.ChatFormatting
import net.minecraft.server.level.ServerPlayer
import org.slf4j.LoggerFactory
import xiaocaoawa.minecraft.mod.cobblebattle.lang.Msg
import xiaocaoawa.minecraft.mod.cobblebattle.net.BattleServerClient
import java.util.UUID

/** 处理观战镜像的创建、玩家挂接和离开。 */
class SpectatorSessions(private val service: CrossServerBattleService) {
    private val logger = LoggerFactory.getLogger("CobbleBattle/Spectate")

    fun begin(document: JsonObject) {
        val remoteBattleId = BattleServerClient.str(document, "battleId", null)
        val watcherId = parseWatcher(BattleServerClient.str(document, "player", null))
        if (remoteBattleId == null || watcherId == null) {
            logger.error("spectate_start was missing required fields")
            return
        }
        try { seatWatcher(document, remoteBattleId, watcherId) } catch (failure: Throwable) {
            logger.error("Seating a spectator in battle {} threw", remoteBattleId, failure)
            service.tellParticipant(watcherId, Msg.of(ChatFormatting.RED, "battle.spectate_failed"))
        }
    }

    private fun seatWatcher(document: JsonObject, remoteBattleId: String, watcherId: UUID) {
        val player = service.server()?.playerList?.getPlayer(watcherId) ?: return
        val existing = CrossServerBattles.byRemoteId(remoteBattleId)
        if (existing != null) {
            attach(existing, player)
            return
        }
        if (BattleServerClient.str(document, "mode", "mirror") != "mirror") {
            logger.error("spectate_start says battle {} is local, but this server has no mirror of it", remoteBattleId)
            service.tellParticipant(watcherId, Msg.of(ChatFormatting.RED, "battle.spectate_failed"))
            return
        }
        val mirror = build(document, remoteBattleId, player) ?: return
        attach(mirror, player)
        service.client().send(BattleControlMessages.acknowledgement(remoteBattleId))
        mirror.release()
    }

    private fun build(document: JsonObject, remoteBattleId: String, viewer: ServerPlayer): MirrorBattle? {
        val parsed = SpectatorTeamLayout.decode(document, RemoteTeamCodec::decode)
        val seats = when (parsed) {
            SpectatorTeams.MissingDescription -> {
                logger.error("spectate_start for {} did not describe the battle", remoteBattleId)
                service.tellParticipant(viewer.uuid, Msg.of(ChatFormatting.RED, "battle.spectate_failed"))
                return null
            }
            SpectatorTeams.MissingSeats -> {
                logger.error("spectate_start for {} did not name both seats", remoteBattleId)
                return null
            }
            is SpectatorTeams.EmptyRoster -> {
                logger.error("Could not rebuild the team of {} to watch battle {}", parsed.showdownId, remoteBattleId)
                service.tellParticipant(viewer.uuid, Msg.of(ChatFormatting.RED, "battle.spectate_failed"))
                return null
            }
            is SpectatorTeams.Ready -> listOf(parsed.first, parsed.second)
        }
        val bodies = MirrorNpc.spawnPair(viewer, seats[0].name, seats[1].name)
        val actors = seats.mapIndexed { index, seat ->
            val body = bodies[index]
            if (body != null) EntityBackedRemoteBattleActor(seat.playerId, seat.name, seat.serverId, seat.showdownId, seat.roster, body)
            else RemoteBattleActor(seat.playerId, seat.name, seat.serverId, seat.showdownId, seat.roster)
        }
        val mirror = MirrorBattle.spectator(remoteBattleId, service.config().debug)
        seats.forEach { MirrorPokemon.claim(mirror, it.roster) }
        fun reclaim() {
            bodies.forEach(MirrorNpc::despawn)
            seats.forEach { MirrorPokemon.release(it.roster) }
        }
        CrossServerBattles.beginConstruction(mirror)
        val started = BattleConstructionAttempt.startWithRecovery({
            BattleRegistry.startBattle(BattleFormatResolver.resolveSpectator(document), BattleSide(actors[0]), BattleSide(actors[1]), false)
        }, { failure ->
            logger.error("Could not build a mirror to watch battle {}", remoteBattleId, failure)
            reclaim()
            service.tellParticipant(viewer.uuid, Msg.of(ChatFormatting.RED, "battle.spectate_failed"))
        }, CrossServerBattles::endConstruction)
        if (!started) return null
        val localBattleId = mirror.localBattleId()
        if (localBattleId == null) {
            logger.error("Mirror for watching {} never reached the showdown hook", remoteBattleId)
            reclaim()
            service.tellParticipant(viewer.uuid, Msg.of(ChatFormatting.RED, "battle.mixin_missing"))
            return null
        }
        actors.forEachIndexed { index, actor -> mirror.attachBody(bodies[index], actor) }
        mirror.attach(BattleRegistry.getBattle(localBattleId))
        logger.info("Built a spectator mirror of battle {} (local {})", remoteBattleId, localBattleId)
        return mirror
    }

    private fun attach(mirror: MirrorBattle, player: ServerPlayer) {
        val battle = mirror.battle()
        if (battle == null) {
            logger.error("Battle {} has no local battle to watch", mirror.remoteBattleId())
            service.tellParticipant(player.uuid, Msg.of(ChatFormatting.RED, "battle.spectate_failed"))
            return
        }
        mirror.addWatcher(player.uuid)
        battle.spectators.add(player.uuid)
        player.sendPacket(BattleInitializePacket(battle, battle.side1))
        player.sendPacket(BattleMessagePacket(battle.chatLog))
        service.tellParticipant(player.uuid, Msg.of(ChatFormatting.AQUA, "battle.spectating"))
        ApiEvents.spectateStarted(player, mirror.remoteBattleId())
    }

    fun end(document: JsonObject) {
        val remoteBattleId = BattleServerClient.str(document, "battleId", "")
        val watcherId = parseWatcher(BattleServerClient.str(document, "player", null))
        val mirror = CrossServerBattles.byRemoteId(remoteBattleId)
        if (mirror == null || watcherId == null) return
        mirror.battle()?.spectators?.remove(watcherId)
        val wasLast = mirror.removeWatcher(watcherId)
        service.tellParticipant(watcherId, Msg.of(ChatFormatting.YELLOW, "battle.spectate_over"))
        service.server()?.playerList?.getPlayer(watcherId)?.let { ApiEvents.spectateEnded(it, remoteBattleId) }
        if (wasLast && mirror.isSpectator) service.closeReplayViewMirror(mirror)
    }

    private fun parseWatcher(rawId: String?): UUID? =
        if (rawId.isNullOrEmpty()) null else try { UUID.fromString(rawId) } catch (_: IllegalArgumentException) { null }
}
