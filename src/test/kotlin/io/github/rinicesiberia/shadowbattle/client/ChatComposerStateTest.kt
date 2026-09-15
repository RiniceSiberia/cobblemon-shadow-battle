package io.github.rinicesiberia.shadowbattle.client

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ChatComposerStateTest {
    @Test
    fun `暂停输入隐藏但保留草稿`() {
        val composer = ChatComposerState(200)
        assertNull(composer.visibleDraft())
        composer.begin()
        assertTrue(composer.append('a'))
        composer.pause()
        assertFalse(composer.isComposing())
        assertNull(composer.visibleDraft())
        composer.begin()
        assertEquals("a", composer.visibleDraft())
    }

    @Test
    fun `控制字符和格式控制符不能写入`() {
        val composer = ChatComposerState(200)
        composer.begin()
        assertFalse(composer.append('\n'))
        assertFalse(composer.append('\u007F'))
        assertFalse(composer.append('\u00A7'))
        assertTrue(composer.append(' '))
        assertEquals(" ", composer.visibleDraft())
    }

    @Test
    fun `达到长度上限后仍消费字符但不增长`() {
        val composer = ChatComposerState(2)
        composer.begin()
        assertTrue(composer.append('a'))
        assertTrue(composer.append('b'))
        assertTrue(composer.append('c'))
        assertEquals("ab", composer.visibleDraft())
    }

    @Test
    fun `退格一次删除完整Unicode码点`() {
        val composer = ChatComposerState(200)
        composer.begin()
        "😀".forEach { composer.append(it) }
        composer.eraseLastCodePoint()
        assertEquals("", composer.visibleDraft())
    }

    @Test
    fun `提交沿用Java首尾裁剪并清空状态`() {
        val composer = ChatComposerState(200)
        composer.begin()
        " \ta\u00A0 ".forEach { composer.append(it) }
        assertEquals("a\u00A0", composer.finish())
        assertFalse(composer.isComposing())
        assertNull(composer.visibleDraft())
    }
}
