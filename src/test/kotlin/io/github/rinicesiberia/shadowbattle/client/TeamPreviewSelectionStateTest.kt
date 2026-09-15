package io.github.rinicesiberia.shadowbattle.client

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TeamPreviewSelectionStateTest {
    @Test
    fun `选择保持插入顺序并支持再次点击移除`() {
        val selection = TeamPreviewSelectionState()
        selection.toggle(2, 3)
        selection.toggle(0, 3)
        assertEquals(listOf(2, 0), selection.snapshot())
        assertEquals(0, selection.orderOf(2))
        assertEquals(1, selection.orderOf(0))
        selection.toggle(2, 3)
        assertEquals(listOf(0), selection.snapshot())
        assertEquals(-1, selection.orderOf(2))
    }

    @Test
    fun `达到选择上限后忽略新增但仍允许移除`() {
        val selection = TeamPreviewSelectionState()
        selection.toggle(1, 2)
        selection.toggle(2, 2)
        selection.toggle(3, 2)
        assertEquals(listOf(1, 2), selection.snapshot())
        selection.toggle(1, 2)
        assertEquals(listOf(2), selection.snapshot())
    }

    @Test
    fun `关闭原因截止时刻和本方准备都会锁定`() {
        val selection = TeamPreviewSelectionState()
        assertFalse(selection.isOver("", 101, 100))
        assertTrue(selection.isOver("", 100, 100))
        assertTrue(selection.isOver("closed", Long.MAX_VALUE, 0))
        assertTrue(selection.isLocked(true, "", Long.MAX_VALUE, 0))
    }

    @Test
    fun `确认要求精确数量且倒计时向上取整`() {
        val selection = TeamPreviewSelectionState()
        selection.toggle(1, 2)
        assertFalse(selection.canConfirm(2, false, "", 2_000, 0))
        selection.toggle(2, 2)
        assertTrue(selection.canConfirm(2, false, "", 2_000, 0))
        assertFalse(selection.canConfirm(2, false, "", 2_000, 2_000))
        assertEquals(0, selection.remainingSeconds(1_000, 1_001))
        assertEquals(1, selection.remainingSeconds(1_001, 1_000))
        assertEquals(2, selection.remainingSeconds(2_001, 1_000))
    }
}
