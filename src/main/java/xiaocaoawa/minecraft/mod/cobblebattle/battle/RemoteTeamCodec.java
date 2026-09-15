package xiaocaoawa.minecraft.mod.cobblebattle.battle;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.abilities.Abilities;
import com.cobblemon.mod.common.api.abilities.AbilityTemplate;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.Moves;
import com.cobblemon.mod.common.api.pokemon.Natures;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.api.types.tera.TeraType;
import com.cobblemon.mod.common.api.types.tera.TeraTypes;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Gender;
import com.cobblemon.mod.common.pokemon.Nature;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.cobblemon.mod.common.pokemon.properties.BattleCloneProperty;
import com.cobblemon.mod.common.pokemon.properties.UncatchableProperty;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import io.github.rinicesiberia.shadowbattle.battle.PackedTeamValueParsing;
import kotlin.Unit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RemoteTeamCodec {
   private static final Logger LOGGER = LoggerFactory.getLogger("CobbleBattle/Team");
   private static final Stat[] STAT_ORDER = new Stat[]{Stats.HP, Stats.ATTACK, Stats.DEFENCE, Stats.SPECIAL_ATTACK, Stats.SPECIAL_DEFENCE, Stats.SPEED};
   private static volatile Map<String, RemoteTeamCodec.FormLookup> speciesTemplateByShowdownId = null;

   private RemoteTeamCodec() {
   }

   public static void invalidateSpeciesCache() {
      speciesTemplateByShowdownId = null;
   }

   public static void warmSpeciesCache() {
      speciesTemplateLookup();
   }

   private static Map<String, RemoteTeamCodec.FormLookup> speciesTemplateLookup() {
      Map<String, RemoteTeamCodec.FormLookup> cached = speciesTemplateByShowdownId;
      if (cached != null) {
         return cached;
      } else {
         Map<String, RemoteTeamCodec.FormLookup> built = new HashMap<>();

         for (Species speciesTemplate : PokemonSpecies.getSpecies()) {
            built.putIfAbsent(speciesTemplate.showdownId(), new RemoteTeamCodec.FormLookup(speciesTemplate, speciesTemplate.getStandardForm()));

            for (FormData form : speciesTemplate.getForms()) {
               built.putIfAbsent(form.showdownId(), new RemoteTeamCodec.FormLookup(speciesTemplate, form));
            }
         }

         speciesTemplateByShowdownId = built;
         LOGGER.debug("Indexed {} showdown species ids", built.size());
         return built;
      }
   }

   public static List<BattlePokemon> decode(String packed) {
      List<BattlePokemon> roster = new ArrayList<>();

      for (String content : packed.split("]")) {
         if (!content.isBlank()) {
            Pokemon creature = decodeOne(content, roster.size());
            if (creature != null) {
               markAsProp(creature);
               roster.add(new BattlePokemon(creature, creature, entity -> {
                  entity.recallWithAnimation();
                  return Unit.INSTANCE;
               }));
            }
         }
      }

      return roster;
   }

   private static void markAsProp(Pokemon creature) {
      try {
         BattleCloneProperty.INSTANCE.isBattleClone().apply(creature);
         UncatchableProperty.INSTANCE.uncatchable().apply(creature);
      } catch (RuntimeException failure) {
         LOGGER.error("Could not mark a rebuilt opposing Pokemon as a battle prop - it may be left in the world when the battle ends", failure);
      }
   }

   private static Pokemon decodeOne(String content, int slot) {
      String[] f = content.split("\\|", -1);
      if (f.length < 17) {
         LOGGER.error("Remote team slot {} has {} fields, expected at least 17 - skipping", slot, f.length);
         return null;
      } else {
         String speciesTemplateId = f[0];
         RemoteTeamCodec.FormLookup lookup = speciesTemplateLookup().get(speciesTemplateId);
         if (lookup == null) {
            LOGGER.error("Remote team slot {} references unknown species '{}' - this server's data is out of step with the battle host", slot, speciesTemplateId);
            return null;
         } else {
            Pokemon creature = new Pokemon();
            creature.setSpecies(lookup.species());
            creature.setForm(lookup.form());
            UUID uuid = PackedTeamValueParsing.uuidOrNull(f[2]);
            if (uuid != null) {
               creature.setUuid(uuid);
            } else {
               LOGGER.warn("Remote team carried a malformed uuid '{}'", f[2]);
            }

            creature.setLevel(PackedTeamValueParsing.boundedInt(f[15], 1, 1, 100));
            creature.setGender(PackedTeamValueParsing.gender(f[12]));
            creature.setShiny("S".equals(f[14]));
            optional(slot, "nature", () -> {
               Nature nature = f[10].isEmpty() ? null : Natures.getNature(f[10]);
               if (nature != null) {
                  creature.setNature(nature);
               }
            });
            applyStats(creature, f[13], true, slot);
            applyStats(creature, f[11], false, slot);
            optional(slot, "ability", () -> {
               AbilityTemplate ability = f[7].isEmpty() ? null : Abilities.get(f[7]);
               if (ability != null) {
                  creature.updateAbility(ability.create(false, Priority.LOWEST));
               }
            });
            applyMoves(creature, f[8], f[9], slot);
            String[] misc = f[16].split(",", -1);
            if (misc.length > 0 && !misc[0].isEmpty()) {
               optional(
                  slot,
                  "friendship",
                  () -> creature.setFriendship(PackedTeamValueParsing.boundedInt(misc[0], creature.getFriendship(), 0, 255), true)
               );
            }

            if (misc.length > 5 && !misc[5].isEmpty()) {
               optional(slot, "tera type", () -> {
                  TeraType tera = teraType(misc[5]);
                  if (tera != null) {
                     creature.setTeraType(tera);
                  }
               });
            }

            int currentHp = PackedTeamValueParsing.boundedInt(f[3], creature.getMaxHealth(), 0, Integer.MAX_VALUE);
            creature.setCurrentHealth(Math.min(currentHp, creature.getMaxHealth()));
            return creature;
         }
      }
   }

   private static void optional(int slot, String what, Runnable apply) {
      try {
         apply.run();
      } catch (Exception failure) {
         LOGGER.warn("Remote team slot {}: could not apply {} ({}) - carrying on without it", new Object[]{slot, what, failure.toString()});
      }
   }

   private static TeraType teraType(String raw) {
      String value = raw.trim();
      TeraType byName = TeraTypes.getByName(value);
      if (byName != null) {
         return byName;
      } else {
         try {
            return TeraTypes.get(value.toLowerCase(Locale.ROOT));
         } catch (Exception failure) {
            LOGGER.warn("Remote team carried an unusable tera type '{}'", raw);
            return null;
         }
      }
   }

   private static void applyStats(Pokemon creature, String csv, boolean ivs, int slot) {
      if (!csv.isEmpty()) {
         String[] parts = csv.split(",", -1);
         if (parts.length != STAT_ORDER.length) {
            LOGGER.warn("Remote team slot {} has {} {} values, expected {}", new Object[]{slot, parts.length, ivs ? "IV" : "EV", STAT_ORDER.length});
         } else {
            for (int i = 0; i < STAT_ORDER.length; i++) {
               int value = PackedTeamValueParsing.boundedInt(parts[i], 0, 0, ivs ? 31 : 252);
               if (ivs) {
                  creature.getIvs().set(STAT_ORDER[i], value);
               } else {
                  creature.getEvs().set(STAT_ORDER[i], value);
               }
            }
         }
      }
   }

   private static void applyMoves(Pokemon creature, String movesCsv, String ppCsv, int slot) {
      if (!movesCsv.isEmpty()) {
         String[] names = movesCsv.split(",", -1);
         String[] pps = ppCsv.isEmpty() ? new String[0] : ppCsv.split(",", -1);
         creature.getMoveSet().clear();

         for (int i = 0; i < names.length && i < 4; i++) {
            if (!names[i].isEmpty()) {
               MoveTemplate template = Moves.getByName(names[i]);
               if (template == null) {
                  LOGGER.warn("Remote team slot {} has unknown move '{}'", slot, names[i]);
               } else {
                  Move move = template.create();
                  if (i < pps.length && pps[i].contains("/")) {
                     String[] pair = pps[i].split("/", 2);
                     move.setCurrentPp(PackedTeamValueParsing.boundedInt(pair[0], move.getCurrentPp(), 0, 99));
                  }

                  creature.getMoveSet().setMove(i, move);
               }
            }
         }
      }
   }

   private record FormLookup(Species species, FormData form) {
   }
}
