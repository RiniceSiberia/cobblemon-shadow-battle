package xiaocaoawa.minecraft.mod.cobblebattle.battle;

import com.google.gson.JsonObject;

final class MirrorFactory {
   private final MatchedBattleAssembly assembly;

   MirrorFactory(CrossServerBattleService service) {
      assembly = new MatchedBattleAssembly(service);
   }

   void build(JsonObject document) {
      assembly.build(document);
   }
}