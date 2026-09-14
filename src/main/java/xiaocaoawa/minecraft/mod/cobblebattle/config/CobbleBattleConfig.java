package xiaocaoawa.minecraft.mod.cobblebattle.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CobbleBattleConfig {
   private static final Logger LOGGER = LoggerFactory.getLogger("CobbleBattle/Config");
   static final Gson GSON = new GsonBuilder().create();
   public String serverHost = "127.0.0.1";
   public int serverPort = 18470;
   public String authToken = "CHANGE_ME_SHARED_SECRET";
   public int connectTimeoutMs = 8000;
   public int reconnectBaseDelayMs = 3000;
   public int reconnectMaxDelayMs = 60000;
   public int maxFrameBytes = 33554432;
   public boolean keepConnectedWhenEmpty = false;
   public int idleDisconnectSeconds = 120;
   public String language = "zh_cn";
   public String defaultRanked = "example";
   public boolean debug = false;
   private static final List<String> CONNECTION_SCOPED = List.of("serverHost", "serverPort", "authToken", "connectTimeoutMs");

   public static CobbleBattleConfig load() {
      return ConfigFile.load();
   }

   public CobbleBattleConfig.Reloaded reload() {
      JsonObject before = GSON.toJsonTree(this).getAsJsonObject();
      CobbleBattleConfig fresh = ConfigFile.readOrThrow();

      for (Field field : CobbleBattleConfig.class.getFields()) {
         if (!Modifier.isStatic(field.getModifiers())) {
            try {
               field.set(this, field.get(fresh));
            } catch (IllegalAccessException var9) {
               LOGGER.warn("Could not reload field {}: {}", field.getName(), var9.getMessage());
            }
         }
      }

      JsonObject after = GSON.toJsonTree(this).getAsJsonObject();
      List<String> live = new ArrayList<>();
      List<String> needsReconnect = new ArrayList<>();

      for (String key : after.keySet()) {
         JsonElement was = before.get(key);
         if (was == null || !was.equals(after.get(key))) {
            (CONNECTION_SCOPED.contains(key) ? needsReconnect : live).add(key);
         }
      }

      return new CobbleBattleConfig.Reloaded(live, needsReconnect);
   }

   void validate() {
      if (this.serverPort <= 0 || this.serverPort > 65535) {
         LOGGER.warn("cobblebattle.yml: serverPort {} is out of range, using 18470", this.serverPort);
         this.serverPort = 18470;
      }

      if (this.maxFrameBytes < 1048576) {
         this.maxFrameBytes = 1048576;
      }
   }

   public record Reloaded(List<String> live, List<String> needsReconnect) {
      public boolean nothingChanged() {
         return this.live.isEmpty() && this.needsReconnect.isEmpty();
      }
   }
}
