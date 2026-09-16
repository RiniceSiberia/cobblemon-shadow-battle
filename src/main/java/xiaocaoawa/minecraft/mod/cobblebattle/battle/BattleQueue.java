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
import io.github.rinicesiberia.shadowbattle.transport.PlayerIdentityPayload;
import io.github.rinicesiberia.shadowbattle.transport.QueueRequests;
import io.github.rinicesiberia.shadowbattle.transport.RoomQueueRequests;

final class BattleQueue {
   private final CrossServerBattleService service;
   private final QueueReferenceBook references = new QueueReferenceBook();

   BattleQueue(CrossServerBattleService service) {
      this.service = service;
   }

   boolean contains(UUID participantUuid) {
      return this.references.contains(participantUuid);
   }

   BattleQueue.QueuedTeam claim(UUID participantUuid) {
      QueueReferenceBook.WaitingTeam entry = this.references.claim(participantUuid);
      return entry == null ? null : new BattleQueue.QueuedTeam(participantUuid, entry.getTeam(), entry.getPacked(), entry.getRankedId());
   }

   void drop(UUID participantUuid) {
      this.references.drop(participantUuid);
   }

   void clear() {
      this.references.clear();
   }

   UUID claimLookupRef(JsonElement ref) {
      return ref != null && !ref.isJsonNull() ? this.references.lookup(ref.getAsInt()) : null;
   }

   UUID claimRefOwner(JsonElement ref) {
      return ref != null && !ref.isJsonNull() ? this.references.owner(ref.getAsInt()) : null;
   }

   private BattleQueue.PreparedRoster prepareRoster(ServerPlayer participant) throws BattleQueue.QueuePreparationFailure {
      return this.prepareRoster(participant, true, true);
   }

