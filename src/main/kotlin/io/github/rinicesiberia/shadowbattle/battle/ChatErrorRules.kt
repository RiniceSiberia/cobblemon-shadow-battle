package io.github.rinicesiberia.shadowbattle.battle

/** 将聊天服务错误码映射为聊天提示翻译键。 */
object ChatErrorRules {
    @JvmStatic
    fun translationKey(code: String?): String? = when (code.orEmpty()) {
        "CHAT_TOO_FAST" -> "chat.err.too_fast"
        "NOT_IN_BATTLE" -> "chat.err.not_in_battle"
        "CHAT_DISABLED" -> "chat.err.disabled"
        "NOT_LOGGED_IN" -> "queue.not_signed_in"
        else -> null
    }
}
