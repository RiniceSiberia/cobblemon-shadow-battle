package io.github.rinicesiberia.shadowbattle.battle

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** 管理镜像 NPC 的活跃身份、皮肤缓存及显示名规范化。 */
class MirrorNpcState<S : Any> {
    private val liveEntities = ConcurrentHashMap.newKeySet<UUID>()
    private val skins = ConcurrentHashMap<String, S>()

    fun markLive(entityId: UUID) { liveEntities.add(entityId) }
    fun markGone(entityId: UUID) { liveEntities.remove(entityId) }
    fun isLive(entityId: UUID): Boolean = liveEntities.contains(entityId)
    fun cachedSkin(name: String): S? = skins[name]
    fun rememberSkin(name: String, skin: S) { skins[name] = skin }

    fun profileName(displayName: String?): String? {
        if (displayName == null) return null
        val marker = displayName.indexOf('#')
        val normalized = (if (marker < 0) displayName else displayName.substring(0, marker)).trim()
        return normalized.ifEmpty { null }
    }
}
