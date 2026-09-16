package io.github.rinicesiberia.shadowbattle.battle

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import xiaocaoawa.minecraft.mod.cobblebattle.network.TeamPreviewPayload
import java.util.UUID

class TeamPreviewEventDecodingTest {
    @Test
    fun `打开事件区分缺失字段和无效玩家并应用协议默认值`() {
        assertEquals(
            TeamPreviewEventDecoding.OpenResult.MissingRequired,
            TeamPreviewEventDecoding.open(JsonObject(), 1_000, 2_000),
        )

        val invalid = openDocument("invalid")
        val invalidResult = assertInstanceOf(
            TeamPreviewEventDecoding.OpenResult.InvalidParticipant::class.java,
            TeamPreviewEventDecoding.open(invalid, 1_000, 2_000),
        )
        assertEquals("invalid", invalidResult.raw)

        val ready = assertInstanceOf(
            TeamPreviewEventDecoding.OpenResult.Ready::class.java,
            TeamPreviewEventDecoding.open(openDocument(PARTICIPANT.toString()), 1_000, 2_000),
        )
        assertEquals("battle", ready.battleId)
        assertEquals(PARTICIPANT, ready.participant)
        assertEquals("p1", ready.seat)
        assertEquals(1, ready.pick)
        assertEquals(1, ready.lead)
        assertEquals(62_000, ready.deadline)
        assertEquals("", ready.opponent)
        assertEquals("", ready.opponentServer)
        assertEquals("pikachu", ready.mine.single().species())
        assertEquals("eevee", ready.theirs.single().species())
    }

    @Test
    fun `远端截止时间仅接受十分钟内的未来值`() {
        assertEquals(1_001, TeamPreviewEventDecoding.deadline(deadlineDocument(1_001), 1_000, 9_000))
        assertEquals(601_000, TeamPreviewEventDecoding.deadline(deadlineDocument(601_000), 1_000, 9_000))
        assertEquals(69_000, TeamPreviewEventDecoding.deadline(deadlineDocument(1_000), 1_000, 9_000))
        assertEquals(69_000, TeamPreviewEventDecoding.deadline(deadlineDocument(601_001), 1_000, 9_000))
    }

    @Test
    fun `状态事件按自身席位和其他席位合并准备状态`() {
        val document = JsonObject().apply {
            addProperty("battleId", "battle")
            add("ready", JsonObject().apply {
                addProperty("p1", true)
                addProperty("p2", false)
                addProperty("p3", true)
                add("metadata", JsonObject())
            })
        }
        val state = requireNotNull(TeamPreviewEventDecoding.state(document))
        val readiness = TeamPreviewEventDecoding.readiness(state.ready, "p1")

        assertEquals("battle", state.battleId)
        assertEquals(true, readiness.mine)
        assertEquals(true, readiness.theirs)
        assertNull(TeamPreviewEventDecoding.state(JsonObject()))
    }

    @Test
    fun `关闭事件和重绘保留既有预览字段`() {
        val closed = TeamPreviewEventDecoding.closed(JsonObject().apply { addProperty("battleId", "battle") })
        assertEquals("closed", closed?.reason)
        assertNull(TeamPreviewEventDecoding.closed(JsonObject()))

        val original = TeamPreviewPayload(
            "battle", "me", "them", "server", 3, 1, 60_000,
            listOf(TeamPreviewPayload.Slot("pikachu", 50, "M", false, "orb")),
            emptyList(), false, true, "",
        )
        val repainted = TeamPreviewEventDecoding.repaint(original, true, false, "timeout")

        assertEquals(
            TeamPreviewPayload(
                "battle", "me", "them", "server", 3, 1, 60_000,
                original.mine(), emptyList(), true, false, "timeout",
            ),
            repainted,
        )
    }

    private fun openDocument(participant: String): JsonObject = JsonObject().apply {
        addProperty("battleId", "battle")
        addProperty("yourPlayer", participant)
        addProperty("yourSeat", "p1")
        addProperty("lead", 0)
        add("rosters", JsonObject().apply {
            add("p1", JsonArray().apply { add(slot("pikachu")) })
            add("p2", JsonArray().apply { add(slot("eevee")) })
        })
    }

    private fun slot(species: String): JsonObject = JsonObject().apply { addProperty("species", species) }

    private fun deadlineDocument(deadline: Long): JsonObject =
        JsonObject().apply { addProperty("deadline", deadline) }

    private companion object {
        val PARTICIPANT: UUID = UUID.fromString("30000000-0000-0000-0000-000000000001")
    }
}
