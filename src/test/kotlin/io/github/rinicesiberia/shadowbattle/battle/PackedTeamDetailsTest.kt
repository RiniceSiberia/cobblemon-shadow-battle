package io.github.rinicesiberia.shadowbattle.battle

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PackedTeamDetailsTest {
    @Test
    fun `附加字段按原协议索引读取并限制四个招式`() {
        val fields = fields()
        fields[7] = "intimidate"
        fields[8] = "tackle,protect,quickattack,rest,extras"
        fields[9] = "10/10,0/20,150/5,bad/5,20/20"
        fields[10] = "adamant"
        fields[16] = "300,,,,,Fire"

        val details = PackedTeamDetailsParser.parse(fields)
        assertEquals("adamant", details.natureName)
        assertEquals("intimidate", details.abilityName)
        assertEquals(listOf("tackle", "protect", "quickattack", "rest"), details.moveNames)
        assertEquals(listOf(10, 0, 99, null), details.movePp)
        assertEquals(255, details.friendship)
        assertEquals("Fire", details.teraName)
    }

    @Test
    fun `空字段和缺失字段沿用无附加值`() {
        val details = PackedTeamDetailsParser.parse(arrayOf("species"))
        assertEquals(null, details.natureName)
        assertEquals(null, details.abilityName)
        assertEquals(emptyList<String>(), details.moveNames)
        assertEquals(emptyList<Int?>(), details.movePp)
        assertEquals(null, details.friendship)
        assertEquals(null, details.teraName)
    }

    @Test
    fun `PP只有斜杠格式才会被应用且保留位置`() {
        val fields = fields()
        fields[8] = "a,b,c"
        fields[9] = "12,20/20,/5"
        val details = PackedTeamDetailsParser.parse(fields)
        assertEquals(listOf(null, 20, null), details.movePp)
    }

    private fun fields(): Array<String> = arrayOf(
        "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
    )
}
