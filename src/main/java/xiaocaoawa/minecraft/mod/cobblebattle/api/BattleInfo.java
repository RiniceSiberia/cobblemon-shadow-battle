package xiaocaoawa.minecraft.mod.cobblebattle.api;

import java.util.UUID;

public record BattleInfo(
   String battleId, String rankedId, String rankedName, boolean casual, String seat, UUID opponentUuid, String opponentName, String opponentServerId
) {
   public boolean ranked() {
      return !this.casual;
   }
}
