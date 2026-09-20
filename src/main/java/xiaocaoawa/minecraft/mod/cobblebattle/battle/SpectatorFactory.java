package xiaocaoawa.minecraft.mod.cobblebattle.battle;

import com.google.gson.JsonObject;

final class SpectatorSessionFactory {
   private final SpectatorSessions sessions;

   SpectatorSessionFactory(CrossServerBattleService battleService) {
      sessions = new SpectatorSessions(battleService);
   }

   void begin(JsonObject spectatorDocument) {
      sessions.begin(spectatorDocument);
   }

   void end(JsonObject spectatorDocument) {
      sessions.end(spectatorDocument);
   }
}
