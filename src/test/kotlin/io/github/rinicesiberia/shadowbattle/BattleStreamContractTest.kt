package io.github.rinicesiberia.shadowbattle

import io.github.rinicesiberia.shadowbattle.battle.BattleProjectionIndex
import io.github.rinicesiberia.shadowbattle.battle.SequencedOutputBuffer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import xiaocaoawa.minecraft.mod.cobblebattle.battle.MirrorBattle
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

class BattleStreamContractTest {
    @Test
    fun `乱序输出在释放后按顺序交付重复序号不再交付`() {
        val delivered = mutableListOf<String>()
        val stream = SequencedOutputBuffer("battle", false, delivered::add)
        stream.enqueue(3, "three")
        stream.enqueue(1, "one")
        stream.enqueue(2, "old-two")
        stream.enqueue(2, "two")
        assertTrue(delivered.isEmpty())
        stream.enableDelivery()
        assertEquals(listOf("one", "two", "three"), delivered)
        stream.enqueue(2, "duplicate")
        stream.enqueue(4, "four")
        assertEquals(listOf("one", "two", "three", "four"), delivered)
    }

    @Test
    fun `补上缺口时仍先检查缓存容量再处理输出`() {
        val stream = SequencedOutputBuffer("battle", false) { fail("溢出的流不应交付输出") }
        stream.enableDelivery()
        (2L..65L).forEach { stream.enqueue(it, "waiting") }
        assertFalse(stream.isTerminated())
        stream.enqueue(1, "fills-gap")
        assertTrue(stream.isTerminated())
        assertFalse(stream.hasPendingOutput())
    }

    @Test
    fun `已结束镜像在两分钟边界内吞掉延迟选择`() {
        val clock = AtomicLong(1000)
        val index = BattleProjectionIndex({ _, _ -> }, clock::get)
        val engine = UUID.fromString("00000000-0000-0000-0000-000000000010")
        index.prepareConstruction(MirrorBattle.spectator("remote", false))
        assertTrue(index.bindConstructedBattle(engine))
        index.completeConstruction()
        index.removeBattle(engine)
        clock.set(121000)
        assertTrue(index.forwardChoices(engine, arrayOf("late")))
        clock.incrementAndGet()
        assertFalse(index.forwardChoices(engine, arrayOf("expired")))
    }
}
