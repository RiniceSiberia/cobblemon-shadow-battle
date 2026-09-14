package xiaocaoawa.minecraft.mod.cobblebattle.mixin;

import com.cobblemon.mod.common.api.storage.player.client.ClientPokedexManager;
import com.cobblemon.mod.common.client.CobblemonClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xiaocaoawa.minecraft.mod.cobblebattle.client.ServerDex;

@Mixin(
   value = {CobblemonClient.class},
   remap = false
)
public abstract class CobblemonClientMixin {
   @Inject(
      method = {"getClientPokedexData"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void cobblebattle$standInKnowledge(CallbackInfoReturnable<ClientPokedexManager> cir) {
      ClientPokedexManager standIn = ServerDex.knowledge();
      if (standIn != null) {
         cir.setReturnValue(standIn);
      }
   }
}
