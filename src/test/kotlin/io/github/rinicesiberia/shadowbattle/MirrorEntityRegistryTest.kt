package io.github.rinicesiberia.shadowbattle

import io.github.rinicesiberia.shadowbattle.battle.MirrorEntityRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MirrorEntityRegistryTest {
    @Test
    fun `实体快照保留登记内容且不清空容器`() {
        val registry = MirrorEntityRegistry<String, Int>()
        registry.addBody(null)
        registry.addBody("npc")
        assertEquals(listOf("npc"), registry.bodySnapshot())
        assertEquals(listOf("npc"), registry.bodySnapshot())
    }

    @Test
    fun `转移实体后再次领取为空`() {
        val registry = MirrorEntityRegistry<String, Int>()
        registry.addBody("actor")
        registry.addProp(1)
        registry.addProp(2)
        assertEquals(listOf("actor"), registry.takeBodies())
        assertEquals(listOf(1, 2), registry.takeProps())
        assertTrue(registry.takeBodies().isEmpty())
        assertTrue(registry.takeProps().isEmpty())
    }
}
