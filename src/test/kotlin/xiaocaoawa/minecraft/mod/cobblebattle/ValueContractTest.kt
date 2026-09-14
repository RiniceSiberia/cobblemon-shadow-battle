package xiaocaoawa.minecraft.mod.cobblebattle

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import xiaocaoawa.minecraft.mod.cobblebattle.account.AuthMode
import xiaocaoawa.minecraft.mod.cobblebattle.api.BattleOutcome
import xiaocaoawa.minecraft.mod.cobblebattle.api.ScoreChange
import xiaocaoawa.minecraft.mod.cobblebattle.net.BattleServerClient

class ValueContractTest {
    @Test
    fun `认证模式 ordinal 保持线协议兼容`() {
        assertEquals(listOf("LOGIN", "REGISTER", "BIND_EMAIL", "CODE_REQUEST"), AuthMode.values().map { it.name })
        for (index in listOf(Int.MIN_VALUE, -1, 4, Int.MAX_VALUE)) assertEquals(AuthMode.LOGIN, AuthMode.byOrdinal(index))
        AuthMode.values().forEachIndexed { index, mode -> assertEquals(mode, AuthMode.byOrdinal(index)) }
    }

    @Test
    fun `积分差值符号及溢出保持 JVM long 语义`() {
        assertEquals("+0", ScoreChange(0, 0).signed())
        assertEquals("+7", ScoreChange(3, 10).signed())
        assertEquals("-7", ScoreChange(10, 3).signed())
        assertEquals("+1", ScoreChange(Long.MAX_VALUE, Long.MIN_VALUE).signed())
        assertEquals(listOf(true, true, false, false), BattleOutcome.values().map { it.decided() })
    }

    @Test
    fun `消息字段保持缺省和 JSON 标量转换行为`() {
        val message = BattleServerClient.msg("hello")
        assertEquals("hello", message["t"].asString)
        assertEquals("fallback", BattleServerClient.str(message, "absent", "fallback"))
        message.add("nil", com.google.gson.JsonNull.INSTANCE)
        assertEquals(9, BattleServerClient.integer(message, "nil", 9))
        message.addProperty("integer", "123")
        assertEquals(123L, BattleServerClient.longer(message, "integer", 0))
        message.addProperty("enabled", "true")
        assertTrue(BattleServerClient.bool(message, "enabled", false))
    }
}
