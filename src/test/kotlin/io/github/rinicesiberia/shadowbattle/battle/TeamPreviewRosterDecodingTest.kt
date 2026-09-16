package io.github.rinicesiberia.shadowbattle.battle

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TeamPreviewRosterDecodingTest {
    @Test
    fun `阵容过滤非对象并保留槽位默认值`() {
        val rosters = JsonObject().apply {
            add("p1", JsonArray().apply {
                add("ignored")
                add(JsonObject().apply { addProperty("species", "pikachu") })
            })
        }
        val slot = TeamPreviewRosterDecoding.roster(rosters, "p1").single()

        assertEquals("pikachu", slot.species())
        assertEquals(1, slot.level())
        assertEquals("", slot.gender())
        assertEquals(false, slot.shiny())
        assertEquals("", slot.item())
    }

    @Test
    fun `缺少席位返回空阵容且对手取第一个不同席位`() {
        val rosters = JsonObject().apply {
            add("p1", JsonArray())
            add("p3", JsonArray())
            add("p2", JsonArray())
        }

        assertTrue(TeamPreviewRosterDecoding.roster(rosters, "missing").isEmpty())
        assertEquals("p3", TeamPreviewRosterDecoding.opponentSeat(rosters, "p1"))
        assertNull(TeamPreviewRosterDecoding.opponentSeat(null, "p1"))
    }
}
