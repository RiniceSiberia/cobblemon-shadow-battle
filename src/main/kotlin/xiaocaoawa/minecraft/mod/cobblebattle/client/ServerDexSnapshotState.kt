package xiaocaoawa.minecraft.mod.cobblebattle.client

import com.cobblemon.mod.common.api.pokemon.stats.Stat
import com.cobblemon.mod.common.api.pokemon.stats.Stats
import xiaocaoawa.minecraft.mod.cobblebattle.network.ServerDexPayload

/** 保存客户端图鉴消息的能力值索引、摘要和最近排行。 */
internal class ServerDexSnapshotState {
    private var statsBySpecies: Map<String, Map<Stat, Int>> = emptyMap()
    private var cachedDigest: String? = ""
    private var lastRankedId = ""

    fun rememberRanked(rankedId: String?) {
        lastRankedId = rankedId ?: ""
    }

    fun rankedId(): String = lastRankedId
    fun digest(): String? = cachedDigest
    fun isEmpty(): Boolean = statsBySpecies.isEmpty()
    fun contains(speciesId: String): Boolean = statsBySpecies.containsKey(speciesId)

    fun accept(payload: ServerDexPayload): ServerDexAcceptance {
        if (payload.unchanged()) {
            if (statsBySpecies.isEmpty() || cachedDigest != payload.digest()) {
                cachedDigest = ""
                return ServerDexAcceptance.RETRY_FULL_SNAPSHOT
            }
            return ServerDexAcceptance.USE_CACHED_SNAPSHOT
        }
        val indexed = HashMap<String, Map<Stat, Int>>(payload.entries().size * 2)
        for (entry in payload.entries()) {
            indexed[entry.id()] = hashMapOf(
                Stats.HP to entry.hp(),
                Stats.ATTACK to entry.atk(),
                Stats.DEFENCE to entry.def(),
                Stats.SPECIAL_ATTACK to entry.spa(),
                Stats.SPECIAL_DEFENCE to entry.spd(),
                Stats.SPEED to entry.spe(),
            )
        }
        statsBySpecies = indexed
        cachedDigest = payload.digest()
        return ServerDexAcceptance.USE_NEW_SNAPSHOT
    }

    fun statsFor(formId: String, speciesId: String): Map<Stat, Int>? =
        statsBySpecies[formId] ?: statsBySpecies[speciesId]
}

/** 客户端收到图鉴消息后对缓存采取的动作。 */
internal enum class ServerDexAcceptance {
    RETRY_FULL_SNAPSHOT,
    USE_CACHED_SNAPSHOT,
    USE_NEW_SNAPSHOT,
}
