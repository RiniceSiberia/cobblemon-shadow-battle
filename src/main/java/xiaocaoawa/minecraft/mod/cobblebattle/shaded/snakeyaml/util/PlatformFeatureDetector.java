package xiaocaoawa.minecraft.mod.cobblebattle.shaded.snakeyaml.util;

public class PlatformFeatureDetector {
   private Boolean isRunningOnAndroid = null;

   public boolean isIntrospectionAvailable() {
      if (this.isRunningOnAndroid()) {
         return false;
      } else {
         try {
            Class.forName("java.beans.Introspector");
            return true;
         } catch (ClassNotFoundException var2) {
            return false;
         }
      }
   }

   public boolean isRunningOnAndroid() {
      if (this.isRunningOnAndroid == null) {
         String name = System.getProperty("java.runtime.name");
         this.isRunningOnAndroid = name != null && name.startsWith("Android Runtime");
      }

      return this.isRunningOnAndroid;
   }
}
