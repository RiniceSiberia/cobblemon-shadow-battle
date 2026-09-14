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
import kotlin.Unit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RemoteTeamCodec {
   private static final Logger LOGGER = LoggerFactory.getLogger("CobbleBattle/Team");
   private static final Stat[] STAT_ORDER = new Stat[]{Stats.HP, Stats.ATTACK, Stats.DEFENCE, Stats.SPECIAL_ATTACK, Stats.SPECIAL_DEFENCE, Stats.SPEED};
   private static volatile Map<String, RemoteTeamCodec.FormLookup> speciesByShowdownId = null;

   private RemoteTeamCodec() {
   }

   public static void invalidateSpeciesCache() {
      speciesByShowdownId = null;
   }

   public static void warmSpeciesCache() {
      speciesLookup();
   }

   private static Map<String, RemoteTeamCodec.FormLookup> speciesLookup() {
      Map<String, RemoteTeamCodec.FormLookup> cached = speciesByShowdownId;
      if (cached != null) {
         return cached;
      } else {
         Map<String, RemoteTeamCodec.FormLookup> built = new HashMap<>();

         for (Species species : PokemonSpecies.getSpecies()) {
            built.putIfAbsent(species.showdownId(), new RemoteTeamCodec.FormLookup(species, species.getStandardForm()));

            for (FormData form : species.getForms()) {
               built.putIfAbsent(form.showdownId(), new RemoteTeamCodec.FormLookup(species, form));
            }
         }

         speciesByShowdownId = built;
         LOGGER.debug("Indexed {} showdown species ids", built.size());
         return built;
      }
   }

   public static List<BattlePokemon> decode(String packed) {
      List<BattlePokemon> team = new ArrayList<>();

      for (String chunk : packed.split("]")) {
         if (!chunk.isBlank()) {
            Pokemon pokemon = decodeOne(chunk, team.size());
            if (pokemon != null) {
               markAsProp(pokemon);
               team.add(new BattlePokemon(pokemon, pokemon, entity -> {
                  entity.recallWithAnimation();
                  return Unit.INSTANCE;
               }));
            }
         }
      }

      return team;
   }

   private static void markAsProp(Pokemon pokemon) {
      try {
         BattleCloneProperty.INSTANCE.isBattleClone().apply(pokemon);
         UncatchableProperty.INSTANCE.uncatchable().apply(pokemon);
      } catch (RuntimeException var2) {
         LOGGER.error("Could not mark a rebuilt opposing Pokemon as a battle prop - it may be left in the world when the battle ends", var2);
      }
   }

   private static Pokemon decodeOne(String chunk, int slot) {
      String[] f = chunk.split("\\|", -1);
      if (f.length < 17) {
         LOGGER.error("Remote team slot {} has {} fields, expected at least 17 - skipping", slot, f.length);
         return null;
      } else {
         String speciesId = f[0];
         RemoteTeamCodec.FormLookup lookup = speciesLookup().get(speciesId);
         if (lookup == null) {
            LOGGER.error("Remote team slot {} references unknown species '{}' - this server's data is out of step with the battle host", slot, speciesId);
            return null;
         } else {
            Pokemon pokemon = new Pokemon();
            pokemon.setSpecies(lookup.species());
            pokemon.setForm(lookup.form());
            UUID uuid = parseUuid(f[2]);
            if (uuid != null) {
               pokemon.setUuid(uuid);
            }

            pokemon.setLevel(parseInt(f[15], 1, 1, 100));
            pokemon.setGender(parseGender(f[12]));
            pokemon.setShiny("S".equals(f[14]));
            optional(slot, "nature", () -> {
               Nature nature = f[10].isEmpty() ? null : Natures.getNature(f[10]);
               if (nature != null) {
                  pokemon.setNature(nature);
               }
            });
            applyStats(pokemon, f[13], true, slot);
            applyStats(pokemon, f[11], false, slot);
            optional(slot, "ability", () -> {
               AbilityTemplate ability = f[7].isEmpty() ? null : Abilities.get(f[7]);
               if (ability != null) {
                  pokemon.updateAbility(ability.create(false, Priority.LOWEST));
               }
            });
            applyMoves(pokemon, f[8], f[9], slot);
            String[] misc = f[16].split(",", -1);
            if (misc.length > 0 && !misc[0].isEmpty()) {
               optional(slot, "friendship", () -> pokemon.setFriendship(parseInt(misc[0], pokemon.getFriendship(), 0, 255), true));
            }

            if (misc.length > 5 && !misc[5].isEmpty()) {
               optional(slot, "tera type", () -> {
                  TeraType tera = teraType(misc[5]);
                  if (tera != null) {
                     pokemon.setTeraType(tera);
                  }
               });
            }

            int currentHp = parseInt(f[3], pokemon.getMaxHealth(), 0, Integer.MAX_VALUE);
            pokemon.setCurrentHealth(Math.min(currentHp, pokemon.getMaxHealth()));
            return pokemon;
         }
      }
   }

   private static void optional(int slot, String what, Runnable apply) {
      try {
         apply.run();
      } catch (Exception var4) {
         LOGGER.warn("Remote team slot {}: could not apply {} ({}) - carrying on without it", new Object[]{slot, what, var4.toString()});
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
         } catch (Exception var4) {
            LOGGER.warn("Remote team carried an unusable tera type '{}'", raw);
            return null;
         }
      }
   }

   private static void applyStats(Pokemon pokemon, String csv, boolean ivs, int slot) {
      if (!csv.isEmpty()) {
         String[] parts = csv.split(",", -1);
         if (parts.length != STAT_ORDER.length) {
            LOGGER.warn("Remote team slot {} has {} {} values, expected {}", new Object[]{slot, parts.length, ivs ? "IV" : "EV", STAT_ORDER.length});
         } else {
            for (int i = 0; i < STAT_ORDER.length; i++) {
               int value = parseInt(parts[i], 0, 0, ivs ? 31 : 252);
               if (ivs) {
                  pokemon.getIvs().set(STAT_ORDER[i], value);
               } else {
                  pokemon.getEvs().set(STAT_ORDER[i], value);
               }
            }
         }
      }
   }

   private static void applyMoves(Pokemon pokemon, String movesCsv, String ppCsv, int slot) {
      if (!movesCsv.isEmpty()) {
         String[] names = movesCsv.split(",", -1);
         String[] pps = ppCsv.isEmpty() ? new String[0] : ppCsv.split(",", -1);
         pokemon.getMoveSet().clear();

         for (int i = 0; i < names.length && i < 4; i++) {
            if (!names[i].isEmpty()) {
               MoveTemplate template = Moves.getByName(names[i]);
               if (template == null) {
                  LOGGER.warn("Remote team slot {} has unknown move '{}'", slot, names[i]);
               } else {
                  Move move = template.create();
                  if (i < pps.length && pps[i].contains("/")) {
                     String[] pair = pps[i].split("/", 2);
                     move.setCurrentPp(parseInt(pair[0], move.getCurrentPp(), 0, 99));
                  }

                  pokemon.getMoveSet().setMove(i, move);
               }
            }
         }
      }
   }

   private static Gender parseGender(String raw) {
      return switch (raw) {
         case "M" -> Gender.MALE;
         case "F" -> Gender.FEMALE;
         default -> Gender.GENDERLESS;
      };
   }

   private static UUID parseUuid(String raw) {
      try {
         return UUID.fromString(raw);
      } catch (IllegalArgumentException var2) {
         LOGGER.warn("Remote team carried a malformed uuid '{}'", raw);
         return null;
      }
   }

   private static int parseInt(String raw, int fallback, int min, int max) {
      try {
         return Math.max(min, Math.min(max, Integer.parseInt(raw.trim())));
      } catch (NumberFormatException var5) {
         return fallback;
      }
   }

   private record FormLookup(Species species, FormData form) {
   }
}
