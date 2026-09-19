package xiaocaoawa.minecraft.mod.cobblebattle.battle;

import com.cobblemon.mod.common.api.storage.party.PartyStore;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.util.PlayerExtensionsKt;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.ClickEvent.Action;
import net.minecraft.server.level.ServerPlayer;
import xiaocaoawa.minecraft.mod.cobblebattle.dex.RemoteDex;
import xiaocaoawa.minecraft.mod.cobblebattle.lang.Msg;
import xiaocaoawa.minecraft.mod.cobblebattle.net.BattleServerClient;
import io.github.rinicesiberia.shadowbattle.battle.QueueReferenceBook;
import io.github.rinicesiberia.shadowbattle.battle.QueueRejectionMessage;
import io.github.rinicesiberia.shadowbattle.battle.QueueRules;
import io.github.rinicesiberia.shadowbattle.battle.QueueMessageRules;
import io.github.rinicesiberia.shadowbattle.battle.QueueResponseDecoding;
import io.github.rinicesiberia.shadowbattle.transport.PlayerIdentityPayload;
import io.github.rinicesiberia.shadowbattle.transport.QueueRequests;
import io.github.rinicesiberia.shadowbattle.transport.RoomQueueRequests;

final class MatchmakingQueueCoordinator {
   private final CrossServerBattleService battleService;
   private final QueueReferenceBook references = new QueueReferenceBook();

   MatchmakingQueueCoordinator(CrossServerBattleService battleService) {
      this.battleService = battleService;
   }

   boolean hasWaitingTeam(UUID participantUuid) {
      return this.references.contains(participantUuid);
   }

   MatchmakingQueueCoordinator.ClaimedQueueTeam claimWaitingTeam(UUID participantUuid) {
      QueueReferenceBook.WaitingTeam entry = this.references.claim(participantUuid);
      return entry == null ? null : new MatchmakingQueueCoordinator.ClaimedQueueTeam(participantUuid, entry.getTeam(), entry.getPacked(), entry.getRankedId());
   }

   void discardWaitingTeam(UUID participantUuid) {
      this.references.drop(participantUuid);
   }

   void clearQueueState() {
      this.references.clear();
   }

   UUID claimLookupRequester(JsonElement referenceToken) {
      return referenceToken != null && !referenceToken.isJsonNull() ? this.references.lookup(referenceToken.getAsInt()) : null;
   }

   UUID claimRequestOwner(JsonElement referenceToken) {
      return referenceToken != null && !referenceToken.isJsonNull() ? this.references.owner(referenceToken.getAsInt()) : null;
   }

   private MatchmakingQueueCoordinator.PreparedRoster prepareRoster(ServerPlayer participant) throws MatchmakingQueueCoordinator.QueuePreparationFailure {
      return this.prepareRoster(participant, true, true);
   }

   private MatchmakingQueueCoordinator.PreparedRoster prepareRoster(ServerPlayer participant, boolean dexAuthority, boolean legality) throws MatchmakingQueueCoordinator.QueuePreparationFailure {
      RemoteDex dex = this.battleService.dex();
      if (!this.battleService.isConnected()) {
         throw new MatchmakingQueueCoordinator.QueuePreparationFailure(this.battleService.notConnected("queue.not_connected"));
      } else if (!this.battleService.auth().isSignedIn(participant.getUUID())) {
         throw new MatchmakingQueueCoordinator.QueuePreparationFailure(Msg.of(ChatFormatting.RED, "queue.not_signed_in"));
      } else if (!dex.isReady()) {
         throw new MatchmakingQueueCoordinator.QueuePreparationFailure(Msg.of(ChatFormatting.RED, "queue.dex_not_ready"));
      } else if (this.references.contains(participant.getUUID())) {
         throw new MatchmakingQueueCoordinator.QueuePreparationFailure(Msg.of(ChatFormatting.YELLOW, "queue.already_queued"));
      } else if (BattleRegistry.getBattleByParticipatingPlayer(participant) != null) {
         throw new MatchmakingQueueCoordinator.QueuePreparationFailure(Msg.of(ChatFormatting.RED, "queue.already_in_battle"));
      } else {
         RemoteTeamCodec.warmSpeciesCache();
         PartyStore party = PlayerExtensionsKt.party(participant);
         List<BattlePokemon> roster = party.toBattleTeam(false, false, null);
         if (roster.isEmpty()) {
            throw new MatchmakingQueueCoordinator.QueuePreparationFailure(Msg.of(ChatFormatting.RED, "queue.no_pokemon"));
         } else {
            List<Pokemon> plain = new ArrayList<>(roster.size());
            List<RemoteDex.Rejection> violations = new ArrayList<>();

            for (int i = 0; i < roster.size(); i++) {
               Pokemon creature = roster.get(i).getEffectedPokemon();
               plain.add(creature);
               if (dexAuthority || legality) {
                  violations.addAll(dex.check(creature, i, dexAuthority));
               }
            }

            if (!violations.isEmpty()) {
               throw new MatchmakingQueueCoordinator.QueuePreparationFailure(QueueRejectionMessage.compose(violations));
            } else {
               return new MatchmakingQueueCoordinator.PreparedRoster(roster, BattleRegistry.INSTANCE.packTeam(roster), dex.describeTeam(plain));
            }
         }
      }
   }

