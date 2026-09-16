package io.github.rinicesiberia.shadowbattle.battle

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class BattleChoiceRestrictionsTest {
    @Test
    fun `五种受限机制映射到对应提示`() {
        val cases = mapOf(
            "mega" to ("Mega Clause" to "battle.banned.mega"),
            "zmove" to ("Z-Move Clause" to "battle.banned.zmove"),
            "max" to ("Dynamax Clause" to "battle.banned.dynamax"),
            "terastal" to ("Terastal Clause" to "battle.banned.terastal"),
            "ultra" to ("Ultra Burst Clause" to "battle.banned.ultra"),
        )

        for ((gimmick, expected) in cases) {
            assertEquals(expected.second, BattleChoiceRestrictions.refusalKey(gimmick, setOf(expected.first)))
        }
    }

    @Test
    fun `机制标识忽略大小写但规则条款保持精确匹配`() {
        assertEquals(
            "battle.banned.mega",
            BattleChoiceRestrictions.refusalKey("MeGa", setOf("Mega Clause")),
        )
        assertNull(BattleChoiceRestrictions.refusalKey("mega", setOf("mega clause")))
    }

    @Test
    fun `缺少机制规则或未知机制不拒绝选择`() {
        assertNull(BattleChoiceRestrictions.refusalKey(null, setOf("Mega Clause")))
        assertNull(BattleChoiceRestrictions.refusalKey("mega", null))
        assertNull(BattleChoiceRestrictions.refusalKey("mega", emptySet()))
        assertNull(BattleChoiceRestrictions.refusalKey("unknown", setOf("Unknown Clause")))
    }
}
