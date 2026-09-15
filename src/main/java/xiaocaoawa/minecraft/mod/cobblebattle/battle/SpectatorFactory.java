package xiaocaoawa.minecraft.mod.cobblebattle.battle;

import com.cobblemon.mod.common.CobblemonNetwork;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.battles.BattleSide;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.entity.npc.NPCEntity;
import com.cobblemon.mod.common.net.messages.client.battle.BattleInitializePacket;
import com.cobblemon.mod.common.net.messages.client.battle.BattleMessagePacket;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.rinicesiberia.shadowbattle.battle.BattleFormatResolver;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xiaocaoawa.minecraft.mod.cobblebattle.lang.Msg;
import xiaocaoawa.minecraft.mod.cobblebattle.net.BattleServerClient;

final class SpectatorFactory {
   private static final Logger LOGGER = LoggerFactory.getLogger("CobbleBattle/Spectate");
   private final CrossServerBattleService service;

   SpectatorFactory(CrossServerBattleService service) {
      this.service = service;
   }

   void begin(JsonObject document) {
      String remoteBattleId = BattleServerClient.str(document, "battleId", null);
      UUID watcher = uuid(BattleServerClient.str(document, "player", null));
      if (remoteBattleId != null && watcher != null) {
         try {
            this.beginOrThrow(document, remoteBattleId, watcher);
         } catch (Throwable failure) {
            LOGGER.error("Seating a spectator in battle {} threw", remoteBattleId, failure);
            this.service.tellParticipant(watcher, Msg.of(ChatFormatting.RED, "battle.spectate_failed"));
         }
      } else {
         LOGGER.error("spectate_start was missing required fields");
      }
   }

   private void beginOrThrow(JsonObject document, String remoteBattleId, UUID watcher) {
      MinecraftServer server = this.service.server();
      ServerPlayer participant = server == null ? null : server.getPlayerList().getPlayer(watcher);
      if (participant != null) {
         MirrorBattle mirror = CrossServerBattles.byRemoteId(remoteBattleId);
         if (mirror != null) {
            this.attach(mirror, participant);
         } else {
            boolean wantsMirror = "mirror".equals(BattleServerClient.str(document, "mode", "mirror"));
            if (!wantsMirror) {
               LOGGER.error("spectate_start says battle {} is local, but this server has no mirror of it", remoteBattleId);
               this.service.tellParticipant(watcher, Msg.of(ChatFormatting.RED, "battle.spectate_failed"));
            } else {
               MirrorBattle built = this.build(document, remoteBattleId, participant);
               if (built != null) {
                  this.attach(built, participant);
                  JsonObject ack = BattleServerClient.msg("battle_ack");
                  ack.addProperty("battleId", remoteBattleId);
                  this.service.client().send(ack);
                  built.release();
               }
            }
         }
      }
   }

