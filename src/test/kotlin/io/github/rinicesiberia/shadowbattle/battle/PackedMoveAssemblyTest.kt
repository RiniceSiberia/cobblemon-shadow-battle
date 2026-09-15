package io.github.rinicesiberia.shadowbattle.battle

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class PackedMoveAssemblyTest {
    private data class TestMove(val name: String, var pp: Int = 12)

    private class Target(private val failure: RuntimeException? = null) : MoveAssemblyTarget<TestMove> {
        val calls = mutableListOf<String>()
        override fun clear() { calls += "clear" }
        override fun create(name: String): TestMove? {
            calls += "create:$name"
            if (name == "broken") throw failure ?: IllegalStateException(name)
            return if (name == "unknown") null else TestMove(name)
        }
        override fun currentPp(move: TestMove): Int {
            calls += "read:${move.name}"
            return move.pp
        }
        override fun updatePp(move: TestMove, value: Int) {
            calls += "pp:${move.name}:$value"
            move.pp = value
        }
        override fun place(index: Int, move: TestMove) { calls += "place:$index:${move.name}:${move.pp}" }
    }

    private fun details(names: List<String>, pp: List<String?> = emptyList()) =
        PackedTeamDetails(null, null, names, pp, null, null)

    @Test
    fun `缺少招式字段不清空但全空槽位仍清空`() {
        val untouched = Target()
        PackedMoveAssembly.assemble(details(emptyList()), untouched)
        assertEquals(emptyList<String>(), untouched.calls)
        val cleared = Target()
        PackedMoveAssembly.assemble(details(listOf("", "")), cleared)
        assertEquals(listOf("clear"), cleared.calls)
    }

    @Test
    fun `空槽和未知招式不改变后续槽位且最多读取四项`() {
        val target = Target()
        PackedMoveAssembly.assemble(details(listOf("", "unknown", "tackle", "rest", "ignored"), listOf("1", "2", "3")), target)
        assertEquals(listOf("clear", "create:unknown", "create:tackle", "read:tackle", "pp:tackle:3", "place:2:tackle:3", "create:rest", "place:3:rest:12"), target.calls)
    }

    @Test
    fun `非法PP仍以当前值调用setter并保留读写顺序`() {
        val target = Target()
        PackedMoveAssembly.assemble(details(listOf("a", "b", "c", "d"), listOf("bad", "-1", "150", null)), target)
        assertEquals(listOf("clear", "create:a", "read:a", "pp:a:12", "place:0:a:12", "create:b", "read:b", "pp:b:0", "place:1:b:0", "create:c", "read:c", "pp:c:99", "place:2:c:99", "create:d", "place:3:d:12"), target.calls)
    }

    @Test
    fun `中途失败继续传播且保留此前已写入招式`() {
        val failure = IllegalArgumentException("creation")
        val target = Target(failure)
        assertSame(failure, assertThrows(IllegalArgumentException::class.java) {
            PackedMoveAssembly.assemble(details(listOf("a", "broken", "c")), target)
        })
        assertEquals(listOf("clear", "create:a", "place:0:a:12", "create:broken"), target.calls)
    }
}
