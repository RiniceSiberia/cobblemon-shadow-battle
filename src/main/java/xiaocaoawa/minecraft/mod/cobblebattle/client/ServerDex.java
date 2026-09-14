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
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.api.storage.player.client.ClientPokedexManager;
import com.cobblemon.mod.common.client.gui.pokedex.PokedexGUI;
import com.cobblemon.mod.common.client.pokedex.PokedexType;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Gender;
import com.cobblemon.mod.common.pokemon.Species;
import dev.architectury.networking.NetworkManager;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
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
   private static final ResourceLocation NATIONAL = ResourceLocation.fromNamespaceAndPath("cobblemon", "national");
   private static Map<String, Map<Stat, Integer>> stats = Map.of();
   private static String cachedDigest = "";
   private static boolean active;
   private static Map<ResourceLocation, PokedexDef> savedDexes;
   private static ClientPokedexManager knowledge;
   private static String lastRanked = "";

   private ServerDex() {
   }

   public static boolean active() {
      return active;
   }

   public static void rememberRanked(String rankedId) {
      lastRanked = rankedId == null ? "" : rankedId;
   }

   public static void requestDex() {
      NetworkManager.sendToServer(new OpenPagePayload("dex", "", cachedDigest));
   }

   public static void requestLeaderboard() {
      NetworkManager.sendToServer(new OpenPagePayload("leaderboard", lastRanked, ""));
   }

   public static void requestRooms() {
      NetworkManager.sendToServer(OpenPagePayload.of("rooms"));
   }

   public static void requestMain() {
      NetworkManager.sendToServer(OpenPagePayload.of("main"));
   }

   public static ClientPokedexManager knowledge() {
      return active ? knowledge : null;
   }

   public static void onPokedexSync() {
      if (active) {
         close();
         Minecraft minecraft = Minecraft.getInstance();
         if (minecraft.screen instanceof PokedexGUI) {
            minecraft.setScreen(null);
         }
      }
   }

   public static void open(ServerDexPayload body) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.player != null) {
         close();
         if (body.unchanged()) {
            if (stats.isEmpty() || !cachedDigest.equals(body.digest())) {
               cachedDigest = "";
               requestDex();
               return;
            }
         } else {
            stats = index(body);
            cachedDigest = body.digest();
         }

         List<PokedexEntry> entries = approvedEntries();
         if (entries.isEmpty()) {
            minecraft.player.sendSystemMessage(Component.translatable("cobblebattle.dex.empty"));
         } else {
            SimplePokedexDef region = new SimplePokedexDef(DEX_ID);
            region.appendEntries(entries.stream().map(PokedexEntry::getId).toList());
            LinkedHashMap<ResourceLocation, PokedexDef> dexes = Dexes.INSTANCE.getDexEntryMap();
            savedDexes = new LinkedHashMap<>(dexes);
            dexes.clear();
            dexes.put(DEX_ID, region);
            knowledge = fullKnowledge(entries);
            active = true;
            PokedexGUI.Companion.open(knowledge, PokedexType.RED, null, null);
         }
      }
   }

   public static void close() {
      if (active) {
         active = false;
         knowledge = null;
         LinkedHashMap<ResourceLocation, PokedexDef> dexes = Dexes.INSTANCE.getDexEntryMap();
         dexes.clear();
         if (savedDexes != null) {
            dexes.putAll(savedDexes);
         }

         savedDexes = null;
      }
   }

   public static void tick() {
      if (active && !(Minecraft.getInstance().screen instanceof PokedexGUI)) {
         close();
      }
   }

   public static Map<Stat, Integer> statsFor(PokedexEntry entry, PokedexForm form) {
      if (entry == null) {
         return null;
      } else {
         Species speciesTemplate = PokemonSpecies.getByIdentifier(entry.getSpeciesId());
         if (speciesTemplate == null) {
            return null;
         } else {
            FormData data = form == null ? null : speciesTemplate.getFormByName(form.getDisplayForm());
            if (data == null) {
               data = speciesTemplate.getStandardForm();
            }

            Map<Stat, Integer> found = stats.get(data.showdownId());
            if (found == null) {
               found = stats.get(speciesTemplate.showdownId());
            }

            return found;
         }
      }
   }

   private static Map<String, Map<Stat, Integer>> index(ServerDexPayload body) {
      Map<String, Map<Stat, Integer>> outputStream = new HashMap<>(body.entries().size() * 2);

      for (ServerDexPayload.Entry e : body.entries()) {
         Map<Stat, Integer> base = new HashMap<>(8);
         base.put(Stats.HP, e.hp());
         base.put(Stats.ATTACK, e.atk());
         base.put(Stats.DEFENCE, e.def());
         base.put(Stats.SPECIAL_ATTACK, e.spa());
         base.put(Stats.SPECIAL_DEFENCE, e.spd());
         base.put(Stats.SPEED, e.spe());
         outputStream.put(e.id(), base);
      }

      return outputStream;
   }

   private static List<PokedexEntry> approvedEntries() {
      Map<ResourceLocation, PokedexDef> dexes = Dexes.INSTANCE.getDexEntryMap();
      PokedexDef national = dexes.get(NATIONAL);
      List<PokedexEntry> source;
      if (national != null) {
         source = national.getEntries();
      } else {
         source = new ArrayList<>();
         Set<ResourceLocation> seen = new LinkedHashSet<>();

         for (PokedexDef def : dexes.values()) {
            for (PokedexEntry entry : def.getEntries()) {
               if (seen.add(entry.getId())) {
                  source.add(entry);
               }
            }
         }
      }

      List<PokedexEntry> outputStream = new ArrayList<>();

      for (PokedexEntry entryx : source) {
         Species speciesTemplate = PokemonSpecies.getByIdentifier(entryx.getSpeciesId());
         if (speciesTemplate != null && approved(speciesTemplate)) {
            outputStream.add(entryx);
         }
      }

      return outputStream;
   }

   private static boolean approved(Species speciesTemplate) {
      if (stats.containsKey(speciesTemplate.showdownId())) {
         return true;
      } else {
         for (FormData form : speciesTemplate.getForms()) {
            if (stats.containsKey(form.showdownId())) {
               return true;
            }
         }

         return false;
      }
   }

   private static ClientPokedexManager fullKnowledge(List<PokedexEntry> entries) {
      Map<ResourceLocation, SpeciesDexRecord> records = new LinkedHashMap<>();
      ClientPokedexManager manager = new ClientPokedexManager(records);

      for (PokedexEntry entry : entries) {
         Species speciesTemplate = PokemonSpecies.getByIdentifier(entry.getSpeciesId());
         SpeciesDexRecord record = records.computeIfAbsent(entry.getSpeciesId(), id -> new SpeciesDexRecord());
         Set<String> aspects = new LinkedHashSet<>(entry.getConditionAspects());
         aspects.addAll(entry.getDisplayAspects());
         ServerDex.Records.aspects(record).addAll(aspects);
         Set<String> formNames = new LinkedHashSet<>();

         for (PokedexForm form : entry.getForms()) {
            formNames.add(form.getDisplayForm());
            formNames.addAll(form.getUnlockForms());
         }

         if (speciesTemplate != null) {
            for (FormData form : speciesTemplate.getForms()) {
               formNames.add(form.getName());
            }
         }

         for (String name : new ArrayList<>(formNames)) {
            formNames.add(name.toLowerCase(Locale.ROOT));
         }

         Map<String, FormDexRecord> forms = ServerDex.Records.forms(record);

         for (String name : formNames) {
            if (!forms.containsKey(name)) {
               FormDexRecord form = new FormDexRecord();
               FormData data = speciesTemplate == null ? null : speciesTemplate.getFormByName(name);
               Set<Gender> genders = ServerDex.Records.genders(form);
               if (data != null && !data.getPossibleGenders().isEmpty()) {
                  genders.addAll(data.getPossibleGenders());
               } else {
                  genders.add(Gender.MALE);
                  genders.add(Gender.FEMALE);
               }

               ServerDex.Records.shinyStates(form).add("normal");
               ServerDex.Records.shinyStates(form).add("shiny");
               ServerDex.Records.setKnowledge(form, CobblemonCompat.OWNED);
               forms.put(name, form);
            }
         }

         record.initialize(manager, entry.getSpeciesId());
      }

      return manager;
   }

   private static final class Records {
      private static final Field ASPECTS = field(SpeciesDexRecord.class, "aspects");
      private static final Field FORMS = field(SpeciesDexRecord.class, "formRecords");
      private static final Field GENDERS = field(FormDexRecord.class, "genders");
      private static final Field SHINY = field(FormDexRecord.class, "seenShinyStates");
      private static final Field KNOWLEDGE = field(FormDexRecord.class, "knowledge");

      private static Field field(Class<?> owner, String name) {
         try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field;
         } catch (NoSuchFieldException failure) {
            throw new IllegalStateException("Cobblemon's " + owner.getSimpleName() + " no longer has a field named '" + name + "'", failure);
         }
      }

      static Set<String> aspects(SpeciesDexRecord record) {
         return (Set<String>)read(ASPECTS, record);
      }

      static Map<String, FormDexRecord> forms(SpeciesDexRecord record) {
         return (Map<String, FormDexRecord>)read(FORMS, record);
      }

      static Set<Gender> genders(FormDexRecord form) {
         return (Set<Gender>)read(GENDERS, form);
      }

      static Set<String> shinyStates(FormDexRecord form) {
         return (Set<String>)read(SHINY, form);
      }

      static void setKnowledge(FormDexRecord form, PokedexEntryProgress knowledge) {
         try {
            KNOWLEDGE.set(form, knowledge);
         } catch (IllegalAccessException failure) {
            throw new IllegalStateException(failure);
         }
      }

      private static Object read(Field field, Object owner) {
         try {
            return field.get(owner);
         } catch (IllegalAccessException failure) {
            throw new IllegalStateException(failure);
         }
      }
   }
}
