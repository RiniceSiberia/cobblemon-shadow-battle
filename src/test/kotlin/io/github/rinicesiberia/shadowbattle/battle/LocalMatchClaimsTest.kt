package io.github.rinicesiberia.shadowbattle.battle

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LocalMatchClaimsTest {
    @Test
    fun `第一份队伍缺失也继续领取第二份`() {
        val teams = mutableMapOf("second" to "team")
        val calls = mutableListOf<String>()
        val result = LocalMatchClaims.take("first", "second") { calls += it; teams.remove(it) }
        assertNull(result.first)
        assertEquals("team", result.second)
        assertEquals(listOf("first", "second"), calls)
        assertTrue(teams.isEmpty())
    }

    @Test
    fun `正常领取保持双方顺序且相同ID仍领取两次`() {
        val teams = mutableMapOf("first" to "a", "second" to "b")
        assertEquals(LocalMatchClaims("a", "b"), LocalMatchClaims.take("first", "second", teams::remove))
        val repeated = mutableMapOf("same" to "team")
        assertEquals(LocalMatchClaims("team", null), LocalMatchClaims.take("same", "same", repeated::remove))
    }

    @Test
    fun `第二次领取失败不撤回第一次领取`() {
        val failure = IllegalStateException("claim")
        val calls = mutableListOf<String>()
        assertSame(failure, assertThrows(IllegalStateException::class.java) {
            LocalMatchClaims.take("first", "second") {
                calls += it
                if (it == "second") throw failure
                "team"
            }
        })
        assertEquals(listOf("first", "second"), calls)
    }
}
