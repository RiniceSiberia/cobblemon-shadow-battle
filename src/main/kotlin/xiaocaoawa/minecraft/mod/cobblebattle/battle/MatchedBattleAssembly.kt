package xiaocaoawa.minecraft.mod.cobblebattle.battle

import com.cobblemon.mod.common.api.battles.model.actor.BattleActor
import com.cobblemon.mod.common.battles.BattleRegistry
import com.cobblemon.mod.common.battles.BattleSide
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor
import com.google.gson.JsonObject
import io.github.rinicesiberia.shadowbattle.battle.BattleConstructionAttempt
import io.github.rinicesiberia.shadowbattle.battle.BattleFormatResolver
import io.github.rinicesiberia.shadowbattle.battle.LocalMatchClaims
import io.github.rinicesiberia.shadowbattle.battle.MatchOpponent
import io.github.rinicesiberia.shadowbattle.battle.MatchOpponentParsing
import io.github.rinicesiberia.shadowbattle.battle.TeamSelection
import io.github.rinicesiberia.shadowbattle.transport.BattleControlMessages
import net.minecraft.ChatFormatting
import org.slf4j.LoggerFactory
import xiaocaoawa.minecraft.mod.cobblebattle.api.BattleInfo
import xiaocaoawa.minecraft.mod.cobblebattle.config.ServerIdentity
import xiaocaoawa.minecraft.mod.cobblebattle.lang.Msg
import xiaocaoawa.minecraft.mod.cobblebattle.net.BattleServerClient
import java.util.UUID

/** 将匹配结果装配为跨服镜像或双本地玩家对战。 */
class MatchedBattleAssembly(private val service: CrossServerBattleService) {
    private val logger = LoggerFactory.getLogger("CobbleBattle")

    fun build(document: JsonObject) {
        val battleId = BattleServerClient.str(document, "battleId", null)
        val participantText = BattleServerClient.str(document, "yourPlayer", null)
        try {
            buildMatch(document, battleId)
        } catch (failure: Throwable) {
            logger.error("Building the mirror for battle {} threw", battleId, failure)
            if (participantText != null) {
                try {
                    service.tellParticipant(UUID.fromString(participantText), Msg.of(ChatFormatting.RED, "battle.setup_failed"))
                } catch (_: RuntimeException) { }
            }
            if (battleId != null) service.sendAbort(battleId, "mirror build threw: ${failure.javaClass.simpleName}: ${failure.message}")
        }
    }

    private fun buildMatch(document: JsonObject, battleId: String?) {
        val localSeat = BattleServerClient.str(document, "yourSeat", null)
        val participantText = BattleServerClient.str(document, "yourPlayer", null)
        if (battleId == null || localSeat == null || participantText == null) {
            logger.error("match_found was missing required fields")
            return
        }
        val participantId = UUID.fromString(participantText)
        val opponent = MatchOpponentParsing.find(document, localSeat)
        if (opponent == null) {
            logger.error("match_found did not describe an opponent seat")
            fail(battleId, listOf(participantId), "battle.setup_failed", "no opponent seat")
            return
        }
        if (opponent.serverId != ServerIdentity.get()) {
            buildRemote(document, battleId, localSeat, participantId, opponent)
        } else if (CrossServerBattles.byRemoteId(battleId) != null) {
            logger.debug("match_found for {} in battle {}: the other seat's frame already built it", participantId, battleId)
        } else {
            buildLocalPair(document, battleId, localSeat, participantId, opponent)
        }
    }

