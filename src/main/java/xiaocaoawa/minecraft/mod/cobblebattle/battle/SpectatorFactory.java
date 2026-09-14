package xiaocaoawa.minecraft.mod.cobblebattle.battle;

import com.cobblemon.mod.common.CobblemonNetwork;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.battles.BattleFormat;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.battles.BattleSide;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.entity.npc.NPCEntity;
import com.cobblemon.mod.common.net.messages.client.battle.BattleInitializePacket;
import com.cobblemon.mod.common.net.messages.client.battle.BattleMessagePacket;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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

   void begin(JsonObject message) {
      String remoteBattleId = BattleServerClient.str(message, "battleId", null);
      UUID watcher = uuid(BattleServerClient.str(message, "player", null));
      if (remoteBattleId != null && watcher != null) {
         try {
            this.beginOrThrow(message, remoteBattleId, watcher);
         } catch (Throwable var5) {
            LOGGER.error("Seating a spectator in battle {} threw", remoteBattleId, var5);
            this.service.tellPlayer(watcher, Msg.of(ChatFormatting.RED, "battle.spectate_failed"));
         }
      } else {
         LOGGER.error("spectate_start was missing required fields");
      }
   }

   private void beginOrThrow(JsonObject message, String remoteBattleId, UUID watcher) {
      MinecraftServer server = this.service.server();
      ServerPlayer player = server == null ? null : server.getPlayerList().getPlayer(watcher);
      if (player != null) {
         MirrorBattle mirror = CrossServerBattles.byRemoteId(remoteBattleId);
         if (mirror != null) {
            this.attach(mirror, player);
         } else {
            boolean wantsMirror = "mirror".equals(BattleServerClient.str(message, "mode", "mirror"));
            if (!wantsMirror) {
               LOGGER.error("spectate_start says battle {} is local, but this server has no mirror of it", remoteBattleId);
               this.service.tellPlayer(watcher, Msg.of(ChatFormatting.RED, "battle.spectate_failed"));
            } else {
               MirrorBattle built = this.build(message, remoteBattleId, player);
               if (built != null) {
                  this.attach(built, player);
                  JsonObject ack = BattleServerClient.msg("battle_ack");
                  ack.addProperty("battleId", remoteBattleId);
                  this.service.client().send(ack);
                  built.release();
               }
            }
         }
      }
   }

   private MirrorBattle build(JsonObject message, String remoteBattleId, ServerPlayer watching) {
      UUID watcher = watching.getUUID();
      JsonObject teams = message.getAsJsonObject("teams");
      JsonArray seats = message.getAsJsonArray("seats");
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
            List<BattlePokemon> team = RemoteTeamCodec.decode(teams.get(showdownId).getAsString());
            if (team.isEmpty()) {
               LOGGER.error("Could not rebuild the team of {} to watch battle {}", showdownId, remoteBattleId);
               this.service.tellPlayer(watcher, Msg.of(ChatFormatting.RED, "battle.spectate_failed"));
               return null;
            }

            showdownIds[index] = showdownId;
            names[index] = who.get("name").getAsString();
            uuids[index] = UUID.fromString(who.get("uuid").getAsString());
            servers[index] = seat.get("serverId").getAsString();
            squads.set(index, team);
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
                     this.readFormat(message), new BattleSide(new BattleActor[]{actors[0]}), new BattleSide(new BattleActor[]{actors[1]}), false
                  );
                  break label178;
               } catch (RuntimeException var23) {
                  LOGGER.error("Could not build a mirror to watch battle {}", remoteBattleId, var23);

                  for (NPCEntity npc : npcs) {
                     MirrorNpc.despawn(npc);
                  }

                  for (int i = 0; i < 2; i++) {
                     MirrorPokemon.release(squads.get(i));
                  }

                  this.service.tellPlayer(watcher, Msg.of(ChatFormatting.RED, "battle.spectate_failed"));
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

               this.service.tellPlayer(watcher, Msg.of(ChatFormatting.RED, "battle.mixin_missing"));
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
         this.service.tellPlayer(watcher, Msg.of(ChatFormatting.RED, "battle.spectate_failed"));
         return null;
      }
   }

   private void attach(MirrorBattle mirror, ServerPlayer player) {
      PokemonBattle battle = mirror.battle();
      if (battle == null) {
         LOGGER.error("Battle {} has no local battle to watch", mirror.remoteBattleId());
         this.service.tellPlayer(player.getUUID(), Msg.of(ChatFormatting.RED, "battle.spectate_failed"));
      } else {
         mirror.addWatcher(player.getUUID());
         battle.getSpectators().add(player.getUUID());
         CobblemonNetwork.INSTANCE.sendPacket(player, new BattleInitializePacket(battle, battle.getSide1()));
         CobblemonNetwork.INSTANCE.sendPacket(player, new BattleMessagePacket(battle.getChatLog()));
         this.service.tellPlayer(player.getUUID(), Msg.of(ChatFormatting.AQUA, "battle.spectating"));
         ApiEvents.spectateStarted(player, mirror.remoteBattleId());
      }
   }

   void end(JsonObject message) {
      String remoteBattleId = BattleServerClient.str(message, "battleId", "");
      UUID watcher = uuid(BattleServerClient.str(message, "player", null));
      MirrorBattle mirror = CrossServerBattles.byRemoteId(remoteBattleId);
      if (mirror != null && watcher != null) {
         PokemonBattle battle = mirror.battle();
         if (battle != null) {
            battle.getSpectators().remove(watcher);
         }

         boolean wasLast = mirror.removeWatcher(watcher);
         this.service.tellPlayer(watcher, Msg.of(ChatFormatting.YELLOW, "battle.spectate_over"));
         MinecraftServer server = this.service.server();
         ServerPlayer player = server == null ? null : server.getPlayerList().getPlayer(watcher);
         if (player != null) {
            ApiEvents.spectateEnded(player, remoteBattleId);
         }

         if (wasLast && mirror.isSpectator()) {
            this.service.closeSpectatorMirror(mirror);
         }
      }
   }

   private BattleFormat readFormat(JsonObject message) {
      JsonObject described = message.getAsJsonObject("formatJson");
      String battleType = BattleServerClient.str(message, "format", "singles");
      if (described != null) {
         JsonObject type = described.getAsJsonObject("battleType");
         if (type != null) {
            battleType = BattleServerClient.str(type, "name", battleType);
         }
      }

      BattleFormat base = BattleFormat.Companion.fromFormatIdentifier(battleType);
      if (described == null) {
         return base;
      } else {
         Set<String> rules = new LinkedHashSet<>();
         JsonElement ruleSet = described.get("ruleSet");
         if (ruleSet != null && ruleSet.isJsonArray()) {
            for (JsonElement rule : ruleSet.getAsJsonArray()) {
               if (rule.isJsonPrimitive()) {
                  rules.add(rule.getAsString());
               }
            }
         }

         if (rules.isEmpty()) {
            rules = base.getRuleSet();
         }

         int adjustLevel = BattleServerClient.integer(described, "adjustLevel", base.getAdjustLevel());
         return base.copy(base.getMod(), base.getBattleType(), rules, base.getGen(), adjustLevel);
      }
   }

   private static UUID uuid(String raw) {
      if (raw != null && !raw.isEmpty()) {
         try {
            return UUID.fromString(raw);
         } catch (IllegalArgumentException var2) {
            return null;
         }
      } else {
         return null;
      }
   }
}
