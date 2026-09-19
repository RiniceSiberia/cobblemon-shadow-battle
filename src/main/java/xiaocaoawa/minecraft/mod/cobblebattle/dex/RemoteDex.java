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
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.rinicesiberia.shadowbattle.dex.DexLegalityRules;

public final class RemoteDex {
   private static final Logger DEX_LOGGER = LoggerFactory.getLogger("CobbleBattle/Dex");
   private static final Gson JSON_CODEC = new Gson();
   private static final Path DEX_CACHE_PATH = Paths.get("config", "cobblebattle-dex.json");
   private static final String[] SHOWDOWN_STAT_KEYS = new String[]{"hp", "atk", "def", "spa", "spd", "spe"};
   private final RemoteDexSnapshotState snapshotState = new RemoteDexSnapshotState();
   private volatile boolean enforceBaseStats = true;
   private volatile boolean enforceAbilities = false;
   private volatile boolean enforceMoves = false;
   private volatile int effortValuePerStatLimit = 0;
   private volatile int totalEffortValueLimit = 0;
   private volatile int individualValueLimit = 0;
   private static final Map<Stat, String> SHOWDOWN_STAT_NAMES = Map.of(
      Stats.HP, "hp", Stats.ATTACK, "atk", Stats.DEFENCE, "def", Stats.SPECIAL_ATTACK, "spa", Stats.SPECIAL_DEFENCE, "spd", Stats.SPEED, "spe"
   );

   public boolean isReady() {
      return this.snapshotState.isReady();
   }

   public String digest() {
      return this.snapshotState.digest();
   }

   public String cachedDigest() {
      return this.snapshotState.cachedDigest();
   }

   public int size() {
      return this.snapshotState.size();
   }

   public RemoteDex.Entry get(String speciesTemplateId) {
      return this.snapshotState.get(speciesTemplateId);
   }

   public Collection<RemoteDex.Entry> entries() {
      return this.snapshotState.entries();
   }

   public void loadFromDisk() {
      if (Files.exists(DEX_CACHE_PATH)) {
         try {
            JsonElement cachedDocument = JsonParser.parseString(Files.readString(DEX_CACHE_PATH, StandardCharsets.UTF_8));
            if (!cachedDocument.isJsonObject()) {
               throw new IllegalStateException("not a JSON object");
            }

            this.replaceSnapshot(cachedDocument.getAsJsonObject());
            DEX_LOGGER.info("Restored {} cached species from {} (digest {})", new Object[]{this.snapshotState.size(), DEX_CACHE_PATH, this.snapshotState.shortDigest()});
         } catch (Exception failure) {
            DEX_LOGGER.warn("Could not read {} ({}); the dex will be fetched again", DEX_CACHE_PATH, failure.getMessage());
            this.snapshotState.discardCachedData();
         }
      }
   }

   public void accept(JsonObject snapshotDocument) {
      if (snapshotDocument.has("unchanged") && snapshotDocument.get("unchanged").getAsBoolean()) {
         if (this.snapshotState.confirmUnchanged()) {
            DEX_LOGGER.info("The battle host confirmed our cached dex ({} species, digest {})", this.snapshotState.size(), this.snapshotState.shortDigest());
         } else {
            DEX_LOGGER.warn("The battle host says our dex is unchanged, but nothing is cached");
         }
      } else {
         this.replaceSnapshot(snapshotDocument);
         this.snapshotState.markAccepted();
         DEX_LOGGER.info("Cached {} species from the battle host (digest {})", this.snapshotState.size(), this.snapshotState.shortDigest());
         this.persistSnapshot();
      }
   }

   private void replaceSnapshot(JsonObject snapshotDocument) {
      this.snapshotState.adopt(snapshotDocument);
   }

   private void persistSnapshot() {
      JsonObject snapshotDocument = this.snapshotState.serializableDocument();
      if (snapshotDocument != null) {
         try {
            Path cacheDirectory = DEX_CACHE_PATH.getParent();
            if (cacheDirectory != null) {
               Files.createDirectories(cacheDirectory);
            }

            Path temporaryCachePath = DEX_CACHE_PATH.resolveSibling(DEX_CACHE_PATH.getFileName() + ".tmp");
            Files.writeString(temporaryCachePath, JSON_CODEC.toJson(snapshotDocument), StandardCharsets.UTF_8);
            Files.move(temporaryCachePath, DEX_CACHE_PATH, StandardCopyOption.REPLACE_EXISTING);
         } catch (Exception failure) {
            DEX_LOGGER.warn("Could not write {}: {}", DEX_CACHE_PATH, failure.getMessage());
         }
      }
   }

   public void suspend(String suspensionReason) {
      this.snapshotState.suspend();
      DEX_LOGGER.info("Dex cache suspended: {} ({} species kept for the next handshake)", suspensionReason, this.snapshotState.size());
   }

