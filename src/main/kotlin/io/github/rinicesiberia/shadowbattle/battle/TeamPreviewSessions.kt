package io.github.rinicesiberia.shadowbattle.battle

import xiaocaoawa.minecraft.mod.cobblebattle.network.TeamPreviewPayload
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** 服务端等待玩家完成选择的队伍预览会话。 */
data class TeamPreviewSession(
    val battleId: String,
    val player: UUID,
    val seat: String,
    val pick: Int,
    val teamSize: Int,
    val shown: TeamPreviewPayload,
)

/** 按玩家保存队伍预览，并提供稳定的对战快照。 */
class TeamPreviewSessionDirectory {
    private val sessionsByPlayer = ConcurrentHashMap<UUID, TeamPreviewSession>()

    fun save(session: TeamPreviewSession) {
        sessionsByPlayer[session.player] = session
    }

    fun find(player: UUID): TeamPreviewSession? = sessionsByPlayer[player]

    fun forget(player: UUID) {
        sessionsByPlayer.remove(player)
    }

    fun clear() {
        sessionsByPlayer.clear()
    }

    fun forBattle(battleId: String): List<TeamPreviewSession> =
        sessionsByPlayer.values.filter { session -> session.battleId == battleId }
}

enum class PreviewPickValidation {
    ACCEPTED,
    WRONG_COUNT,
    INVALID_SLOT,
}

/** 队伍预览在没有客户端或收到客户端选择时使用的规则。 */
object TeamPreviewRules {
    @JvmStatic
    fun frontPicks(rosterSize: Int, requestedPicks: Int): List<Int> =
        (0 until minOf(requestedPicks, rosterSize)).toList()

    @JvmStatic
    fun validatePicks(picks: List<Int>, expectedPicks: Int, rosterSize: Int): PreviewPickValidation {
        if (picks.size != expectedPicks) return PreviewPickValidation.WRONG_COUNT
        val uniquePicks = LinkedHashSet<Int>()
        for (pick in picks) {
            if (pick < 0 || pick >= rosterSize || !uniquePicks.add(pick)) {
                return PreviewPickValidation.INVALID_SLOT
            }
        }
        return PreviewPickValidation.ACCEPTED
    }
}
