package xiaocaoawa.minecraft.mod.cobblebattle.client;

import io.github.rinicesiberia.shadowbattle.client.chat.ConversationState;

public final class ChatState {
    private ChatState() {}
    public static boolean enabled() { return ConversationState.isChatAvailable(); }
    public static void setEnabled(boolean availabilityEnabled) { ConversationState.updateAvailability(availabilityEnabled); }
    public static int scroll() { return ConversationState.currentScroll(); }
    public static void setScroll(int scrollRows) { ConversationState.updateScroll(scrollRows); }
    public static boolean signedIn() { return ConversationState.hasAuthenticatedAccount(); }
    public static boolean inBattle() { return ConversationState.isParticipating(); }
    public static long uid() { return ConversationState.currentAccountNumber(); }
    public static String name() { return ConversationState.currentDisplayLabel(); }
    public static ChatLog.Channel channel() { return ConversationState.currentConversation(); }
    public static void select(ChatLog.Channel conversationChannel) { ConversationState.selectConversation(conversationChannel); }
    public static void accept(boolean signedInNow, boolean inBattleNow, long accountNumberNow, String displayNameNow) {
        ConversationState.updateSession(signedInNow, inBattleNow, accountNumberNow, displayNameNow);
    }
    public static void reset() { ConversationState.clearSession(); }
}
