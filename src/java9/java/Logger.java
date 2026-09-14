package xiaocaoawa.minecraft.mod.cobblebattle.shaded.snakeyaml.internal;

public class Logger {
   private final java.lang.System.Logger logger;

   private Logger(String name) {
      this.logger = System.getLogger(name);
   }

   public static xiaocaoawa.minecraft.mod.cobblebattle.shaded.snakeyaml.internal.Logger getLogger(String name) {
      return new xiaocaoawa.minecraft.mod.cobblebattle.shaded.snakeyaml.internal.Logger(name);
   }

   public boolean isLoggable(xiaocaoawa.minecraft.mod.cobblebattle.shaded.snakeyaml.internal.Logger.Level level) {
      return this.logger.isLoggable(level.level);
   }

   public void warn(String msg) {
      this.logger.log(xiaocaoawa.minecraft.mod.cobblebattle.shaded.snakeyaml.internal.Logger.Level.WARNING.level, msg);
   }

   public void debug(String msg) {
      this.logger.log(xiaocaoawa.minecraft.mod.cobblebattle.shaded.snakeyaml.internal.Logger.Level.DEBUG.level, msg);
   }

   public static enum Level {
      WARNING(java.lang.System.Logger.Level.WARNING),
      DEBUG(java.lang.System.Logger.Level.DEBUG);

      private final java.lang.System.Logger.Level level;

      private Level(java.lang.System.Logger.Level level) {
         this.level = level;
      }
   }
}
