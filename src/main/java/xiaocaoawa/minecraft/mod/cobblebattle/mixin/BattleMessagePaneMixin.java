package xiaocaoawa.minecraft.mod.cobblebattle.mixin;

import com.cobblemon.mod.common.client.gui.battle.widgets.BattleMessagePane;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xiaocaoawa.minecraft.mod.cobblebattle.client.ChatPanel;
import xiaocaoawa.minecraft.mod.cobblebattle.client.ChatState;

@Mixin({BattleMessagePane.class})
public class BattleMessagePaneMixin {
   @Inject(
      method = {"getAppropriateY"},
      at = {@At("RETURN")},
      cancellable = true,
      remap = false
   )
   private void cobblebattle$makeRoomForChat(CallbackInfoReturnable<Integer> info) {
      if (ChatState.signedIn() && ChatState.inBattle()) {
         info.setReturnValue((Integer)info.getReturnValue() - ChatPanel.battleLogShift());
      }
   }
}
