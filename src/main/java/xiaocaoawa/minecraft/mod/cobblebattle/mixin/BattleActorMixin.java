package xiaocaoawa.minecraft.mod.cobblebattle.mixin;

import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.battles.MoveActionResponse;
import com.cobblemon.mod.common.battles.ShowdownActionRequest;
import com.cobblemon.mod.common.battles.ShowdownActionResponse;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.net.messages.client.battle.BattleMakeChoicePacket;
import com.cobblemon.mod.common.net.messages.client.battle.BattleQueueRequestPacket;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.ChatFormatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xiaocaoawa.minecraft.mod.cobblebattle.battle.CrossServerBattles;
import xiaocaoawa.minecraft.mod.cobblebattle.lang.Msg;

@Mixin({BattleActor.class})
public class BattleActorMixin {
   private static final Logger LOGGER = LoggerFactory.getLogger("CobbleBattle");
   private static final Map<String, String> COBBLEBATTLE$RULE_FOR_GIMMICK = Map.of(
      "mega", "Mega Clause", "zmove", "Z-Move Clause", "max", "Dynamax Clause", "terastal", "Terastal Clause", "ultra", "Ultra Burst Clause"
   );
   private static final Map<String, String> COBBLEBATTLE$MESSAGE_FOR_GIMMICK = Map.of(
      "mega",
      "battle.banned.mega",
      "zmove",
      "battle.banned.zmove",
      "max",
      "battle.banned.dynamax",
      "terastal",
      "battle.banned.terastal",
      "ultra",
      "battle.banned.ultra"
   );

   @Inject(
      method = {"setActionResponses"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   private void cobblebattle$refuseBannedGimmick(List<ShowdownActionResponse> responses, CallbackInfo ci) {
      try {
         BattleActor actor = (BattleActor)(Object)this;
         if (!(actor instanceof PlayerBattleActor)) {
            return;
         }

         if (!CrossServerBattles.isMirror(actor.getBattle().getBattleId())) {
            return;
         }

         Set<String> rules = actor.getBattle().getFormat().getRuleSet();
         if (rules == null || rules.isEmpty()) {
            return;
         }

         ShowdownActionRequest request = actor.getRequest();
         if (request == null) {
            return;
         }

         for (ShowdownActionResponse response : responses) {
            if (response instanceof MoveActionResponse move) {
               String gimmick = move.getGimmickID();
               if (gimmick != null) {
                  String rule = COBBLEBATTLE$RULE_FOR_GIMMICK.get(gimmick.toLowerCase(Locale.ROOT));
                  if (rule != null && rules.contains(rule)) {
                     actor.getResponses().clear();
                     actor.setMustChoose(true);
                     actor.sendUpdate(new BattleQueueRequestPacket(request));
                     actor.sendUpdate(new BattleMakeChoicePacket());
                     String key = COBBLEBATTLE$MESSAGE_FOR_GIMMICK.getOrDefault(gimmick.toLowerCase(Locale.ROOT), "battle.banned.gimmick");
                     actor.sendMessage(Msg.of(ChatFormatting.RED, key));
                     ci.cancel();
                     return;
                  }
               }
            }
         }
      } catch (Exception var12) {
         LOGGER.error("Could not check the ranked rules for a choice", var12);
      }
   }
}
