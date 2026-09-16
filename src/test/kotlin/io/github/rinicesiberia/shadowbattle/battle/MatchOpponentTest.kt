package io.github.rinicesiberia.shadowbattle.battle

import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

class MatchOpponentTest {
    private fun seat(id: String, uuid: String = UUID(0, 9).toString()) = JsonObject().apply {
        addProperty("showdownId", id)
        addProperty("serverId", "remote")
        add("player", JsonObject().apply { addProperty("uuid", uuid); addProperty("name", id) })
    }
    private fun frame(vararg seats: JsonObject) = JsonObject().apply {
        add("seats", JsonArray().apply { seats.forEach(::add) })
    }

    @Test
    fun `没有其他席位时返回空且不解析本地玩家信息`() {
        assertNull(MatchOpponentParsing.find(frame(), "p1"))
        assertNull(MatchOpponentParsing.find(frame(seat("p1", "bad")), "p1"))
    }

    @Test
    fun `保留最后对手且不解析被覆盖席位的非法UUID`() {
        val last = seat("custom")
        val result = requireNotNull(MatchOpponentParsing.find(frame(seat("p2", "bad"), seat("p1"), last), "p1"))
        assertEquals("custom", result.seatId)
        assertEquals(UUID(0, 9), result.playerId)
        assertEquals("remote", result.serverId)
        assertSame(last, result.description)
    }

    @Test
    fun `主机标志保持缺省空值和布尔值语义`() {
        val document = frame(seat("p2"))
        assertFalse(requireNotNull(MatchOpponentParsing.find(document, "p1")).authoritative)
        document.add("authoritative", JsonNull.INSTANCE)
        assertFalse(requireNotNull(MatchOpponentParsing.find(document, "p1")).authoritative)
        document.addProperty("authoritative", true)
        assertTrue(requireNotNull(MatchOpponentParsing.find(document, "p1")).authoritative)
    }

    @Test
    fun `最终对手的错误继续传播给建场失败入口`() {
        assertThrows(IllegalArgumentException::class.java) { MatchOpponentParsing.find(frame(seat("p2", "bad")), "p1") }
        assertThrows(RuntimeException::class.java) { MatchOpponentParsing.find(JsonObject(), "p1") }
    }
}
