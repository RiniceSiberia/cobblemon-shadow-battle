package io.github.rinicesiberia.shadowbattle.battle

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import xiaocaoawa.minecraft.mod.cobblebattle.network.TeamPreviewPayload
import java.util.UUID

class TeamPreviewSessionsTest {
    @Test
    fun `会话按玩家替换并按对战隔离快照`() {
        val directory = TeamPreviewSessionDirectory()
        val firstPlayer = UUID.fromString("10000000-0000-0000-0000-000000000001")
        val secondPlayer = UUID.fromString("10000000-0000-0000-0000-000000000002")
        directory.save(session("battle-a", firstPlayer, false))
        directory.save(session("battle-b", secondPlayer, false))
        directory.save(session("battle-a", firstPlayer, true))

        assertEquals(true, directory.find(firstPlayer)?.shown?.mineReady())
        assertEquals(listOf(firstPlayer), directory.forBattle("battle-a").map { it.player })
        assertEquals(listOf(secondPlayer), directory.forBattle("battle-b").map { it.player })
    }

    @Test
    fun `遗忘和清空移除等待会话`() {
        val directory = TeamPreviewSessionDirectory()
        val firstPlayer = UUID.fromString("20000000-0000-0000-0000-000000000001")
        val secondPlayer = UUID.fromString("20000000-0000-0000-0000-000000000002")
        directory.save(session("battle", firstPlayer, false))
        directory.save(session("battle", secondPlayer, false))
        directory.forget(firstPlayer)
        assertNull(directory.find(firstPlayer))
        assertEquals(1, directory.forBattle("battle").size)
        directory.clear()
        assertEquals(emptyList<TeamPreviewSession>(), directory.forBattle("battle"))
    }

    @Test
    fun `默认选择取请求数和队伍长度的较小值`() {
        assertEquals(listOf(0, 1, 2), TeamPreviewRules.frontPicks(6, 3))
        assertEquals(listOf(0, 1), TeamPreviewRules.frontPicks(2, 6))
        assertEquals(emptyList<Int>(), TeamPreviewRules.frontPicks(0, 3))
    }

    @Test
    fun `玩家选择先校验数量再校验范围和重复`() {
        assertEquals(PreviewPickValidation.WRONG_COUNT, TeamPreviewRules.validatePicks(listOf(0), 2, 6))
        assertEquals(PreviewPickValidation.INVALID_SLOT, TeamPreviewRules.validatePicks(listOf(0, 0), 2, 6))
        assertEquals(PreviewPickValidation.INVALID_SLOT, TeamPreviewRules.validatePicks(listOf(-1, 1), 2, 6))
        assertEquals(PreviewPickValidation.INVALID_SLOT, TeamPreviewRules.validatePicks(listOf(0, 6), 2, 6))
        assertEquals(PreviewPickValidation.ACCEPTED, TeamPreviewRules.validatePicks(listOf(4, 1), 2, 6))
    }

    private fun session(battleId: String, player: UUID, mineReady: Boolean): TeamPreviewSession =
        TeamPreviewSession(
            battleId = battleId,
            player = player,
            seat = "p1",
            pick = 3,
            teamSize = 6,
            shown = TeamPreviewPayload(battleId, "me", "them", "server", 3, 1, 60_000, emptyList(), emptyList(), mineReady, false, ""),
        )
}
