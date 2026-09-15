package io.github.rinicesiberia.shadowbattle.battle

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class PackedStatAssemblyTest {
    private fun effects(raw: String, individual: Boolean): List<String> {
        val calls = mutableListOf<String>()
        PackedStatAssembly.visitValues(raw, individual, { calls += "count:$it" }) { index, value -> calls += "$index:$value" }
        return calls
    }

    @Test
    fun `空字段无副作用且非六维字段只报告数量`() {
        assertEquals(emptyList<String>(), effects("", true))
        assertEquals(listOf("count:1"), effects(" ", true))
        assertEquals(listOf("count:5"), effects("1,2,3,4,5", false))
        assertEquals(listOf("count:7"), effects("1,2,3,4,5,6,", false))
    }

    @Test
    fun `尾部空值保留且按顺序写入六维`() {
        assertEquals(listOf("0:1", "1:2", "2:3", "3:4", "4:5", "5:0"), effects("1,2,3,4,5,", true))
        assertEquals((0..5).map { "$it:0" }, effects(",,,,,", false))
    }

    @Test
    fun `分别限制个体值努力值并保留非法值回退`() {
        assertEquals(listOf("0:0", "1:31", "2:31", "3:0", "4:0", "5:7"), effects("-1,40,300,bad,2147483648, 7 ", true))
        assertEquals(listOf("0:0", "1:40", "2:252", "3:0", "4:0", "5:7"), effects("-1,40,300,bad,2147483648, 7 ", false))
    }

    @Test
    fun `写入失败不继续后续维度并传播相同异常`() {
        val failure = IllegalStateException("stat write")
        val written = mutableListOf<Int>()
        assertSame(failure, assertThrows(IllegalStateException::class.java) {
            PackedStatAssembly.visitValues("1,2,3,4,5,6", true, { error("unexpected count") }) { index, _ ->
                if (index == 2) throw failure
                written += index
            }
        })
        assertEquals(listOf(0, 1), written)
    }
}
