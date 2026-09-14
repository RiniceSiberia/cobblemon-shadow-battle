package io.github.rinicesiberia.shadowbattle.client.chat

import xiaocaoawa.minecraft.mod.cobblebattle.client.ChatLog.Channel

/** 维护聊天视图的会话状态、所选频道和各频道滚动位置。 */
object ConversationState {
    @Volatile private var authenticated = false
    @Volatile private var participating = false
    @Volatile private var chatAvailable = true
    @Volatile private var accountNumber = 0L
    @Volatile private var displayLabel = ""
    @Volatile private var selectedConversation = Channel.GLOBAL
    @Volatile private var previousConversation = Channel.GLOBAL
    private val scrollOffsets = IntArray(Channel.values().size)

    @JvmStatic fun isChatAvailable() = chatAvailable
    @JvmStatic fun updateAvailability(available: Boolean) { chatAvailable = available }
    @JvmStatic fun currentScroll() = scrollOffsets[selectedConversation.ordinal]
    @JvmStatic fun updateScroll(offset: Int) { scrollOffsets[selectedConversation.ordinal] = maxOf(0, offset) }
    @JvmStatic fun hasAuthenticatedAccount() = authenticated
    @JvmStatic fun isParticipating() = participating
    @JvmStatic fun currentAccountNumber() = accountNumber
    @JvmStatic fun currentDisplayLabel() = displayLabel
    @JvmStatic fun currentConversation() = selectedConversation
    @JvmStatic fun selectConversation(conversation: Channel) { selectedConversation = conversation }

    @JvmStatic fun updateSession(signedIn: Boolean, inBattle: Boolean, numericIdentity: Long, nickname: String?) {
        if (participating && !inBattle) {
            ChatHistory.clearBattleConversation()
            resetScroll(Channel.BATTLE)
            if (selectedConversation == Channel.BATTLE) selectedConversation = previousConversation
        }
        if (authenticated && !signedIn) {
            ChatHistory.clearConversations()
            resetScroll(Channel.GLOBAL)
            resetScroll(Channel.BATTLE)
            selectedConversation = Channel.GLOBAL
        }
        if (!participating && inBattle) {
            previousConversation = selectedConversation
            selectedConversation = Channel.BATTLE
        }
        authenticated = signedIn
        participating = inBattle
        accountNumber = numericIdentity
        displayLabel = nickname ?: ""
    }

    @JvmStatic fun clearSession() {
        authenticated = false
        participating = false
        accountNumber = 0
        displayLabel = ""
        selectedConversation = Channel.GLOBAL
        previousConversation = Channel.GLOBAL
        resetScroll(Channel.GLOBAL)
        resetScroll(Channel.BATTLE)
        ChatHistory.clearConversations()
    }
    private fun resetScroll(conversation: Channel) { scrollOffsets[conversation.ordinal] = 0 }
}