   private MirrorBattle build(JsonObject document, String remoteBattleId, ServerPlayer watching) {
      UUID watcher = watching.getUUID();
      JsonObject teams = document.getAsJsonObject("teams");
      JsonArray seats = document.getAsJsonArray("seats");
      if (teams != null && seats != null && seats.size() >= 2) {
         String[] names = new String[2];
         UUID[] uuids = new UUID[2];
         String[] servers = new String[2];
         String[] showdownIds = new String[2];
         List<List<BattlePokemon>> squads = new ArrayList<>();
         squads.add(List.of());
         squads.add(List.of());

         for (JsonElement element : seats) {
            JsonObject seat = element.getAsJsonObject();
            String showdownId = seat.get("showdownId").getAsString();
            int index = "p1".equals(showdownId) ? 0 : 1;
            JsonObject who = seat.getAsJsonObject("player");
            List<BattlePokemon> roster = RemoteTeamCodec.decode(teams.get(showdownId).getAsString());
            if (roster.isEmpty()) {
               LOGGER.error("Could not rebuild the team of {} to watch battle {}", showdownId, remoteBattleId);
               this.service.tellParticipant(watcher, Msg.of(ChatFormatting.RED, "battle.spectate_failed"));
               return null;
            }

            showdownIds[index] = showdownId;
            names[index] = who.get("name").getAsString();
            uuids[index] = UUID.fromString(who.get("uuid").getAsString());
            servers[index] = seat.get("serverId").getAsString();
            squads.set(index, roster);
         }

         if (showdownIds[0] != null && showdownIds[1] != null) {
            NPCEntity[] npcs = MirrorNpc.spawnPair(watching, names[0], names[1]);
            RemoteBattleActor[] actors = new RemoteBattleActor[2];

            for (int i = 0; i < 2; i++) {
               actors[i] = (RemoteBattleActor)(npcs[i] != null
                  ? new EntityBackedRemoteBattleActor(uuids[i], names[i], servers[i], showdownIds[i], squads.get(i), npcs[i])
                  : new RemoteBattleActor(uuids[i], names[i], servers[i], showdownIds[i], squads.get(i)));
            }

            MirrorBattle mirror = MirrorBattle.spectator(remoteBattleId, this.service.config().debug);

            for (int i = 0; i < 2; i++) {
               MirrorPokemon.claim(mirror, squads.get(i));
            }

            CrossServerBattles.beginConstruction(mirror);

            label178: {
               Object var33;
               try {
                  BattleRegistry.startBattle(
                     BattleFormatResolver.resolveSpectator(document),
                     new BattleSide(new BattleActor[]{actors[0]}),
                     new BattleSide(new BattleActor[]{actors[1]}),
                     false
                  );
                  break label178;
               } catch (RuntimeException failure) {
                  LOGGER.error("Could not build a mirror to watch battle {}", remoteBattleId, failure);

                  for (NPCEntity npc : npcs) {
                     MirrorNpc.despawn(npc);
                  }

                  for (int i = 0; i < 2; i++) {
                     MirrorPokemon.release(squads.get(i));
                  }

                  this.service.tellParticipant(watcher, Msg.of(ChatFormatting.RED, "battle.spectate_failed"));
                  var33 = null;
               } finally {
                  CrossServerBattles.endConstruction();
               }

               return (MirrorBattle)var33;
            }

            UUID localBattleId = mirror.localBattleId();
            if (localBattleId == null) {
               LOGGER.error("Mirror for watching {} never reached the showdown hook", remoteBattleId);

               for (NPCEntity npc : npcs) {
                  MirrorNpc.despawn(npc);
               }

               for (int i = 0; i < 2; i++) {
                  MirrorPokemon.release(squads.get(i));
               }

               this.service.tellParticipant(watcher, Msg.of(ChatFormatting.RED, "battle.mixin_missing"));
               return null;
            } else {
               for (int i = 0; i < 2; i++) {
                  mirror.attachBody(npcs[i], actors[i]);
               }

               mirror.attach(BattleRegistry.getBattle(localBattleId));
               LOGGER.info("Built a spectator mirror of battle {} (local {})", remoteBattleId, localBattleId);
               return mirror;
            }
         } else {
            LOGGER.error("spectate_start for {} did not name both seats", remoteBattleId);
            return null;
         }
      } else {
         LOGGER.error("spectate_start for {} did not describe the battle", remoteBattleId);
         this.service.tellParticipant(watcher, Msg.of(ChatFormatting.RED, "battle.spectate_failed"));
         return null;
      }
   }

   private void attach(MirrorBattle mirror, ServerPlayer participant) {
      PokemonBattle battle = mirror.battle();
      if (battle == null) {
         LOGGER.error("Battle {} has no local battle to watch", mirror.remoteBattleId());
         this.service.tellParticipant(participant.getUUID(), Msg.of(ChatFormatting.RED, "battle.spectate_failed"));
      } else {
         mirror.addWatcher(participant.getUUID());
         battle.getSpectators().add(participant.getUUID());
         CobblemonNetwork.INSTANCE.sendPacket(participant, new BattleInitializePacket(battle, battle.getSide1()));
         CobblemonNetwork.INSTANCE.sendPacket(participant, new BattleMessagePacket(battle.getChatLog()));
         this.service.tellParticipant(participant.getUUID(), Msg.of(ChatFormatting.AQUA, "battle.spectating"));
         ApiEvents.spectateStarted(participant, mirror.remoteBattleId());
      }
   }

   void end(JsonObject document) {
      String remoteBattleId = BattleServerClient.str(document, "battleId", "");
      UUID watcher = uuid(BattleServerClient.str(document, "player", null));
      MirrorBattle mirror = CrossServerBattles.byRemoteId(remoteBattleId);
      if (mirror != null && watcher != null) {
         PokemonBattle battle = mirror.battle();
         if (battle != null) {
            battle.getSpectators().remove(watcher);
         }

         boolean wasLast = mirror.removeWatcher(watcher);
         this.service.tellParticipant(watcher, Msg.of(ChatFormatting.YELLOW, "battle.spectate_over"));
         MinecraftServer server = this.service.server();
         ServerPlayer participant = server == null ? null : server.getPlayerList().getPlayer(watcher);
         if (participant != null) {
            ApiEvents.spectateEnded(participant, remoteBattleId);
         }

         if (wasLast && mirror.isSpectator()) {
            this.service.closeReplayViewMirror(mirror);
         }
      }
   }

   private static UUID uuid(String raw) {
      if (raw != null && !raw.isEmpty()) {
         try {
            return UUID.fromString(raw);
         } catch (IllegalArgumentException failure) {
            return null;
         }
      } else {
         return null;
      }
   }
}
