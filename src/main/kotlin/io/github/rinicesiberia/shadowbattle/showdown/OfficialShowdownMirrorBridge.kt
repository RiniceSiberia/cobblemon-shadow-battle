package io.github.rinicesiberia.shadowbattle.showdown

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.battles.BattleFormat
import com.cobblemon.mod.common.battles.BattleRegistry
import com.cobblemon.mod.common.battles.BattleSide
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor
import com.cobblemon.mod.common.pokemon.Pokemon
import com.google.gson.JsonObject
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import xiaocaoawa.minecraft.mod.cobblebattle.battle.CrossServerBattles
import xiaocaoawa.minecraft.mod.cobblebattle.battle.MirrorBattle
import xiaocaoawa.minecraft.mod.cobblebattle.battle.RemoteBattleActor
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/** 将官方 PS battle room 的公开事件接入现有 MirrorBattle。 */
object OfficialShowdownMirrorBridge {
    private val rooms = ConcurrentHashMap<String, RoomBridge>()
    @Volatile private var server: MinecraftServer? = null

    fun onServerStarted(server: MinecraftServer) { this.server = server; rooms.clear() }

    fun accept(player: UUID, frame: ShowdownFrame) {
        val roomId = frame.roomId ?: return
        if (!roomId.startsWith("battle-")) return
        val room = rooms.computeIfAbsent(roomId) { RoomBridge(roomId, player) }
        server?.execute { room.accept(frame.lines) } ?: room.accept(frame.lines)
    }

    fun forwardChoice(relay: CrossServerBattles.ChoiceRelay) {
        val room = rooms[relay.remoteBattleId()] ?: return
        room.forwardChoice(relay.line())
    }

    private class RoomBridge(private val roomId: String, private val localPlayer: UUID) {
        private val lines = ArrayList<String>()
        private val speciesBySeat = linkedMapOf<String, MutableList<String>>()
        private val names = linkedMapOf<String, String>()
        private val sequence = AtomicLong(1)
        @Volatile private var mirror: MirrorBattle? = null
        @Volatile private var closed = false

        @Synchronized
        fun accept(incoming: List<String>) {
            if (closed || incoming.isEmpty()) return
            val start = incoming.any { it == "|start|" }
            incoming.forEach(::inspect)
            if (mirror == null) {
                lines += incoming
                if (!start) return
                buildMirror()
                val stream = lines.dropWhile { it != "|start|" }.drop(1)
                stream.takeIf { it.isNotEmpty() }?.let { deliver(it) }
                lines.clear()
            } else {
                deliver(incoming)
            }
            if (incoming.any { it.startsWith("|win|") || it.startsWith("|tie|") || it.startsWith("|forfeit|") }) finish()
        }

        private fun inspect(line: String) {
            val fields = line.split('|')
            when (fields.getOrNull(1)) {
                "player" -> if (fields.size > 3) names[fields[2]] = fields[3]
                "poke" -> if (fields.size > 3) {
                    val species = fields[3].substringBefore(',').trim()
                    if (species.isNotEmpty()) speciesBySeat.getOrPut(fields[2]) { mutableListOf() }.add(ShowdownIdentifiers.speciesId(species))
                }
            }
        }

        private fun buildMirror() {
            val activeServer = server ?: return finish()
            val player = activeServer.playerList.getPlayer(localPlayer) ?: return finish()
            val localSeat = names.entries.firstOrNull { ShowdownIdentifiers.same(it.value, player.gameProfile.name) }?.key ?: "p1"
            val opponentSeat = if (localSeat == "p1") "p2" else "p1"
            val opponentName = names[opponentSeat] ?: "Pokémon Showdown"
            val localRoster = Cobblemon.storage.getParty(player).toBattleTeam(true, true, null)
            ShowdownBattleStatePolicy.normalize(localRoster)
            if (localRoster.isEmpty()) return finish()
            val remoteRoster = placeholderRoster(speciesBySeat[opponentSeat].orEmpty())
            if (remoteRoster.isEmpty()) return finish()
            val localActor = PlayerBattleActor(localPlayer, localRoster)
            val remoteActor = RemoteBattleActor(UUID.nameUUIDFromBytes("$roomId:$opponentSeat".toByteArray()), opponentName, "pokemonshowdown", opponentSeat, remoteRoster)
            val first: BattleSide
            val second: BattleSide
            if (localSeat == "p1") {
                first = BattleSide(localActor); second = BattleSide(remoteActor)
            } else {
                first = BattleSide(remoteActor); second = BattleSide(localActor)
            }
            val projection = MirrorBattle(roomId, localSeat, opponentSeat, localPlayer, opponentName, "pokemonshowdown", false, false)
            CrossServerBattles.beginConstruction(projection)
            try {
                BattleRegistry.startBattle(BattleFormat.fromFormatIdentifier("singles"), first, second, false)
            } finally {
                CrossServerBattles.endConstruction()
            }
            val battleId = projection.localBattleId() ?: return finish()
            projection.attach(BattleRegistry.getBattle(battleId))
            mirror = projection
            projection.release()
        }

        private fun placeholderRoster(species: List<String>): List<com.cobblemon.mod.common.battles.pokemon.BattlePokemon> {
            if (species.isEmpty()) return emptyList()
            val rows = species.take(6).map { name ->
                val fields = MutableList(17) { "" }
                fields[0] = name
                fields[2] = UUID.randomUUID().toString()
                fields[3] = "9999"
                fields[15] = "100"
                fields.joinToString("|")
            }
            return io.github.rinicesiberia.shadowbattle.battle.RemoteRosterAssembly.decode(rows.joinToString("]"))
        }

        private fun deliver(stream: List<String>) {
            mirror?.accept(sequence.getAndIncrement(), stream.joinToString("\n"))
        }

        fun forwardChoice(choice: String) {
            val session = ShowdownPlayerSessions.sessionFor(localPlayer) ?: return
            val command = choice.trim().let { if (it.startsWith("/")) it else "/choose $it" }
            session.sendToRoom(roomId, command)
        }

        private fun finish() {
            closed = true
            mirror?.markFinished()
            mirror?.localBattleId()?.let { id ->
                CrossServerBattles.forget(id)
                server?.execute {
                    BattleRegistry.getBattle(id)?.let { battle ->
                        if (!battle.ended) battle.end()
                        BattleRegistry.closeBattle(battle)
                    }
                }
            }
            rooms.remove(roomId)
        }

        fun closeWithoutStream() {
            if (!closed) finish()
        }
    }

    fun onServerStopping() {
        rooms.values.toList().forEach { it.closeWithoutStream() }
        rooms.clear()
        server = null
    }
}
