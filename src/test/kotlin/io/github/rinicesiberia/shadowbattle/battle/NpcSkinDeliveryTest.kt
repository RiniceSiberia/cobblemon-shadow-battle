package io.github.rinicesiberia.shadowbattle.battle

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.concurrent.Executor

class NpcSkinDeliveryTest {
    private class PendingTasks : Executor {
        val tasks = ArrayDeque<Runnable>()
        override fun execute(command: Runnable) { tasks.addLast(command) }
        fun runNext() = tasks.removeFirst().run()
    }

    @Test
    fun `缓存命中同步应用且空名字不安排任务`() {
        val state = MirrorNpcState<String>()
        state.rememberSkin("player", "texture")
        val worker = PendingTasks()
        val server = PendingTasks()
        val applied = mutableListOf<String>()
        val delivery = NpcSkinDelivery(state, worker)
        for (name in listOf(null, " #123", "player#123")) {
            delivery.deliver(name, server, { error("cached") }, { true }, { true }, applied::add)
        }
        assertEquals(listOf("texture"), applied)
        assertEquals(0, worker.tasks.size)
        assertEquals(0, server.tasks.size)
    }

    @Test
    fun `查询先缓存再调度主线程且移除检查发生在执行时`() {
        val state = MirrorNpcState<String>()
        val worker = PendingTasks()
        val server = PendingTasks()
        val applied = mutableListOf<String>()
        var removed = false
        NpcSkinDelivery(state, worker).deliver("player", server, { "texture" }, { true }, { removed }, applied::add)
        assertEquals(null, state.cachedSkin("player"))
        worker.runNext()
        assertEquals("texture", state.cachedSkin("player"))
        assertEquals(emptyList<String>(), applied)
        removed = true
        server.runNext()
        assertEquals(emptyList<String>(), applied)
    }

    @Test
    fun `正常查询在主线程应用且无皮肤结果也缓存`() {
        val state = MirrorNpcState<String>()
        val worker = PendingTasks()
        val server = PendingTasks()
        val applied = mutableListOf<String>()
        val delivery = NpcSkinDelivery(state, worker)
        delivery.deliver("player", server, { "texture" }, { true }, { false }, applied::add)
        worker.runNext()
        server.runNext()
        assertEquals(listOf("texture"), applied)
        delivery.deliver("missing", server, { "none" }, { false }, { false }, applied::add)
        worker.runNext()
        assertEquals("none", state.cachedSkin("missing"))
        assertEquals(0, server.tasks.size)
    }
}
