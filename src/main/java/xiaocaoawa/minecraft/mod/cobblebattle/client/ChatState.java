package xiaocaoawa.minecraft.mod.cobblebattle.client;

import io.github.rinicesiberia.shadowbattle.client.chat.ConversationState;

public final class ChatState {
    private ChatState() {}
    public static boolean enabled() { return ConversationState.isChatAvailable(); }
    public static void setEnabled(boolean value) { ConversationState.updateAvailability(value); }
    public static int scroll() { return ConversationState.currentScroll(); }
    public static void setScroll(int rows) { ConversationState.updateScroll(rows); }
    public static boolean signedIn() { return ConversationState.hasAuthenticatedAccount(); }
    public static boolean inBattle() { return ConversationState.isParticipating(); }
    public static long uid() { return ConversationState.currentAccountNumber(); }
    public static String name() { return ConversationState.currentDisplayLabel(); }
    public static ChatLog.Channel channel() { return ConversationState.currentConversation(); }
    public static void select(ChatLog.Channel wanted) { ConversationState.selectConversation(wanted); }
    public static void accept(boolean nowSignedIn, boolean nowInBattle, long nowUid, String nowName) {
        ConversationState.updateSession(nowSignedIn, nowInBattle, nowUid, nowName);
    }
    public static void reset() { ConversationState.clearSession(); }
}
