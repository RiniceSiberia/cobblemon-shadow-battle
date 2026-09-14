package io.github.rinicesiberia.shadowbattle

import io.github.rinicesiberia.shadowbattle.battle.RoomDirectoryState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class RoomDirectoryStateTest {
    private val participant = UUID.fromString("00000000-0000-0000-0000-000000000001")

    @Test
    fun `普通打开与强制刷新使用各自缓存时限`() {
        val state = RoomDirectoryState<String>()
        state.complete("rooms", "hash", 10_000L)

        assertEquals("rooms", state.reusable(10_999L, false)?.content)
        assertNull(state.reusable(11_000L, false))
        assertEquals("rooms", state.reusable(14_499L, true)?.content)
        assertNull(state.reusable(14_500L, true))
    }

    @Test
    fun `同一玩家的合并请求保留更弱的刷新要求`() {
        val another = UUID.fromString("00000000-0000-0000-0000-000000000002")
        val state = RoomDirectoryState<String>()
        state.enqueue(participant, true)
        state.enqueue(participant, false)
        state.enqueue(another, true)
        state.markFetchStarted()

        assertFalse(state.needsFetch())
        val completed = state.complete("rooms", "hash", 1L)
        assertEquals(listOf(false, true), completed.waiters.map { it.refresh })
        assertTrue(state.needsFetch())
    }

    @Test
    fun `只有成功发送才记录刷新摘要`() {
        val state = RoomDirectoryState<String>()
        assertTrue(state.shouldDeliver(participant, "hash", true))
        state.recordDelivery(participant, "hash")
        assertFalse(state.shouldDeliver(participant, "hash", true))
        assertTrue(state.shouldDeliver(participant, "hash", false))
        state.forgetDelivery(participant)
        assertTrue(state.shouldDeliver(participant, "hash", true))
        state.recordDelivery(participant, "hash")
        state.clearSession()
        assertTrue(state.shouldDeliver(participant, "hash", true))
    }

    @Test
    fun `失败与取消按原范围回收等待者`() {
        val another = UUID.fromString("00000000-0000-0000-0000-000000000002")
        val state = RoomDirectoryState<String>()
        state.enqueue(participant, false)
        state.enqueue(another, true)
        state.abandon(participant)
        assertEquals(listOf(another), state.complete("rooms", "hash", 1L).waiters.map { it.participant })

        state.enqueue(participant, false)
        state.markFetchStarted()
        state.cancelRequests()
        assertTrue(state.needsFetch())
        assertTrue(state.complete("next", "next", 2L).waiters.isEmpty())
    }
}
