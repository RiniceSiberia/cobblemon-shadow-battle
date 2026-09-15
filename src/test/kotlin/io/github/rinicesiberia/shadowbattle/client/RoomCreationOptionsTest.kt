package io.github.rinicesiberia.shadowbattle.client

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RoomCreationOptionsTest {
    @Test
    fun `默认选项保持原有创建参数`() {
        val options = RoomCreationOptions()
        assertEquals("singles", options.battleType())
        assertEquals(-1, options.level())
        assertEquals(6, options.pick())
        assertTrue(options.fullHeal())
        assertFalse(options.hostEngine())
        assertTrue(options.legality())
        assertEquals(6, options.passwordRow())
    }

    @Test
    fun `三个列表选项按原顺序双向循环`() {
        val options = RoomCreationOptions()
        options.cycleBattleType(-1)
        options.cycleLevel(-1)
        options.cyclePick(-1)
        assertEquals("triples", options.battleType())
        assertEquals(100, options.level())
        assertEquals(4, options.pick())

        options.cycleBattleType(1)
        options.cycleLevel(1)
        options.cyclePick(1)
        assertEquals("singles", options.battleType())
        assertEquals(-1, options.level())
        assertEquals(6, options.pick())
    }

    @Test
    fun `开关翻转并随主机引擎移动密码行`() {
        val options = RoomCreationOptions()
        options.toggleFullHeal()
        options.toggleHostEngine()
        options.toggleLegality()
        assertFalse(options.fullHeal())
        assertTrue(options.hostEngine())
        assertFalse(options.legality())
        assertEquals(7, options.passwordRow())
    }

    @Test
    fun `创建请求沿用Java裁剪和空名称回退`() {
        val options = RoomCreationOptions()
        options.cycleBattleType(1)
        options.cycleLevel(1)
        options.cyclePick(1)
        val request = options.createRequest(" \t\r\n", " secret \t", "default")
        assertEquals("default", request.name)
        assertEquals("secret", request.password)
        assertEquals("doubles", request.battleType)
        assertEquals(50, request.level)
        assertEquals(3, request.pick)
    }

    @Test
    fun `邀请码与座位类型保持边界规则`() {
        assertNull(RoomLobbyRules.invitationCode(" \t\r\n"))
        assertEquals("\u00a0code\u00a0", RoomLobbyRules.invitationCode("\u00a0code\u00a0"))
        assertEquals("", RoomLobbyRules.seatType(true, "doubles"))
        assertEquals("doubles", RoomLobbyRules.seatType(false, "doubles"))
    }
}
