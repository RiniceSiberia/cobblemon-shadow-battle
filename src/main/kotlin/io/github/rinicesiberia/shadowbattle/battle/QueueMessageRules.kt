package io.github.rinicesiberia.shadowbattle.battle

/** 房间和队列响应原因到翻译键的映射。 */
object QueueMessageRules {
    @JvmStatic
    fun roomClosedKey(reason: String?): String? = when (reason.orEmpty()) {
        "host_left" -> "room.closed.host_left"
        "started" -> "room.closed.started"
        "banned" -> "room.closed.banned"
        "finished" -> "room.closed.finished"
        "gone" -> "room.closed.gone"
        else -> null
    }

    @JvmStatic
    fun queueLeftKey(reason: String?): String = when (reason.orEmpty()) {
        "busy" -> "queue.left_busy"
        "banned" -> "queue.left_banned"
        else -> "queue.left"
    }
}
