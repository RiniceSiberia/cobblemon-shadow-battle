package io.github.rinicesiberia.shadowbattle.battle

import com.google.gson.JsonObject
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ChatLineDecodingTest {
    @Test
    fun `空文本不产生聊天消息`() {
        assertNull(ChatLineDecoding.decode(JsonObject()))
        assertNull(ChatLineDecoding.decode(JsonObject().apply { addProperty("text", "") }))
    }

    @Test
    fun `缺少字段保持全局频道和发送者默认值`() {
        val decoded = requireNotNull(ChatLineDecoding.decode(JsonObject().apply { addProperty("text", "hello") }))

        assertEquals("global", decoded.channel)
        assertEquals("", decoded.battleId)
        assertEquals(0L, decoded.payload.uid())
        assertEquals("", decoded.payload.id())
        assertEquals("?", decoded.payload.name())
        assertEquals(UUID(0L, 0L), decoded.payload.sender())
        assertEquals("hello", decoded.payload.text())
    }

    @Test
    fun `战斗消息保留完整字段且无效UUID回退零值`() {
        val document = JsonObject().apply {
            addProperty("channel", "battle")
            addProperty("battleId", "b1")
            addProperty("uid", 8L)
            addProperty("id", "account")
            addProperty("name", "Player")
            addProperty("player", "invalid")
            addProperty("text", "move")
        }
        val decoded = requireNotNull(ChatLineDecoding.decode(document))

        assertEquals("battle", decoded.channel)
        assertEquals("b1", decoded.battleId)
        assertEquals(8L, decoded.payload.uid())
        assertEquals("account", decoded.payload.id())
        assertEquals(UUID(0L, 0L), decoded.payload.sender())
    }
}
