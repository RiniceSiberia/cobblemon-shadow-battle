package io.github.rinicesiberia.shadowbattle

import io.github.rinicesiberia.shadowbattle.battle.CleanupTiming
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CleanupTimingTest {
    @Test
    fun `零或负宽限时间立即执行`() {
        val events = mutableListOf<String>()

        for (delay in listOf(0L, -1L)) {
            CleanupTiming.schedule(delay, Runnable { events += "run:$delay" }) { _, _ -> events += "defer:$delay" }
        }

        assertEquals(listOf("run:0", "run:-1"), events)
    }

    @Test
    fun `正宽限时间只提交延迟任务`() {
        val events = mutableListOf<String>()
        var deferredWork: Runnable? = null

        CleanupTiming.schedule(2_000L, Runnable { events += "run" }) { delay, work ->
            events += "defer:$delay"
            deferredWork = work
        }

        assertEquals(listOf("defer:2000"), events)
        deferredWork?.run()
        assertEquals(listOf("defer:2000", "run"), events)
    }
}
