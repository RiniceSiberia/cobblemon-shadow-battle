package xiaocaoawa.minecraft.mod.cobblebattle.client;

import io.github.rinicesiberia.shadowbattle.client.ClientSettingsStore;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ClientSettings {
   private static final Logger LOGGER = LoggerFactory.getLogger("CobbleBattle/Settings");
   private static final ClientSettingsStore SETTINGS = new ClientSettingsStore(ClientSettings::file, message -> LOGGER.warn("{}", message));

   private ClientSettings() {
   }

   public static boolean chatHud() {
      return SETTINGS.chatHudEnabled();
   }

   public static void setChatHud(boolean on) {
      SETTINGS.updateChatHud(on);
   }

   private static Path file() {
      return Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("cobblebattle-client.json");
   }

}
