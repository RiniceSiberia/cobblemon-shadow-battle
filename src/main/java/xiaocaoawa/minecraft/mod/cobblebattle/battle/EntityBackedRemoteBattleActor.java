package xiaocaoawa.minecraft.mod.cobblebattle.battle;

import com.cobblemon.mod.common.api.battles.model.actor.EntityBackedBattleActor;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.entity.npc.NPCEntity;
import java.util.List;
import java.util.UUID;
import net.minecraft.world.phys.Vec3;

public final class EntityBackedRemoteBattleActor extends RemoteBattleActor implements EntityBackedBattleActor<NPCEntity> {
   private final NPCEntity backingNpc;
   private final Vec3 initialPosition;

   public EntityBackedRemoteBattleActor(UUID participantUuid, String displayName, String originServerId, String seat, List<BattlePokemon> creature, NPCEntity npcEntity) {
      super(participantUuid, displayName, originServerId, seat, creature);
      if (npcEntity == null) {
         throw new IllegalArgumentException("an entity-backed mirror actor needs an entity");
      } else {
         this.backingNpc = npcEntity;
         this.initialPosition = npcEntity.position();
      }
   }

   public NPCEntity getEntity() {
      return this.backingNpc;
   }

   public Vec3 getInitialPos() {
      return this.initialPosition;
   }
}
