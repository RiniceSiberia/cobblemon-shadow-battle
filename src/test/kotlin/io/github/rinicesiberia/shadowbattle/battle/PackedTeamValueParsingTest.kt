package io.github.rinicesiberia.shadowbattle.battle

import com.cobblemon.mod.common.pokemon.Gender
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.UUID

class PackedTeamValueParsingTest {
    @Test
    fun `整数按上下界截断`() {
        assertEquals(1, PackedTeamValueParsing.boundedInt("-8", 50, 1, 100))
        assertEquals(75, PackedTeamValueParsing.boundedInt("75", 50, 1, 100))
        assertEquals(100, PackedTeamValueParsing.boundedInt("150", 50, 1, 100))
    }

    @Test
    fun `整数沿用Java裁剪且无效值回退`() {
        assertEquals(31, PackedTeamValueParsing.boundedInt(" \t31\r\n", 7, 0, 31))
        assertEquals(7, PackedTeamValueParsing.boundedInt("bad", 7, 0, 31))
        assertEquals(7, PackedTeamValueParsing.boundedInt("\u00a031\u00a0", 7, 0, 31))
    }

    @Test
    fun `UUID合法时返回原值且非法时为空`() {
        val expected = UUID.fromString("30000000-0000-0000-0000-000000000001")
        assertEquals(expected, PackedTeamValueParsing.uuidOrNull(expected.toString()))
        assertNull(PackedTeamValueParsing.uuidOrNull("not-a-uuid"))
    }

    @Test
    fun `性别标记只接受大写协议值`() {
        assertEquals(Gender.MALE, PackedTeamValueParsing.gender("M"))
        assertEquals(Gender.FEMALE, PackedTeamValueParsing.gender("F"))
        assertEquals(Gender.GENDERLESS, PackedTeamValueParsing.gender("m"))
        assertEquals(Gender.GENDERLESS, PackedTeamValueParsing.gender(""))
    }
}
