package io.github.rinicesiberia.shadowbattle.battle

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.UUID

class SpectatorTeamLayoutTest {
    private fun frame(vararg seats: Pair<String, String>): JsonObject {
        val teams = JsonObject()
        val participants = JsonArray()
        seats.forEach { (seatId, name) ->
            teams.addProperty(seatId, "team:$seatId")
            participants.add(JsonObject().apply {
                addProperty("showdownId", seatId)
                addProperty("serverId", "server:$name")
                add("player", JsonObject().apply {
                    addProperty("name", name)
                    addProperty("uuid", UUID(0, 7).toString())
                })
            })
        }
        return JsonObject().apply { add("teams", teams); add("seats", participants) }
    }

    @Test
    fun `缺少描述或不足两席不触发队伍重建`() {
        assertEquals(SpectatorTeams.MissingDescription, SpectatorTeamLayout.decode<String>(JsonObject()) { error("decode") })
        assertEquals(SpectatorTeams.MissingDescription, SpectatorTeamLayout.decode<String>(frame("p1" to "one")) { error("decode") })
    }

    @Test
    fun `任意非p1席位归入第二席且按输入顺序重建`() {
        val calls = mutableListOf<String>()
        val result = SpectatorTeamLayout.decode(frame("custom" to "two", "p1" to "one")) {
            calls += it
            listOf(it)
        }
        assertTrue(result is SpectatorTeams.Ready)
        if (result is SpectatorTeams.Ready) {
            assertEquals("one", result.first.name)
            assertEquals("custom", result.second.showdownId)
            assertEquals(listOf("team:custom"), result.second.roster)
            assertEquals(UUID(0, 7), result.first.playerId)
        }
        assertEquals(listOf("team:custom", "team:p1"), calls)
    }

    @Test
    fun `重复席位采用最后描述而缺少另一席单独报告`() {
        val result = SpectatorTeamLayout.decode(frame("p1" to "old", "p2" to "two", "p1" to "new")) { listOf(it) }
        assertTrue(result is SpectatorTeams.Ready)
        if (result is SpectatorTeams.Ready) assertEquals("new", result.first.name)
        assertEquals(SpectatorTeams.MissingSeats, SpectatorTeamLayout.decode(frame("p2" to "a", "p3" to "b")) { listOf(it) })
    }

    @Test
    fun `空队伍在UUID校验和后续席位之前中止`() {
        val document = frame("p1" to "one", "p2" to "two")
        document.getAsJsonArray("seats")[0].asJsonObject.getAsJsonObject("player").addProperty("uuid", "invalid")
        val calls = mutableListOf<String>()
        assertEquals(SpectatorTeams.EmptyRoster("p1"), SpectatorTeamLayout.decode<String>(document) { calls += it; emptyList() })
        assertEquals(listOf("team:p1"), calls)
        assertThrows(IllegalArgumentException::class.java) { SpectatorTeamLayout.decode(document) { listOf(it) } }
    }
}
