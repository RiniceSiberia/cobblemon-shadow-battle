package xiaocaoawa.minecraft.mod.cobblebattle.client;

public final class ChatScreenHandler {
   private ChatScreenHandler() {
   }

   public static void init() {
      ClientHandlerRuntime.initializeChat();
   }

   static void send(ChatLog.Channel conversationChannel, String messageText) {
      ClientHandlerRuntime.sendChat(conversationChannel, messageText);
   }

   public static void onDisconnect() {
      ClientHandlerRuntime.clearChatSession();
   }
}
