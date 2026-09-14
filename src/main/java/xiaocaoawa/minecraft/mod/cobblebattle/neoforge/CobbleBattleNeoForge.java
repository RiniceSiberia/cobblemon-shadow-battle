package xiaocaoawa.minecraft.mod.cobblebattle.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import xiaocaoawa.minecraft.mod.cobblebattle.CobbleBattle;
import xiaocaoawa.minecraft.mod.cobblebattle.client.CobbleBattleClient;

@Mod("cobblebattle")
public final class CobbleBattleNeoForge {
   public CobbleBattleNeoForge() {
      CobbleBattle.init();
      if (FMLEnvironment.dist == Dist.CLIENT) {
         CobbleBattleClient.init();
      }
   }
}
