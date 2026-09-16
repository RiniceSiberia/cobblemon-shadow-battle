package xiaocaoawa.minecraft.mod.cobblebattle.battle;

import com.cobblemon.mod.common.entity.npc.NPCEntity;
import io.github.rinicesiberia.shadowbattle.battle.MirrorNpcRuntime;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class MirrorNpc {
   private MirrorNpc() {
   }

   public static boolean isOrphan(Entity entity) {
      return MirrorNpcRuntime.isOrphan(entity);
   }

   public static NPCEntity spawn(ServerPlayer viewer, String opponentName) {
      return MirrorNpcRuntime.spawn(viewer, opponentName);
   }

   public static NPCEntity[] spawnPair(ServerPlayer viewer, String firstName, String secondName) {
      return MirrorNpcRuntime.spawnPair(viewer, firstName, secondName);
   }

   public static void despawn(NPCEntity npc) {
      MirrorNpcRuntime.despawn(npc);
   }
}