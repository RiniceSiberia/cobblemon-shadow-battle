package io.github.rinicesiberia.shadowbattle.battle

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** 管理排队请求、房间查询和房主引用，保证发送失败时引用可回收。 */
class QueueReferenceBook {
    data class WaitingTeam<T>(val participant: UUID, val team: T, val packed: String, val rankedId: String)

    private val waiting = ConcurrentHashMap<UUID, WaitingTeam<*>>()
    private val owners = ConcurrentHashMap<Int, UUID>()
    private val lookups = ConcurrentHashMap<Int, UUID>()

    fun contains(participant: UUID): Boolean = waiting.containsKey(participant)
    fun claim(participant: UUID): WaitingTeam<*>? = waiting.remove(participant)
    fun drop(participant: UUID) { waiting.remove(participant) }
    fun clear() { waiting.clear(); owners.clear(); lookups.clear() }
    fun putWaiting(team: WaitingTeam<*>) { waiting[team.participant] = team }
    fun owner(ref: Int): UUID? = owners.remove(ref)
    fun lookup(ref: Int): UUID? = lookups.remove(ref)
    fun bindOwner(ref: Int, participant: UUID) { owners[ref] = participant }
    fun bindLookup(ref: Int, participant: UUID) { lookups[ref] = participant }
    fun removeOwner(ref: Int) { owners.remove(ref) }
    fun removeLookup(ref: Int) { lookups.remove(ref) }
}
