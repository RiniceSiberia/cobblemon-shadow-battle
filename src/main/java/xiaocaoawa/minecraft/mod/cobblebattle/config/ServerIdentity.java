package xiaocaoawa.minecraft.mod.cobblebattle.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ServerIdentity {
   private static final Logger LOGGER = LoggerFactory.getLogger("CobbleBattle/Config");
   private static final Path PATH = Paths.get("config", "cobblebattle-id.txt");
   private static volatile String cached;

   private ServerIdentity() {
   }

   public static String get() {
      String known = cached;
      if (known != null) {
         return known;
      } else {
         synchronized (ServerIdentity.class) {
            if (cached == null) {
               cached = loadOrCreate();
            }

            return cached;
         }
      }
   }

   private static String loadOrCreate() {
      try {
         if (Files.exists(PATH)) {
            String text = Files.readString(PATH, StandardCharsets.UTF_8).trim();
            if (!text.isEmpty()) {
               return text;
            }

            LOGGER.warn("{} was empty, generating a new server id", PATH);
         }
      } catch (IOException var3) {
         LOGGER.warn("Could not read {}: {}", PATH, var3.getMessage());
      }

      String generated = generate();

      try {
         Files.createDirectories(PATH.getParent());
         Files.writeString(PATH, generated, StandardCharsets.UTF_8);
         LOGGER.info("Generated server id {} and saved it to {}", generated, PATH);
      } catch (IOException var2) {
         LOGGER.warn("Generated server id {} but could not save it: {}", generated, var2.getMessage());
         LOGGER.warn("A new one will be generated next start, which the battle server sees as a new client.");
      }

      return generated;
   }

   private static String generate() {
      return "mc-" + Long.toHexString(UUID.randomUUID().getMostSignificantBits());
   }
}
