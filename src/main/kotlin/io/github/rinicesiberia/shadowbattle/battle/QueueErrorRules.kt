package io.github.rinicesiberia.shadowbattle.battle

/** 关联 ref 的房间和邀请码错误到翻译键的映射。 */
object QueueErrorRules {
    @JvmStatic
    fun roomRefusalKey(code: String?): String? = when (code.orEmpty()) {
        "NO_SUCH_ROOM" -> "room.err.no_such"
        "ROOM_LOCKED" -> "room.err.locked"
        "BAD_INVITE_CODE" -> "room.err.bad_invite"
        "OWN_ROOM" -> "room.err.own"
        "NOT_ROOM_HOST" -> "room.err.not_host"
        "ROOM_NOT_READY" -> "room.err.not_ready"
        else -> null
    }

    @JvmStatic
    fun lookupFailureKey(code: String?): String? = when (code.orEmpty()) {
        "BAD_INVITE_CODE" -> "room.err.bad_invite"
        "NOT_LOGGED_IN" -> "queue.not_signed_in"
        "ALREADY_QUEUED" -> "queue.already_queued"
        "ALREADY_IN_BATTLE" -> "queue.already_in_battle"
        "DEX_NOT_READY" -> "queue.dex_not_ready"
        else -> null
    }
}
