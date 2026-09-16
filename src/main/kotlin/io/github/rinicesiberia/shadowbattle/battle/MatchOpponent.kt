package io.github.rinicesiberia.shadowbattle.battle

import com.google.gson.JsonObject
import java.util.UUID

/** 匹配帧中最后一个不同于本地席位的对手描述。 */
data class MatchOpponent(
    val seatId: String,
    val playerId: UUID,
    val name: String,
    val serverId: String,
    val authoritative: Boolean,
    val description: JsonObject,
)

object MatchOpponentParsing {
    @JvmStatic
    fun find(document: JsonObject, localSeat: String): MatchOpponent? {
        var opponent: JsonObject? = null
        for (element in document.getAsJsonArray("seats")) {
            val seat = element.asJsonObject
            if (localSeat != seat.get("showdownId").asString) opponent = seat
        }
        val seat = opponent ?: return null
        val seatId = seat.get("showdownId").asString
        val participant = seat.getAsJsonObject("player")
        val playerId = UUID.fromString(participant.get("uuid").asString)
        val name = participant.get("name").asString
        val serverId = seat.get("serverId").asString
        val authoritative = document.has("authoritative") && !document.get("authoritative").isJsonNull && document.get("authoritative").asBoolean
        return MatchOpponent(seatId, playerId, name, serverId, authoritative, seat)
    }
}
