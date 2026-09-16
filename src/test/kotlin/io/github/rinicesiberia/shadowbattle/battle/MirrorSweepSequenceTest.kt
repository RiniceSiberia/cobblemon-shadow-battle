package io.github.rinicesiberia.shadowbattle.battle

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.concurrent.Executor

class MirrorSweepSequenceTest {
    private class Target(val online: Boolean = true, val failure: RuntimeException? = null) : MirrorSweepTarget<String, String> {
        val calls = mutableListOf<String>()
        val mainTasks = ArrayDeque<Runnable>()
        override fun releaseRoster(body: String) { calls += "release:$body" }
        override fun mainThread(): Executor? {
            calls += "server"
            return if (online) Executor { calls += "enqueue"; mainTasks.addLast(it) } else null
        }
        override fun discardProp(prop: String) {
            calls += "prop:$prop"
            if (failure != null) throw failure
        }
        override fun discardBody(body: String) { calls += "body:$body" }
    }

    @Test
    fun `空快照无操作且缺少服务端仍先释放归属`() {
        val empty = Target()
        MirrorSweepSequence.arrange(emptyList(), emptyList(), 0, empty) { _, _ -> error("defer") }
        assertEquals(emptyList<String>(), empty.calls)
        val offline = Target(false)
        MirrorSweepSequence.arrange(listOf("a", "b"), listOf("p"), 2000, offline) { _, _ -> error("defer") }
        assertEquals(listOf("release:a", "release:b", "server"), offline.calls)
    }

    @Test
    fun `立即回收也要经过主线程且先prop再body`() {
        val target = Target()
        MirrorSweepSequence.arrange(listOf("a", "b"), listOf("p", "q"), 0, target) { _, _ -> error("defer") }
        assertEquals(listOf("release:a", "release:b", "server", "enqueue"), target.calls)
        target.mainTasks.removeFirst().run()
        assertEquals(listOf("release:a", "release:b", "server", "enqueue", "prop:p", "prop:q", "body:a", "body:b"), target.calls)
    }

    @Test
    fun `正延迟先释放归属并保持宽限时间到期后再提交主线程`() {
        val target = Target()
        val deferred = mutableListOf<Pair<Long, Runnable>>()
        MirrorSweepSequence.arrange(listOf("a"), listOf("p"), 2000, target) { delay, work -> deferred += delay to work }
        assertEquals(listOf("release:a", "server"), target.calls)
        assertEquals(2000L, deferred.single().first)
        deferred.single().second.run()
        assertEquals(1, target.mainTasks.size)
        target.mainTasks.removeFirst().run()
        assertEquals(listOf("release:a", "server", "enqueue", "prop:p", "body:a"), target.calls)
    }

    @Test
    fun `实体销毁失败保持中止后续回收的行为`() {
        val failure = IllegalStateException("discard")
        val target = Target(failure = failure)
        MirrorSweepSequence.arrange(listOf("a"), listOf("p", "q"), -1, target) { _, _ -> error("defer") }
        assertSame(failure, assertThrows(IllegalStateException::class.java) { target.mainTasks.removeFirst().run() })
        assertEquals(listOf("release:a", "server", "enqueue", "prop:p"), target.calls)
    }
}
