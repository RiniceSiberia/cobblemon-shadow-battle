package io.github.rinicesiberia.shadowbattle.transport

import com.google.gson.JsonObject
import java.util.UUID
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

    @JvmStatic
    fun lookup(reference: Int, uuid: UUID, name: String, inviteCode: String): JsonObject =
        playerRequest("room_lookup", reference, uuid, name).apply { addProperty("inviteCode", inviteCode) }

    @JvmStatic
    fun leave(reference: Int, uuid: UUID, name: String): JsonObject = playerRequest("room_leave", reference, uuid, name)

    @JvmStatic
    fun start(reference: Int, uuid: UUID, name: String): JsonObject = playerRequest("room_start", reference, uuid, name)

    private fun playerRequest(type: String, reference: Int, uuid: UUID, name: String): JsonObject =
        BattleServerClient.msg(type).apply {
            addProperty("ref", reference)
            add("player", PlayerIdentityPayload.create(uuid, name))
        }
}
