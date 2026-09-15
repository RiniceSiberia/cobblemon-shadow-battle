package io.github.rinicesiberia.shadowbattle.client

/** 管理客户端聊天输入的编辑状态与提交边界。 */
class ChatComposerState(private val maximumLength: Int) {
    private var composing = false
    private var draft = ""

    init {
        require(maximumLength >= 0) { "maximumLength must not be negative" }
    }

    fun isComposing(): Boolean = composing

    fun visibleDraft(): String? = draft.takeIf { composing }

    fun begin() {
        composing = true
    }

    fun pause() {
        composing = false
    }

    fun append(character: Char): Boolean {
        if (!composing || !isAllowed(character)) return false
        if (draft.length < maximumLength) draft += character
        return true
    }

    fun eraseLastCodePoint() {
        if (draft.isEmpty()) return
        val start = draft.offsetByCodePoints(draft.length, -1)
        draft = draft.substring(0, start)
    }

    fun finish(): String {
        val message = draft.trim { it.code <= SPACE_CODE_POINT }
        clear()
        return message
    }

    fun clear() {
        composing = false
        draft = ""
    }

    private fun isAllowed(character: Char): Boolean =
        character >= ' ' && character != DELETE && character != SECTION_SIGN

    private companion object {
        const val SPACE_CODE_POINT = 0x20
        const val DELETE = '\u007F'
        const val SECTION_SIGN = '\u00A7'
    }
}
