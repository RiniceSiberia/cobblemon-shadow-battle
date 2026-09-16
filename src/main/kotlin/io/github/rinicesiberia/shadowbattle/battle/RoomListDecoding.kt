package io.github.rinicesiberia.shadowbattle.battle

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import xiaocaoawa.minecraft.mod.cobblebattle.net.BattleServerClient
import xiaocaoawa.minecraft.mod.cobblebattle.network.RoomListPayload

/** 解码跨服房间列表，集中维护字段默认值与对象过滤规则。 */
object RoomListDecoding {
    @JvmStatic
    fun decodeRooms(document: JsonObject): List<RoomListPayload.Room> {
        val source = if (document.has("rooms") && document.get("rooms").isJsonArray) {
            document.getAsJsonArray("rooms")
        } else {
            JsonArray()
        }
        val rooms = ArrayList<RoomListPayload.Room>()
        for (element: JsonElement in source) {
            if (!element.isJsonObject) continue
            val room = element.asJsonObject
            rooms += RoomListPayload.Room(
                BattleServerClient.str(room, "id", ""),
                BattleServerClient.str(room, "name", ""),
                BattleServerClient.str(room, "host", "?"),
                if (room.has("hostUid") && !room.get("hostUid").isJsonNull) room.get("hostUid").asLong else 0L,
                BattleServerClient.str(room, "battleType", "singles"),
                BattleServerClient.integer(room, "level", -1),
                BattleServerClient.integer(room, "pick", 6),
                BattleServerClient.bool(room, "fullHeal", true),
                BattleServerClient.bool(room, "locked", false),
                BattleServerClient.str(room, "lead", ""),
                BattleServerClient.bool(room, "hasGuest", false),
                BattleServerClient.integer(room, "watchers", 0),
                false,
                BattleServerClient.str(room, "engine", "server") == "host",
                BattleServerClient.bool(room, "fighting", false),
                BattleServerClient.bool(room, "legality", true)
            )
        }
        return rooms.toList()
    }
}
