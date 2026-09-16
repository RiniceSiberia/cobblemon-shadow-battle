package io.github.rinicesiberia.shadowbattle.battle

import com.google.gson.JsonParser
import io.github.rinicesiberia.shadowbattle.transport.BattleControlMessages
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BattleControlMessagesTest {
    @Test
    fun `确认和中止帧保持准确类型字段及键顺序`() {
        assertEquals("""{"t":"battle_ack","battleId":"battle-7"}""", BattleControlMessages.acknowledgement("battle-7").toString())
        assertEquals("""{"t":"battle_abort","battleId":"battle-7","reason":"mirror construction failed"}""", BattleControlMessages.abort("battle-7", "mirror construction failed").toString())
    }

    @Test
    fun `空值保留为JSON空值且文本不裁剪`() {
        assertEquals("""{"t":"battle_ack","battleId":null}""", BattleControlMessages.acknowledgement(null).toString())
        assertEquals("""{"t":"battle_abort","battleId":null,"reason":null}""", BattleControlMessages.abort(null, null).toString())
        val text = " 原因\n\"reason\" "
        val parsed = JsonParser.parseString(BattleControlMessages.abort("", text).toString()).asJsonObject
        assertEquals("", parsed.get("battleId").asString)
        assertEquals(text, parsed.get("reason").asString)
    }

    @Test
    fun `每次创建独立帧不会污染后续发送`() {
        val first = BattleControlMessages.acknowledgement("first")
        val second = BattleControlMessages.acknowledgement("second")
        first.addProperty("extra", true)
        assertFalse(second.has("extra"))
        assertEquals("second", second.get("battleId").asString)
    }
}
