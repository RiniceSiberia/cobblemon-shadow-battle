package xiaocaoawa.minecraft.mod.cobblebattle.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import xiaocaoawa.minecraft.mod.cobblebattle.shaded.snakeyaml.LoaderOptions;
import xiaocaoawa.minecraft.mod.cobblebattle.shaded.snakeyaml.Yaml;
import xiaocaoawa.minecraft.mod.cobblebattle.shaded.snakeyaml.constructor.SafeConstructor;

final class YamlTree {
   private YamlTree() {
   }

   static JsonObject parse(String text) {
      Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
      Object loaded = yaml.load(text);
      if (loaded == null) {
         return new JsonObject();
      } else {
         JsonElement tree = toJson(loaded);
         if (!tree.isJsonObject()) {
            throw new IllegalArgumentException("the top level of cobblebattle.yml must be a mapping");
         } else {
            return tree.getAsJsonObject();
         }
      }
   }

   private static JsonElement toJson(Object value) {
      if (value == null) {
         return JsonNull.INSTANCE;
      } else if (value instanceof Map<?, ?> map) {
         JsonObject out = new JsonObject();

         for (Entry<?, ?> e : map.entrySet()) {
            out.add(String.valueOf(e.getKey()), toJson(e.getValue()));
         }

         return out;
      } else if (!(value instanceof List<?> list)) {
         if (value instanceof Boolean b) {
            return new JsonPrimitive(b);
         } else {
            return value instanceof Number n ? new JsonPrimitive(n) : new JsonPrimitive(String.valueOf(value));
         }
      } else {
         JsonArray out = new JsonArray();

         for (Object item : list) {
            out.add(toJson(item));
         }

         return out;
      }
   }
}
