package io.github.rinicesiberia.shadowbattle

import io.github.rinicesiberia.shadowbattle.dex.DexLegalityRules
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DexLegalityRulesTest {
    @Test
    fun `Showdown标识移除大小写与非字母数字差异`() {
        assertEquals("mrmimegalar", DexLegalityRules.normalizedId("Mr. Mime-Galar"))
        assertEquals("", DexLegalityRules.normalizedId(null))
    }

    @Test
    fun `空能力与无能力标记直接合法`() {
        assertTrue(DexLegalityRules.abilityAllowed("", emptyList()))
        assertTrue(DexLegalityRules.abilityAllowed("No Ability", emptyList()))
        assertTrue(DexLegalityRules.abilityAllowed("Static", listOf("static", "Lightning Rod")))
        assertFalse(DexLegalityRules.abilityAllowed("Levitate", listOf("Static")))
    }

    @Test
    fun `招式清单规范化并在空清单时放行`() {
        val legal = DexLegalityRules.legalMoveIds(listOf("Thunder Bolt", "Quick-Attack", "Thunder Bolt"))
        assertEquals(setOf("thunderbolt", "quickattack"), legal)
        assertTrue(DexLegalityRules.moveAllowed("Any Move", emptySet()))
        assertTrue(DexLegalityRules.moveAllowed("", legal))
        assertFalse(DexLegalityRules.moveAllowed("Surf", legal))
    }

    @Test
    fun `数值上限小于一时停用检查`() {
        assertEquals(0, DexLegalityRules.nonNegativeLimit(-1))
        assertFalse(DexLegalityRules.exceedsLimit(1, 0))
        assertFalse(DexLegalityRules.exceedsLimit(31, 31))
        assertTrue(DexLegalityRules.exceedsLimit(32, 31))
    }
}
