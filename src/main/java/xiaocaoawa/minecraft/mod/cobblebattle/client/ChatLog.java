package xiaocaoawa.minecraft.mod.cobblebattle.client;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

public final class ChatLog {
   private static final int KEPT = 40;
   private static final Deque<ChatLog.Line> GLOBAL = new ArrayDeque<>();
   private static final Deque<ChatLog.Line> BATTLE = new ArrayDeque<>();

   private ChatLog() {
   }

   private static Deque<ChatLog.Line> of(ChatLog.Channel channel) {
      return channel == ChatLog.Channel.BATTLE ? BATTLE : GLOBAL;
   }

   public static void add(ChatLog.Channel channel, ChatLog.Line line) {
      Deque<ChatLog.Line> room = of(channel);
      room.addLast(line);

      while (room.size() > 40) {
         room.removeFirst();
      }
   }

   public static List<ChatLog.Line> lines(ChatLog.Channel channel) {
      return List.copyOf(of(channel));
   }

   public static void clearBattle() {
      BATTLE.clear();
   }

   public static void clear() {
      GLOBAL.clear();
      BATTLE.clear();
   }

   public static enum Channel {
      GLOBAL("global"),
      BATTLE("battle");

      public final String id;

      private Channel(String id) {
         this.id = id;
      }

      public static ChatLog.Channel of(String id) {
         return BATTLE.id.equals(id) ? BATTLE : GLOBAL;
      }
   }

   public record Line(long uid, String id, String name, UUID sender, String text) {
   }
}
