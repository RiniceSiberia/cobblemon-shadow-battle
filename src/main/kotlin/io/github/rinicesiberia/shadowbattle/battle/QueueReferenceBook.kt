package io.github.rinicesiberia.shadowbattle.battle

import com.cobblemon.mod.common.battles.pokemon.BattlePokemon
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** 管理排队请求、房间查询和房主引用，保证发送失败及玩家离线时引用可回收。 */
class QueueReferenceBook {
    data class WaitingTeam(
        val participant: UUID,
        val team: List<BattlePokemon>,
        val packed: String,
        val rankedId: String
    )

    private val waiting = ConcurrentHashMap<UUID, WaitingTeam>()
    private val owners = ConcurrentHashMap<Int, UUID>()
    private val lookups = ConcurrentHashMap<Int, UUID>()

    fun contains(participant: UUID): Boolean = waiting.containsKey(participant)
    fun claim(participant: UUID): WaitingTeam? = waiting.remove(participant)
    fun drop(participant: UUID) { waiting.remove(participant) }
    fun clear() { waiting.clear(); owners.clear(); lookups.clear() }
    fun putWaiting(team: WaitingTeam) { waiting[team.participant] = team }
    fun owner(ref: Int): UUID? = owners.remove(ref)
    fun lookup(ref: Int): UUID? = lookups.remove(ref)
    fun bindOwner(ref: Int, participant: UUID) { owners[ref] = participant }
    fun bindLookup(ref: Int, participant: UUID) { lookups[ref] = participant }
    fun removeOwner(ref: Int) { owners.remove(ref) }
    fun removeLookup(ref: Int) { lookups.remove(ref) }

    /** 删除玩家的等待队伍及所有尚未收到响应的引用，并返回其是否仍在等待队列。 */
    fun forgetParticipant(participant: UUID): Boolean {
        val wasWaiting = waiting.remove(participant) != null
        owners.entries.removeIf { it.value == participant }
        lookups.entries.removeIf { it.value == participant }
        return wasWaiting
    }
}
