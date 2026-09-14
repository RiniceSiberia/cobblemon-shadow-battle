package io.github.rinicesiberia.shadowbattle

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.github.rinicesiberia.shadowbattle.battle.BattleResultProjection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import xiaocaoawa.minecraft.mod.cobblebattle.api.BattleOutcome
import xiaocaoawa.minecraft.mod.cobblebattle.api.ScoreChange
import java.util.UUID

class BattleResultProjectionTest {
    private val participant = UUID.fromString("00000000-0000-0000-0000-000000000001")

    @Test
    fun `胜负按原因和获胜座位投影`() {
        assertEquals(BattleOutcome.WIN, BattleResultProjection.outcome("win", "p1", "p1"))
        assertEquals(BattleOutcome.LOSS, BattleResultProjection.outcome("win", "p1", "p2"))
        assertEquals(BattleOutcome.TIE, BattleResultProjection.outcome("tie", "", "p1"))
        assertEquals(BattleOutcome.ABORTED, BattleResultProjection.outcome("win", "", "p1"))
        assertEquals(BattleOutcome.ABORTED, BattleResultProjection.outcome("error", "p1", "p1"))
    }

    @Test
    fun `分数解析忽略非法玩家并保留远端顺序`() {
        val other = UUID.fromString("00000000-0000-0000-0000-000000000002")
        val document = JsonObject().apply {
            add("scores", JsonArray().apply {
                add("ignored")
                add(score("bad", 1, 2, true))
                add(score(participant.toString(), 10, 15, true))
                add(score(other.toString(), 20, 18, false))
            })
        }
        val parsed = BattleResultProjection.participantScores(document)
        assertEquals(listOf(participant, other), parsed.map { it.participant })
        assertEquals(listOf(5L, -2L), parsed.map { it.after - it.before })
        assertEquals(listOf(true, false), parsed.map { it.won })
    }

    @Test
    fun `指定玩家分数缺失时返回空`() {
        val document = JsonObject().apply {
            add("scores", JsonArray().apply {
                add(score(participant.toString(), 10, 15, true))
                add(JsonObject().apply {
                    addProperty("player", UUID.randomUUID().toString())
                    addProperty("after", "bad")
                })
            })
        }
        assertEquals(ScoreChange(10, 15), BattleResultProjection.scoreFor(document, participant))
        assertNull(BattleResultProjection.scoreFor(document, UUID.randomUUID()))
        assertNull(BattleResultProjection.scoreFor(JsonObject(), participant))
    }

    private fun score(player: String, before: Long, after: Long, won: Boolean) = JsonObject().apply {
        addProperty("player", player)
        addProperty("before", before)
        addProperty("after", after)
        addProperty("won", won)
    }
}
