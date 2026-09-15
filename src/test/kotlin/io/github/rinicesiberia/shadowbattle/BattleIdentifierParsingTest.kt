package io.github.rinicesiberia.shadowbattle

import io.github.rinicesiberia.shadowbattle.battle.BattleIdentifierParsing
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.UUID

class BattleIdentifierParsingTest {
    @Test
    fun `UUID解析接受规范值并拒绝空白及非法值`() {
        val expected = UUID.fromString("12345678-1234-5678-9abc-def012345678")

        assertEquals(expected, BattleIdentifierParsing.uuidOrNull(expected.toString()))
        for (raw in listOf<String?>(null, "", "   ", " ${expected} ", "not-a-uuid")) {
            assertNull(BattleIdentifierParsing.uuidOrNull(raw))
        }
    }
}
