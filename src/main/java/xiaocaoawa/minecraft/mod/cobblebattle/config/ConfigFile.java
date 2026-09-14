package xiaocaoawa.minecraft.mod.cobblebattle.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ConfigFile {
   private static final Logger LOGGER = LoggerFactory.getLogger("CobbleBattle/Config");
   static final Path PATH = Paths.get("config", "cobblebattle.yml");
   private static final String TEMPLATE_RESOURCE = "cobblebattle.yml";

   private ConfigFile() {
   }

   static CobbleBattleConfig load() {
      try {
         if (!Files.exists(PATH)) {
            writeDefault();
         }

         if (Files.exists(PATH)) {
            String text = Files.readString(PATH, StandardCharsets.UTF_8);
            CobbleBattleConfig loaded = (CobbleBattleConfig)CobbleBattleConfig.GSON.fromJson(YamlTree.parse(text), CobbleBattleConfig.class);
            if (loaded != null) {
               loaded.validate();
               return loaded;
            }

            LOGGER.warn("config/cobblebattle.yml was empty, falling back to defaults");
         }
      } catch (RuntimeException | IOException var2) {
         LOGGER.error("Could not read config/cobblebattle.yml, using defaults", var2);
      }

      CobbleBattleConfig fresh = new CobbleBattleConfig();
      fresh.validate();
      return fresh;
   }

   static CobbleBattleConfig readOrThrow() {
      try {
         if (!Files.exists(PATH)) {
            throw new IllegalStateException(PATH + " does not exist");
         } else {
            String text = Files.readString(PATH, StandardCharsets.UTF_8);
            CobbleBattleConfig parsed = (CobbleBattleConfig)CobbleBattleConfig.GSON.fromJson(YamlTree.parse(text), CobbleBattleConfig.class);
            if (parsed == null) {
               throw new IllegalStateException(PATH + " is empty");
            } else {
               parsed.validate();
               return parsed;
            }
         }
      } catch (IOException var2) {
         throw new IllegalStateException("could not read " + PATH + ": " + var2.getMessage(), var2);
      } catch (RuntimeException var3) {
         String detail = var3.getMessage() == null ? var3.toString() : var3.getMessage();
         throw new IllegalStateException(detail.split("\n")[0], var3);
      }
   }

   private static InputStream openTemplate() {
      InputStream in = ConfigFile.class.getResourceAsStream("/cobblebattle.yml");
      return in != null ? in : ConfigFile.class.getClassLoader().getResourceAsStream("cobblebattle.yml");
   }

   private static void writeDefault() throws IOException {
      try (InputStream in = openTemplate()) {
         if (in == null) {
            LOGGER.error("Missing bundled resource {} - starting with built-in defaults", "cobblebattle.yml");
            return;
         }

         String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
         checkTemplateMatchesDefaults(text);
         Files.createDirectories(PATH.getParent());
         Files.writeString(PATH, text, StandardCharsets.UTF_8);
         LOGGER.info("Wrote default config to {}", PATH.toAbsolutePath());
      }
   }

   private static void checkTemplateMatchesDefaults(String text) {
      JsonObject shipped;
      try {
         shipped = YamlTree.parse(text);
      } catch (RuntimeException var7) {
         LOGGER.warn("Bundled {} is not readable: {}", "cobblebattle.yml", var7.getMessage());
         return;
      }

      JsonObject defaults = CobbleBattleConfig.GSON.toJsonTree(new CobbleBattleConfig()).getAsJsonObject();

      for (String key : defaults.keySet()) {
         JsonElement want = defaults.get(key);
         JsonElement have = shipped.get(key);
         if (have == null) {
            LOGGER.warn("Bundled {} is missing '{}'", "cobblebattle.yml", key);
         } else if (!want.equals(have)) {
            LOGGER.warn("Bundled {} has {} = {} but the built-in default is {}", new Object[]{"cobblebattle.yml", key, have, want});
         }
      }
   }
}
