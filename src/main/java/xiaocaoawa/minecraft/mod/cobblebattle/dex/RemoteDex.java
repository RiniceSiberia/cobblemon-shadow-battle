package xiaocaoawa.minecraft.mod.cobblebattle.dex;

import com.cobblemon.mod.common.api.abilities.AbilityTemplate;
import com.cobblemon.mod.common.api.abilities.PotentialAbility;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.pokemon.moves.Learnset;
import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RemoteDex {
   private static final Logger LOGGER = LoggerFactory.getLogger("CobbleBattle/Dex");
   private static final Gson GSON = new Gson();
   private static final Path CACHE_FILE = Paths.get("config", "cobblebattle-dex.json");
   private static final String[] STAT_KEYS = new String[]{"hp", "atk", "def", "spa", "spd", "spe"};
   private volatile Map<String, RemoteDex.Entry> speciesTemplate = Collections.emptyMap();
   private volatile String digest = null;
   private volatile boolean ready = false;
   private volatile boolean strictBaseStats = true;
   private volatile boolean strictAbilities = false;
   private volatile boolean strictMoves = false;
   private volatile int maxEvPerStat = 0;
   private volatile int maxEvTotal = 0;
   private volatile int maxIv = 0;
   private static final Map<Stat, String> STAT_NAMES = Map.of(
      Stats.HP, "hp", Stats.ATTACK, "atk", Stats.DEFENCE, "def", Stats.SPECIAL_ATTACK, "spa", Stats.SPECIAL_DEFENCE, "spd", Stats.SPEED, "spe"
   );

   public boolean isReady() {
      return this.ready;
   }

   public String digest() {
      return this.digest;
   }

   public String cachedDigest() {
      return this.speciesTemplate.isEmpty() ? null : this.digest;
   }

   public int size() {
      return this.speciesTemplate.size();
   }

   public RemoteDex.Entry get(String speciesTemplateId) {
      return this.speciesTemplate.get(speciesTemplateId);
   }

   public Collection<RemoteDex.Entry> entries() {
      return this.speciesTemplate.values();
   }

   public void loadFromDisk() {
      if (Files.exists(CACHE_FILE)) {
         try {
            JsonElement parsed = JsonParser.parseString(Files.readString(CACHE_FILE, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) {
               throw new IllegalStateException("not a JSON object");
            }

            this.adopt(parsed.getAsJsonObject());
            LOGGER.info("Restored {} cached species from {} (digest {})", new Object[]{this.speciesTemplate.size(), CACHE_FILE, this.shortDigest()});
         } catch (Exception failure) {
            LOGGER.warn("Could not read {} ({}); the dex will be fetched again", CACHE_FILE, failure.getMessage());
            this.speciesTemplate = Collections.emptyMap();
            this.digest = null;
         }
      }
   }

   public void accept(JsonObject snapshot) {
      if (snapshot.has("unchanged") && snapshot.get("unchanged").getAsBoolean()) {
         this.ready = !this.speciesTemplate.isEmpty();
         if (this.ready) {
            LOGGER.info("The battle host confirmed our cached dex ({} species, digest {})", this.speciesTemplate.size(), this.shortDigest());
         } else {
            LOGGER.warn("The battle host says our dex is unchanged, but nothing is cached");
         }
      } else {
         this.adopt(snapshot);
         this.ready = true;
         LOGGER.info("Cached {} species from the battle host (digest {})", this.speciesTemplate.size(), this.shortDigest());
         this.saveToDisk();
      }
   }

   private void adopt(JsonObject snapshot) {
      JsonArray array = snapshot.getAsJsonArray("species");
      Map<String, RemoteDex.Entry> parsed = new HashMap<>(array.size() * 2);

      for (JsonElement element : array) {
         JsonObject object = element.getAsJsonObject();
         JsonObject stats = object.getAsJsonObject("baseStats");
         Map<String, Integer> baseStats = new HashMap<>(8);

         for (String key : STAT_KEYS) {
            baseStats.put(key, stats.get(key).getAsInt());
         }

         String id = object.get("id").getAsString();
         parsed.put(id, new RemoteDex.Entry(id, Collections.unmodifiableMap(baseStats)));
      }

      this.speciesTemplate = Collections.unmodifiableMap(parsed);
      this.digest = snapshot.has("digest") ? snapshot.get("digest").getAsString() : null;
   }

   private void saveToDisk() {
      if (this.digest != null && !this.speciesTemplate.isEmpty()) {
         try {
            JsonArray array = new JsonArray();

            for (RemoteDex.Entry entry : this.speciesTemplate.values()) {
               JsonObject stats = new JsonObject();

               for (String key : STAT_KEYS) {
                  stats.addProperty(key, entry.baseStats().getOrDefault(key, 0));
               }

               JsonObject object = new JsonObject();
               object.addProperty("id", entry.id());
               object.add("baseStats", stats);
               array.add(object);
            }

            JsonObject document = new JsonObject();
            document.addProperty("digest", this.digest);
            document.add("species", array);
            Path parent = CACHE_FILE.getParent();
            if (parent != null) {
               Files.createDirectories(parent);
            }

            Path temporary = CACHE_FILE.resolveSibling(CACHE_FILE.getFileName() + ".tmp");
            Files.writeString(temporary, GSON.toJson(document), StandardCharsets.UTF_8);
            Files.move(temporary, CACHE_FILE, StandardCopyOption.REPLACE_EXISTING);
         } catch (Exception failure) {
            LOGGER.warn("Could not write {}: {}", CACHE_FILE, failure.getMessage());
         }
      }
   }

   public void suspend(String reason) {
      this.ready = false;
      LOGGER.info("Dex cache suspended: {} ({} species kept for the next handshake)", reason, this.speciesTemplate.size());
   }

   public void invalidate(String reason) {
      this.ready = false;
      this.speciesTemplate = Collections.emptyMap();
      this.digest = null;
      LOGGER.info("Dex cache invalidated: {}", reason);
   }

   private String shortDigest() {
      return this.digest == null ? "?" : this.digest.substring(0, Math.min(12, this.digest.length()));
   }

   public void setStrictBaseStats(boolean strict) {
      this.strictBaseStats = strict;
   }

   public void setTeamRules(boolean strictAbilities, boolean strictMoves, int maxEvPerStat, int maxEvTotal, int maxIv) {
      this.strictAbilities = strictAbilities;
      this.strictMoves = strictMoves;
      this.maxEvPerStat = Math.max(0, maxEvPerStat);
      this.maxEvTotal = Math.max(0, maxEvTotal);
      this.maxIv = Math.max(0, maxIv);
   }

   public List<RemoteDex.Rejection> check(Pokemon creature, int slot) {
      return this.check(creature, slot, true);
   }

   public List<RemoteDex.Rejection> check(Pokemon creature, int slot, boolean dexAuthority) {
      String speciesTemplateId = creature.showdownId();
      Component name = creature.getSpecies().getTranslatedName();
      RemoteDex.Entry entry = this.speciesTemplate.get(speciesTemplateId);
      if (entry == null && dexAuthority) {
         return List.of(new RemoteDex.Rejection(slot, name, speciesTemplateId, RemoteDex.Rejection.Kind.UNKNOWN_SPECIES, Component.literal(speciesTemplateId)));
      } else {
         List<RemoteDex.Rejection> outputStream = new ArrayList<>();
         if (this.strictBaseStats && dexAuthority) {
            Map<Stat, Integer> local = creature.getForm().getBaseStats();
            Stat[] stats = new Stat[]{Stats.HP, Stats.ATTACK, Stats.DEFENCE, Stats.SPECIAL_ATTACK, Stats.SPECIAL_DEFENCE, Stats.SPEED};

            for (int i = 0; i < STAT_KEYS.length; i++) {
               Integer localValue = local.get(stats[i]);
               Integer hostValue = entry.baseStats().get(STAT_KEYS[i]);
               if (localValue == null || hostValue == null || !localValue.equals(hostValue)) {
                  outputStream.add(
                     new RemoteDex.Rejection(
                        slot,
                        name,
                        speciesTemplateId,
                        RemoteDex.Rejection.Kind.BASE_STAT_MISMATCH,
                        Component.literal(STAT_KEYS[i] + ": " + localValue + " / " + hostValue)
                     )
                  );
                  break;
               }
            }
         }

         this.checkAbility(creature, slot, name, speciesTemplateId, outputStream);
         this.checkMoves(creature, slot, name, speciesTemplateId, outputStream);
         this.checkEffortAndIndividual(creature, slot, name, speciesTemplateId, outputStream);
         return outputStream;
      }
   }

   private void checkAbility(Pokemon creature, int slot, Component name, String speciesTemplateId, List<RemoteDex.Rejection> outputStream) {
      if (this.strictAbilities) {
         AbilityTemplate template = creature.getAbility().getTemplate();
         String ability = template.getName();
         String id = showdownId(ability);
         if (!id.isEmpty() && !"noability".equals(id)) {
            for (PotentialAbility potential : creature.getForm().getAbilities()) {
               if (id.equals(showdownId(potential.getTemplate().getName()))) {
                  return;
               }
            }

            outputStream.add(
               new RemoteDex.Rejection(
                  slot, name, speciesTemplateId, RemoteDex.Rejection.Kind.ILLEGAL_ABILITY, Component.translatableWithFallback(template.getDisplayName(), ability)
               )
            );
         }
      }
   }

   private void checkMoves(Pokemon creature, int slot, Component name, String speciesTemplateId, List<RemoteDex.Rejection> outputStream) {
      if (this.strictMoves) {
         Learnset learnset = creature.getForm().getMoves();
         Set<String> legal = new HashSet<>();

         for (MoveTemplate move : learnset.getAllLegalMoves()) {
            legal.add(showdownId(move.getName()));
         }

         for (MoveTemplate move : learnset.getLegacyMoves()) {
            legal.add(showdownId(move.getName()));
         }

         for (MoveTemplate move : learnset.getSpecialMoves()) {
            legal.add(showdownId(move.getName()));
         }

         if (!legal.isEmpty()) {
            for (Move move : creature.getMoveSet().getMoves()) {
               String id = showdownId(move.getTemplate().getName());
               if (!id.isEmpty() && !legal.contains(id)) {
                  outputStream.add(new RemoteDex.Rejection(slot, name, speciesTemplateId, RemoteDex.Rejection.Kind.ILLEGAL_MOVE, move.getTemplate().getDisplayName()));
               }
            }
         }
      }
   }

   private void checkEffortAndIndividual(Pokemon creature, int slot, Component name, String speciesTemplateId, List<RemoteDex.Rejection> outputStream) {
      if (this.maxEvPerStat > 0 || this.maxEvTotal > 0) {
         int total = 0;

         for (Map.Entry<? extends Stat, ? extends Integer> entry : creature.getEvs()) {
            int value = entry.getValue();
            total += value;
            if (this.maxEvPerStat > 0 && value > this.maxEvPerStat) {
               outputStream.add(
                  new RemoteDex.Rejection(
                     slot,
                     name,
                     speciesTemplateId,
                     RemoteDex.Rejection.Kind.EV_OVER_CAP,
                     Component.literal(statName(entry.getKey()) + " " + value + " > " + this.maxEvPerStat)
                  )
               );
            }
         }

         if (this.maxEvTotal > 0 && total > this.maxEvTotal) {
            outputStream.add(
               new RemoteDex.Rejection(
                  slot, name, speciesTemplateId, RemoteDex.Rejection.Kind.EV_OVER_CAP, Component.literal("total " + total + " > " + this.maxEvTotal)
               )
            );
         }
      }

      if (this.maxIv > 0) {
         for (Map.Entry<? extends Stat, ? extends Integer> entryx : creature.getIvs()) {
            int value = entryx.getValue();
            if (value > this.maxIv) {
               outputStream.add(
                  new RemoteDex.Rejection(
                     slot,
                     name,
                     speciesTemplateId,
                     RemoteDex.Rejection.Kind.IV_OVER_CAP,
                     Component.literal(statName(entryx.getKey()) + " " + value + " > " + this.maxIv)
                  )
               );
            }
         }
      }
   }

   private static String statName(Stat stat) {
      String known = STAT_NAMES.get(stat);
      return known != null ? known : showdownId(stat.getIdentifier().getPath());
   }

   private static String showdownId(String raw) {
      return raw == null ? "" : raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
   }

   public JsonArray describeTeam(List<Pokemon> roster) {
      JsonArray meta = new JsonArray();

      for (int i = 0; i < roster.size(); i++) {
         Pokemon creature = roster.get(i);
         Map<Stat, Integer> local = creature.getForm().getBaseStats();
         JsonObject baseStats = new JsonObject();
         baseStats.addProperty("hp", local.getOrDefault(Stats.HP, 1));
         baseStats.addProperty("atk", local.getOrDefault(Stats.ATTACK, 1));
         baseStats.addProperty("def", local.getOrDefault(Stats.DEFENCE, 1));
         baseStats.addProperty("spa", local.getOrDefault(Stats.SPECIAL_ATTACK, 1));
         baseStats.addProperty("spd", local.getOrDefault(Stats.SPECIAL_DEFENCE, 1));
         baseStats.addProperty("spe", local.getOrDefault(Stats.SPEED, 1));
         JsonArray types = new JsonArray();

         for (ElementalType type : creature.getForm().getTypes()) {
            types.add(showdownId(type.getName()));
         }

         JsonObject slot = new JsonObject();
         slot.addProperty("index", i);
         slot.addProperty("speciesId", creature.showdownId());
         slot.add("baseStats", baseStats);
         slot.add("types", types);
         meta.add(slot);
      }

      return meta;
   }

   public record Entry(String id, Map<String, Integer> baseStats) {
   }

   public record Rejection(int slot, Component pokemon, String speciesId, RemoteDex.Rejection.Kind kind, Component detail) {
      public static enum Kind {
         UNKNOWN_SPECIES,
         BASE_STAT_MISMATCH,
         ILLEGAL_ABILITY,
         ILLEGAL_MOVE,
         EV_OVER_CAP,
         IV_OVER_CAP;
      }
   }
}
