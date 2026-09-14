package io.github.rinicesiberia.shadowbattle.battle

import java.util.UUID

/** 管理房间目录缓存、合并查询和客户端刷新摘要。 */
class RoomDirectoryState<T>(
    private val refreshReuseMillis: Long = 4_500L,
    private val openReuseMillis: Long = 1_000L
) {
    data class Snapshot<T>(val content: T, val hash: String, val storedAt: Long)
    data class Waiter(val participant: UUID, val refresh: Boolean)
    data class Completion<T>(val snapshot: Snapshot<T>, val waiters: List<Waiter>)

    private var snapshot: Snapshot<T>? = null
    private val waiting = LinkedHashMap<UUID, Boolean>()
    private val deliveredHashes = HashMap<UUID, String>()
    private var fetchInFlight = false

    fun reusable(now: Long, refresh: Boolean): Snapshot<T>? {
        val current = snapshot ?: return null
        val maximumAge = if (refresh) refreshReuseMillis else openReuseMillis
        return current.takeIf { now - it.storedAt < maximumAge }
    }

    fun current(): Snapshot<T>? = snapshot

    fun enqueue(participant: UUID, refresh: Boolean) {
        waiting.merge(participant, refresh) { previous, added -> previous && added }
    }

    fun needsFetch(): Boolean = !fetchInFlight
    fun markFetchStarted() { fetchInFlight = true }
    fun abandon(participant: UUID) { waiting.remove(participant) }

    fun cancelRequests() {
        fetchInFlight = false
        waiting.clear()
    }

    fun complete(content: T, hash: String, now: Long): Completion<T> {
        val accepted = Snapshot(content, hash, now)
        snapshot = accepted
        fetchInFlight = false
        val recipients = waiting.map { Waiter(it.key, it.value) }
        waiting.clear()
        return Completion(accepted, recipients)
    }

    fun invalidate() { snapshot = null }

    fun shouldDeliver(participant: UUID, hash: String, refresh: Boolean): Boolean =
        !refresh || deliveredHashes[participant] != hash

    fun recordDelivery(participant: UUID, hash: String) {
        deliveredHashes[participant] = hash
    }

    fun forgetDelivery(participant: UUID) {
        deliveredHashes.remove(participant)
    }

    fun clearSession() {
        snapshot = null
        waiting.clear()
        deliveredHashes.clear()
        fetchInFlight = false
    }
}
