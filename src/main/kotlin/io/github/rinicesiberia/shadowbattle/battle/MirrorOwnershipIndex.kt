package io.github.rinicesiberia.shadowbattle.battle

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** 通过实体业务 UUID 查找当前负责清理它的镜像对战。 */
class MirrorOwnershipIndex<T : Any> {
    private val owners = ConcurrentHashMap<UUID, T>()

    fun assign(entityId: UUID, owner: T) {
        owners[entityId] = owner
    }

    fun release(entityId: UUID) {
        owners.remove(entityId)
    }

    fun find(entityId: UUID): T? = owners[entityId]
}
