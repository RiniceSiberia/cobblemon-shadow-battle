package io.github.rinicesiberia.shadowbattle.battle

import com.google.gson.JsonObject
import java.util.UUID

/** 观战席位及其对应的远端队伍。 */
data class SpectatorSeat<T>(val showdownId: String, val name: String, val playerId: UUID, val serverId: String, val roster: List<T>)

/** 双席描述的业务校验结果。 */
sealed interface SpectatorTeams<out T> {
    data object MissingDescription : SpectatorTeams<Nothing>
    data object MissingSeats : SpectatorTeams<Nothing>
    data class EmptyRoster(val showdownId: String) : SpectatorTeams<Nothing>
    data class Ready<T>(val first: SpectatorSeat<T>, val second: SpectatorSeat<T>) : SpectatorTeams<T>
}

/** 按帧中顺序重建队伍；重复席位保留后出现的描述。 */
object SpectatorTeamLayout {
    fun <T> decode(document: JsonObject, rebuild: (String) -> List<T>): SpectatorTeams<T> {
        val teams = document.getAsJsonObject("teams")
        val seats = document.getAsJsonArray("seats")
        if (teams == null || seats == null || seats.size() < 2) return SpectatorTeams.MissingDescription
        var first: SpectatorSeat<T>? = null
        var second: SpectatorSeat<T>? = null
        for (element in seats) {
            val seat = element.asJsonObject
            val showdownId = seat.get("showdownId").asString
            val participant = seat.getAsJsonObject("player")
            val roster = rebuild(teams.get(showdownId).asString)
            if (roster.isEmpty()) return SpectatorTeams.EmptyRoster(showdownId)
            val parsed = SpectatorSeat(showdownId, participant.get("name").asString, UUID.fromString(participant.get("uuid").asString), seat.get("serverId").asString, roster)
            if (showdownId == "p1") first = parsed else second = parsed
        }
        val firstSeat = first ?: return SpectatorTeams.MissingSeats
        val secondSeat = second ?: return SpectatorTeams.MissingSeats
        return SpectatorTeams.Ready(firstSeat, secondSeat)
    }
}
