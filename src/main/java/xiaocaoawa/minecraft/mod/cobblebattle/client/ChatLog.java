package xiaocaoawa.minecraft.mod.cobblebattle.client;

import io.github.rinicesiberia.shadowbattle.client.chat.ChatHistory;
import java.util.List;
import java.util.UUID;

public final class ChatLog {
    private ChatLog() {}
    public static void add(Channel channel, Line line) { ChatHistory.append(channel, line); }
    public static List<Line> lines(Channel channel) { return ChatHistory.snapshot(channel); }
    public static void clearBattle() { ChatHistory.clearBattleConversation(); }
    public static void clear() { ChatHistory.clearConversations(); }
    public enum Channel {
        GLOBAL("global"), BATTLE("battle");
        public final String id;
        Channel(String id) { this.id = id; }
        public static Channel of(String id) { return BATTLE.id.equals(id) ? BATTLE : GLOBAL; }
    }
    public record Line(long uid, String id, String name, UUID sender, String text) {}
}
