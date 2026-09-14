package xiaocaoawa.minecraft.mod.cobblebattle.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ClientSettings {
   private static final Logger LOGGER = LoggerFactory.getLogger("CobbleBattle/Settings");
   private static boolean loaded;
   private static boolean chatHud = true;

   private ClientSettings() {
   }

   public static boolean chatHud() {
      load();
      return chatHud;
   }

   public static void setChatHud(boolean on) {
      load();
      chatHud = on;
      save();
   }

   private static Path file() {
      return Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("cobblebattle-client.json");
   }

   private static void load() {
      if (!loaded) {
         loaded = true;

         try {
            Path file = file();
            if (!Files.exists(file)) {
               return;
            }

            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            if (root.has("chatHud")) {
               chatHud = root.get("chatHud").getAsBoolean();
            }
         } catch (Exception failure) {
            LOGGER.warn("Could not read client settings: {}", failure.toString());
         }
      }
   }

   private static void save() {
      try {
         Path file = file();
         Files.createDirectories(file.getParent());
         JsonObject root = new JsonObject();
         root.addProperty("chatHud", chatHud);
         Files.writeString(file, root.toString(), StandardCharsets.UTF_8);
      } catch (Exception failure) {
         LOGGER.warn("Could not save client settings: {}", failure.toString());
      }
   }
}
