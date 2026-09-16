package io.github.rinicesiberia.shadowbattle.transport

import com.google.gson.JsonObject
import java.util.UUID
import xiaocaoawa.minecraft.mod.cobblebattle.net.BattleServerClient

/** 构造菜单、排行榜和聊天使用的跨服请求。 */
object ServiceRequests {
    @JvmStatic
    fun leaderboard(reference: Int, rankedId: String, uuid: UUID, name: String): JsonObject =
        BattleServerClient.msg("leaderboard_query").apply {
            addProperty("ref", reference)
            addProperty("ranked", rankedId)
            add("player", PlayerIdentityPayload.create(uuid, name))
        }

    @JvmStatic
    fun chat(reference: Int, selectedConversation: String?, text: String, uuid: UUID, name: String): JsonObject =
        BattleServerClient.msg("chat_send").apply {
            addProperty("ref", reference)
            addProperty("channel", if (selectedConversation == "battle") "battle" else "global")
            addProperty("text", text)
            add("player", PlayerIdentityPayload.create(uuid, name))
        }
}
