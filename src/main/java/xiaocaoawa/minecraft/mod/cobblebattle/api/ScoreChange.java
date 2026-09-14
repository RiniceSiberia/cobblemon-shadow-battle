package xiaocaoawa.minecraft.mod.cobblebattle.api;

public record ScoreChange(long before, long after) {
   public long delta() {
      return this.after - this.before;
   }

   public String signed() {
      long delta = this.delta();
      return (delta >= 0L ? "+" : "") + delta;
   }
}
