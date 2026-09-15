package io.github.rinicesiberia.shadowbattle.client

/** 管理队伍预览中的选择顺序、锁定和确认条件。 */
class TeamPreviewSelectionState {
    private val selectedSlots = mutableListOf<Int>()

    fun count(): Int = selectedSlots.size

    fun orderOf(slot: Int): Int = selectedSlots.indexOf(slot)

    fun toggle(slot: Int, selectionLimit: Int) {
        if (selectedSlots.remove(slot)) return
        if (selectedSlots.size < selectionLimit) selectedSlots += slot
    }

    fun snapshot(): List<Int> = selectedSlots.toList()

    fun isOver(closedReason: String, deadlineMillis: Long, nowMillis: Long): Boolean =
        closedReason.isNotEmpty() || nowMillis >= deadlineMillis

    fun isLocked(mineReady: Boolean, closedReason: String, deadlineMillis: Long, nowMillis: Long): Boolean =
        mineReady || isOver(closedReason, deadlineMillis, nowMillis)

    fun canConfirm(
        requiredSelections: Int,
        mineReady: Boolean,
        closedReason: String,
        deadlineMillis: Long,
        nowMillis: Long,
    ): Boolean = !isLocked(mineReady, closedReason, deadlineMillis, nowMillis) && selectedSlots.size == requiredSelections

    fun remainingSeconds(deadlineMillis: Long, nowMillis: Long): Long {
        val remainingMillis = maxOf(0L, deadlineMillis - nowMillis)
        return (remainingMillis + MILLIS_PER_SECOND - 1L) / MILLIS_PER_SECOND
    }

    private companion object {
        const val MILLIS_PER_SECOND = 1_000L
    }
}
