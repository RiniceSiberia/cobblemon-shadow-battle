package xiaocaoawa.minecraft.mod.cobblebattle.client;

import com.cobblemon.mod.common.api.pokedex.Dexes;
import com.cobblemon.mod.common.api.pokedex.FormDexRecord;
import com.cobblemon.mod.common.api.pokedex.PokedexEntryProgress;
import com.cobblemon.mod.common.api.pokedex.SpeciesDexRecord;
import com.cobblemon.mod.common.api.pokedex.def.PokedexDef;
import com.cobblemon.mod.common.api.pokedex.def.SimplePokedexDef;
import com.cobblemon.mod.common.api.pokedex.entry.PokedexEntry;
import com.cobblemon.mod.common.api.pokedex.entry.PokedexForm;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.api.storage.player.client.ClientPokedexManager;
import com.cobblemon.mod.common.client.gui.pokedex.PokedexGUI;
import com.cobblemon.mod.common.client.pokedex.PokedexType;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Gender;
import com.cobblemon.mod.common.pokemon.Species;
import dev.architectury.networking.NetworkManager;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import xiaocaoawa.minecraft.mod.cobblebattle.network.OpenPagePayload;
import xiaocaoawa.minecraft.mod.cobblebattle.network.ServerDexPayload;

public final class ServerDex {
   public static final ResourceLocation DEX_ID = ResourceLocation.fromNamespaceAndPath("cobblebattle", "server");
   private static final ResourceLocation NATIONAL_DEX_ID = ResourceLocation.fromNamespaceAndPath("cobblemon", "national");
   private static final ServerDexSnapshotState SNAPSHOT = new ServerDexSnapshotState();
   private static boolean serverDexActive;
   private static Map<ResourceLocation, PokedexDef> previousDexDefinitions;
   private static ClientPokedexManager serverKnowledge;

   private ServerDex() {
   }

   public static boolean active() {
      return serverDexActive;
   }

   public static void rememberRanked(String rankedCompetitionId) {
      SNAPSHOT.rememberRanked(rankedCompetitionId);
   }

   public static void requestDex() {
      NetworkManager.sendToServer(new OpenPagePayload("dex", "", SNAPSHOT.digest()));
   }

   public static void requestLeaderboard() {
      NetworkManager.sendToServer(new OpenPagePayload("leaderboard", SNAPSHOT.rankedId(), ""));
   }

   public static void requestRooms() {
      NetworkManager.sendToServer(OpenPagePayload.of("rooms"));
   }

   public static void requestMain() {
      NetworkManager.sendToServer(OpenPagePayload.of("main"));
   }

   public static ClientPokedexManager knowledge() {
      return serverDexActive ? serverKnowledge : null;
   }

   public static void onPokedexSync() {
      if (serverDexActive) {
         close();
         Minecraft client = Minecraft.getInstance();
         if (client.screen instanceof PokedexGUI) {
            client.setScreen(null);
         }
      }
   }

   public static void open(ServerDexPayload body) {
      Minecraft client = Minecraft.getInstance();
      if (client.player != null) {
         close();
         if (SNAPSHOT.accept(body) == ServerDexAcceptance.RETRY_FULL_SNAPSHOT) {
            requestDex();
            return;
         }

         List<PokedexEntry> approvedDexEntries = collectApprovedEntries();
         if (approvedDexEntries.isEmpty()) {
            client.player.sendSystemMessage(Component.translatable("cobblebattle.dex.empty"));
         } else {
            SimplePokedexDef serverDexDefinition = new SimplePokedexDef(DEX_ID);
            serverDexDefinition.appendEntries(approvedDexEntries.stream().map(PokedexEntry::getId).toList());
            LinkedHashMap<ResourceLocation, PokedexDef> dexDefinitions = Dexes.INSTANCE.getDexEntryMap();
            previousDexDefinitions = new LinkedHashMap<>(dexDefinitions);
            dexDefinitions.clear();
            dexDefinitions.put(DEX_ID, serverDexDefinition);
            serverKnowledge = buildCompleteKnowledge(approvedDexEntries);
            serverDexActive = true;
            PokedexGUI.Companion.open(serverKnowledge, PokedexType.RED, null, null);
         }
      }
   }

