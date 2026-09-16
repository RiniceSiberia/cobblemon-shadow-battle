package io.github.rinicesiberia.shadowbattle.battle

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import xiaocaoawa.minecraft.mod.cobblebattle.network.RoomStatePayload

class RoomStateDecodingTest {
    @Test
    fun `缺少可选字段时保留房间状态默认值并过滤非对象观战者`() {
        val document = JsonObject().apply {
            addProperty("player", UUID(0L, 1L).toString())
            add("watchers", JsonArray().apply {
                add(JsonObject().apply { addProperty("name", "Alice") })
                add("ignored")
            })
        }

        val result = RoomStateDecoding.decode(document)

        requireNotNull(result)
        assertEquals(UUID(0L, 1L), result.participant)
        assertEquals("singles", result.payload.battleType())
        assertEquals(-1, result.payload.level())
        assertEquals(6, result.payload.pick())
        assertTrue(result.payload.fullHeal())
        assertFalse(result.payload.hostEngine())
        assertTrue(result.payload.legality())
        assertEquals(RoomStatePayload.Member.NOBODY, result.payload.host())
        assertFalse(result.payload.hasGuest())
        assertEquals(1, result.payload.watchers().size)
        assertEquals("Alice", result.payload.watchers()[0].name())
    }

    @Test
    fun `主客和引擎字段按原协议解码`() {
        val document = JsonObject().apply {
            addProperty("player", UUID(0L, 2L).toString())
            add("host", JsonObject().apply {
                addProperty("name", "Host")
                addProperty("uid", 42L)
                addProperty("lead", "Pikachu")
                addProperty("teamSize", 3)
            })
            add("guest", JsonObject().apply { addProperty("name", "Guest") })
            addProperty("engine", "host")
            addProperty("fighting", true)
            addProperty("youAre", "guest")
        }

        val payload = requireNotNull(RoomStateDecoding.decode(document)).payload

        assertEquals("Host", payload.host().name())
        assertEquals(42L, payload.host().uid())
        assertEquals(3, payload.host().teamSize())
        assertTrue(payload.hasGuest())
        assertEquals("Guest", payload.guest().name())
        assertTrue(payload.hostEngine())
        assertTrue(payload.fighting())
        assertEquals("guest", payload.youAre())
    }

    @Test
    fun `无效玩家标识不产生发送结果`() {
        val document = JsonObject().apply { addProperty("player", "invalid") }

        assertNull(RoomStateDecoding.decode(document))
    }
}
