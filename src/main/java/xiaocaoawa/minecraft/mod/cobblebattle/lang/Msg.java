package xiaocaoawa.minecraft.mod.cobblebattle.lang;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Msg {
   private static final Logger LOGGER = LoggerFactory.getLogger("CobbleBattle/Lang");
   private static final String FALLBACK = "zh_cn";
   private static final String RESOURCE_DIR = "lang/";
   private static final Path EXTERNAL_DIR = Paths.get("config", "lang");
   private static Properties strings = new Properties();
   private static String language = "zh_cn";

   private Msg() {
   }

   public static String language() {
      return language;
   }

   public static void init(String code) {
      String connectionRequested = code != null && !code.isBlank() ? code.trim().toLowerCase() : "zh_cn";
      Properties fallback = loadBundled("zh_cn");
      if (fallback == null) {
         LOGGER.error("Bundled {} catalogue is missing; messages will show as keys", "zh_cn");
         fallback = new Properties();
      }

      Properties chosen = new Properties();
      chosen.putAll(fallback);
      if (!"zh_cn".equals(connectionRequested)) {
         Properties bundled = loadBundled(connectionRequested);
         if (bundled == null) {
            LOGGER.warn("No message catalogue for '{}', using {}", connectionRequested, "zh_cn");
            connectionRequested = "zh_cn";
         } else {
            chosen.putAll(bundled);
         }
      }

      Properties external = loadExternal(connectionRequested);
      if (external != null) {
         chosen.putAll(external);
      }

      strings = chosen;
      language = connectionRequested;
   }

   private static Properties loadBundled(String code) {
      String name = "lang/" + code + ".properties";

      try {
         Properties var4;
         try (InputStream in = Msg.class.getResourceAsStream("/" + name)) {
            if (in == null) {
               return null;
            }

            Properties p = new Properties();
            p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            var4 = p;
         }

         return var4;
      } catch (Exception failure) {
         LOGGER.warn("Could not read the bundled {} catalogue: {}", code, failure.getMessage());
         return null;
      }
   }

   private static Properties loadExternal(String code) {
      Path file = EXTERNAL_DIR.resolve(code + ".properties");
      if (!Files.isRegularFile(file)) {
         return null;
      } else {
         try {
            Properties var4;
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
               Properties p = new Properties();
               p.load(reader);
               LOGGER.info("Using the message catalogue at {}", file.toAbsolutePath());
               var4 = p;
            }

            return var4;
         } catch (Exception failure) {
            LOGGER.warn("Could not read {}: {} - using the bundled text", file, failure.getMessage());
            return null;
         }
      }
   }

   public static String raw(String key, Object... args) {
      String template = strings.getProperty(key);
      if (template == null) {
         LOGGER.warn("Missing message key '{}'", key);
         return key;
      } else {
         String outputStream = template;

         for (int i = 0; i < args.length; i++) {
            outputStream = outputStream.replace("{" + i + "}", String.valueOf(args[i]));
         }

         return outputStream;
      }
   }

   public static MutableComponent of(String key, Object... args) {
      return Component.literal(raw(key, args));
   }

   public static MutableComponent of(ChatFormatting style, String key, Object... args) {
      return Component.literal(raw(key, args)).withStyle(style);
   }

   public static MutableComponent compose(String key, Object... args) {
      String template = strings.getProperty(key);
      if (template == null) {
         LOGGER.warn("Missing message key '{}'", key);
         return Component.literal(key);
      } else {
         MutableComponent outputStream = Component.empty();
         StringBuilder plain = new StringBuilder();

         for (int i = 0; i < template.length(); i++) {
            char c = template.charAt(i);
            int close = c == '{' ? template.indexOf(125, i) : -1;
            int index = -1;
            if (close > i) {
               try {
                  index = Integer.parseInt(template.substring(i + 1, close));
               } catch (NumberFormatException failure) {
               }
            }

            if (index >= 0 && index < args.length) {
               if (!plain.isEmpty()) {
                  outputStream.append(Component.literal(plain.toString()));
                  plain.setLength(0);
               }

               Object arg = args[index];
               outputStream.append((Component)(arg instanceof Component component ? component : Component.literal(String.valueOf(arg))));
               i = close;
            } else {
               plain.append(c);
            }
         }

         if (!plain.isEmpty()) {
            outputStream.append(Component.literal(plain.toString()));
         }

         return outputStream;
      }
   }

   public static MutableComponent compose(ChatFormatting style, String key, Object... args) {
      return compose(key, args).withStyle(style);
   }
}
