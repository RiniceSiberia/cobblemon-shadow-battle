package io.github.rinicesiberia.shadowbattle.transport

import com.google.gson.JsonObject
import xiaocaoawa.minecraft.mod.cobblebattle.net.BattleServerClient

/** 构造连接、图鉴、房间目录、对战转发和观察者协议消息。 */
object ServiceProtocolMessages {
    @JvmStatic
    fun hello(reference: Int, token: String, serverId: String, cobblemonVersion: String): JsonObject =
        BattleServerClient.msg("hello").apply {
            addProperty("ref", reference)
            addProperty("protocol", 10)
            addProperty("token", token)
            addProperty("serverId", serverId)
            addProperty("modVersion", "1.0")
            addProperty("cobblemonVersion", cobblemonVersion)
        }

    @JvmStatic
    fun dexQuery(reference: Int, cachedDigest: String?): JsonObject = BattleServerClient.msg("dex_query").apply {
        addProperty("ref", reference)
        if (cachedDigest != null) addProperty("have", cachedDigest)
    }

    @JvmStatic
    fun roomList(reference: Int, cachedHash: String?): JsonObject = BattleServerClient.msg("room_list").apply {
        addProperty("ref", reference)
        if (cachedHash != null) addProperty("hash", cachedHash)
    }

    @JvmStatic
    fun battleOutput(battleId: String, data: String): JsonObject = BattleServerClient.msg("battle_output").apply {
        addProperty("battleId", battleId)
        addProperty("data", data)
    }

    @JvmStatic
    fun choice(battleId: String, line: String): JsonObject = BattleServerClient.msg("choice").apply {
        addProperty("battleId", battleId)
        addProperty("line", line)
    }

    @JvmStatic
    fun chatWatch(watchers: Int): JsonObject = BattleServerClient.msg("chat_watch").apply {
        addProperty("watchers", watchers)
    }
}