   public static void close() {
      if (serverDexActive) {
         serverDexActive = false;
         serverKnowledge = null;
         LinkedHashMap<ResourceLocation, PokedexDef> dexDefinitions = Dexes.INSTANCE.getDexEntryMap();
         dexDefinitions.clear();
         if (previousDexDefinitions != null) {
            dexDefinitions.putAll(previousDexDefinitions);
         }

         previousDexDefinitions = null;
      }
   }

   public static void tick() {
      if (serverDexActive && !(Minecraft.getInstance().screen instanceof PokedexGUI)) {
         close();
      }
   }

   public static Map<Stat, Integer> statsFor(PokedexEntry selectedEntry, PokedexForm selectedForm) {
      if (selectedEntry == null) {
         return null;
      } else {
         Species speciesTemplate = PokemonSpecies.getByIdentifier(selectedEntry.getSpeciesId());
         if (speciesTemplate == null) {
            return null;
         } else {
            FormData resolvedForm = selectedForm == null ? null : speciesTemplate.getFormByName(selectedForm.getDisplayForm());
            if (resolvedForm == null) {
               resolvedForm = speciesTemplate.getStandardForm();
            }

            return SNAPSHOT.statsFor(resolvedForm.showdownId(), speciesTemplate.showdownId());
         }
      }
   }

   private static List<PokedexEntry> collectApprovedEntries() {
      Map<ResourceLocation, PokedexDef> dexDefinitions = Dexes.INSTANCE.getDexEntryMap();
      PokedexDef nationalDex = dexDefinitions.get(NATIONAL_DEX_ID);
      List<PokedexEntry> candidateEntries;
      if (nationalDex != null) {
         candidateEntries = nationalDex.getEntries();
      } else {
         candidateEntries = new ArrayList<>();
         Set<ResourceLocation> seenEntryIds = new LinkedHashSet<>();

         for (PokedexDef dexDefinition : dexDefinitions.values()) {
            for (PokedexEntry dexEntry : dexDefinition.getEntries()) {
               if (seenEntryIds.add(dexEntry.getId())) {
                  candidateEntries.add(dexEntry);
               }
            }
         }
      }

      List<PokedexEntry> outputStream = new ArrayList<>();

      for (PokedexEntry candidateEntry : candidateEntries) {
         Species speciesTemplate = PokemonSpecies.getByIdentifier(candidateEntry.getSpeciesId());
         if (speciesTemplate != null && isApprovedSpecies(speciesTemplate)) {
            outputStream.add(candidateEntry);
         }
      }

      return outputStream;
   }

   private static boolean isApprovedSpecies(Species speciesTemplate) {
      if (SNAPSHOT.contains(speciesTemplate.showdownId())) {
         return true;
      } else {
         for (FormData speciesForm : speciesTemplate.getForms()) {
            if (SNAPSHOT.contains(speciesForm.showdownId())) {
               return true;
            }
         }

         return false;
      }
   }

