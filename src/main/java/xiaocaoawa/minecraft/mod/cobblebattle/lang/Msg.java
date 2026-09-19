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
   private static final Logger LOG = LoggerFactory.getLogger("CobbleBattle/Lang");
   private static final String DEFAULT_LANGUAGE_CODE = "zh_cn";
   private static final String BUNDLED_CATALOGUE_DIRECTORY = "lang/";
   private static final Path EXTERNAL_CATALOGUE_DIRECTORY = Paths.get("config", "lang");
   private static Properties activeMessages = new Properties();
   private static String activeLanguageCode = DEFAULT_LANGUAGE_CODE;

   private Msg() {
   }

   public static String language() {
      return activeLanguageCode;
   }

   public static void init(String languageCode) {
      String resolvedLanguageCode = languageCode != null && !languageCode.isBlank() ? languageCode.trim().toLowerCase() : DEFAULT_LANGUAGE_CODE;
      Properties fallbackMessages = loadBundledCatalogue(DEFAULT_LANGUAGE_CODE);
      if (fallbackMessages == null) {
         LOG.error("Bundled {} catalogue is missing; messages will show as keys", DEFAULT_LANGUAGE_CODE);
         fallbackMessages = new Properties();
      }

      Properties mergedMessages = new Properties();
      mergedMessages.putAll(fallbackMessages);
      if (!DEFAULT_LANGUAGE_CODE.equals(resolvedLanguageCode)) {
         Properties localizedMessages = loadBundledCatalogue(resolvedLanguageCode);
         if (localizedMessages == null) {
            LOG.warn("No message catalogue for '{}', using {}", resolvedLanguageCode, DEFAULT_LANGUAGE_CODE);
            resolvedLanguageCode = DEFAULT_LANGUAGE_CODE;
         } else {
            mergedMessages.putAll(localizedMessages);
         }
      }

      Properties overrideMessages = loadExternalCatalogue(resolvedLanguageCode);
      if (overrideMessages != null) {
         mergedMessages.putAll(overrideMessages);
      }

      activeMessages = mergedMessages;
      activeLanguageCode = resolvedLanguageCode;
   }

   private static Properties loadBundledCatalogue(String languageCode) {
      String resourcePath = BUNDLED_CATALOGUE_DIRECTORY + languageCode + ".properties";

      try {
         Properties loadedCatalogue;
         try (InputStream resourceStream = Msg.class.getResourceAsStream("/" + resourcePath)) {
            if (resourceStream == null) {
               return null;
            }

            Properties catalogue = new Properties();
            catalogue.load(new InputStreamReader(resourceStream, StandardCharsets.UTF_8));
            loadedCatalogue = catalogue;
         }

         return loadedCatalogue;
      } catch (Exception failure) {
         LOG.warn("Could not read the bundled {} catalogue: {}", languageCode, failure.getMessage());
         return null;
      }
   }

   private static Properties loadExternalCatalogue(String languageCode) {
      Path catalogueFile = EXTERNAL_CATALOGUE_DIRECTORY.resolve(languageCode + ".properties");
      if (!Files.isRegularFile(catalogueFile)) {
         return null;
      } else {
         try {
            Properties loadedCatalogue;
            try (Reader catalogueReader = Files.newBufferedReader(catalogueFile, StandardCharsets.UTF_8)) {
               Properties catalogue = new Properties();
               catalogue.load(catalogueReader);
               LOG.info("Using the message catalogue at {}", catalogueFile.toAbsolutePath());
               loadedCatalogue = catalogue;
            }

            return loadedCatalogue;
         } catch (Exception failure) {
            LOG.warn("Could not read {}: {} - using the bundled text", catalogueFile, failure.getMessage());
            return null;
         }
      }
   }

   public static String raw(String messageKey, Object... substitutions) {
      String messageTemplate = activeMessages.getProperty(messageKey);
      if (messageTemplate == null) {
         LOG.warn("Missing message key '{}'", messageKey);
         return messageKey;
      } else {
         String renderedText = messageTemplate;

         for (int substitutionIndex = 0; substitutionIndex < substitutions.length; substitutionIndex++) {
            renderedText = renderedText.replace("{" + substitutionIndex + "}", String.valueOf(substitutions[substitutionIndex]));
         }

         return renderedText;
      }
   }

   public static MutableComponent of(String messageKey, Object... substitutions) {
      return Component.literal(raw(messageKey, substitutions));
   }

   public static MutableComponent of(ChatFormatting textStyle, String messageKey, Object... substitutions) {
      return Component.literal(raw(messageKey, substitutions)).withStyle(textStyle);
   }

   public static MutableComponent compose(String messageKey, Object... substitutions) {
      String messageTemplate = activeMessages.getProperty(messageKey);
      if (messageTemplate == null) {
         LOG.warn("Missing message key '{}'", messageKey);
         return Component.literal(messageKey);
      } else {
         MutableComponent assembledMessage = Component.empty();
         StringBuilder literalBuffer = new StringBuilder();

         for (int cursorIndex = 0; cursorIndex < messageTemplate.length(); cursorIndex++) {
            char currentCharacter = messageTemplate.charAt(cursorIndex);
            int placeholderEnd = currentCharacter == '{' ? messageTemplate.indexOf(125, cursorIndex) : -1;
            int substitutionIndex = -1;
            if (placeholderEnd > cursorIndex) {
               try {
                  substitutionIndex = Integer.parseInt(messageTemplate.substring(cursorIndex + 1, placeholderEnd));
               } catch (NumberFormatException failure) {
               }
            }

            if (substitutionIndex >= 0 && substitutionIndex < substitutions.length) {
               if (!literalBuffer.isEmpty()) {
                  assembledMessage.append(Component.literal(literalBuffer.toString()));
                  literalBuffer.setLength(0);
               }

               Object substitution = substitutions[substitutionIndex];
               assembledMessage.append((Component)(substitution instanceof Component componentSubstitution ? componentSubstitution : Component.literal(String.valueOf(substitution))));
               cursorIndex = placeholderEnd;
            } else {
               literalBuffer.append(currentCharacter);
            }
         }

         if (!literalBuffer.isEmpty()) {
            assembledMessage.append(Component.literal(literalBuffer.toString()));
         }

         return assembledMessage;
      }
   }

   public static MutableComponent compose(ChatFormatting textStyle, String messageKey, Object... substitutions) {
      return compose(messageKey, substitutions).withStyle(textStyle);
   }
}
