package xiaocaoawa.minecraft.mod.cobblebattle.battle;

import com.cobblemon.mod.common.api.battles.model.actor.EntityBackedBattleActor;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.entity.npc.NPCEntity;
import java.util.List;
import java.util.UUID;
import net.minecraft.world.phys.Vec3;

public final class EntityBackedRemoteBattleActor extends RemoteBattleActor implements EntityBackedBattleActor<NPCEntity> {
   private final NPCEntity npc;
   private final Vec3 initialPos;

   public EntityBackedRemoteBattleActor(UUID playerUuid, String displayName, String originServerId, String seat, List<BattlePokemon> pokemon, NPCEntity npc) {
      super(playerUuid, displayName, originServerId, seat, pokemon);
      if (npc == null) {
         throw new IllegalArgumentException("an entity-backed mirror actor needs an entity");
      } else {
         this.npc = npc;
         this.initialPos = npc.position();
      }
   }

   public NPCEntity getEntity() {
      return this.npc;
   }

   public Vec3 getInitialPos() {
      return this.initialPos;
   }
}
