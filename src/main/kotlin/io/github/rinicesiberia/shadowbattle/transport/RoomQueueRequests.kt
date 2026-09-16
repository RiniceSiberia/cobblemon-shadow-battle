package io.github.rinicesiberia.shadowbattle.transport

import com.google.gson.JsonObject
import xiaocaoawa.minecraft.mod.cobblebattle.net.BattleServerClient

/** 构造房间创建与加入请求的固定协议字段。 */
object RoomQueueRequests {
    @JvmStatic
    fun create(
        name: String,
        password: String,
        battleType: String,
        level: Int,
        pick: Int,
        fullHeal: Boolean,
        hostEngine: Boolean,
        legality: Boolean
    ): JsonObject = BattleServerClient.msg("room_create").apply {
        addProperty("name", name)
        addProperty("password", password)
        addProperty("battleType", battleType)
        addProperty("level", level)
        addProperty("pick", pick)
        addProperty("fullHeal", fullHeal)
        addProperty("engine", if (hostEngine) "host" else "server")
        addProperty("legality", !hostEngine || legality)
    }

    @JvmStatic
    fun join(roomId: String, password: String, inviteCode: String): JsonObject =
        BattleServerClient.msg("room_join").apply {
            addProperty("roomId", roomId)
            addProperty("password", password)
            if (inviteCode.isNotEmpty()) addProperty("inviteCode", inviteCode)
        }
}
