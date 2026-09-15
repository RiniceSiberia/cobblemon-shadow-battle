package xiaocaoawa.minecraft.mod.cobblebattle

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import xiaocaoawa.minecraft.mod.cobblebattle.client.ChatInput
import xiaocaoawa.minecraft.mod.cobblebattle.client.ChatLog
import xiaocaoawa.minecraft.mod.cobblebattle.client.ChatState
import java.util.UUID

class ChatContractTest {
    @BeforeEach
    @AfterEach
    fun reset() { ChatState.reset(); ChatState.setEnabled(true); ChatInput.reset() }

    @Test
    fun `聊天记录每个频道最多保留四十条且返回不可变快照`() {
        val sender = UUID.fromString("00000000-0000-0000-0000-000000000001")
        repeat(45) { sequence -> ChatLog.add(ChatLog.Channel.GLOBAL, ChatLog.Line(1, "a", "name", sender, "$sequence")) }
        val snapshot = ChatLog.lines(ChatLog.Channel.GLOBAL)
        assertEquals(40, snapshot.size)
        assertEquals("5", snapshot.first().text())
        assertThrows(UnsupportedOperationException::class.java) { (snapshot as MutableList<*>).clear() }
        ChatLog.clear()
        assertEquals(40, snapshot.size)
        assertEquals(ChatLog.Channel.GLOBAL, ChatLog.Channel.of("unknown"))
        assertEquals(ChatLog.Channel.GLOBAL, ChatLog.Channel.of(null))
    }

    @Test
    fun `进入战斗切换频道退出恢复注销清空滚动`() {
        ChatState.accept(true, false, 1, "name")
        ChatState.setScroll(10)
        ChatState.accept(true, true, 1, "name")
        assertEquals(ChatLog.Channel.BATTLE, ChatState.channel())
        ChatState.setScroll(8)
        ChatState.accept(true, false, 1, "name")
        assertEquals(ChatLog.Channel.GLOBAL, ChatState.channel())
        assertEquals(10, ChatState.scroll())
        ChatState.accept(false, false, 0, null)
        assertEquals(0, ChatState.scroll())
        assertEquals("", ChatState.name())
        ChatState.setScroll(-1)
        assertEquals(0, ChatState.scroll())
    }

    @Test
    fun `重置不改变聊天启用开关`() {
        ChatState.setEnabled(false)
        ChatState.reset()
        assertFalse(ChatState.enabled())
    }

    @Test
    fun `聊天输入兼容入口保留草稿退格和频道切换`() {
        assertFalse(ChatInput.type('a'))
        ChatInput.start()
        "a😀".forEach { assertTrue(ChatInput.type(it)) }
        ChatInput.backspace()
        assertEquals("a", ChatInput.draftOrNull())
        ChatInput.stop()
        assertNull(ChatInput.draftOrNull())
        ChatInput.start()
        assertEquals("a", ChatInput.draftOrNull())
        assertEquals(ChatLog.Channel.GLOBAL, ChatState.channel())
        ChatInput.toggleChannel()
        assertEquals(ChatLog.Channel.BATTLE, ChatState.channel())
    }
}

