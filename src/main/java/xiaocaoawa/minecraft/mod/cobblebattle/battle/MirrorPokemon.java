package xiaocaoawa.minecraft.mod.cobblebattle.battle;

import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import java.util.List;
import net.minecraft.world.entity.Entity;

public final class MirrorPokemon {
   static final String TAG = "cobblebattle_mirror";

   private MirrorPokemon() {
   }

   static void claim(MirrorBattle mirrorBattle, List<BattlePokemon> roster) {
      MirrorPropEntities.claim(mirrorBattle, roster);
   }

   static void release(List<BattlePokemon> roster) {
      MirrorPropEntities.release(roster);
   }

   public static boolean onEntityAdded(Entity addedEntity) {
      return MirrorPropEntities.onEntityAdded(addedEntity);
   }
}