   private static ClientPokedexManager buildCompleteKnowledge(List<PokedexEntry> approvedEntries) {
      Map<ResourceLocation, SpeciesDexRecord> speciesRecords = new LinkedHashMap<>();
      ClientPokedexManager knowledgeManager = new ClientPokedexManager(speciesRecords);

      for (PokedexEntry pokedexEntry : approvedEntries) {
         Species speciesTemplate = PokemonSpecies.getByIdentifier(pokedexEntry.getSpeciesId());
         SpeciesDexRecord speciesRecord = speciesRecords.computeIfAbsent(pokedexEntry.getSpeciesId(), speciesId -> new SpeciesDexRecord());
         Set<String> discoveredAspects = new LinkedHashSet<>(pokedexEntry.getConditionAspects());
         discoveredAspects.addAll(pokedexEntry.getDisplayAspects());
         ServerDex.RecordFields.mutableAspects(speciesRecord).addAll(discoveredAspects);
         Set<String> knownFormNames = new LinkedHashSet<>();

         for (PokedexForm pokedexForm : pokedexEntry.getForms()) {
            knownFormNames.add(pokedexForm.getDisplayForm());
            knownFormNames.addAll(pokedexForm.getUnlockForms());
         }

         if (speciesTemplate != null) {
            for (FormData speciesForm : speciesTemplate.getForms()) {
               knownFormNames.add(speciesForm.getName());
            }
         }

         for (String formName : new ArrayList<>(knownFormNames)) {
            knownFormNames.add(formName.toLowerCase(Locale.ROOT));
         }

         Map<String, FormDexRecord> formRecords = ServerDex.RecordFields.mutableForms(speciesRecord);

         for (String knownFormName : knownFormNames) {
            if (!formRecords.containsKey(knownFormName)) {
               FormDexRecord formRecord = new FormDexRecord();
               FormData formData = speciesTemplate == null ? null : speciesTemplate.getFormByName(knownFormName);
               Set<Gender> allowedGenders = ServerDex.RecordFields.mutableGenders(formRecord);
               if (formData != null && !formData.getPossibleGenders().isEmpty()) {
                  allowedGenders.addAll(formData.getPossibleGenders());
               } else {
                  allowedGenders.add(Gender.MALE);
                  allowedGenders.add(Gender.FEMALE);
               }

               ServerDex.RecordFields.mutableShinyStates(formRecord).add("normal");
               ServerDex.RecordFields.mutableShinyStates(formRecord).add("shiny");
               ServerDex.RecordFields.writeKnowledge(formRecord, CobblemonCompat.OWNED);
               formRecords.put(knownFormName, formRecord);
            }
         }

         speciesRecord.initialize(knowledgeManager, pokedexEntry.getSpeciesId());
      }

      return knowledgeManager;
   }

   private static final class RecordFields {
      private static final Field SPECIES_ASPECTS_FIELD = locateField(SpeciesDexRecord.class, "aspects");
      private static final Field FORM_RECORDS_FIELD = locateField(SpeciesDexRecord.class, "formRecords");
      private static final Field GENDERS_FIELD = locateField(FormDexRecord.class, "genders");
      private static final Field SHINY_STATES_FIELD = locateField(FormDexRecord.class, "seenShinyStates");
      private static final Field KNOWLEDGE_FIELD = locateField(FormDexRecord.class, "knowledge");

      private static Field locateField(Class<?> recordType, String fieldName) {
         try {
            Field reflectedField = recordType.getDeclaredField(fieldName);
            reflectedField.setAccessible(true);
            return reflectedField;
         } catch (NoSuchFieldException failure) {
            throw new IllegalStateException("Cobblemon's " + recordType.getSimpleName() + " no longer has a field named '" + fieldName + "'", failure);
         }
      }

      static Set<String> mutableAspects(SpeciesDexRecord speciesRecord) {
         return (Set<String>)readFieldValue(SPECIES_ASPECTS_FIELD, speciesRecord);
      }

      static Map<String, FormDexRecord> mutableForms(SpeciesDexRecord speciesRecord) {
         return (Map<String, FormDexRecord>)readFieldValue(FORM_RECORDS_FIELD, speciesRecord);
      }

      static Set<Gender> mutableGenders(FormDexRecord formRecord) {
         return (Set<Gender>)readFieldValue(GENDERS_FIELD, formRecord);
      }

      static Set<String> mutableShinyStates(FormDexRecord formRecord) {
         return (Set<String>)readFieldValue(SHINY_STATES_FIELD, formRecord);
      }

      static void writeKnowledge(FormDexRecord formRecord, PokedexEntryProgress progress) {
         try {
            KNOWLEDGE_FIELD.set(formRecord, progress);
         } catch (IllegalAccessException failure) {
            throw new IllegalStateException(failure);
         }
      }

      private static Object readFieldValue(Field reflectedField, Object recordInstance) {
         try {
            return reflectedField.get(recordInstance);
         } catch (IllegalAccessException failure) {
            throw new IllegalStateException(failure);
         }
      }
   }
}
