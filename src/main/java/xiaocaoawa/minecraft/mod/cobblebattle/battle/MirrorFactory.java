package xiaocaoawa.minecraft.mod.cobblebattle.battle;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.battles.BattleFormat;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.battles.BattleSide;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.entity.npc.NPCEntity;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
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
import io.github.rinicesiberia.shadowbattle.battle.BattleFormatResolver;
import io.github.rinicesiberia.shadowbattle.battle.TeamSelection;

final class MirrorFactory {
   private static final Logger LOGGER = LoggerFactory.getLogger("CobbleBattle");
   private final CrossServerBattleService service;

   MirrorFactory(CrossServerBattleService service) {
      this.service = service;
   }

   void build(JsonObject document) {
      String remoteBattleId = BattleServerClient.str(document, "battleId", null);
      String myParticipantRaw = BattleServerClient.str(document, "yourPlayer", null);

      try {
         this.buildOrThrow(document, remoteBattleId);
      } catch (Throwable failure) {
         LOGGER.error("Building the mirror for battle {} threw", remoteBattleId, failure);
         if (myParticipantRaw != null) {
            try {
               this.service.tellParticipant(UUID.fromString(myParticipantRaw), Msg.of(ChatFormatting.RED, "battle.setup_failed"));
            } catch (RuntimeException var6) {
            }
         }

         if (remoteBattleId != null) {
            this.service.sendAbort(remoteBattleId, "mirror build threw: " + failure.getClass().getSimpleName() + ": " + failure.getMessage());
         }
      }
   }

