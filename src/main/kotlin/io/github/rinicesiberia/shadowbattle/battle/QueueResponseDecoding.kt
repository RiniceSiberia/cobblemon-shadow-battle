package io.github.rinicesiberia.shadowbattle.battle

import com.google.gson.JsonObject
import java.util.UUID
import xiaocaoawa.minecraft.mod.cobblebattle.net.BattleServerClient

/** 解码排队与房间操作的远端响应字段。 */
object QueueResponseDecoding {
    data class RoomBattleDetails(
        val roomId: String,
        val inviteCode: String,
        val battleType: String,
        val hostEngine: Boolean,
        val legality: Boolean,
    )

    data class RoomClosed(val participant: UUID, val reason: String)
    data class RoomCreated(val participant: UUID, val name: String, val inviteCode: String)
    data class QueueAccepted(
        val participant: UUID,
        val waiting: Int,
        val competitionName: String,
        val competitionId: String,
    )

    data class QueueDeparted(val participant: UUID, val wasQueued: Boolean, val reason: String)
    data class QueuePosition(val participant: UUID, val position: Int, val waiting: Int)

    @JvmStatic
    fun roomBattleDetails(document: JsonObject): RoomBattleDetails = RoomBattleDetails(
        BattleServerClient.str(document, "id", ""),
        BattleServerClient.str(document, "inviteCode", ""),
        if (BattleServerClient.bool(document, "fighting", false)) ""
        else BattleServerClient.str(document, "battleType", "singles"),
        BattleServerClient.str(document, "engine", "server") == "host",
        BattleServerClient.bool(document, "legality", true),
    )

    @JvmStatic
    fun roomClosed(document: JsonObject): RoomClosed = RoomClosed(
        participant(document),
        BattleServerClient.str(document, "why", ""),
    )

    @JvmStatic
    fun roomCreated(document: JsonObject): RoomCreated = RoomCreated(
        participant(document),
        BattleServerClient.str(document, "name", ""),
        BattleServerClient.str(document, "inviteCode", ""),
    )

    @JvmStatic
    fun queueAccepted(document: JsonObject): QueueAccepted = QueueAccepted(
        participant(document),
        BattleServerClient.integer(document, "waiting", 1),
        BattleServerClient.str(document, "rankedName", ""),
        BattleServerClient.str(document, "ranked", ""),
    )

    @JvmStatic
    fun queueDeparted(document: JsonObject): QueueDeparted = QueueDeparted(
        participant(document),
        BattleServerClient.bool(document, "wasQueued", false),
        BattleServerClient.str(document, "why", ""),
    )

    @JvmStatic
    fun queuePosition(document: JsonObject): QueuePosition = QueuePosition(
        participant(document),
        if (document.has("position")) document.get("position").asInt else 0,
        if (document.has("waiting")) document.get("waiting").asInt else 0,
    )

    private fun participant(document: JsonObject): UUID =
        UUID.fromString(BattleServerClient.str(document, "player", ""))
}