   private Component describeSlotShortage(List<BattlePokemon> roster, String battleType) {
      if (battleType != null && !battleType.isEmpty()) {
         int slots = QueueRules.requiredSlots(battleType);
         return roster.size() >= slots
            ? null
            : Msg.of(ChatFormatting.RED, "queue.too_few", Msg.raw("room.type." + battleType.toLowerCase(Locale.ROOT)), slots, roster.size());
      } else {
         return null;
      }
   }

   Component joinCompetitionQueue(ServerPlayer participant, String competitionId) {
      MatchmakingQueueCoordinator.PreparedRoster preparedTeam;
      try {
         preparedTeam = this.prepareRoster(participant);
      } catch (MatchmakingQueueCoordinator.QueuePreparationFailure failure) {
         return failure.message;
      }

      CrossServerBattleService.Ranked rankedCompetition = this.battleService.ranked(competitionId);
      if (rankedCompetition != null) {
         Component slotShortage = this.describeSlotShortage(preparedTeam.team(), rankedCompetition.battleType());
         if (slotShortage != null) {
            return slotShortage;
         }
      }

      JsonObject joinRequest = QueueRequests.join(competitionId);
      return this.sendPreparedRequest(participant, joinRequest, preparedTeam, competitionId);
   }

   Component createBattleRoom(
      ServerPlayer participant, String roomName, String roomPassword, String roomBattleType, int targetLevel, int leadSelectionCount, boolean restoreParty, boolean hostBattleEngine, boolean enforceLegality
   ) {
      MatchmakingQueueCoordinator.PreparedRoster preparedTeam;
      try {
         preparedTeam = this.prepareRoster(participant, !hostBattleEngine, !hostBattleEngine || enforceLegality);
      } catch (MatchmakingQueueCoordinator.QueuePreparationFailure failure) {
         return failure.message;
      }

      Component slotShortage = this.describeSlotShortage(preparedTeam.team(), roomBattleType);
      if (slotShortage != null) {
         return slotShortage;
      } else {
         JsonObject createRequest = RoomQueueRequests.create(roomName, roomPassword, roomBattleType, targetLevel, leadSelectionCount, restoreParty, hostBattleEngine, enforceLegality);
         return this.sendPreparedRequest(participant, createRequest, preparedTeam, "");
      }
   }

   Component joinBattleRoom(ServerPlayer participant, String roomIdentifier, String roomPassword, String roomBattleType, boolean hostBattleEngine, boolean enforceLegality, String roomInviteCode) {
      MatchmakingQueueCoordinator.PreparedRoster preparedTeam;
      try {
         preparedTeam = this.prepareRoster(participant, !hostBattleEngine, !hostBattleEngine || enforceLegality);
      } catch (MatchmakingQueueCoordinator.QueuePreparationFailure failure) {
         return failure.message;
      }

      Component slotShortage = this.describeSlotShortage(preparedTeam.team(), roomBattleType);
      if (slotShortage != null) {
         return slotShortage;
      } else {
         JsonObject joinRequest = RoomQueueRequests.join(roomIdentifier, roomPassword, roomInviteCode);
         return this.sendPreparedRequest(participant, joinRequest, preparedTeam, "");
      }
   }

