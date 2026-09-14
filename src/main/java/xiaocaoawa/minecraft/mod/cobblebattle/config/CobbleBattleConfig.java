package xiaocaoawa.minecraft.mod.cobblebattle.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

   public static CobbleBattleConfig load() {
      return io.github.rinicesiberia.shadowbattle.configuration.ConfigurationRepository.loadOrDefault();
   }

   public CobbleBattleConfig.Reloaded reload() {
      return io.github.rinicesiberia.shadowbattle.configuration.ConfigurationReloading.refresh(this);
   }

   public void validate() {
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