    private fun buildRemote(document: JsonObject, battleId: String, localSeat: String, participantId: UUID, opponent: MatchOpponent) {
        val queued = service.battleQueue().claim(participantId)
        if (queued == null) {
            logger.error("match_found for {} but we have no queued team for them", participantId)
            fail(battleId, listOf(participantId), "battle.setup_failed", "no queued team on this server")
            return
        }
        val packedRoster = document.getAsJsonObject("teams").get(opponent.seatId).asString
        val remoteRoster = RemoteTeamCodec.decode(packedRoster)
        if (remoteRoster.isEmpty()) {
            logger.error("Could not rebuild the opposing team for battle {}", battleId)
            fail(battleId, listOf(participantId), "battle.team_rebuild_failed", "opposing team could not be rebuilt")
            return
        }
        val expectedSize = BattleServerClient.integer(opponent.description, "teamSize", remoteRoster.size)
        if (opponent.authoritative && remoteRoster.size < expectedSize) {
            logger.error("Battle {}: rebuilt only {} of {} opposing Pokemon - this server's data cannot run this battle", battleId, remoteRoster.size, expectedSize)
            fail(battleId, listOf(participantId), "battle.team_rebuild_failed", "this server knows only ${remoteRoster.size} of the $expectedSize Pokemon on the opposing team")
            return
        }
        val localRoster = TeamSelection.choose(queued.team(), document, localSeat)
        if (localRoster.isEmpty()) {
            logger.error("Battle {}: none of this player's team was picked", battleId)
            fail(battleId, listOf(participantId), "battle.setup_failed", "no Pokemon left after the team preview")
            return
        }
        val localActor = PlayerBattleActor(participantId, localRoster)
        val player = service.server()?.playerList?.getPlayer(participantId)
        val body = player?.let { MirrorNpc.spawn(it, opponent.name) }
        val remoteActor = if (body != null) EntityBackedRemoteBattleActor(opponent.playerId, opponent.name, opponent.serverId, opponent.seatId, remoteRoster, body)
            else RemoteBattleActor(opponent.playerId, opponent.name, opponent.serverId, opponent.seatId, remoteRoster)
        val firstActor: BattleActor = if (localSeat == "p1") localActor else remoteActor
        val secondActor: BattleActor = if (localSeat == "p1") remoteActor else localActor
        val firstSide = BattleSide(firstActor)
        val secondSide = BattleSide(secondActor)
        val format = BattleFormatResolver.resolve(document)
        val mirror = MirrorBattle(battleId, localSeat, opponent.seatId, participantId, opponent.name, opponent.serverId, opponent.authoritative, service.config().debug)
        mirror.attachBody(body, remoteActor)
        MirrorPokemon.claim(mirror, remoteRoster)
        CrossServerBattles.beginConstruction(mirror)
        val failure = BattleConstructionAttempt.captureFailure({ BattleRegistry.startBattle(format, firstSide, secondSide, false) }, CrossServerBattles::endConstruction)
        if (failure != null) {
            logger.error("Failed to build the mirror battle for {}", battleId, failure)
            MirrorNpc.despawn(body)
            MirrorPokemon.release(remoteRoster)
            CrossServerBattles.forget(mirror.localBattleId() ?: UUID(0L, 0L))
            fail(battleId, listOf(participantId), "battle.setup_failed", "mirror construction failed")
            return
        }
        val localBattleId = mirror.localBattleId()
        if (localBattleId == null) {
            logger.error("Mirror battle for {} never reached the showdown hook - is the GraalShowdownService mixin applied?", battleId)
            MirrorNpc.despawn(body)
            MirrorPokemon.release(remoteRoster)
            fail(battleId, listOf(participantId), "battle.mixin_missing", MISSING_HOOK)
            return
        }
        mirror.attach(BattleRegistry.getBattle(localBattleId))
        logger.info("{} battle {} <-> local {} built: {} vs {}@{}", if (opponent.authoritative) "Host-run" else "Mirror", battleId, localBattleId, localSeat, opponent.name, opponent.serverId)
        val details = BattleInfo(battleId, BattleServerClient.str(document, "ranked", ""), BattleServerClient.str(document, "rankedName", ""), BattleServerClient.bool(document, "casual", false), localSeat, opponent.playerId, opponent.name, opponent.serverId)
        mirror.describe(details)
        service.tellParticipant(participantId, Msg.of(ChatFormatting.AQUA, "battle.found", opponent.name, opponent.serverId))
        if (player != null) ApiEvents.battleStarted(player, details)
        acknowledge(battleId)
    }

