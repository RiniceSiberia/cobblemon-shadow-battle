package xiaocaoawa.minecraft.mod.cobblebattle.battle;

import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MirrorPokemon {
   private static final Logger LOGGER = LoggerFactory.getLogger("CobbleBattle/Mirror");
   static final String TAG = "cobblebattle_mirror";
   private static final Map<UUID, MirrorBattle> LIVE = new ConcurrentHashMap<>();

   private MirrorPokemon() {
   }

   static void claim(MirrorBattle mirror, List<BattlePokemon> team) {
      for (BattlePokemon battlePokemon : team) {
         Pokemon pokemon = battlePokemon.getEffectedPokemon();
         if (pokemon != null) {
            LIVE.put(pokemon.getUuid(), mirror);
         }
      }
   }

   static void release(List<BattlePokemon> team) {
      for (BattlePokemon battlePokemon : team) {
         Pokemon pokemon = battlePokemon.getEffectedPokemon();
         if (pokemon != null) {
            LIVE.remove(pokemon.getUuid());
         }
      }
   }

   public static boolean onEntityAdded(Entity entity) {
      if (entity instanceof PokemonEntity pokemonEntity) {
         Pokemon pokemon = pokemonEntity.getPokemon();
         MirrorBattle mirror = pokemon == null ? null : LIVE.get(pokemon.getUuid());
         if (mirror != null) {
            entity.addTag("cobblebattle_mirror");
            mirror.attachProp(pokemonEntity);
            return false;
         } else if (!entity.getTags().contains("cobblebattle_mirror")) {
            return false;
         } else {
            LOGGER.info("Removed a leftover mirror Pokemon ({}) at {}", pokemon == null ? "?" : pokemon.getSpecies().getName(), entity.blockPosition());
            entity.discard();
            return true;
         }
      } else {
         return false;
      }
   }
}
