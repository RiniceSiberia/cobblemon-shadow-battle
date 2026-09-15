package io.github.rinicesiberia.shadowbattle.battle

import java.util.UUID

/** 解析远端消息中的对战或玩家 UUID，无效输入返回空值。 */
object BattleIdentifierParsing {
    @JvmStatic
    fun uuidOrNull(raw: String?): UUID? {
        if (raw.isNullOrBlank()) return null
        return try {
            UUID.fromString(raw)
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