   Component findRoomByInvite(ServerPlayer participant, String roomInviteCode) {
      String normalizedInviteCode = roomInviteCode == null ? "" : roomInviteCode.trim();
      if (normalizedInviteCode.isEmpty()) {
         return Msg.of(ChatFormatting.RED, "room.invite_empty");
      } else if (!this.battleService.isConnected()) {
         return this.battleService.notConnected("queue.not_connected");
      } else if (!this.battleService.auth().isSignedIn(participant.getUUID())) {
         return Msg.of(ChatFormatting.RED, "queue.not_signed_in");
      } else {
         BattleServerClient remoteClient = this.battleService.serverClient();
         int requestReference = remoteClient.nextRef();
         JsonObject lookupRequest = RoomQueueRequests.lookup(requestReference, participant.getUUID(), participant.getGameProfile().getName(), normalizedInviteCode);
         if (!this.references.sendLookup(requestReference, participant.getUUID(), () -> remoteClient.send(lookupRequest))) {
            return Msg.of(ChatFormatting.RED, "queue.send_failed");
         } else {
            return null;
         }
      }
   }

   void onRoomBattleDetails(JsonObject document) {
      UUID participantUuid = this.claimLookupRequester(document.get("ref"));
      if (participantUuid != null) {
         QueueResponseDecoding.RoomBattleDetails details = QueueResponseDecoding.roomBattleDetails(document);
         this.battleService.onServerThreadWithParticipant(participantUuid, participant -> {
            Component refusal = this.joinBattleRoom(
               participant, details.getRoomId(), "", details.getBattleType(), details.getHostEngine(), details.getLegality(), details.getInviteCode()
            );
            if (refusal != null) {
               this.battleService.tellParticipant(participantUuid, refusal);
            }
         });
      }
   }

   Component leaveBattleRoom(ServerPlayer participant) {
      BattleServerClient remoteClient = this.battleService.serverClient();
      JsonObject leaveRequest = RoomQueueRequests.leave(remoteClient.nextRef(), participant.getUUID(), participant.getGameProfile().getName());
      remoteClient.send(leaveRequest);
      return null;
   }

   Component startBattleRoom(ServerPlayer participant) {
      BattleServerClient remoteClient = this.battleService.serverClient();
      int requestReference = remoteClient.nextRef();
      JsonObject startRequest = RoomQueueRequests.start(requestReference, participant.getUUID(), participant.getGameProfile().getName());
      if (!this.references.sendOwner(requestReference, participant.getUUID(), () -> remoteClient.send(startRequest))) {
         return Msg.of(ChatFormatting.RED, "queue.send_failed");
      } else {
         return null;
      }
   }

   void handleRoomClosed(JsonObject document) {
      this.claimRequestOwner(document.get("ref"));
      QueueResponseDecoding.RoomClosed closed = QueueResponseDecoding.roomClosed(document);
      this.references.drop(closed.getParticipant());

      String messageKey = QueueMessageRules.roomClosedKey(closed.getReason());
      if (messageKey != null) {
         this.battleService.tellParticipant(closed.getParticipant(), Msg.of(ChatFormatting.YELLOW, messageKey));
      }
   }

   private Component sendPreparedRequest(ServerPlayer participant, JsonObject request, MatchmakingQueueCoordinator.PreparedRoster preparedTeam, String rankedId) {
      BattleServerClient client = this.battleService.serverClient();
      int ref = client.nextRef();
      request.addProperty("ref", ref);
      request.add("player", PlayerIdentityPayload.create(participant.getUUID(), participant.getGameProfile().getName()));
      request.addProperty("team", preparedTeam.packed());
      request.add("teamMeta", preparedTeam.meta());
      QueueReferenceBook.WaitingTeam waiting = new QueueReferenceBook.WaitingTeam(
         participant.getUUID(), preparedTeam.team(), preparedTeam.packed(), rankedId
      );
      if (!this.references.sendWaiting(waiting, ref, () -> client.send(request))) {
         return Msg.of(ChatFormatting.RED, "queue.send_failed");
      } else {
         return null;
      }
   }

