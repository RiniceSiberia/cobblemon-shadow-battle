package io.github.rinicesiberia.shadowbattle

import io.github.rinicesiberia.shadowbattle.battle.MirrorParticipantState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import xiaocaoawa.minecraft.mod.cobblebattle.api.BattleInfo
import java.util.UUID

class MirrorParticipantStateTest {
    private val first = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val second = UUID.fromString("00000000-0000-0000-0000-000000000002")

    @Test
    fun `双本地参与者保持顺序与座位映射`() {
        val state = state(second)
        assertTrue(state.hasTwoLocalParticipants())
        assertEquals(listOf(first, second), state.localParticipants())
        assertEquals("p1", state.seatOf(first))
        assertEquals("p2", state.seatOf(second))
        assertNull(state.seatOf(null))
        assertFalse(state.includes(UUID.randomUUID()))
    }

    @Test
    fun `观战者快照和最后离开结果保持一致`() {
        val state = state(null)
        state.addObserver(first)
        state.addObserver(second)
        assertEquals(setOf(first, second), state.observerSnapshot())
        assertFalse(state.removeObserver(first))
        assertTrue(state.removeObserver(second))
    }

    @Test
    fun `每个本地参与者取得自己的业务描述`() {
        val state = state(second)
        val firstInfo = info("p1", second)
        val secondInfo = info("p2", first)
        state.describePrimary(firstInfo)
        state.describeSecondary(secondInfo)
        assertEquals(firstInfo, state.primaryDescription())
        assertEquals(firstInfo, state.descriptionFor(first))
        assertEquals(secondInfo, state.descriptionFor(second))
        assertNull(state.descriptionFor(UUID.randomUUID()))
    }

    private fun state(secondParticipant: UUID?) = MirrorParticipantState(
        "remote", "p1", "p2", first, secondParticipant, "opponent", "server", true, false
    )

    private fun info(seat: String, opponent: UUID) = BattleInfo(
        "remote", "ranked", "Ranked", false, seat, opponent, "opponent", "server"
    )
}
