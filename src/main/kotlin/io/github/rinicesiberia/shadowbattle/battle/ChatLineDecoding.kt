package io.github.rinicesiberia.shadowbattle.battle

import com.google.gson.JsonObject
import java.util.UUID
import xiaocaoawa.minecraft.mod.cobblebattle.net.BattleServerClient
import xiaocaoawa.minecraft.mod.cobblebattle.network.ChatLinePayload

/** 解码远端聊天消息；空文本不产生可分发消息。 */
object ChatLineDecoding {
    data class Result(val channel: String, val battleId: String, val payload: ChatLinePayload)

    @JvmStatic
    fun decode(document: JsonObject): Result? {
        val channel = BattleServerClient.str(document, "channel", "global")
        val text = BattleServerClient.str(document, "text", "")
        if (text.isEmpty()) return null
        val sender = BattleIdentifierParsing.uuidOrNull(BattleServerClient.str(document, "player", "")) ?: UUID(0L, 0L)
        return Result(
            channel,
            BattleServerClient.str(document, "battleId", ""),
            ChatLinePayload(
                channel,
                if (document.has("uid")) document.get("uid").asLong else 0L,
                BattleServerClient.str(document, "id", ""),
                BattleServerClient.str(document, "name", "?"),
                sender,
                text
            )
        )
    }
}
