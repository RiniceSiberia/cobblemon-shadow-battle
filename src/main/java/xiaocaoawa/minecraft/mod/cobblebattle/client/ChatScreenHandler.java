package xiaocaoawa.minecraft.mod.cobblebattle.client;

public final class ChatScreenHandler {
   private ChatScreenHandler() {
   }

   public static void init() {
      ClientHandlerRuntime.initializeChat();
   }

   static void send(ChatLog.Channel selectedConversation, String text) {
      ClientHandlerRuntime.sendChat(selectedConversation, text);
   }

   public static void onDisconnect() {
      ClientHandlerRuntime.clearChatSession();
   }
}
