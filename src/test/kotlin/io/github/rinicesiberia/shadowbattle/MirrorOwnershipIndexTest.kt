package io.github.rinicesiberia.shadowbattle

import io.github.rinicesiberia.shadowbattle.battle.MirrorOwnershipIndex
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.UUID

class MirrorOwnershipIndexTest {
    private val entityId = UUID.fromString("00000000-0000-0000-0000-000000000001")

    @Test
    fun `重复登记由最新镜像接管`() {
        val index = MirrorOwnershipIndex<String>()
        index.assign(entityId, "first")
        index.assign(entityId, "second")
        assertEquals("second", index.find(entityId))
    }

    @Test
    fun `释放后未知实体没有归属`() {
        val index = MirrorOwnershipIndex<String>()
        index.assign(entityId, "owner")
        index.release(entityId)
        assertNull(index.find(entityId))
        index.release(entityId)
        assertNull(index.find(UUID.randomUUID()))
    }
}
