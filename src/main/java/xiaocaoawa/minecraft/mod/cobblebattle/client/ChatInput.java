package xiaocaoawa.minecraft.mod.cobblebattle.client;

import io.github.rinicesiberia.shadowbattle.client.ChatComposerState;

public final class ChatInput {
   private static final ChatComposerState COMPOSER = new ChatComposerState(200);

   private ChatInput() {
   }

   public static boolean typing() {
      return COMPOSER.isComposing();
   }

   public static String draftOrNull() {
      return COMPOSER.visibleDraft();
   }

   public static void start() {
      COMPOSER.begin();
   }

   public static void stop() {
      COMPOSER.pause();
   }

   public static boolean type(char ch) {
      return COMPOSER.append(ch);
   }

   public static void backspace() {
      COMPOSER.eraseLastCodePoint();
   }

   public static void toggleChannel() {
      ChatState.select(ChatState.channel() == ChatLog.Channel.GLOBAL ? ChatLog.Channel.BATTLE : ChatLog.Channel.GLOBAL);
   }

   public static void send() {
      String text = COMPOSER.finish();
      if (!text.isEmpty()) {
         ChatScreenHandler.send(ChatState.channel(), text);
      }
   }

   public static void cancel() {
      COMPOSER.pause();
   }

   public static void reset() {
      COMPOSER.clear();
   }
}
