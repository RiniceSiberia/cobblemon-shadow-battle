package io.github.rinicesiberia.shadowbattle

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.github.rinicesiberia.shadowbattle.battle.TeamSelection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class TeamSelectionTest {
    private val roster = listOf("one", "two", "three")

    @Test
    fun `有效序号按远端给定顺序选队`() {
        assertEquals(listOf("three", "one"), TeamSelection.choose(roster, document(2, 0), "p1"))
    }

    @Test
    fun `缺少选择或空选择沿用原队伍实例`() {
        assertSame(roster, TeamSelection.choose(roster, JsonObject(), "p1"))
        assertSame(roster, TeamSelection.choose(roster, document(), "p1"))
    }

    @Test
    fun `重复或越界序号回退完整队伍`() {
        assertSame(roster, TeamSelection.choose(roster, document(1, 1), "p1"))
        assertSame(roster, TeamSelection.choose(roster, document(3), "p1"))
        assertSame(roster, TeamSelection.choose(roster, document(-1), "p1"))
    }

    @Test
    fun `非数字选择回退完整队伍`() {
        val picks = JsonArray().apply { add("bad") }
        val document = JsonObject().apply { add("picks", JsonObject().apply { add("p1", picks) }) }
        assertSame(roster, TeamSelection.choose(roster, document, "p1"))
    }

    private fun document(vararg indexes: Int): JsonObject {
        val picks = JsonArray().apply { indexes.forEach(::add) }
        return JsonObject().apply { add("picks", JsonObject().apply { add("p1", picks) }) }
    }
}
