package xiaocaoawa.minecraft.mod.cobblebattle.battle;

import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import io.github.rinicesiberia.shadowbattle.battle.RemoteRosterAssembly;
import java.util.List;

public final class RemoteTeamCodec {
   private RemoteTeamCodec() {
   }

   public static void invalidateSpeciesCache() {
      RemoteRosterAssembly.invalidateSpeciesCache();
   }

   public static void warmSpeciesCache() {
      RemoteRosterAssembly.warmSpeciesCache();
   }

   public static List<BattlePokemon> decode(String packed) {
      return RemoteRosterAssembly.decode(packed);
   }
}