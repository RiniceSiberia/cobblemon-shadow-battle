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
        assertEquals(listOf("10", "0", "150", "bad"), details.movePp)
        assertEquals("300", details.friendship)
        assertEquals("Fire", details.teraName)
    }

    @Test
    fun `空字段和缺失字段沿用无附加值`() {
        val details = PackedTeamDetailsParser.parse(arrayOf("species"))
        assertEquals(null, details.natureName)
        assertEquals(null, details.abilityName)
        assertEquals(emptyList<String>(), details.moveNames)
        assertEquals(emptyList<String?>(), details.movePp)
        assertEquals(null, details.friendship)
        assertEquals(null, details.teraName)
    }

    @Test
    fun `PP只有斜杠格式才会被应用且保留位置`() {
        val fields = fields()
        fields[8] = "a,b,c"
        fields[9] = "12,20/20,/5"
        val details = PackedTeamDetailsParser.parse(fields)
        assertEquals(listOf(null, "20", ""), details.movePp)
    }

    @Test
    fun `空招式槽位不会移动后续招式和PP`() {
        val fields = fields()
        fields[8] = ",protect,,rest,ignored"
        fields[9] = "1/5,2/5,3/5,4/5,5/5"
        val details = PackedTeamDetailsParser.parse(fields)
        assertEquals(listOf("", "protect", "", "rest"), details.moveNames)
        assertEquals("2", details.movePp[1])
        assertEquals("4", details.movePp[3])
        fields[8] = ",,,"
        assertEquals(listOf("", "", "", ""), PackedTeamDetailsParser.parse(fields).moveNames)
    }

    @Test
    fun `与原Java字段分割和setter调用决策逐项对照`() {
        val separator = java.util.regex.Pattern.compile(",")
        val namesCases = listOf("", ",,,", "a,,b,", ",b,c,d,e")
        val ppCases = listOf("", "12,20/20,/5,bad/5", "-2/9,999/9, 7 /9,2147483648/9")
        val friendshipCases = listOf("", "bad", "300", " 42 ")
        for (names in namesCases) for (pps in ppCases) for (friendship in friendshipCases) {
            val fields = fields()
            fields[8] = names
            fields[9] = pps
            fields[16] = friendship
            val details = PackedTeamDetailsParser.parse(fields)
            val oldNames = if (names.isEmpty()) emptyList() else separator.split(names, -1).take(4)
            val oldPp = if (pps.isEmpty()) emptyList() else separator.split(pps, -1).toList()
            fun originalNumber(raw: String, fallback: Int, max: Int): Int = try {
                Math.max(0, Math.min(max, Integer.parseInt(raw.trim { it.code <= 32 })))
            } catch (_: NumberFormatException) { fallback }
            val originalCalls = mutableListOf<String>()
            if (names.isNotEmpty()) originalCalls += "clear"
            oldNames.forEachIndexed { slot, name ->
                if (name.isNotEmpty()) {
                    if (slot < oldPp.size && oldPp[slot].contains('/')) {
                        originalCalls += "pp:$slot:${originalNumber(oldPp[slot].substringBefore('/'), 17, 99)}"
                    }
                    originalCalls += "move:$slot:$name"
                }
            }
            if (friendship.isNotEmpty()) originalCalls += "friendship:${originalNumber(friendship, 70, 255)}"
            val parsedCalls = mutableListOf<String>()
            if (details.moveNames.isNotEmpty()) parsedCalls += "clear"
            details.moveNames.forEachIndexed { slot, name ->
                if (name.isNotEmpty()) {
                    details.movePp.getOrNull(slot)?.let {
                        parsedCalls += "pp:$slot:${PackedTeamValueParsing.boundedInt(it, 17, 0, 99)}"
                    }
                    parsedCalls += "move:$slot:$name"
                }
            }
            details.friendship?.let {
                parsedCalls += "friendship:${PackedTeamValueParsing.boundedInt(it, 70, 0, 255)}"
            }
            assertEquals(originalCalls, parsedCalls, "names=$names; pp=$pps; friendship=$friendship")
        }
    }

    private fun fields(): Array<String> = arrayOf(
        "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
    )
}
