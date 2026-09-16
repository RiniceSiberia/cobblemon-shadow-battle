package io.github.rinicesiberia.shadowbattle.battle

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RoomListDecodingTest {
    @Test
    fun `房间列表过滤非对象并保留默认值`() {
        val document = JsonObject().apply {
            add("rooms", JsonArray().apply {
                add("ignored")
                add(JsonObject().apply { addProperty("name", "Room") })
            })
        }

        val rooms = RoomListDecoding.decodeRooms(document)

        assertEquals(1, rooms.size)
        val room = rooms.single()
        assertEquals("", room.id())
        assertEquals("Room", room.name())
        assertEquals("?", room.host())
        assertEquals(0L, room.hostUid())
        assertEquals("singles", room.battleType())
        assertEquals(-1, room.level())
        assertEquals(6, room.pick())
        assertTrue(room.fullHeal())
        assertFalse(room.locked())
        assertFalse(room.hasGuest())
        assertFalse(room.hostEngine())
        assertTrue(room.legality())
    }

    @Test
    fun `房主引擎和房间字段按协议读取`() {
        val room = JsonObject().apply {
            addProperty("id", "r1")
            addProperty("hostUid", 12L)
            addProperty("battleType", "doubles")
            addProperty("engine", "host")
            addProperty("fighting", true)
            addProperty("watchers", 4)
            addProperty("hasGuest", true)
            addProperty("legality", false)
        }
        val rooms = RoomListDecoding.decodeRooms(JsonObject().apply { add("rooms", JsonArray().apply { add(room) }) })

        val decoded = rooms.single()
        assertEquals("r1", decoded.id())
        assertEquals(12L, decoded.hostUid())
        assertEquals("doubles", decoded.battleType())
        assertTrue(decoded.hostEngine())
        assertTrue(decoded.fighting())
        assertEquals(4, decoded.watchers())
        assertTrue(decoded.hasGuest())
        assertFalse(decoded.legality())
    }

    @Test
    fun `缺少rooms字段返回空列表`() {
        assertTrue(RoomListDecoding.decodeRooms(JsonObject()).isEmpty())
    }
}
