package io.github.rinicesiberia.shadowbattle.battle

import net.minecraft.world.phys.Vec3
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MirrorStageLayoutTest {
    @Test
    fun `单人位置保持四格距离并朝向观察者`() {
        val origin = Vec3(12.0, 63.0, -8.0)
        for (yaw in listOf(-450f, -90f, 0f, 45f, 180f, 359f)) {
            val result = MirrorStageLayout.opponent(origin, yaw)
            val expected = origin.add(Vec3.directionFromRotation(0f, yaw).normalize().scale(4.0))
            assertEquals(expected, result.location)
            assertEquals(4.0, origin.distanceTo(result.location), 1e-10)
            assertEquals(yaw + 180f, result.yaw)
        }
    }

    @Test
    fun `观战双方维持原位置顺序间距与朝向`() {
        val origin = Vec3(-4.0, 80.0, 9.0)
        for (yaw in listOf(-180f, 0f, 37f, 90f, 360f)) {
            val pair = MirrorStageLayout.spectatorPair(origin, yaw)
            val centre = origin.add(Vec3.directionFromRotation(0f, yaw).normalize().scale(7.0))
            val offset = Vec3.directionFromRotation(0f, yaw + 90f).normalize().scale(2.0)
            assertEquals(centre.subtract(offset), pair[0].location)
            assertEquals(centre.add(offset), pair[1].location)
            assertEquals(4.0, pair[0].location.distanceTo(pair[1].location), 1e-10)
            assertEquals(listOf(yaw + 90f, yaw - 90f), pair.map { it.yaw })
        }
    }
}
