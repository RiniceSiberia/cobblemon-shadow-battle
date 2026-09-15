package io.github.rinicesiberia.shadowbattle.client

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RoomInteractionStateTest {
    @Test
    fun `初始状态没有冷却和复制提示`() {
        val state = RoomInteractionState()
        assertFalse(state.starting())
        assertFalse(state.copied())
        assertEquals(0, state.startCooldownTicks())
        assertEquals(0, state.copyFeedbackTicks())
    }

    @Test
    fun `房主仅能在对手就位且尚未开战时开始`() {
        val state = RoomInteractionState()
        assertTrue(state.canStart("host", true, false))
        assertFalse(state.canStart("guest", true, false))
        assertFalse(state.canStart("watcher", true, false))
        assertFalse(state.canStart("host", false, false))
        assertFalse(state.canStart("host", true, true))
    }

    @Test
    fun `开始请求设置二百tick冷却并阻止重复提交`() {
        val state = RoomInteractionState()
        assertTrue(state.beginStart("host", true, false))
        assertEquals(200, state.startCooldownTicks())
        assertFalse(state.beginStart("host", true, false))
        repeat(199) { state.tick() }
        assertTrue(state.starting())
        state.tick()
        assertFalse(state.starting())
        assertTrue(state.canStart("host", true, false))
    }

    @Test
    fun `复制提示持续四十tick且计数不会为负`() {
        val state = RoomInteractionState()
        state.markInvitationCopied()
        assertEquals(40, state.copyFeedbackTicks())
        repeat(39) { state.tick() }
        assertTrue(state.copied())
        state.tick()
        state.tick()
        assertFalse(state.copied())
        assertEquals(0, state.copyFeedbackTicks())
    }
}