   private void buildOrThrow(JsonObject document, String remoteBattleId) {
      String mySeat = BattleServerClient.str(document, "yourSeat", null);
      String myParticipantRaw = BattleServerClient.str(document, "yourPlayer", null);
      if (remoteBattleId != null && mySeat != null && myParticipantRaw != null) {
         UUID myParticipantUuid = UUID.fromString(myParticipantRaw);
         JsonObject opponentSeat = null;

         for (JsonElement element : document.getAsJsonArray("seats")) {
            JsonObject seat = element.getAsJsonObject();
            if (!mySeat.equals(seat.get("showdownId").getAsString())) {
               opponentSeat = seat;
            }
         }

         if (opponentSeat == null) {
            LOGGER.error("match_found did not describe an opponent seat");
            this.service.tellParticipant(myParticipantUuid, Msg.of(ChatFormatting.RED, "battle.setup_failed"));
            this.service.sendAbort(remoteBattleId, "no opponent seat");
         } else {
            String opponentSeatId = opponentSeat.get("showdownId").getAsString();
            JsonObject opponentParticipant = opponentSeat.getAsJsonObject("player");
            UUID opponentUuid = UUID.fromString(opponentParticipant.get("uuid").getAsString());
            String opponentName = opponentParticipant.get("name").getAsString();
            String opponentServerId = opponentSeat.get("serverId").getAsString();
            boolean sourceOfTruth = document.has("authoritative") && !document.get("authoritative").isJsonNull() && document.get("authoritative").getAsBoolean();
            if (!opponentServerId.equals(ServerIdentity.get())) {
               BattleQueue.QueuedTeam mine = this.service.battleQueue().claim(myParticipantUuid);
               if (mine == null) {
                  LOGGER.error("match_found for {} but we have no queued team for them", myParticipantUuid);
                  this.service.tellParticipant(myParticipantUuid, Msg.of(ChatFormatting.RED, "battle.setup_failed"));
                  this.service.sendAbort(remoteBattleId, "no queued team on this server");
               } else {
                  String opponentPacked = document.getAsJsonObject("teams").get(opponentSeatId).getAsString();
                  List<BattlePokemon> opponentRoster = RemoteTeamCodec.decode(opponentPacked);
                  if (opponentRoster.isEmpty()) {
                     LOGGER.error("Could not rebuild the opposing team for battle {}", remoteBattleId);
                     this.service.tellParticipant(myParticipantUuid, Msg.of(ChatFormatting.RED, "battle.team_rebuild_failed"));
                     this.service.sendAbort(remoteBattleId, "opposing team could not be rebuilt");
                  } else {
                     int expected = BattleServerClient.integer(opponentSeat, "teamSize", opponentRoster.size());
                     if (sourceOfTruth && opponentRoster.size() < expected) {
                        LOGGER.error(
                           "Battle {}: rebuilt only {} of {} opposing Pokemon - this server's data cannot run this battle",
                           new Object[]{remoteBattleId, opponentRoster.size(), expected}
                        );
                        this.service.tellParticipant(myParticipantUuid, Msg.of(ChatFormatting.RED, "battle.team_rebuild_failed"));
                        this.service
                           .sendAbort(remoteBattleId, "this server knows only " + opponentRoster.size() + " of the " + expected + " Pokemon on the opposing team");
                     } else {
                        List<BattlePokemon> myRoster = TeamSelection.choose(mine.team(), document, mySeat);
                        if (myRoster.isEmpty()) {
                           LOGGER.error("Battle {}: none of this player's team was picked", remoteBattleId);
                           this.service.tellParticipant(myParticipantUuid, Msg.of(ChatFormatting.RED, "battle.setup_failed"));
                           this.service.sendAbort(remoteBattleId, "no Pokemon left after the team preview");
                        } else {
                           PlayerBattleActor localActor = new PlayerBattleActor(myParticipantUuid, myRoster);
                           MinecraftServer server = this.service.server();
                           ServerPlayer localParticipant = server == null ? null : server.getPlayerList().getPlayer(myParticipantUuid);
                           NPCEntity npc = localParticipant == null ? null : MirrorNpc.spawn(localParticipant, opponentName);
                           RemoteBattleActor remoteActor = (RemoteBattleActor)(npc != null
                              ? new EntityBackedRemoteBattleActor(opponentUuid, opponentName, opponentServerId, opponentSeatId, opponentRoster, npc)
                              : new RemoteBattleActor(opponentUuid, opponentName, opponentServerId, opponentSeatId, opponentRoster));
                           boolean iAmP1 = "p1".equals(mySeat);
                           BattleSide side1 = new BattleSide(new BattleActor[]{(BattleActor)(iAmP1 ? localActor : remoteActor)});
                           BattleSide side2 = new BattleSide(new BattleActor[]{(BattleActor)(iAmP1 ? remoteActor : localActor)});
                           BattleFormat format = BattleFormatResolver.resolve(document);
                           MirrorBattle mirror = new MirrorBattle(
                              remoteBattleId, mySeat, opponentSeatId, myParticipantUuid, opponentName, opponentServerId, sourceOfTruth, this.service.config().debug
                           );
                           mirror.attachBody(npc, remoteActor);
                           MirrorPokemon.claim(mirror, opponentRoster);
                           CrossServerBattles.beginConstruction(mirror);

                           label164: {
                              try {
                                 BattleRegistry.startBattle(format, side1, side2, false);
                                 break label164;
                              } catch (RuntimeException failure) {
                                 LOGGER.error("Failed to build the mirror battle for {}", remoteBattleId, failure);
                                 MirrorNpc.despawn(npc);
                                 MirrorPokemon.release(opponentRoster);
                                 CrossServerBattles.forget(mirror.localBattleId() == null ? new UUID(0L, 0L) : mirror.localBattleId());
                                 this.service.tellParticipant(myParticipantUuid, Msg.of(ChatFormatting.RED, "battle.setup_failed"));
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
                              MirrorPokemon.release(opponentRoster);
                              this.service.tellParticipant(myParticipantUuid, Msg.of(ChatFormatting.RED, "battle.mixin_missing"));
                              this.service.sendAbort(remoteBattleId, "mixin did not fire - CobbleBattle is not hooked into Cobblemon on this server");
                           } else {
                              PokemonBattle localBattle = BattleRegistry.getBattle(localBattleId);
                              mirror.attach(localBattle);
                              LOGGER.info(
                                 "{} battle {} <-> local {} built: {} vs {}@{}",
                                 new Object[]{sourceOfTruth ? "Host-run" : "Mirror", remoteBattleId, localBattleId, mySeat, opponentName, opponentServerId}
                              );
                              BattleInfo battleDetails = new BattleInfo(
                                 remoteBattleId,
                                 BattleServerClient.str(document, "ranked", ""),
                                 BattleServerClient.str(document, "rankedName", ""),
                                 BattleServerClient.bool(document, "casual", false),
                                 mySeat,
                                 opponentUuid,
                                 opponentName,
                                 opponentServerId
                              );
                              mirror.describe(battleDetails);
                              this.service.tellParticipant(myParticipantUuid, Msg.of(ChatFormatting.AQUA, "battle.found", opponentName, opponentServerId));
                              if (localParticipant != null) {
                                 ApiEvents.battleStarted(localParticipant, battleDetails);
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
               LOGGER.debug("match_found for {} in battle {}: the other seat's frame already built it", myParticipantUuid, remoteBattleId);
            } else {
               this.buildLocalPair(document, remoteBattleId, mySeat, myParticipantUuid, opponentSeatId, opponentUuid, opponentName, sourceOfTruth);
            }
         }
      } else {
         LOGGER.error("match_found was missing required fields");
      }
   }

   private void buildLocalPair(
      JsonObject document,
      String remoteBattleId,
      String mySeat,
      UUID myParticipantUuid,
      String opponentSeatId,
      UUID opponentUuid,
      String opponentName,
      boolean sourceOfTruth
   ) {
      BattleQueue queue = this.service.battleQueue();
      BattleQueue.QueuedTeam mine = queue.claim(myParticipantUuid);
      BattleQueue.QueuedTeam theirs = queue.claim(opponentUuid);
      if (mine != null && theirs != null) {
         List<BattlePokemon> myRoster = TeamSelection.choose(mine.team(), document, mySeat);
         List<BattlePokemon> theirRoster = TeamSelection.choose(theirs.team(), document, opponentSeatId);
         if (!myRoster.isEmpty() && !theirRoster.isEmpty()) {
            MinecraftServer server = this.service.server();
            ServerPlayer me = server == null ? null : server.getPlayerList().getPlayer(myParticipantUuid);
            ServerPlayer them = server == null ? null : server.getPlayerList().getPlayer(opponentUuid);
            String myName = me != null ? me.getGameProfile().getName() : BattleServerClient.str(document, "yourName", "?");
            PlayerBattleActor myActor = new PlayerBattleActor(myParticipantUuid, myRoster);
            PlayerBattleActor theirActor = new PlayerBattleActor(opponentUuid, theirRoster);
            boolean iAmP1 = "p1".equals(mySeat);
            BattleSide side1 = new BattleSide(new BattleActor[]{iAmP1 ? myActor : theirActor});
            BattleSide side2 = new BattleSide(new BattleActor[]{iAmP1 ? theirActor : myActor});
            BattleFormat format = BattleFormatResolver.resolve(document);
            String here = ServerIdentity.get();
            MirrorBattle mirror = new MirrorBattle(
               remoteBattleId, mySeat, opponentSeatId, myParticipantUuid, opponentUuid, opponentName, here, sourceOfTruth, this.service.config().debug
            );
            CrossServerBattles.beginConstruction(mirror);

            label180: {
               try {
                  BattleRegistry.startBattle(format, side1, side2, false);
                  break label180;
               } catch (RuntimeException failure) {
                  LOGGER.error("Failed to build the two-player battle for {}", remoteBattleId, failure);
                  CrossServerBattles.forget(mirror.localBattleId() == null ? new UUID(0L, 0L) : mirror.localBattleId());

                  for (UUID who : List.of(myParticipantUuid, opponentUuid)) {
                     this.service.tellParticipant(who, Msg.of(ChatFormatting.RED, "battle.setup_failed"));
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
                  new Object[]{sourceOfTruth ? "Host-run" : "Mirror", remoteBattleId, localBattleId, mySeat, myName, opponentSeatId, opponentName}
               );
               String ranked = BattleServerClient.str(document, "ranked", "");
               String rankedName = BattleServerClient.str(document, "rankedName", "");
               boolean casual = BattleServerClient.bool(document, "casual", false);
               BattleInfo myBattleDetails = new BattleInfo(remoteBattleId, ranked, rankedName, casual, mySeat, opponentUuid, opponentName, here);
               BattleInfo theirBattleDetails = new BattleInfo(remoteBattleId, ranked, rankedName, casual, opponentSeatId, myParticipantUuid, myName, here);
               mirror.describe(myBattleDetails);
               mirror.describeSecond(theirBattleDetails);
               this.service.tellParticipant(myParticipantUuid, Msg.of(ChatFormatting.AQUA, "battle.found_local", opponentName));
               this.service.tellParticipant(opponentUuid, Msg.of(ChatFormatting.AQUA, "battle.found_local", myName));
               if (me != null) {
                  ApiEvents.battleStarted(me, myBattleDetails);
               }

               if (them != null) {
                  ApiEvents.battleStarted(them, theirBattleDetails);
               }

               JsonObject ack = BattleServerClient.msg("battle_ack");
               ack.addProperty("battleId", remoteBattleId);
               this.service.client().send(ack);
            } else {
               LOGGER.error("Two-player battle for {} never reached the showdown hook - is the GraalShowdownService mixin applied?", remoteBattleId);

               for (UUID who : List.of(myParticipantUuid, opponentUuid)) {
                  this.service.tellParticipant(who, Msg.of(ChatFormatting.RED, "battle.mixin_missing"));
               }

               this.service.sendAbort(remoteBattleId, "mixin did not fire - CobbleBattle is not hooked into Cobblemon on this server");
            }
         } else {
            LOGGER.error("Battle {}: a side has nothing left after the team preview", remoteBattleId);

            for (UUID who : List.of(myParticipantUuid, opponentUuid)) {
               this.service.tellParticipant(who, Msg.of(ChatFormatting.RED, "battle.setup_failed"));
            }

            this.service.sendAbort(remoteBattleId, "no Pokemon left after the team preview");
         }
      } else {
         LOGGER.error(
            "match_found for {} and {} on this server, but a queued team is missing ({} / {})",
            new Object[]{myParticipantUuid, opponentUuid, mine != null, theirs != null}
         );

         for (UUID who : List.of(myParticipantUuid, opponentUuid)) {
            this.service.tellParticipant(who, Msg.of(ChatFormatting.RED, "battle.setup_failed"));
         }

         this.service.sendAbort(remoteBattleId, "no queued team on this server");
      }
   }
}
