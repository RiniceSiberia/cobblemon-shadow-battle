package io.github.rinicesiberia.shadowbattle

import io.github.rinicesiberia.shadowbattle.battle.MirrorNpcState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class MirrorNpcStateTest {
    @Test
    fun `显示名去除编号并裁剪空白`() {
        val state = MirrorNpcState<String>()
        assertEquals("Player", state.profileName("  Player#42  "))
        assertEquals("Player", state.profileName(" Player "))
        assertNull(state.profileName(" #42 "))
        assertNull(state.profileName(null))
    }

    @Test
    fun `活跃身份登记与移除保持幂等`() {
        val state = MirrorNpcState<String>()
        val entityId = UUID.fromString("00000000-0000-0000-0000-000000000001")
        assertFalse(state.isLive(entityId))
        state.markLive(entityId)
        state.markLive(entityId)
        assertTrue(state.isLive(entityId))
        state.markGone(entityId)
        state.markGone(entityId)
        assertFalse(state.isLive(entityId))
    }

    @Test
    fun `皮肤缓存由最后一次结果覆盖`() {
        val state = MirrorNpcState<String>()
        assertNull(state.cachedSkin("Player"))
        state.rememberSkin("Player", "first")
        state.rememberSkin("Player", "second")
        assertEquals("second", state.cachedSkin("Player"))
    }
}
