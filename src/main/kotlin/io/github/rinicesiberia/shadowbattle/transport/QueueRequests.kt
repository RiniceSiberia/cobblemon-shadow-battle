package io.github.rinicesiberia.shadowbattle.transport

import com.google.gson.JsonObject
import java.util.UUID
import xiaocaoawa.minecraft.mod.cobblebattle.net.BattleServerClient

/** 构造匹配队列加入和离开请求。 */
object QueueRequests {
    @JvmStatic
    fun join(rankedId: String): JsonObject = BattleServerClient.msg("queue_join").apply {
        addProperty("ranked", rankedId)
    }

    @JvmStatic
    fun leave(reference: Int?, uuid: UUID, name: String): JsonObject = BattleServerClient.msg("queue_leave").apply {
        if (reference != null) addProperty("ref", reference)
        add("player", PlayerIdentityPayload.create(uuid, name))
    }
}
