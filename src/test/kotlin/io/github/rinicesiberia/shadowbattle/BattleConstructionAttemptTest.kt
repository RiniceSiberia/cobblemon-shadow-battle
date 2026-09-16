package io.github.rinicesiberia.shadowbattle

import io.github.rinicesiberia.shadowbattle.battle.BattleConstructionAttempt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class BattleConstructionAttemptTest {
    @Test
    fun `结束上下文失败时覆盖启动异常而不进入后续回收`() {
        val finishFailure = IllegalArgumentException("finish")
        val actual = assertThrows(IllegalArgumentException::class.java) {
            BattleConstructionAttempt.captureFailure(
                Runnable { throw IllegalStateException("start") },
                Runnable { throw finishFailure },
            )
        }
        assertSame(finishFailure, actual)
    }

    @Test
    fun `普通匹配的失败回收只能发生在结束上下文之后`() {
        val events = mutableListOf<String>()
        val failure = BattleConstructionAttempt.captureFailure(
            Runnable { events += "start"; throw IllegalStateException("start") },
            Runnable { events += "finish" },
        )
        if (failure != null) events += "recover"
        assertEquals(listOf("start", "finish", "recover"), events)
    }

    @Test
    fun `启动成功后结束构造上下文`() {
        val events = mutableListOf<String>()

        val failure = BattleConstructionAttempt.captureFailure(
            Runnable { events += "start" },
            Runnable { events += "finish" },
        )

        assertNull(failure)
        assertEquals(listOf("start", "finish"), events)
    }

    @Test
    fun `运行时异常返回给调用方且仍结束上下文`() {
        val events = mutableListOf<String>()
        val expected = IllegalStateException("failed")

        val failure = BattleConstructionAttempt.captureFailure(
            Runnable { events += "start"; throw expected },
            Runnable { events += "finish" },
        )

        assertSame(expected, failure)
        assertEquals(listOf("start", "finish"), events)
    }

    @Test
    fun `错误继续抛出且仍结束上下文`() {
        val events = mutableListOf<String>()
        val expected = AssertionError("failed")

        val actual = assertThrows(AssertionError::class.java) {
            BattleConstructionAttempt.captureFailure(
                Runnable { events += "start"; throw expected },
                Runnable { events += "finish" },
            )
        }

        assertSame(expected, actual)
        assertEquals(listOf("start", "finish"), events)
    }
}
