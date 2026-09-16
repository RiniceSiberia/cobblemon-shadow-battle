package io.github.rinicesiberia.shadowbattle.battle

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class QueueMessageRulesTest {
    @Test
    fun `room close reasons keep translation keys`() {
        assertEquals("room.closed.host_left", QueueMessageRules.roomClosedKey("host_left"))
        assertEquals("room.closed.gone", QueueMessageRules.roomClosedKey("gone"))
        assertNull(QueueMessageRules.roomClosedKey("unknown"))
        assertNull(QueueMessageRules.roomClosedKey(null))
    }

    @Test
    fun `queue leave reasons keep fallback`() {
        assertEquals("queue.left_busy", QueueMessageRules.queueLeftKey("busy"))
        assertEquals("queue.left_banned", QueueMessageRules.queueLeftKey("banned"))
        assertEquals("queue.left", QueueMessageRules.queueLeftKey("other"))
        assertEquals("queue.left", QueueMessageRules.queueLeftKey(null))
    }
}
