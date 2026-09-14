package xiaocaoawa.minecraft.mod.cobblebattle.shaded.snakeyaml.util;

public class EnumUtils {
   public static <T extends Enum<T>> T findEnumInsensitiveCase(Class<T> enumType, String name) {
      for (T constant : enumType.getEnumConstants()) {
         if (constant.name().compareToIgnoreCase(name) == 0) {
            return constant;
         }
      }

      throw new IllegalArgumentException("No enum constant " + enumType.getCanonicalName() + "." + name);
   }
}
