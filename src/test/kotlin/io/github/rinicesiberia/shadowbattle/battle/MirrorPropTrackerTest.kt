package io.github.rinicesiberia.shadowbattle.battle

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

class MirrorPropTrackerTest {
    private val creatureId = UUID(0, 17)

    @Test
    fun `已登记实体优先挂接且不读取遗留标签`() {
        val tracker = MirrorPropTracker<String>()
        tracker.claim(creatureId, "first")
        tracker.claim(creatureId, "latest")
        val attached = mutableListOf<String>()
        assertFalse(tracker.onAdded(creatureId, { error("tag") }, attached::add, { error("discard") }))
        assertEquals(listOf("latest"), attached)
    }

    @Test
    fun `释放后带标签实体移除且重复释放无影响`() {
        val tracker = MirrorPropTracker<String>()
        tracker.claim(creatureId, "battle")
        tracker.release(creatureId)
        tracker.release(creatureId)
        val calls = mutableListOf<String>()
        assertTrue(tracker.onAdded(creatureId, { calls += "tag"; true }, { error("attach") }, { calls += "discard" }))
        assertEquals(listOf("tag", "discard"), calls)
    }

    @Test
    fun `无归属且无标签不处理而缺少Pokemon仍检查标签`() {
        val tracker = MirrorPropTracker<String>()
        assertFalse(tracker.onAdded(creatureId, { false }, { error("attach") }, { error("discard") }))
        var discarded = false
        assertTrue(tracker.onAdded(null, { true }, { error("attach") }, { discarded = true }))
        assertTrue(discarded)
    }

    @Test
    fun `销毁和挂接错误保持原异常传播`() {
        val tracker = MirrorPropTracker<String>()
        val failure = IllegalStateException("entity")
        assertSame(failure, assertThrows(IllegalStateException::class.java) {
            tracker.onAdded(creatureId, { true }, { error("attach") }, { throw failure })
        })
        tracker.claim(creatureId, "battle")
        assertSame(failure, assertThrows(IllegalStateException::class.java) {
            tracker.onAdded(creatureId, { error("tag") }, { throw failure }, { error("discard") })
        })
    }
}
