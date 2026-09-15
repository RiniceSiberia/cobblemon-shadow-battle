package io.github.rinicesiberia.shadowbattle.client

/** 管理房间界面的开战冷却和邀请码复制提示。 */
class RoomInteractionState {
    private var startCooldown = 0
    private var copyFeedback = 0

    fun tick() {
        if (startCooldown > 0) startCooldown--
        if (copyFeedback > 0) copyFeedback--
    }

    fun startCooldownTicks(): Int = startCooldown

    fun copyFeedbackTicks(): Int = copyFeedback

    fun starting(): Boolean = startCooldown > 0

    fun copied(): Boolean = copyFeedback > 0

    fun markInvitationCopied() {
        copyFeedback = COPY_FEEDBACK_TICKS
    }

    fun canStart(role: String, hasGuest: Boolean, fighting: Boolean): Boolean =
        role == HOST_ROLE && hasGuest && !fighting && startCooldown == 0

    fun beginStart(role: String, hasGuest: Boolean, fighting: Boolean): Boolean {
        if (!canStart(role, hasGuest, fighting)) return false
        startCooldown = START_COOLDOWN_TICKS
        return true
    }

    private companion object {
        const val HOST_ROLE = "host"
        const val START_COOLDOWN_TICKS = 200
        const val COPY_FEEDBACK_TICKS = 40
    }
}
