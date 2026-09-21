package xiaocaoawa.minecraft.mod.cobblebattle.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.List;

public final class CobbleBattleConfig {
   static final Gson CONFIG_GSON = new GsonBuilder().create();
   static final Gson GSON = CONFIG_GSON;
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
   /** 后端类型：LOCAL_SERVICE 保留原自建服务，POKEMON_SHOWDOWN 使用官方 WebSocket。 */
   public Backend backend = Backend.LOCAL_SERVICE;
   public String showdownWebSocket = "wss://sim3.psim.us/showdown/websocket";
   public String showdownFormat = "gen9ou";
   public String showdownUsername = "";
   public String showdownPassword = "";

   public enum Backend {
      LOCAL_SERVICE,
      POKEMON_SHOWDOWN
   }

   public static CobbleBattleConfig load() {
      return io.github.rinicesiberia.shadowbattle.configuration.ConfigurationRepository.loadOrDefault();
   }

   public CobbleBattleConfig.Reloaded reload() {
      return io.github.rinicesiberia.shadowbattle.configuration.ConfigurationReloading.refresh(this);
   }

   public record Reloaded(List<String> live, List<String> needsReconnect) {
      public boolean nothingChanged() {
         return this.live.isEmpty() && this.needsReconnect.isEmpty();
      }
   }
}


