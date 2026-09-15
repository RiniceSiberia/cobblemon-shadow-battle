package io.github.rinicesiberia.shadowbattle

import io.github.rinicesiberia.shadowbattle.command.CommandDecisions
import io.github.rinicesiberia.shadowbattle.command.ReloadFollowUp
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CommandDecisionsTest {
    @Test
    fun `缺少排行参数时使用配置默认值`() {
        val selection = CommandDecisions.rankedSelection(null, "default", listOf("default", "double"))
        assertEquals("default", selection.chosenId)
        assertTrue(selection.accepted)
        assertEquals("default, double", selection.offeredIds)
    }

    @Test
    fun `空排行清单接受任意目标`() {
        assertTrue(CommandDecisions.rankedSelection("future", "default", emptyList()).accepted)
    }

    @Test
    fun `非空排行清单拒绝未知目标`() {
        val selection = CommandDecisions.rankedSelection("unknown", "default", listOf("single", "double"))
        assertFalse(selection.accepted)
        assertEquals("single, double", selection.offeredIds)
    }

    @Test
    fun `连接字段变化优先触发重连`() {
        assertEquals(ReloadFollowUp.RECONNECT, CommandDecisions.reloadFollowUp(listOf("serverHost"), "refused"))
        assertEquals(ReloadFollowUp.RETRY, CommandDecisions.reloadFollowUp(emptyList(), "refused"))
        assertEquals(ReloadFollowUp.NONE, CommandDecisions.reloadFollowUp(emptyList(), null))
    }
}
