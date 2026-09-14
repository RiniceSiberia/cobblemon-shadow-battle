package xiaocaoawa.minecraft.mod.cobblebattle.client;

public final class CobbleBattleClient {
   private CobbleBattleClient() {
   }

   public static void init() {
      AuthScreenHandler.init();
      ChatScreenHandler.init();
      LeaderboardScreenHandler.init();
      ServerDexHandler.init();
      RoomScreenHandler.init();
      MainMenuHandler.init();
      TeamPreviewHandler.init();
   }
}
