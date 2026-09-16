package io.github.rinicesiberia.shadowbattle.battle

import java.util.UUID

/** 跟踪临时 Pokémon 的归属，并决定实体加入世界时的处理方式。 */
class MirrorPropTracker<T : Any> {
    private val owners = MirrorOwnershipIndex<T>()

    fun claim(creatureId: UUID, owner: T) = owners.assign(creatureId, owner)
    fun release(creatureId: UUID) = owners.release(creatureId)

    fun onAdded(creatureId: UUID?, hasTag: () -> Boolean, attach: (T) -> Unit, discard: () -> Unit): Boolean {
        val owner = creatureId?.let(owners::find)
        if (owner != null) {
            attach(owner)
            return false
        }
        if (!hasTag()) return false
        discard()
        return true
    }
}