   private BattleQueue.PreparedRoster prepareRoster(ServerPlayer participant, boolean dexAuthority, boolean legality) throws BattleQueue.QueuePreparationFailure {
      RemoteDex dex = this.service.dex();
      if (!this.service.isConnected()) {
         throw new BattleQueue.QueuePreparationFailure(this.service.notConnected("queue.not_connected"));
      } else if (!this.service.auth().isSignedIn(participant.getUUID())) {
         throw new BattleQueue.QueuePreparationFailure(Msg.of(ChatFormatting.RED, "queue.not_signed_in"));
      } else if (!dex.isReady()) {
         throw new BattleQueue.QueuePreparationFailure(Msg.of(ChatFormatting.RED, "queue.dex_not_ready"));
      } else if (this.references.contains(participant.getUUID())) {
         throw new BattleQueue.QueuePreparationFailure(Msg.of(ChatFormatting.YELLOW, "queue.already_queued"));
      } else if (BattleRegistry.getBattleByParticipatingPlayer(participant) != null) {
         throw new BattleQueue.QueuePreparationFailure(Msg.of(ChatFormatting.RED, "queue.already_in_battle"));
      } else {
         RemoteTeamCodec.warmSpeciesCache();
         PartyStore party = PlayerExtensionsKt.party(participant);
         List<BattlePokemon> roster = party.toBattleTeam(false, false, null);
         if (roster.isEmpty()) {
            throw new BattleQueue.QueuePreparationFailure(Msg.of(ChatFormatting.RED, "queue.no_pokemon"));
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
               throw new BattleQueue.QueuePreparationFailure(QueueRejectionMessage.compose(violations));
            } else {
               return new BattleQueue.PreparedRoster(roster, BattleRegistry.INSTANCE.packTeam(roster), dex.describeTeam(plain));
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

   Component join(ServerPlayer participant, String rankedId) {
      BattleQueue.PreparedRoster preparedTeam;
      try {
         preparedTeam = this.prepareRoster(participant);
      } catch (BattleQueue.QueuePreparationFailure failure) {
         return failure.message;
      }

      CrossServerBattleService.Ranked competition = this.service.ranked(rankedId);
      if (competition != null) {
         Component tooFew = this.describeSlotShortage(preparedTeam.team(), competition.battleType());
         if (tooFew != null) {
            return tooFew;
         }
      }

      JsonObject join = QueueRequests.join(rankedId);
      return this.sendPreparedRequest(participant, join, preparedTeam, rankedId);
   }

   Component createRoom(
      ServerPlayer participant, String name, String password, String battleType, int level, int pick, boolean fullHeal, boolean hostEngine, boolean legality
   ) {
      BattleQueue.PreparedRoster preparedTeam;
      try {
         preparedTeam = this.prepareRoster(participant, !hostEngine, !hostEngine || legality);
      } catch (BattleQueue.QueuePreparationFailure failure) {
         return failure.message;
      }

      Component tooFew = this.describeSlotShortage(preparedTeam.team(), battleType);
      if (tooFew != null) {
         return tooFew;
      } else {
         JsonObject create = RoomQueueRequests.create(name, password, battleType, level, pick, fullHeal, hostEngine, legality);
         return this.sendPreparedRequest(participant, create, preparedTeam, "");
      }
   }

   Component joinRoom(ServerPlayer participant, String roomId, String password, String battleType, boolean hostEngine, boolean legality, String inviteCode) {
      BattleQueue.PreparedRoster preparedTeam;
      try {
         preparedTeam = this.prepareRoster(participant, !hostEngine, !hostEngine || legality);
      } catch (BattleQueue.QueuePreparationFailure failure) {
         return failure.message;
      }

      Component tooFew = this.describeSlotShortage(preparedTeam.team(), battleType);
      if (tooFew != null) {
         return tooFew;
      } else {
         JsonObject join = RoomQueueRequests.join(roomId, password, inviteCode);
         return this.sendPreparedRequest(participant, join, preparedTeam, "");
      }
   }

   Component lookupRoom(ServerPlayer participant, String inviteCode) {
      String code = inviteCode == null ? "" : inviteCode.trim();
      if (code.isEmpty()) {
         return Msg.of(ChatFormatting.RED, "room.invite_empty");
      } else if (!this.service.isConnected()) {
         return this.service.notConnected("queue.not_connected");
      } else if (!this.service.auth().isSignedIn(participant.getUUID())) {
         return Msg.of(ChatFormatting.RED, "queue.not_signed_in");
      } else {
         BattleServerClient client = this.service.client();
         int ref = client.nextRef();
         JsonObject lookup = RoomQueueRequests.lookup(ref, participant.getUUID(), participant.getGameProfile().getName(), code);
         if (!this.references.sendLookup(ref, participant.getUUID(), () -> client.send(lookup))) {
            return Msg.of(ChatFormatting.RED, "queue.send_failed");
         } else {
            return null;
         }
      }
   }

   void onRoomBattleDetails(JsonObject document) {
      UUID participantUuid = this.claimLookupRef(document.get("ref"));
      if (participantUuid != null) {
         String roomId = BattleServerClient.str(document, "id", "");
         String inviteCode = BattleServerClient.str(document, "inviteCode", "");
         String battleType = BattleServerClient.bool(document, "fighting", false) ? "" : BattleServerClient.str(document, "battleType", "singles");
         boolean hostEngine = "host".equals(BattleServerClient.str(document, "engine", "server"));
         boolean legality = BattleServerClient.bool(document, "legality", true);
         this.service.onServerThreadWithParticipant(participantUuid, participant -> {
            Component refusal = this.joinRoom(participant, roomId, "", battleType, hostEngine, legality, inviteCode);
            if (refusal != null) {
               this.service.tellParticipant(participantUuid, refusal);
            }
         });
      }
   }

   Component leaveRoom(ServerPlayer participant) {
      BattleServerClient client = this.service.client();
      JsonObject leave = RoomQueueRequests.leave(client.nextRef(), participant.getUUID(), participant.getGameProfile().getName());
      client.send(leave);
      return null;
   }

   Component startRoom(ServerPlayer participant) {
      BattleServerClient client = this.service.client();
      int ref = client.nextRef();
      JsonObject start = RoomQueueRequests.start(ref, participant.getUUID(), participant.getGameProfile().getName());
      if (!this.references.sendOwner(ref, participant.getUUID(), () -> client.send(start))) {
         return Msg.of(ChatFormatting.RED, "queue.send_failed");
      } else {
         return null;
      }
   }

   void onRoomClosed(JsonObject document) {
      this.claimRefOwner(document.get("ref"));
      UUID participantUuid = UUID.fromString(BattleServerClient.str(document, "player", ""));
      this.references.drop(participantUuid);
      String why = BattleServerClient.str(document, "why", "");

      String key = QueueMessageRules.roomClosedKey(why);
      if (key != null) {
         this.service.tellParticipant(participantUuid, Msg.of(ChatFormatting.YELLOW, key));
      }
   }

   private Component sendPreparedRequest(ServerPlayer participant, JsonObject request, BattleQueue.PreparedRoster preparedTeam, String rankedId) {
      BattleServerClient client = this.service.client();
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

   void onRoomCreated(JsonObject document) {
      this.claimRefOwner(document.get("ref"));
      UUID participantUuid = UUID.fromString(BattleServerClient.str(document, "player", ""));
      String name = BattleServerClient.str(document, "name", "");
      this.service.tellParticipant(participantUuid, Msg.of(ChatFormatting.GREEN, "room.created", name));
      String inviteCode = BattleServerClient.str(document, "inviteCode", "");
      if (!inviteCode.isEmpty()) {
         this.service
            .tellParticipant(
               participantUuid,
               Msg.of(ChatFormatting.AQUA, "room.invite_code", inviteCode)
                  .withStyle(
                     style -> style.withClickEvent(new ClickEvent(Action.COPY_TO_CLIPBOARD, inviteCode))
                        .withHoverEvent(new HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, Msg.of("room.invite_copy_hint")))
                  )
            );
      }
   }

   Component leave(ServerPlayer participant) {
      if (!this.references.contains(participant.getUUID())) {
         return Msg.of(ChatFormatting.YELLOW, "queue.not_in_queue");
      } else {
         BattleServerClient client = this.service.client();
         JsonObject leave = QueueRequests.leave(client.nextRef(), participant.getUUID(), participant.getGameProfile().getName());
         client.send(leave);
         return null;
      }
   }

   void onParticipantDisconnect(ServerPlayer participant) {
      if (this.references.forgetParticipant(participant.getUUID())) {
         BattleServerClient client = this.service.client();
         if (client != null) {
            JsonObject leave = QueueRequests.leave(null, participant.getUUID(), participant.getGameProfile().getName());
            client.send(leave);
         }
      }
   }

   Component describePartyCompatibility(ServerPlayer participant) {
      PartyStore party = PlayerExtensionsKt.party(participant);
      List<BattlePokemon> roster = party.toBattleTeam(false, false, null);
      if (roster.isEmpty()) {
         return Msg.of(ChatFormatting.YELLOW, "check.no_pokemon");
      } else {
         List<RemoteDex.Rejection> violations = new ArrayList<>();

         for (int i = 0; i < roster.size(); i++) {
            violations.addAll(this.service.dex().check(roster.get(i).getEffectedPokemon(), i));
         }

         return (Component)(violations.isEmpty() ? Msg.of(ChatFormatting.GREEN, "check.all_ok", roster.size()) : QueueRejectionMessage.compose(violations));
      }
   }

   void onQueueAck(JsonObject document) {
      this.claimRefOwner(document.get("ref"));
      UUID participantUuid = UUID.fromString(BattleServerClient.str(document, "player", ""));
      int waiting = BattleServerClient.integer(document, "waiting", 1);
      String competition = BattleServerClient.str(document, "rankedName", "");
      this.service
         .tellParticipant(
            participantUuid,
            competition.isEmpty()
               ? Msg.of(ChatFormatting.GREEN, "queue.acked", waiting)
               : Msg.of(ChatFormatting.GREEN, "queue.acked_ranked", competition, waiting)
         );
      String rankedId = BattleServerClient.str(document, "ranked", "");
      this.service.onServerThreadWithParticipant(participantUuid, participant -> ApiEvents.queueJoined(participant, rankedId, competition, waiting));
   }

   void onQueueLeft(JsonObject document) {
      UUID participantUuid = UUID.fromString(BattleServerClient.str(document, "player", ""));
      this.references.drop(participantUuid);
      if (BattleServerClient.bool(document, "wasQueued", false)) {
         String why = BattleServerClient.str(document, "why", "");

         String key = QueueMessageRules.queueLeftKey(why);
         this.service.tellParticipant(participantUuid, Msg.of(ChatFormatting.YELLOW, key));
         this.service.onServerThreadWithParticipant(participantUuid, participant -> ApiEvents.queueLeft(participant, why));
      }
   }

   void onQueueWait(JsonObject document) {
      UUID participantUuid = UUID.fromString(BattleServerClient.str(document, "player", ""));
      int position = document.has("position") ? document.get("position").getAsInt() : 0;
      int waiting = document.has("waiting") ? document.get("waiting").getAsInt() : 0;
      this.service.tellParticipant(participantUuid, Msg.of(ChatFormatting.YELLOW, "queue.waiting", position, waiting));
   }

   private record PreparedRoster(List<BattlePokemon> team, String packed, JsonArray meta) {
   }

   record QueuedTeam(UUID playerUuid, List<BattlePokemon> team, String packed, String rankedId) {
   }

   private static final class QueuePreparationFailure extends Exception {
      final Component message;

      QueuePreparationFailure(Component message) {
         super(null, null, false, false);
         this.message = message;
      }
   }
}
