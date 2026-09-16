package io.github.rinicesiberia.shadowbattle.battle

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class QueueRulesTest {
    @Test
    fun `battle type aliases resolve to required slots`() {
        assertEquals(1, QueueRules.requiredSlots(null))
        assertEquals(1, QueueRules.requiredSlots("singles"))
        assertEquals(2, QueueRules.requiredSlots("DOUBLE_BATTLE"))
        assertEquals(3, QueueRules.requiredSlots("triple"))
    }
}
