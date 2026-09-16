package xiaocaoawa.minecraft.mod.cobblebattle.battle;

import com.google.gson.JsonObject;

final class SpectatorFactory {
   private final SpectatorSessions sessions;

   SpectatorFactory(CrossServerBattleService service) {
      sessions = new SpectatorSessions(service);
   }

   void begin(JsonObject document) {
      sessions.begin(document);
   }

   void end(JsonObject document) {
      sessions.end(document);
   }
}