package xiaocaoawa.minecraft.mod.cobblebattle.dex

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.util.Collections

/** 保存远端图鉴物种快照、摘要及当前可用状态。 */
internal class RemoteDexSnapshotState {
    @Volatile
    private var speciesById: Map<String, RemoteDex.Entry> = emptyMap()

    @Volatile
    private var snapshotDigest: String? = null

    @Volatile
    private var ready = false

    fun isReady(): Boolean = ready
    fun digest(): String? = snapshotDigest
    fun cachedDigest(): String? = if (speciesById.isEmpty()) null else snapshotDigest
    fun size(): Int = speciesById.size
    fun get(speciesId: String): RemoteDex.Entry? = speciesById[speciesId]
    fun entries(): Collection<RemoteDex.Entry> = speciesById.values

    fun adopt(document: JsonObject) {
        val species = document.getAsJsonArray("species")
        val parsed = HashMap<String, RemoteDex.Entry>(species.size() * 2)
        for (element in species) {
            val encoded = element.asJsonObject
            val encodedStats = encoded.getAsJsonObject("baseStats")
            val baseStats = HashMap<String, Int>(8)
            for (stat in STAT_KEYS) baseStats[stat] = encodedStats[stat].asInt
            val speciesId = encoded["id"].asString
            parsed[speciesId] = RemoteDex.Entry(speciesId, Collections.unmodifiableMap(baseStats))
        }
        speciesById = Collections.unmodifiableMap(parsed)
        snapshotDigest = document["digest"]?.asString
    }

    fun confirmUnchanged(): Boolean {
        ready = speciesById.isNotEmpty()
        return ready
    }

    fun markAccepted() {
        ready = true
    }

    fun discardCachedData() {
        speciesById = emptyMap()
        snapshotDigest = null
    }

    fun suspend() {
        ready = false
    }

    fun invalidate() {
        ready = false
        discardCachedData()
    }

    fun shortDigest(): String = snapshotDigest?.take(12) ?: "?"

    fun serializableDocument(): JsonObject? {
        val digest = snapshotDigest ?: return null
        if (speciesById.isEmpty()) return null
        val species = JsonArray()
        for (entry in speciesById.values) {
            val baseStats = JsonObject()
            for (stat in STAT_KEYS) baseStats.addProperty(stat, entry.baseStats().getOrDefault(stat, 0))
            species.add(
                JsonObject().apply {
                    addProperty("id", entry.id())
                    add("baseStats", baseStats)
                },
            )
        }
        return JsonObject().apply {
            addProperty("digest", digest)
            add("species", species)
        }
    }

    private companion object {
        val STAT_KEYS = arrayOf("hp", "atk", "def", "spa", "spd", "spe")
    }
}
