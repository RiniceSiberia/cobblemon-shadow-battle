package xiaocaoawa.minecraft.mod.cobblebattle.client;

public final class ChatInput {
   private static final int MAX_LENGTH = 200;
   private static boolean typing;
   private static String draft = "";

   private ChatInput() {
   }

   public static boolean typing() {
      return typing;
   }

   public static String draftOrNull() {
      return typing ? draft : null;
   }

   public static void start() {
      typing = true;
   }

   public static void stop() {
      typing = false;
   }

   private static boolean typable(char ch) {
      return ch >= ' ' && ch != 127 && ch != 167;
   }

   public static boolean type(char ch) {
      if (typing && typable(ch)) {
         if (draft.length() < 200) {
            draft = draft + ch;
         }

         return true;
      } else {
         return false;
      }
   }

   public static void backspace() {
      if (!draft.isEmpty()) {
         draft = draft.substring(0, draft.offsetByCodePoints(draft.length(), -1));
      }
   }

   public static void toggleChannel() {
      ChatState.select(ChatState.channel() == ChatLog.Channel.GLOBAL ? ChatLog.Channel.BATTLE : ChatLog.Channel.GLOBAL);
   }

   public static void send() {
      String text = draft.trim();
      draft = "";
      typing = false;
      if (!text.isEmpty()) {
         ChatScreenHandler.send(ChatState.channel(), text);
      }
   }

   public static void cancel() {
      typing = false;
   }

   public static void reset() {
      typing = false;
      draft = "";
   }
}
