package io.github.rinicesiberia.shadowbattle.client.chat

import xiaocaoawa.minecraft.mod.cobblebattle.client.ChatLog
import java.util.ArrayDeque

/** 按频道保存有限条消息，并向视图提供不可变快照。 */
object ChatHistory {
    private const val RETAINED_MESSAGES = 40
    private val globalMessages = ArrayDeque<ChatLog.Line>()
    private val battleMessages = ArrayDeque<ChatLog.Line>()

    @JvmStatic fun append(channel: ChatLog.Channel, message: ChatLog.Line) {
        val messages = messagesFor(channel)
        messages.addLast(message)
        while (messages.size > RETAINED_MESSAGES) messages.removeFirst()
    }
    @JvmStatic fun snapshot(channel: ChatLog.Channel): List<ChatLog.Line> = java.util.List.copyOf(messagesFor(channel))
    @JvmStatic fun clearBattleConversation() { battleMessages.clear() }
    @JvmStatic fun clearConversations() { globalMessages.clear(); battleMessages.clear() }
    private fun messagesFor(channel: ChatLog.Channel) = if (channel == ChatLog.Channel.BATTLE) battleMessages else globalMessages
}
