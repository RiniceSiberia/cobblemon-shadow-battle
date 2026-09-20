package xiaocaoawa.minecraft.mod.cobblebattle.battle;

import com.google.gson.JsonObject;

final class MirrorBattleFactory {
   private final MatchedBattleAssembly assembly;

   MirrorBattleFactory(CrossServerBattleService battleService) {
      assembly = new MatchedBattleAssembly(battleService);
   }

   void build(JsonObject battleDocument) {
      assembly.build(battleDocument);
   }
}
