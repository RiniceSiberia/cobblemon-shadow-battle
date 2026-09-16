package io.github.rinicesiberia.shadowbattle.battle

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class QueueErrorRulesTest {
    @Test
    fun `room refusal codes keep keys`() {
        assertEquals("room.err.no_such", QueueErrorRules.roomRefusalKey("NO_SUCH_ROOM"))
        assertEquals("room.err.not_ready", QueueErrorRules.roomRefusalKey("ROOM_NOT_READY"))
        assertNull(QueueErrorRules.roomRefusalKey("UNKNOWN"))
    }

    @Test
    fun `lookup failure codes keep keys and fallback`() {
        assertEquals("room.err.bad_invite", QueueErrorRules.lookupFailureKey("BAD_INVITE_CODE"))
        assertEquals("queue.not_signed_in", QueueErrorRules.lookupFailureKey("NOT_LOGGED_IN"))
        assertEquals("queue.already_queued", QueueErrorRules.lookupFailureKey("ALREADY_QUEUED"))
        assertEquals("queue.already_in_battle", QueueErrorRules.lookupFailureKey("ALREADY_IN_BATTLE"))
        assertEquals("queue.dex_not_ready", QueueErrorRules.lookupFailureKey("DEX_NOT_READY"))
        assertNull(QueueErrorRules.lookupFailureKey(null))
    }
}
