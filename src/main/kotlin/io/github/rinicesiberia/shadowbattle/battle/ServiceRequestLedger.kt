package io.github.rinicesiberia.shadowbattle.battle

import java.util.UUID

/** 保存服务请求与玩家之间的临时关联，并按响应类型独立领取。 */
class ServiceRequestLedger {
    private val menuRequests = LinkedHashMap<Int, UUID>()
    private val leaderboardRequests = CappedReferenceMap(MAX_TRANSIENT_REFERENCES)
    private val chatRequests = CappedReferenceMap(MAX_TRANSIENT_REFERENCES)

    fun bindMenu(reference: Int, participant: UUID) {
        menuRequests[reference] = participant
    }

    fun bindLeaderboard(reference: Int, participant: UUID) {
        leaderboardRequests[reference] = participant
    }

    fun bindChat(reference: Int, participant: UUID) {
        chatRequests[reference] = participant
    }

    fun claimMenu(reference: Int): UUID? = menuRequests.remove(reference)
    fun claimLeaderboard(reference: Int): UUID? = leaderboardRequests.remove(reference)
    fun claimChat(reference: Int): UUID? = chatRequests.remove(reference)
    fun removeMenu(reference: Int) { menuRequests.remove(reference) }
    fun removeLeaderboard(reference: Int) { leaderboardRequests.remove(reference) }

    private class CappedReferenceMap(private val capacity: Int) : LinkedHashMap<Int, UUID>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, UUID>?): Boolean = size > capacity
    }

    private companion object {
        const val MAX_TRANSIENT_REFERENCES = 64
    }
}