   void handleRoomCreated(JsonObject document) {
      this.claimRequestOwner(document.get("ref"));
      QueueResponseDecoding.RoomCreated created = QueueResponseDecoding.roomCreated(document);
      this.battleService.tellParticipant(created.getParticipant(), Msg.of(ChatFormatting.GREEN, "room.created", created.getName()));
      if (!created.getInviteCode().isEmpty()) {
         this.battleService
            .tellParticipant(
               created.getParticipant(),
               Msg.of(ChatFormatting.AQUA, "room.invite_code", created.getInviteCode())
                  .withStyle(
                     messageStyle -> messageStyle.withClickEvent(new ClickEvent(Action.COPY_TO_CLIPBOARD, created.getInviteCode()))
                        .withHoverEvent(new HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, Msg.of("room.invite_copy_hint")))
                  )
            );
      }
   }

   Component leaveMatchmakingQueue(ServerPlayer participant) {
      if (!this.references.contains(participant.getUUID())) {
         return Msg.of(ChatFormatting.YELLOW, "queue.not_in_queue");
      } else {
         BattleServerClient remoteClient = this.battleService.serverClient();
         JsonObject leaveRequest = QueueRequests.leave(remoteClient.nextRef(), participant.getUUID(), participant.getGameProfile().getName());
         remoteClient.send(leaveRequest);
         return null;
      }
   }

   void onParticipantDisconnect(ServerPlayer participant) {
      if (this.references.forgetParticipant(participant.getUUID())) {
         BattleServerClient client = this.battleService.serverClient();
         if (client != null) {
            JsonObject leave = QueueRequests.leave(null, participant.getUUID(), participant.getGameProfile().getName());
            client.send(leave);
         }
      }
   }

   Component evaluatePartyCompatibility(ServerPlayer participant) {
      PartyStore partyStore = PlayerExtensionsKt.party(participant);
      List<BattlePokemon> roster = partyStore.toBattleTeam(false, false, null);
      if (roster.isEmpty()) {
         return Msg.of(ChatFormatting.YELLOW, "check.no_pokemon");
      } else {
         List<RemoteDex.Rejection> violations = new ArrayList<>();

         for (int partySlot = 0; partySlot < roster.size(); partySlot++) {
            violations.addAll(this.battleService.dex().check(roster.get(partySlot).getEffectedPokemon(), partySlot));
         }

         return (Component)(violations.isEmpty() ? Msg.of(ChatFormatting.GREEN, "check.all_ok", roster.size()) : QueueRejectionMessage.compose(violations));
      }
   }

   void handleQueueAccepted(JsonObject document) {
      this.claimRequestOwner(document.get("ref"));
      QueueResponseDecoding.QueueAccepted accepted = QueueResponseDecoding.queueAccepted(document);
      this.battleService
         .tellParticipant(
            accepted.getParticipant(),
            accepted.getCompetitionName().isEmpty()
               ? Msg.of(ChatFormatting.GREEN, "queue.acked", accepted.getWaiting())
               : Msg.of(ChatFormatting.GREEN, "queue.acked_ranked", accepted.getCompetitionName(), accepted.getWaiting())
         );
      this.battleService.onServerThreadWithParticipant(
         accepted.getParticipant(),
         participant -> ApiEvents.queueJoined(participant, accepted.getCompetitionId(), accepted.getCompetitionName(), accepted.getWaiting())
      );
   }

   void handleQueueDeparted(JsonObject document) {
      QueueResponseDecoding.QueueDeparted departed = QueueResponseDecoding.queueDeparted(document);
      this.references.drop(departed.getParticipant());
      if (departed.getWasQueued()) {
         String messageKey = QueueMessageRules.queueLeftKey(departed.getReason());
         this.battleService.tellParticipant(departed.getParticipant(), Msg.of(ChatFormatting.YELLOW, messageKey));
         this.battleService.onServerThreadWithParticipant(
            departed.getParticipant(), participant -> ApiEvents.queueLeft(participant, departed.getReason())
         );
      }
   }

   void handleQueuePosition(JsonObject document) {
      QueueResponseDecoding.QueuePosition queuePosition = QueueResponseDecoding.queuePosition(document);
      this.battleService.tellParticipant(
         queuePosition.getParticipant(), Msg.of(ChatFormatting.YELLOW, "queue.waiting", queuePosition.getPosition(), queuePosition.getWaiting())
      );
   }

   private record PreparedRoster(List<BattlePokemon> team, String packed, JsonArray meta) {
   }

   record ClaimedQueueTeam(UUID playerUuid, List<BattlePokemon> team, String packed, String rankedId) {
   }

   private static final class QueuePreparationFailure extends Exception {
      final Component message;

      QueuePreparationFailure(Component message) {
         super(null, null, false, false);
         this.message = message;
      }
   }
}