   public void invalidate(String invalidationReason) {
      this.snapshotState.invalidate();
      DEX_LOGGER.info("Dex cache invalidated: {}", invalidationReason);
   }

   public void setStrictBaseStats(boolean enableBaseStatValidation) {
      this.enforceBaseStats = enableBaseStatValidation;
   }

   public void setTeamRules(boolean enforceAbilities, boolean enforceMoves, int effortValuePerStatLimit, int totalEffortValueLimit, int individualValueLimit) {
      this.enforceAbilities = enforceAbilities;
      this.enforceMoves = enforceMoves;
      this.effortValuePerStatLimit = DexLegalityRules.nonNegativeLimit(effortValuePerStatLimit);
      this.totalEffortValueLimit = DexLegalityRules.nonNegativeLimit(totalEffortValueLimit);
      this.individualValueLimit = DexLegalityRules.nonNegativeLimit(individualValueLimit);
   }

   public List<RemoteDex.Rejection> check(Pokemon creature, int rosterSlot) {
      return this.check(creature, rosterSlot, true);
   }

   public List<RemoteDex.Rejection> check(Pokemon creature, int rosterSlot, boolean enforceDexSnapshot) {
      String speciesTemplateId = creature.showdownId();
      Component speciesDisplayName = creature.getSpecies().getTranslatedName();
      RemoteDex.Entry remoteSpecies = this.snapshotState.get(speciesTemplateId);
      if (remoteSpecies == null && enforceDexSnapshot) {
         return List.of(new RemoteDex.Rejection(rosterSlot, speciesDisplayName, speciesTemplateId, RemoteDex.Rejection.Kind.UNKNOWN_SPECIES, Component.literal(speciesTemplateId)));
      } else {
         List<RemoteDex.Rejection> outputStream = new ArrayList<>();
         if (this.enforceBaseStats && enforceDexSnapshot) {
            Map<Stat, Integer> localBaseStats = creature.getForm().getBaseStats();
            Stat[] statOrder = new Stat[]{Stats.HP, Stats.ATTACK, Stats.DEFENCE, Stats.SPECIAL_ATTACK, Stats.SPECIAL_DEFENCE, Stats.SPEED};

            for (int statIndex = 0; statIndex < SHOWDOWN_STAT_KEYS.length; statIndex++) {
               Integer localStatValue = localBaseStats.get(statOrder[statIndex]);
               Integer remoteStatValue = remoteSpecies.baseStats().get(SHOWDOWN_STAT_KEYS[statIndex]);
               if (localStatValue == null || remoteStatValue == null || !localStatValue.equals(remoteStatValue)) {
                  outputStream.add(
                     new RemoteDex.Rejection(
                        rosterSlot,
                        speciesDisplayName,
                        speciesTemplateId,
                        RemoteDex.Rejection.Kind.BASE_STAT_MISMATCH,
                        Component.literal(SHOWDOWN_STAT_KEYS[statIndex] + ": " + localStatValue + " / " + remoteStatValue)
                     )
                  );
                  break;
               }
            }
         }

         this.validateAbility(creature, rosterSlot, speciesDisplayName, speciesTemplateId, outputStream);
         this.validateMoves(creature, rosterSlot, speciesDisplayName, speciesTemplateId, outputStream);
         this.validateTrainingValues(creature, rosterSlot, speciesDisplayName, speciesTemplateId, outputStream);
         return outputStream;
      }
   }

   private void validateAbility(Pokemon creature, int rosterSlot, Component speciesDisplayName, String speciesTemplateId, List<RemoteDex.Rejection> outputStream) {
      if (this.enforceAbilities) {
         AbilityTemplate selectedAbilityTemplate = creature.getAbility().getTemplate();
         String selectedAbilityId = selectedAbilityTemplate.getName();
         List<String> available = new ArrayList<>();
         for (PotentialAbility availableAbility : creature.getForm().getAbilities()) {
            available.add(availableAbility.getTemplate().getName());
         }
         if (!DexLegalityRules.abilityAllowed(selectedAbilityId, available)) {
            outputStream.add(
               new RemoteDex.Rejection(
                  rosterSlot, speciesDisplayName, speciesTemplateId, RemoteDex.Rejection.Kind.ILLEGAL_ABILITY, Component.translatableWithFallback(selectedAbilityTemplate.getDisplayName(), selectedAbilityId)
               )
            );
         }
      }
   }

