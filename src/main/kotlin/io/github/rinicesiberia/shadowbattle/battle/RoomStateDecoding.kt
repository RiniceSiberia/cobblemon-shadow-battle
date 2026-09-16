package io.github.rinicesiberia.shadowbattle.battle

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.util.UUID
import xiaocaoawa.minecraft.mod.cobblebattle.net.BattleServerClient
import xiaocaoawa.minecraft.mod.cobblebattle.network.RoomStatePayload

/** 将跨服房间状态消息解码为客户端可发送的状态包。 */
object RoomStateDecoding {
    data class Result(val participant: UUID, val payload: RoomStatePayload)

    @JvmStatic
    fun decode(document: JsonObject): Result? {
        val participant = BattleIdentifierParsing.uuidOrNull(BattleServerClient.str(document, "player", ""))
            ?: return null
        val hasGuest = document.has("guest") && document.get("guest").isJsonObject
        val guest = if (hasGuest) memberOf(document.getAsJsonObject("guest")) else RoomStatePayload.Member.NOBODY
        val watchers = ArrayList<RoomStatePayload.Member>()
        if (document.has("watchers") && document.get("watchers").isJsonArray) {
            for (element: JsonElement in document.getAsJsonArray("watchers")) {
                if (element.isJsonObject) watchers += memberOf(element.asJsonObject)
            }
        }
        return Result(
            participant,
            RoomStatePayload(
                BattleServerClient.str(document, "roomId", ""),
                BattleServerClient.str(document, "inviteCode", ""),
                BattleServerClient.str(document, "name", ""),
                BattleServerClient.bool(document, "locked", false),
                BattleServerClient.str(document, "battleType", "singles"),
                BattleServerClient.integer(document, "level", -1),
                BattleServerClient.integer(document, "pick", 6),
                BattleServerClient.bool(document, "fullHeal", true),
                BattleServerClient.str(document, "engine", "server") == "host",
                BattleServerClient.bool(document, "legality", true),
                BattleServerClient.bool(document, "fighting", false),
                readMember(document, "host"),
                hasGuest,
                guest,
                watchers,
                BattleServerClient.str(document, "youAre", "watcher")
            )
        )
    }

    private fun memberOf(document: JsonObject): RoomStatePayload.Member = RoomStatePayload.Member(
        BattleServerClient.str(document, "name", "?"),
        if (document.has("uid")) document.get("uid").asLong else 0L,
        BattleServerClient.str(document, "lead", ""),
        BattleServerClient.integer(document, "teamSize", 0)
    )

    private fun readMember(document: JsonObject, key: String): RoomStatePayload.Member =
        if (document.has(key) && document.get(key).isJsonObject) memberOf(document.getAsJsonObject(key))
        else RoomStatePayload.Member.NOBODY
}
