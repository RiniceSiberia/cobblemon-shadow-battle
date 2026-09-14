package xiaocaoawa.minecraft.mod.cobblebattle.mixin;

import com.cobblemon.mod.common.api.storage.player.client.ClientInstancedPlayerData;
import com.cobblemon.mod.common.api.storage.player.client.ClientPokedexManager.Companion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xiaocaoawa.minecraft.mod.cobblebattle.client.ServerDex;

@Mixin(
   value = {Companion.class},
   remap = false
)
public abstract class ClientPokedexSyncMixin {
   @Inject(
      method = {"runAction"},
      at = {@At("HEAD")}
   )
   private void cobblebattle$beforeFullSync(ClientInstancedPlayerData data, CallbackInfo ci) {
      ServerDex.onPokedexSync();
   }

   @Inject(
      method = {"runIncremental"},
      at = {@At("HEAD")}
   )
   private void cobblebattle$beforeIncrementalSync(ClientInstancedPlayerData data, CallbackInfo ci) {
      ServerDex.onPokedexSync();
   }
}
