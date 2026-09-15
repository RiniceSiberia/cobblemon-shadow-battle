package io.github.rinicesiberia.shadowbattle

import io.github.rinicesiberia.shadowbattle.battle.BattleConstructionAttempt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class BattleConstructionAttemptTest {
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
