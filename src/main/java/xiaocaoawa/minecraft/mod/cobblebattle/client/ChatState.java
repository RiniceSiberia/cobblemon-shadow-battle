package xiaocaoawa.minecraft.mod.cobblebattle.client;

public final class ChatState {
   private static volatile boolean signedIn;
   private static volatile boolean inBattle;
   private static volatile boolean enabled = true;
   private static volatile long uid;
   private static volatile String name = "";
   private static volatile ChatLog.Channel channel = ChatLog.Channel.GLOBAL;
   private static final int[] scroll = new int[ChatLog.Channel.values().length];
   private static volatile ChatLog.Channel beforeBattle = ChatLog.Channel.GLOBAL;

   private ChatState() {
   }

   public static boolean enabled() {
      return enabled;
   }

   public static void setEnabled(boolean nowEnabled) {
      enabled = nowEnabled;
   }

   public static int scroll() {
      return scroll[channel.ordinal()];
   }

   public static void setScroll(int rows) {
      scroll[channel.ordinal()] = Math.max(0, rows);
   }

   private static void rewind(ChatLog.Channel room) {
      scroll[room.ordinal()] = 0;
   }

   public static boolean signedIn() {
      return signedIn;
   }

   public static boolean inBattle() {
      return inBattle;
   }

   public static long uid() {
      return uid;
   }

   public static String name() {
      return name;
   }

   public static ChatLog.Channel channel() {
      return channel;
   }

   public static void select(ChatLog.Channel wanted) {
      channel = wanted;
   }

   public static void accept(boolean nowSignedIn, boolean nowInBattle, long nowUid, String nowName) {
      if (inBattle && !nowInBattle) {
         ChatLog.clearBattle();
         rewind(ChatLog.Channel.BATTLE);
         if (channel == ChatLog.Channel.BATTLE) {
            channel = beforeBattle;
         }
      }

      if (signedIn && !nowSignedIn) {
         ChatLog.clear();
         rewind(ChatLog.Channel.GLOBAL);
         rewind(ChatLog.Channel.BATTLE);
         channel = ChatLog.Channel.GLOBAL;
      }

      if (!inBattle && nowInBattle) {
         beforeBattle = channel;
         channel = ChatLog.Channel.BATTLE;
      }

      signedIn = nowSignedIn;
      inBattle = nowInBattle;
      uid = nowUid;
      name = nowName == null ? "" : nowName;
   }

   public static void reset() {
      signedIn = false;
      inBattle = false;
      uid = 0L;
      name = "";
      channel = ChatLog.Channel.GLOBAL;
      beforeBattle = ChatLog.Channel.GLOBAL;
      rewind(ChatLog.Channel.GLOBAL);
      rewind(ChatLog.Channel.BATTLE);
      ChatLog.clear();
   }
}
