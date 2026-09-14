package xiaocaoawa.minecraft.mod.cobblebattle.api;

public enum BattleOutcome {
   WIN,
   LOSS,
   TIE,
   ABORTED;

   public boolean decided() {
      return this == WIN || this == LOSS;
   }
}
