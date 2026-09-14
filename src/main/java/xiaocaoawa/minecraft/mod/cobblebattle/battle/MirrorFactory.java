package xiaocaoawa.minecraft.mod.cobblebattle.battle;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.battles.BattleFormat;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.battles.BattleSide;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.entity.npc.NPCEntity;
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
import xiaocaoawa.minecraft.mod.cobblebattle.api.BattleInfo;
import xiaocaoawa.minecraft.mod.cobblebattle.config.ServerIdentity;
import xiaocaoawa.minecraft.mod.cobblebattle.lang.Msg;
import xiaocaoawa.minecraft.mod.cobblebattle.net.BattleServerClient;

final class MirrorFactory {
   private static final Logger LOGGER = LoggerFactory.getLogger("CobbleBattle");
   private final CrossServerBattleService service;

   private BattleFormat readFormat(JsonObject message) {
      JsonObject described = message.getAsJsonObject("formatJson");
      String battleType = "singles";
      if (described != null) {
         JsonObject type = described.getAsJsonObject("battleType");
         if (type != null) {
            battleType = BattleServerClient.str(type, "name", battleType);
         }
      } else {
         battleType = BattleServerClient.str(message, "format", battleType);
      }

      BattleFormat base = BattleFormat.Companion.fromFormatIdentifier(battleType);
      if (described == null) {
         LOGGER.warn("The battle server sent no rulebook for this battle; falling back to plain {}", battleType);
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

   MirrorFactory(CrossServerBattleService service) {
      this.service = service;
   }

   private static List<BattlePokemon> pickedFrom(List<BattlePokemon> team, JsonObject message, String mySeat) {
      JsonObject picks = message.getAsJsonObject("picks");
      JsonElement mine = picks == null ? null : picks.get(mySeat);
      if (mine != null && mine.isJsonArray()) {
         JsonArray array = mine.getAsJsonArray();
         if (array.isEmpty()) {
            return team;
         } else {
            List<BattlePokemon> out = new ArrayList<>(array.size());
            Set<Integer> seen = new LinkedHashSet<>();

            for (JsonElement element : array) {
               int index;
               try {
                  index = element.getAsInt();
               } catch (RuntimeException var12) {
                  LOGGER.warn("A pick for seat {} was not a slot number; using the whole team", mySeat);
                  return team;
               }

               if (index < 0 || index >= team.size() || !seen.add(index)) {
                  LOGGER.warn("Pick {} for seat {} is not a slot in a team of {}; using the whole team", new Object[]{index, mySeat, team.size()});
                  return team;
               }

               out.add(team.get(index));
            }

            return out;
         }
      } else {
         return team;
      }
   }

   void build(JsonObject message) {
      String remoteBattleId = BattleServerClient.str(message, "battleId", null);
      String myPlayerRaw = BattleServerClient.str(message, "yourPlayer", null);

      try {
         this.buildOrThrow(message, remoteBattleId);
      } catch (Throwable var7) {
         LOGGER.error("Building the mirror for battle {} threw", remoteBattleId, var7);
         if (myPlayerRaw != null) {
            try {
               this.service.tellPlayer(UUID.fromString(myPlayerRaw), Msg.of(ChatFormatting.RED, "battle.setup_failed"));
            } catch (RuntimeException var6) {
            }
         }

         if (remoteBattleId != null) {
            this.service.sendAbort(remoteBattleId, "mirror build threw: " + var7.getClass().getSimpleName() + ": " + var7.getMessage());
         }
      }
   }

   private void buildOrThrow(JsonObject message, String remoteBattleId) {
      String mySeat = BattleServerClient.str(message, "yourSeat", null);
      String myPlayerRaw = BattleServerClient.str(message, "yourPlayer", null);
      if (remoteBattleId != null && mySeat != null && myPlayerRaw != null) {
         UUID myPlayerUuid = UUID.fromString(myPlayerRaw);
         JsonObject opponentSeat = null;

         for (JsonElement element : message.getAsJsonArray("seats")) {
            JsonObject seat = element.getAsJsonObject();
            if (!mySeat.equals(seat.get("showdownId").getAsString())) {
               opponentSeat = seat;
            }
         }

         if (opponentSeat == null) {
            LOGGER.error("match_found did not describe an opponent seat");
            this.service.tellPlayer(myPlayerUuid, Msg.of(ChatFormatting.RED, "battle.setup_failed"));
            this.service.sendAbort(remoteBattleId, "no opponent seat");
         } else {
            String opponentSeatId = opponentSeat.get("showdownId").getAsString();
            JsonObject opponentPlayer = opponentSeat.getAsJsonObject("player");
            UUID opponentUuid = UUID.fromString(opponentPlayer.get("uuid").getAsString());
            String opponentName = opponentPlayer.get("name").getAsString();
            String opponentServerId = opponentSeat.get("serverId").getAsString();
            boolean authoritative = message.has("authoritative") && !message.get("authoritative").isJsonNull() && message.get("authoritative").getAsBoolean();
            if (!opponentServerId.equals(ServerIdentity.get())) {
               BattleQueue.QueuedTeam mine = this.service.battleQueue().claim(myPlayerUuid);
               if (mine == null) {
                  LOGGER.error("match_found for {} but we have no queued team for them", myPlayerUuid);
                  this.service.tellPlayer(myPlayerUuid, Msg.of(ChatFormatting.RED, "battle.setup_failed"));
                  this.service.sendAbort(remoteBattleId, "no queued team on this server");
               } else {
                  String opponentPacked = message.getAsJsonObject("teams").get(opponentSeatId).getAsString();
                  List<BattlePokemon> opponentTeam = RemoteTeamCodec.decode(opponentPacked);
                  if (opponentTeam.isEmpty()) {
                     LOGGER.error("Could not rebuild the opposing team for battle {}", remoteBattleId);
                     this.service.tellPlayer(myPlayerUuid, Msg.of(ChatFormatting.RED, "battle.team_rebuild_failed"));
                     this.service.sendAbort(remoteBattleId, "opposing team could not be rebuilt");
                  } else {
                     int expected = BattleServerClient.integer(opponentSeat, "teamSize", opponentTeam.size());
                     if (authoritative && opponentTeam.size() < expected) {
                        LOGGER.error(
                           "Battle {}: rebuilt only {} of {} opposing Pokemon - this server's data cannot run this battle",
                           new Object[]{remoteBattleId, opponentTeam.size(), expected}
                        );
                        this.service.tellPlayer(myPlayerUuid, Msg.of(ChatFormatting.RED, "battle.team_rebuild_failed"));
                        this.service
                           .sendAbort(remoteBattleId, "this server knows only " + opponentTeam.size() + " of the " + expected + " Pokemon on the opposing team");
                     } else {
                        List<BattlePokemon> myTeam = pickedFrom(mine.team(), message, mySeat);
                        if (myTeam.isEmpty()) {
                           LOGGER.error("Battle {}: none of this player's team was picked", remoteBattleId);
                           this.service.tellPlayer(myPlayerUuid, Msg.of(ChatFormatting.RED, "battle.setup_failed"));
                           this.service.sendAbort(remoteBattleId, "no Pokemon left after the team preview");
                        } else {
                           PlayerBattleActor localActor = new PlayerBattleActor(myPlayerUuid, myTeam);
                           MinecraftServer server = this.service.server();
                           ServerPlayer localPlayer = server == null ? null : server.getPlayerList().getPlayer(myPlayerUuid);
                           NPCEntity npc = localPlayer == null ? null : MirrorNpc.spawn(localPlayer, opponentName);
                           RemoteBattleActor remoteActor = (RemoteBattleActor)(npc != null
                              ? new EntityBackedRemoteBattleActor(opponentUuid, opponentName, opponentServerId, opponentSeatId, opponentTeam, npc)
                              : new RemoteBattleActor(opponentUuid, opponentName, opponentServerId, opponentSeatId, opponentTeam));
                           boolean iAmP1 = "p1".equals(mySeat);
                           BattleSide side1 = new BattleSide(new BattleActor[]{(BattleActor)(iAmP1 ? localActor : remoteActor)});
                           BattleSide side2 = new BattleSide(new BattleActor[]{(BattleActor)(iAmP1 ? remoteActor : localActor)});
                           BattleFormat format = this.readFormat(message);
                           MirrorBattle mirror = new MirrorBattle(
                              remoteBattleId, mySeat, opponentSeatId, myPlayerUuid, opponentName, opponentServerId, authoritative, this.service.config().debug
                           );
                           mirror.attachBody(npc, remoteActor);
                           MirrorPokemon.claim(mirror, opponentTeam);
                           CrossServerBattles.beginConstruction(mirror);

                           label164: {
                              try {
                                 BattleRegistry.startBattle(format, side1, side2, false);
                                 break label164;
                              } catch (RuntimeException var35) {
                                 LOGGER.error("Failed to build the mirror battle for {}", remoteBattleId, var35);
                                 MirrorNpc.despawn(npc);
                                 MirrorPokemon.release(opponentTeam);
                                 CrossServerBattles.forget(mirror.localBattleId() == null ? new UUID(0L, 0L) : mirror.localBattleId());
                                 this.service.tellPlayer(myPlayerUuid, Msg.of(ChatFormatting.RED, "battle.setup_failed"));
                                 this.service.sendAbort(remoteBattleId, "mirror construction failed");
                              } finally {
                                 CrossServerBattles.endConstruction();
                              }

                              return;
                           }

                           UUID localBattleId = mirror.localBattleId();
                           if (localBattleId == null) {
                              LOGGER.error("Mirror battle for {} never reached the showdown hook - is the GraalShowdownService mixin applied?", remoteBattleId);
                              MirrorNpc.despawn(npc);
                              MirrorPokemon.release(opponentTeam);
                              this.service.tellPlayer(myPlayerUuid, Msg.of(ChatFormatting.RED, "battle.mixin_missing"));
                              this.service.sendAbort(remoteBattleId, "mixin did not fire - CobbleBattle is not hooked into Cobblemon on this server");
                           } else {
                              PokemonBattle localBattle = BattleRegistry.getBattle(localBattleId);
                              mirror.attach(localBattle);
                              LOGGER.info(
                                 "{} battle {} <-> local {} built: {} vs {}@{}",
                                 new Object[]{authoritative ? "Host-run" : "Mirror", remoteBattleId, localBattleId, mySeat, opponentName, opponentServerId}
                              );
                              BattleInfo info = new BattleInfo(
                                 remoteBattleId,
                                 BattleServerClient.str(message, "ranked", ""),
                                 BattleServerClient.str(message, "rankedName", ""),
                                 BattleServerClient.bool(message, "casual", false),
                                 mySeat,
                                 opponentUuid,
                                 opponentName,
                                 opponentServerId
                              );
                              mirror.describe(info);
                              this.service.tellPlayer(myPlayerUuid, Msg.of(ChatFormatting.AQUA, "battle.found", opponentName, opponentServerId));
                              if (localPlayer != null) {
                                 ApiEvents.battleStarted(localPlayer, info);
                              }

                              JsonObject ack = BattleServerClient.msg("battle_ack");
                              ack.addProperty("battleId", remoteBattleId);
                              this.service.client().send(ack);
                           }
                        }
                     }
                  }
               }
            } else if (CrossServerBattles.byRemoteId(remoteBattleId) != null) {
               LOGGER.debug("match_found for {} in battle {}: the other seat's frame already built it", myPlayerUuid, remoteBattleId);
            } else {
               this.buildLocalPair(message, remoteBattleId, mySeat, myPlayerUuid, opponentSeatId, opponentUuid, opponentName, authoritative);
            }
         }
      } else {
         LOGGER.error("match_found was missing required fields");
      }
   }

   private void buildLocalPair(
      JsonObject message,
      String remoteBattleId,
      String mySeat,
      UUID myPlayerUuid,
      String opponentSeatId,
      UUID opponentUuid,
      String opponentName,
      boolean authoritative
   ) {
      BattleQueue queue = this.service.battleQueue();
      BattleQueue.QueuedTeam mine = queue.claim(myPlayerUuid);
      BattleQueue.QueuedTeam theirs = queue.claim(opponentUuid);
      if (mine != null && theirs != null) {
         List<BattlePokemon> myTeam = pickedFrom(mine.team(), message, mySeat);
         List<BattlePokemon> theirTeam = pickedFrom(theirs.team(), message, opponentSeatId);
         if (!myTeam.isEmpty() && !theirTeam.isEmpty()) {
            MinecraftServer server = this.service.server();
            ServerPlayer me = server == null ? null : server.getPlayerList().getPlayer(myPlayerUuid);
            ServerPlayer them = server == null ? null : server.getPlayerList().getPlayer(opponentUuid);
            String myName = me != null ? me.getGameProfile().getName() : BattleServerClient.str(message, "yourName", "?");
            PlayerBattleActor myActor = new PlayerBattleActor(myPlayerUuid, myTeam);
            PlayerBattleActor theirActor = new PlayerBattleActor(opponentUuid, theirTeam);
            boolean iAmP1 = "p1".equals(mySeat);
            BattleSide side1 = new BattleSide(new BattleActor[]{iAmP1 ? myActor : theirActor});
            BattleSide side2 = new BattleSide(new BattleActor[]{iAmP1 ? theirActor : myActor});
            BattleFormat format = this.readFormat(message);
            String here = ServerIdentity.get();
            MirrorBattle mirror = new MirrorBattle(
               remoteBattleId, mySeat, opponentSeatId, myPlayerUuid, opponentUuid, opponentName, here, authoritative, this.service.config().debug
            );
            CrossServerBattles.beginConstruction(mirror);

            label180: {
               try {
                  BattleRegistry.startBattle(format, side1, side2, false);
                  break label180;
               } catch (RuntimeException var35) {
                  LOGGER.error("Failed to build the two-player battle for {}", remoteBattleId, var35);
                  CrossServerBattles.forget(mirror.localBattleId() == null ? new UUID(0L, 0L) : mirror.localBattleId());

                  for (UUID who : List.of(myPlayerUuid, opponentUuid)) {
                     this.service.tellPlayer(who, Msg.of(ChatFormatting.RED, "battle.setup_failed"));
                  }

                  this.service.sendAbort(remoteBattleId, "mirror construction failed");
               } finally {
                  CrossServerBattles.endConstruction();
               }

               return;
            }

            UUID localBattleId = mirror.localBattleId();
            if (localBattleId != null) {
               mirror.attach(BattleRegistry.getBattle(localBattleId));
               LOGGER.info(
                  "{} battle {} <-> local {} built: {} ({}) vs {} ({}), both on this server",
                  new Object[]{authoritative ? "Host-run" : "Mirror", remoteBattleId, localBattleId, mySeat, myName, opponentSeatId, opponentName}
               );
               String ranked = BattleServerClient.str(message, "ranked", "");
               String rankedName = BattleServerClient.str(message, "rankedName", "");
               boolean casual = BattleServerClient.bool(message, "casual", false);
               BattleInfo myInfo = new BattleInfo(remoteBattleId, ranked, rankedName, casual, mySeat, opponentUuid, opponentName, here);
               BattleInfo theirInfo = new BattleInfo(remoteBattleId, ranked, rankedName, casual, opponentSeatId, myPlayerUuid, myName, here);
               mirror.describe(myInfo);
               mirror.describeSecond(theirInfo);
               this.service.tellPlayer(myPlayerUuid, Msg.of(ChatFormatting.AQUA, "battle.found_local", opponentName));
               this.service.tellPlayer(opponentUuid, Msg.of(ChatFormatting.AQUA, "battle.found_local", myName));
               if (me != null) {
                  ApiEvents.battleStarted(me, myInfo);
               }

               if (them != null) {
                  ApiEvents.battleStarted(them, theirInfo);
               }

               JsonObject ack = BattleServerClient.msg("battle_ack");
               ack.addProperty("battleId", remoteBattleId);
               this.service.client().send(ack);
            } else {
               LOGGER.error("Two-player battle for {} never reached the showdown hook - is the GraalShowdownService mixin applied?", remoteBattleId);

               for (UUID who : List.of(myPlayerUuid, opponentUuid)) {
                  this.service.tellPlayer(who, Msg.of(ChatFormatting.RED, "battle.mixin_missing"));
               }

               this.service.sendAbort(remoteBattleId, "mixin did not fire - CobbleBattle is not hooked into Cobblemon on this server");
            }
         } else {
            LOGGER.error("Battle {}: a side has nothing left after the team preview", remoteBattleId);

            for (UUID who : List.of(myPlayerUuid, opponentUuid)) {
               this.service.tellPlayer(who, Msg.of(ChatFormatting.RED, "battle.setup_failed"));
            }

            this.service.sendAbort(remoteBattleId, "no Pokemon left after the team preview");
         }
      } else {
         LOGGER.error(
            "match_found for {} and {} on this server, but a queued team is missing ({} / {})",
            new Object[]{myPlayerUuid, opponentUuid, mine != null, theirs != null}
         );

         for (UUID who : List.of(myPlayerUuid, opponentUuid)) {
            this.service.tellPlayer(who, Msg.of(ChatFormatting.RED, "battle.setup_failed"));
         }

         this.service.sendAbort(remoteBattleId, "no queued team on this server");
      }
   }
}
