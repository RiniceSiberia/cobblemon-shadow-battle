package xiaocaoawa.minecraft.mod.cobblebattle.battle;

import com.cobblemon.mod.common.entity.npc.NPCEntity;
import io.github.rinicesiberia.shadowbattle.battle.MirrorNpcRuntime;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class MirrorNpc {
   private MirrorNpc() {
   }

   public static boolean isOrphan(Entity candidateEntity) {
      return MirrorNpcRuntime.isOrphan(candidateEntity);
   }

   public static NPCEntity spawn(ServerPlayer observer, String opponentName) {
      return MirrorNpcRuntime.spawn(observer, opponentName);
   }

   public static NPCEntity[] spawnPair(ServerPlayer observer, String firstOpponentName, String secondOpponentName) {
      return MirrorNpcRuntime.spawnPair(observer, firstOpponentName, secondOpponentName);
   }

   public static void despawn(NPCEntity npcEntity) {
      MirrorNpcRuntime.despawn(npcEntity);
   }
}
