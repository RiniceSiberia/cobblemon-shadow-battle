package io.github.rinicesiberia.shadowbattle.battle

import com.google.gson.JsonObject
import java.util.UUID
import xiaocaoawa.minecraft.mod.cobblebattle.net.BattleServerClient
import xiaocaoawa.minecraft.mod.cobblebattle.network.TeamPreviewPayload

/** 解码队伍预览打开、状态和关闭事件。 */
object TeamPreviewEventDecoding {
    sealed interface OpenResult {
        data object MissingRequired : OpenResult
        data class InvalidParticipant(val raw: String) : OpenResult
        data class Ready(
            val battleId: String,
            val participant: UUID,
            val seat: String,
            val pick: Int,
            val lead: Int,
            val deadline: Long,
            val opponent: String,
            val opponentServer: String,
            val mine: List<TeamPreviewPayload.Slot>,
            val theirs: List<TeamPreviewPayload.Slot>
        ) : OpenResult
    }

    data class State(val battleId: String, val ready: JsonObject)
    data class Closed(val battleId: String, val reason: String)
    data class Readiness(val mine: Boolean, val theirs: Boolean)

    @JvmStatic
    fun open(document: JsonObject, checkTime: Long, fallbackTime: Long): OpenResult {
        val battleId = BattleServerClient.str(document, "battleId", null)
        val participantRaw = BattleServerClient.str(document, "yourPlayer", null)
        val seat = BattleServerClient.str(document, "yourSeat", null)
        if (battleId == null || participantRaw == null || seat == null) return OpenResult.MissingRequired
        val participant = BattleIdentifierParsing.uuidOrNull(participantRaw)
            ?: return OpenResult.InvalidParticipant(participantRaw)
        val rosters = document.getAsJsonObject("rosters")
        val mine = TeamPreviewRosterDecoding.roster(rosters, seat)
        val theirs = TeamPreviewRosterDecoding.roster(rosters, TeamPreviewRosterDecoding.opponentSeat(rosters, seat))
        return OpenResult.Ready(
            battleId,
            participant,
            seat,
            BattleServerClient.integer(document, "pick", mine.size),
            BattleServerClient.integer(document, "lead", 1).coerceAtLeast(1),
            deadline(document, checkTime, fallbackTime),
            BattleServerClient.str(document, "opponent", ""),
            BattleServerClient.str(document, "opponentServer", ""),
            mine,
            theirs
        )
    }

    @JvmStatic
    fun state(document: JsonObject): State? {
        val battleId = BattleServerClient.str(document, "battleId", null) ?: return null
        val ready = document.getAsJsonObject("ready") ?: return null
        return State(battleId, ready)
    }

    @JvmStatic
    fun readiness(ready: JsonObject, seat: String): Readiness {
        var mine = false
        var theirs = false
        for ((key, element) in ready.entrySet()) {
            val value = element.isJsonPrimitive && element.asBoolean
            if (key == seat) mine = value else theirs = theirs || value
        }
        return Readiness(mine, theirs)
    }

    @JvmStatic
    fun closed(document: JsonObject): Closed? {
        val battleId = BattleServerClient.str(document, "battleId", null) ?: return null
        return Closed(battleId, BattleServerClient.str(document, "reason", "closed"))
    }

    @JvmStatic
    fun deadline(document: JsonObject, checkTime: Long, fallbackTime: Long): Long {
        val remote = BattleServerClient.longer(document, "deadline", 0L)
        val remaining = remote - checkTime
        return if (remaining > 0L && remaining <= 600_000L) remote else fallbackTime + 60_000L
    }

    @JvmStatic
    fun repaint(shown: TeamPreviewPayload, mineReady: Boolean, theirsReady: Boolean, closed: String): TeamPreviewPayload =
        TeamPreviewPayload(
            shown.battleId(), shown.you(), shown.opponent(), shown.opponentServer(), shown.pick(), shown.lead(),
            shown.deadlineMs(), shown.mine(), shown.theirs(), mineReady, theirsReady, closed
        )
}
