package io.github.rinicesiberia.shadowbattle.battle

import net.minecraft.world.phys.Vec3

/** 镜像 NPC 的出生位置与朝向。 */
data class MirrorStagePosition(val location: Vec3, val yaw: Float)

/** 根据观察者的水平朝向布置单人对手或双人观战场地。 */
object MirrorStageLayout {
    @JvmStatic
    fun opponent(origin: Vec3, yaw: Float): MirrorStagePosition {
        val forward = Vec3.directionFromRotation(0.0f, yaw).normalize()
        return MirrorStagePosition(origin.add(forward.scale(4.0)), yaw + 180.0f)
    }

    @JvmStatic
    fun spectatorPair(origin: Vec3, yaw: Float): List<MirrorStagePosition> {
        val forward = Vec3.directionFromRotation(0.0f, yaw).normalize()
        val right = Vec3.directionFromRotation(0.0f, yaw + 90.0f).normalize()
        val centre = origin.add(forward.scale(7.0))
        return listOf(
            MirrorStagePosition(centre.subtract(right.scale(2.0)), yaw + 90.0f),
            MirrorStagePosition(centre.add(right.scale(2.0)), yaw - 90.0f),
        )
    }
}
