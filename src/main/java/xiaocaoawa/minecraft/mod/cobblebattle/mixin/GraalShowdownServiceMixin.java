package xiaocaoawa.minecraft.mod.cobblebattle.mixin;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.battles.runner.graal.GraalShowdownService;
import java.util.UUID;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xiaocaoawa.minecraft.mod.cobblebattle.battle.CrossServerBattles;

@Mixin({GraalShowdownService.class})
public abstract class GraalShowdownServiceMixin {
   @Inject(
      method = {"startBattle"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   private void cobblebattle$startBattle(PokemonBattle battle, String[] messages, CallbackInfo ci) {
      if (CrossServerBattles.claimStart(battle.getBattleId())) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"send"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   private void cobblebattle$send(UUID battleId, String[] messages, CallbackInfo ci) {
      if (CrossServerBattles.relayChoices(battleId, messages)) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"sendFromShowdown"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   private void cobblebattle$sendFromShowdown(String battleId, String message, CallbackInfo ci) {
      UUID id;
      try {
         id = UUID.fromString(battleId);
      } catch (IllegalArgumentException var6) {
         return;
      }

      if (CrossServerBattles.captureOutput(id, message)) {
         ci.cancel();
      }
   }
}
