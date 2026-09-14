package xiaocaoawa.minecraft.mod.cobblebattle.battle;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

final class MirrorTeardown {
   static final long RECALL_GRACE_MS = 2000L;
   private final CrossServerBattleService service;

   MirrorTeardown(CrossServerBattleService service) {
      this.service = service;
   }

   void sweepEntities(MirrorBattle mirror, long delayMs) {
      List<MirrorBattle.Body> bodies = mirror.takeBodies();
      List<PokemonEntity> props = mirror.takeProps();
      if (!bodies.isEmpty() || !props.isEmpty()) {
         for (MirrorBattle.Body body : bodies) {
            if (body.actor() != null) {
               MirrorPokemon.release(body.actor().mirrorTeam());
            }
         }

         MinecraftServer server = this.service.server();
         if (server != null) {
            Runnable sweep = () -> server.execute(() -> {
               for (PokemonEntity entity : props) {
                  if (!entity.isRemoved()) {
                     entity.discard();
                  }
               }

               for (MirrorBattle.Body bodyx : bodies) {
                  RemoteBattleActor actor = bodyx.actor();
                  if (actor != null) {
                     for (BattlePokemon battleCreature : actor.mirrorTeam()) {
                        PokemonEntity entityx = battleCreature.getEntity();
                        if (entityx != null && !entityx.isRemoved()) {
                           entityx.discard();
                        }
                     }
                  }

                  MirrorNpc.despawn(bodyx.npc());
               }
            });
            if (delayMs <= 0L) {
               sweep.run();
            } else {
               CompletableFuture.runAsync(sweep, CompletableFuture.delayedExecutor(delayMs, TimeUnit.MILLISECONDS));
            }
         }
      }
   }

   void abort(MirrorBattle mirror, Component reason) {
      mirror.markFinished();
      UUID localId = mirror.localBattleId();
      if (localId != null) {
         CrossServerBattles.forget(localId);
         MinecraftServer server = this.service.server();
         if (server != null) {
            server.execute(() -> {
               PokemonBattle battle = BattleRegistry.getBattle(localId);
               if (battle != null && !battle.getEnded()) {
                  for (BattleActor actor : battle.getActors()) {
                     actor.sendMessage(reason);
                  }

                  battle.end();
                  BattleRegistry.closeBattle(battle);
               }
            });
         }
      }

      this.sweepEntities(mirror, 0L);

      for (UUID seated : mirror.localPlayers()) {
         this.service.tellParticipant(seated, reason);
      }
   }
}
