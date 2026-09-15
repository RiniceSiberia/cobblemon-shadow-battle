package io.github.rinicesiberia.shadowbattle.battle

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import xiaocaoawa.minecraft.mod.cobblebattle.battle.RemoteTeamCodec
import java.util.regex.Pattern

class PackedRosterDecodingTest {
    @Test
    fun `过滤和槽位编号与Java基线逐项一致`() {
        for (packed in listOf("", "]", "a]b]", "a]]skip]b", " \t]\u2003]a", "\u00a0]skip]b", "skip]skip]a")) {
            val expectedCalls = mutableListOf<String>()
            val expectedRoster = mutableListOf<String>()
            for (content in Pattern.compile("]").split(packed)) {
                if (!content.codePoints().allMatch(Character::isWhitespace)) {
                    expectedCalls += "decode:${expectedRoster.size}:$content"
                    if (content != "skip") {
                        expectedCalls += "wrap:$content"
                        expectedRoster += content.uppercase()
                    }
                }
            }
            val actualCalls = mutableListOf<String>()
            val actualRoster = PackedRosterDecoding.assemble(packed, { content, slot ->
                actualCalls += "decode:$slot:$content"
                content.takeUnless { it == "skip" }
            }) { content ->
                actualCalls += "wrap:$content"
                content.uppercase()
            }
            assertEquals(expectedCalls, actualCalls, packed)
            assertEquals(expectedRoster, actualRoster, packed)
        }
    }

    @Test
    fun `包装失败传播且后续条目不再解析`() {
        val failure = IllegalArgumentException("wrap")
        val calls = mutableListOf<String>()
        assertSame(failure, assertThrows(IllegalArgumentException::class.java) {
            PackedRosterDecoding.assemble("a]b]c", { content, slot ->
                calls += "decode:$slot:$content"
                content
            }) { content ->
                calls += "wrap:$content"
                if (content == "b") throw failure
                content
            }
        })
        assertEquals(listOf("decode:0:a", "wrap:a", "decode:1:b", "wrap:b"), calls)
    }

    @Test
    fun `可选属性异常记录后继续且成功不记录`() {
        val failure = Exception("optional")
        val reported = mutableListOf<Exception>()
        PackedRosterDecoding.applyOptional({ throw failure }, reported::add)
        PackedRosterDecoding.applyOptional({}, reported::add)
        assertEquals(listOf(failure), reported)
    }

    @Test
    fun `可选属性不吞Error且报告失败继续传播`() {
        val error = AssertionError("fatal")
        assertSame(error, assertThrows(AssertionError::class.java) {
            PackedRosterDecoding.applyOptional({ throw error }, { throw IllegalStateException("unexpected report") })
        })
        val reportFailure = IllegalStateException("report")
        assertSame(reportFailure, assertThrows(IllegalStateException::class.java) {
            PackedRosterDecoding.applyOptional({ throw Exception("optional") }, { throw reportFailure })
        })
    }

    @Test
    fun `公开入口跳过空白与不足十七字段的条目`() {
        assertEquals(emptyList<Any>(), RemoteTeamCodec.decode(" ]invalid]a|b|c]]"))
    }
}