    private fun buildLocalPair(document: JsonObject, battleId: String, localSeat: String, participantId: UUID, opponent: MatchOpponent) {
        val claims = LocalMatchClaims.take(participantId, opponent.playerId, service.battleQueue()::claim)
        val localQueue = claims.first
        val opponentQueue = claims.second
        val participants = listOf(participantId, opponent.playerId)
        if (localQueue == null || opponentQueue == null) {
            logger.error("match_found for {} and {} on this server, but a queued team is missing ({} / {})", participantId, opponent.playerId, localQueue != null, opponentQueue != null)
            fail(battleId, participants, "battle.setup_failed", "no queued team on this server")
            return
        }
        val localRoster = TeamSelection.choose(localQueue.team(), document, localSeat)
        val opponentRoster = TeamSelection.choose(opponentQueue.team(), document, opponent.seatId)
        if (localRoster.isEmpty() || opponentRoster.isEmpty()) {
            logger.error("Battle {}: a side has nothing left after the team preview", battleId)
            fail(battleId, participants, "battle.setup_failed", "no Pokemon left after the team preview")
            return
        }
        val server = service.server()
        val localPlayer = server?.playerList?.getPlayer(participantId)
        val opponentPlayer = server?.playerList?.getPlayer(opponent.playerId)
        val localName = if (localPlayer != null) localPlayer.gameProfile.name else BattleServerClient.str(document, "yourName", "?")
        val localActor = PlayerBattleActor(participantId, localRoster)
        val opponentActor = PlayerBattleActor(opponent.playerId, opponentRoster)
        val firstSide = BattleSide(if (localSeat == "p1") localActor else opponentActor)
        val secondSide = BattleSide(if (localSeat == "p1") opponentActor else localActor)
        val format = BattleFormatResolver.resolve(document)
        val serverId = ServerIdentity.get()
        val mirror = MirrorBattle(battleId, localSeat, opponent.seatId, participantId, opponent.playerId, opponent.name, serverId, opponent.authoritative, service.config().debug)
        CrossServerBattles.beginConstruction(mirror)
        val failure = BattleConstructionAttempt.captureFailure({ BattleRegistry.startBattle(format, firstSide, secondSide, false) }, CrossServerBattles::endConstruction)
        if (failure != null) {
            logger.error("Failed to build the two-player battle for {}", battleId, failure)
            CrossServerBattles.forget(mirror.localBattleId() ?: UUID(0L, 0L))
            fail(battleId, participants, "battle.setup_failed", "mirror construction failed")
            return
        }
        val localBattleId = mirror.localBattleId()
        if (localBattleId == null) {
            logger.error("Two-player battle for {} never reached the showdown hook - is the GraalShowdownService mixin applied?", battleId)
            fail(battleId, participants, "battle.mixin_missing", MISSING_HOOK)
            return
        }
        mirror.attach(BattleRegistry.getBattle(localBattleId))
        logger.info("{} battle {} <-> local {} built: {} ({}) vs {} ({}), both on this server", if (opponent.authoritative) "Host-run" else "Mirror", battleId, localBattleId, localSeat, localName, opponent.seatId, opponent.name)
        val ranked = BattleServerClient.str(document, "ranked", "")
        val rankedName = BattleServerClient.str(document, "rankedName", "")
        val casual = BattleServerClient.bool(document, "casual", false)
        val localDetails = BattleInfo(battleId, ranked, rankedName, casual, localSeat, opponent.playerId, opponent.name, serverId)
        val opponentDetails = BattleInfo(battleId, ranked, rankedName, casual, opponent.seatId, participantId, localName, serverId)
        mirror.describe(localDetails)
        mirror.describeSecond(opponentDetails)
        service.tellParticipant(participantId, Msg.of(ChatFormatting.AQUA, "battle.found_local", opponent.name))
        service.tellParticipant(opponent.playerId, Msg.of(ChatFormatting.AQUA, "battle.found_local", localName))
        if (localPlayer != null) ApiEvents.battleStarted(localPlayer, localDetails)
        if (opponentPlayer != null) ApiEvents.battleStarted(opponentPlayer, opponentDetails)
        acknowledge(battleId)
    }

    private fun fail(battleId: String, participants: List<UUID>, messageKey: String, reason: String) {
        for (participant in participants) service.tellParticipant(participant, Msg.of(ChatFormatting.RED, messageKey))
        service.sendAbort(battleId, reason)
    }

    private fun acknowledge(battleId: String) {
        service.client().send(BattleControlMessages.acknowledgement(battleId))
    }

    private companion object {
        const val MISSING_HOOK = "mixin did not fire - CobbleBattle is not hooked into Cobblemon on this server"
    }
}