   private void validateMoves(Pokemon creature, int rosterSlot, Component speciesDisplayName, String speciesTemplateId, List<RemoteDex.Rejection> outputStream) {
      if (this.enforceMoves) {
         Learnset speciesLearnset = creature.getForm().getMoves();
         List<String> available = new ArrayList<>();

         for (MoveTemplate standardMoveTemplate : speciesLearnset.getAllLegalMoves()) {
            available.add(standardMoveTemplate.getName());
         }

         for (MoveTemplate legacyMoveTemplate : speciesLearnset.getLegacyMoves()) {
            available.add(legacyMoveTemplate.getName());
         }

         for (MoveTemplate specialMoveTemplate : speciesLearnset.getSpecialMoves()) {
            available.add(specialMoveTemplate.getName());
         }

         Set<String> legalMoveIds = DexLegalityRules.legalMoveIds(available);
         for (Move selectedMove : creature.getMoveSet().getMoves()) {
            if (!DexLegalityRules.moveAllowed(selectedMove.getTemplate().getName(), legalMoveIds)) {
               outputStream.add(new RemoteDex.Rejection(rosterSlot, speciesDisplayName, speciesTemplateId, RemoteDex.Rejection.Kind.ILLEGAL_MOVE, selectedMove.getTemplate().getDisplayName()));
            }
         }
      }
   }

   private void validateTrainingValues(Pokemon creature, int rosterSlot, Component speciesDisplayName, String speciesTemplateId, List<RemoteDex.Rejection> outputStream) {
      if (this.effortValuePerStatLimit > 0 || this.totalEffortValueLimit > 0) {
         int totalEffortValues = 0;

         for (Map.Entry<? extends Stat, ? extends Integer> effortEntry : creature.getEvs()) {
            int effortValue = effortEntry.getValue();
            totalEffortValues += effortValue;
            if (DexLegalityRules.exceedsLimit(effortValue, this.effortValuePerStatLimit)) {
               outputStream.add(
                  new RemoteDex.Rejection(
                     rosterSlot,
                     speciesDisplayName,
                     speciesTemplateId,
                     RemoteDex.Rejection.Kind.EV_OVER_CAP,
                     Component.literal(showdownStatName(effortEntry.getKey()) + " " + effortValue + " > " + this.effortValuePerStatLimit)
                  )
               );
            }
         }

         if (DexLegalityRules.exceedsLimit(totalEffortValues, this.totalEffortValueLimit)) {
            outputStream.add(
               new RemoteDex.Rejection(
                  rosterSlot, speciesDisplayName, speciesTemplateId, RemoteDex.Rejection.Kind.EV_OVER_CAP, Component.literal("total " + totalEffortValues + " > " + this.totalEffortValueLimit)
               )
            );
         }
      }

      if (this.individualValueLimit > 0) {
         for (Map.Entry<? extends Stat, ? extends Integer> individualEntry : creature.getIvs()) {
            int individualValue = individualEntry.getValue();
            if (DexLegalityRules.exceedsLimit(individualValue, this.individualValueLimit)) {
               outputStream.add(
                  new RemoteDex.Rejection(
                     rosterSlot,
                     speciesDisplayName,
                     speciesTemplateId,
                     RemoteDex.Rejection.Kind.IV_OVER_CAP,
                     Component.literal(showdownStatName(individualEntry.getKey()) + " " + individualValue + " > " + this.individualValueLimit)
                  )
               );
            }
         }
      }
   }

   private static String showdownStatName(Stat pokemonStat) {
      String knownName = SHOWDOWN_STAT_NAMES.get(pokemonStat);
      return knownName != null ? knownName : normalizeShowdownId(pokemonStat.getIdentifier().getPath());
   }

   private static String normalizeShowdownId(String rawName) {
      return DexLegalityRules.normalizedId(rawName);
   }

   public JsonArray describeTeam(List<Pokemon> roster) {
      JsonArray rosterMetadata = new JsonArray();

      for (int rosterIndex = 0; rosterIndex < roster.size(); rosterIndex++) {
         Pokemon creature = roster.get(rosterIndex);
         Map<Stat, Integer> localBaseStats = creature.getForm().getBaseStats();
         JsonObject serializedBaseStats = new JsonObject();
         serializedBaseStats.addProperty("hp", localBaseStats.getOrDefault(Stats.HP, 1));
         serializedBaseStats.addProperty("atk", localBaseStats.getOrDefault(Stats.ATTACK, 1));
         serializedBaseStats.addProperty("def", localBaseStats.getOrDefault(Stats.DEFENCE, 1));
         serializedBaseStats.addProperty("spa", localBaseStats.getOrDefault(Stats.SPECIAL_ATTACK, 1));
         serializedBaseStats.addProperty("spd", localBaseStats.getOrDefault(Stats.SPECIAL_DEFENCE, 1));
         serializedBaseStats.addProperty("spe", localBaseStats.getOrDefault(Stats.SPEED, 1));
         JsonArray serializedTypes = new JsonArray();

         for (ElementalType elementalType : creature.getForm().getTypes()) {
            serializedTypes.add(normalizeShowdownId(elementalType.getName()));
         }

         JsonObject slotDocument = new JsonObject();
         slotDocument.addProperty("index", rosterIndex);
         slotDocument.addProperty("speciesId", creature.showdownId());
         slotDocument.add("baseStats", serializedBaseStats);
         slotDocument.add("types", serializedTypes);
         rosterMetadata.add(slotDocument);
      }

      return rosterMetadata;
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
