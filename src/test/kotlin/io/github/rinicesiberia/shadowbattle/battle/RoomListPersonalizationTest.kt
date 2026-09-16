package io.github.rinicesiberia.shadowbattle.battle

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import xiaocaoawa.minecraft.mod.cobblebattle.network.RoomListPayload

class RoomListPersonalizationTest {
    @Test
    fun `账号零不拥有房间且复用原对象`() {
        val room = room("a", 0L)
        val result = RoomListPersonalization.apply(listOf(room), "hash", 0L)

        assertEquals("hash", result.deliveryStamp)
        assertSame(room, result.rooms.single())
        assertFalse(result.rooms.single().mine())
    }

    @Test
    fun `匹配房主账号标记所有自有房间并追加摘要后缀`() {
        val ownedFirst = room("a", 7L)
        val other = room("b", 8L)
        val ownedSecond = room("c", 7L)
        val result = RoomListPersonalization.apply(listOf(ownedFirst, other, ownedSecond), "hash", 7L)

        assertEquals("hash:own", result.deliveryStamp)
        assertTrue(result.rooms[0].mine())
        assertSame(other, result.rooms[1])
        assertTrue(result.rooms[2].mine())
        assertEquals(listOf("a", "b", "c"), result.rooms.map { it.id() })
    }

    private fun room(id: String, hostUid: Long) = RoomListPayload.Room(
        id, "name", "host", hostUid, "singles", -1, 6, true, false, "lead", false, 0, false, false, false, true
    )
}
