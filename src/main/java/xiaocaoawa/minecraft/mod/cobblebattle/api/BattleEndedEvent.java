package xiaocaoawa.minecraft.mod.cobblebattle.api;

import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public record BattleEndedEvent(ServerPlayer player, BattleInfo battle, BattleOutcome outcome, String reason, @Nullable ScoreChange score) {
   public boolean won() {
      return this.outcome == BattleOutcome.WIN;
   }
}
