package io.github.rinicesiberia.shadowbattle.battle

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SpectatorConstructionTest {
    @Test
    fun `成功启动结束上下文且不回收`() {
        val calls = mutableListOf<String>()
        assertTrue(BattleConstructionAttempt.startWithRecovery(
            { calls += "start" }, { calls += "recover" }, { calls += "finish" },
        ))
        assertEquals(listOf("start", "finish"), calls)
    }

    @Test
    fun `运行时异常先执行原回收顺序再结束上下文`() {
        val calls = mutableListOf<String>()
        val cause = IllegalStateException("start failed")
        assertFalse(BattleConstructionAttempt.startWithRecovery(
            { calls += "start"; throw cause },
            { failure ->
                assertSame(cause, failure)
                calls += listOf("despawn-0", "despawn-1", "release-0", "release-1", "notify")
            },
            { calls += "finish" },
        ))
        assertEquals(listOf("start", "despawn-0", "despawn-1", "release-0", "release-1", "notify", "finish"), calls)
    }

    @Test
    fun `回收异常仍结束上下文并保留原异常对象`() {
        val calls = mutableListOf<String>()
        val cleanupFailure = IllegalArgumentException("cleanup failed")
        val actual = assertThrows(IllegalArgumentException::class.java) {
            BattleConstructionAttempt.startWithRecovery(
                { throw IllegalStateException("start failed") },
                { calls += "recover"; throw cleanupFailure },
                { calls += "finish" },
            )
        }
        assertSame(cleanupFailure, actual)
        assertEquals(listOf("recover", "finish"), calls)
    }

    @Test
    fun `Error沿用原行为跳过回收但结束上下文`() {
        val calls = mutableListOf<String>()
        val cause = AssertionError("fatal")
        assertSame(cause, assertThrows(AssertionError::class.java) {
            BattleConstructionAttempt.startWithRecovery(
                { throw cause }, { calls += "recover" }, { calls += "finish" },
            )
        })
        assertEquals(listOf("finish"), calls)
    }
}
