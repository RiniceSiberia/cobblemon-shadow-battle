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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.ClickEvent.Action;
import net.minecraft.server.level.ServerPlayer;
import xiaocaoawa.minecraft.mod.cobblebattle.dex.RemoteDex;
import xiaocaoawa.minecraft.mod.cobblebattle.lang.Msg;
import xiaocaoawa.minecraft.mod.cobblebattle.net.BattleServerClient;

final class BattleQueue {
   private final CrossServerBattleService service;
   private final Map<UUID, BattleQueue.QueuedTeam> queued = new ConcurrentHashMap<>();
   private final Map<Integer, UUID> refOwners = new ConcurrentHashMap<>();
   private final Map<Integer, UUID> lookupRefs = new ConcurrentHashMap<>();

   BattleQueue(CrossServerBattleService service) {
      this.service = service;
   }

   boolean contains(UUID playerUuid) {
      return this.queued.containsKey(playerUuid);
   }

   BattleQueue.QueuedTeam claim(UUID playerUuid) {
      return this.queued.remove(playerUuid);
   }

   void drop(UUID playerUuid) {
      this.queued.remove(playerUuid);
   }

   void clear() {
      this.queued.clear();
      this.refOwners.clear();
      this.lookupRefs.clear();
   }

   UUID claimLookupRef(JsonElement ref) {
      return ref != null && !ref.isJsonNull() ? this.lookupRefs.remove(ref.getAsInt()) : null;
   }

   UUID claimRefOwner(JsonElement ref) {
      return ref != null && !ref.isJsonNull() ? this.refOwners.remove(ref.getAsInt()) : null;
   }

   private BattleQueue.Prepared prepare(ServerPlayer player) throws BattleQueue.Refused {
      return this.prepare(player, true, true);
   }

   private BattleQueue.Prepared prepare(ServerPlayer player, boolean dexAuthority, boolean legality) throws BattleQueue.Refused {
      RemoteDex dex = this.service.dex();
      if (!this.service.isConnected()) {
         throw new BattleQueue.Refused(this.service.notConnected("queue.not_connected"));
      } else if (!this.service.auth().isSignedIn(player.getUUID())) {
         throw new BattleQueue.Refused(Msg.of(ChatFormatting.RED, "queue.not_signed_in"));
      } else if (!dex.isReady()) {
         throw new BattleQueue.Refused(Msg.of(ChatFormatting.RED, "queue.dex_not_ready"));
      } else if (this.queued.containsKey(player.getUUID())) {
         throw new BattleQueue.Refused(Msg.of(ChatFormatting.YELLOW, "queue.already_queued"));
      } else if (BattleRegistry.getBattleByParticipatingPlayer(player) != null) {
         throw new BattleQueue.Refused(Msg.of(ChatFormatting.RED, "queue.already_in_battle"));
      } else {
         RemoteTeamCodec.warmSpeciesCache();
         PartyStore party = PlayerExtensionsKt.party(player);
         List<BattlePokemon> team = party.toBattleTeam(false, false, null);
         if (team.isEmpty()) {
            throw new BattleQueue.Refused(Msg.of(ChatFormatting.RED, "queue.no_pokemon"));
         } else {
            List<Pokemon> plain = new ArrayList<>(team.size());
            List<RemoteDex.Rejection> rejections = new ArrayList<>();

            for (int i = 0; i < team.size(); i++) {
               Pokemon pokemon = team.get(i).getEffectedPokemon();
               plain.add(pokemon);
               if (dexAuthority || legality) {
                  rejections.addAll(dex.check(pokemon, i, dexAuthority));
               }
            }

            if (!rejections.isEmpty()) {
               throw new BattleQueue.Refused(describeRejections(rejections));
            } else {
               return new BattleQueue.Prepared(team, BattleRegistry.INSTANCE.packTeam(team), dex.describeTeam(plain));
            }
         }
      }
   }

   private static int slotsOf(String battleType) {
      String var1 = battleType == null ? "" : battleType.toLowerCase(Locale.ROOT);

      return switch (var1) {
         case "doubles", "double", "double_battle" -> 2;
         case "triples", "triple", "triple_battle" -> 3;
         default -> 1;
      };
   }

   private Component tooFewFor(List<BattlePokemon> team, String battleType) {
      if (battleType != null && !battleType.isEmpty()) {
         int slots = slotsOf(battleType);
         return team.size() >= slots
            ? null
            : Msg.of(ChatFormatting.RED, "queue.too_few", Msg.raw("room.type." + battleType.toLowerCase(Locale.ROOT)), slots, team.size());
      } else {
         return null;
      }
   }

   Component join(ServerPlayer player, String rankedId) {
      BattleQueue.Prepared prepared;
      try {
         prepared = this.prepare(player);
      } catch (BattleQueue.Refused var6) {
         return var6.message;
      }

      CrossServerBattleService.Ranked competition = this.service.ranked(rankedId);
      if (competition != null) {
         Component tooFew = this.tooFewFor(prepared.team(), competition.battleType());
         if (tooFew != null) {
            return tooFew;
         }
      }

      JsonObject join = BattleServerClient.msg("queue_join");
      join.addProperty("ranked", rankedId);
      return this.send(player, join, prepared, rankedId);
   }

   Component createRoom(
      ServerPlayer player, String name, String password, String battleType, int level, int pick, boolean fullHeal, boolean hostEngine, boolean legality
   ) {
      BattleQueue.Prepared prepared;
      try {
         prepared = this.prepare(player, !hostEngine, !hostEngine || legality);
      } catch (BattleQueue.Refused var13) {
         return var13.message;
      }

      Component tooFew = this.tooFewFor(prepared.team(), battleType);
      if (tooFew != null) {
         return tooFew;
      } else {
         JsonObject create = BattleServerClient.msg("room_create");
         create.addProperty("name", name);
         create.addProperty("password", password);
         create.addProperty("battleType", battleType);
         create.addProperty("level", level);
         create.addProperty("pick", pick);
         create.addProperty("fullHeal", fullHeal);
         create.addProperty("engine", hostEngine ? "host" : "server");
         create.addProperty("legality", !hostEngine || legality);
         return this.send(player, create, prepared, "");
      }
   }

   Component joinRoom(ServerPlayer player, String roomId, String password, String battleType, boolean hostEngine, boolean legality, String inviteCode) {
      BattleQueue.Prepared prepared;
      try {
         prepared = this.prepare(player, !hostEngine, !hostEngine || legality);
      } catch (BattleQueue.Refused var11) {
         return var11.message;
      }

      Component tooFew = this.tooFewFor(prepared.team(), battleType);
      if (tooFew != null) {
         return tooFew;
      } else {
         JsonObject join = BattleServerClient.msg("room_join");
         join.addProperty("roomId", roomId);
         join.addProperty("password", password);
         if (!inviteCode.isEmpty()) {
            join.addProperty("inviteCode", inviteCode);
         }

         return this.send(player, join, prepared, "");
      }
   }

   Component lookupRoom(ServerPlayer player, String inviteCode) {
      String code = inviteCode == null ? "" : inviteCode.trim();
      if (code.isEmpty()) {
         return Msg.of(ChatFormatting.RED, "room.invite_empty");
      } else if (!this.service.isConnected()) {
         return this.service.notConnected("queue.not_connected");
      } else if (!this.service.auth().isSignedIn(player.getUUID())) {
         return Msg.of(ChatFormatting.RED, "queue.not_signed_in");
      } else {
         BattleServerClient client = this.service.client();
         int ref = client.nextRef();
         JsonObject lookup = BattleServerClient.msg("room_lookup");
         lookup.addProperty("ref", ref);
         lookup.add("player", playerObject(player));
         lookup.addProperty("inviteCode", code);
         this.lookupRefs.put(ref, player.getUUID());
         if (!client.send(lookup)) {
            this.lookupRefs.remove(ref);
            return Msg.of(ChatFormatting.RED, "queue.send_failed");
         } else {
            return null;
         }
      }
   }

   void onRoomInfo(JsonObject message) {
      UUID playerUuid = this.claimLookupRef(message.get("ref"));
      if (playerUuid != null) {
         String roomId = BattleServerClient.str(message, "id", "");
         String inviteCode = BattleServerClient.str(message, "inviteCode", "");
         String battleType = BattleServerClient.bool(message, "fighting", false) ? "" : BattleServerClient.str(message, "battleType", "singles");
         boolean hostEngine = "host".equals(BattleServerClient.str(message, "engine", "server"));
         boolean legality = BattleServerClient.bool(message, "legality", true);
         this.service.onServerThreadWithPlayer(playerUuid, player -> {
            Component refusal = this.joinRoom(player, roomId, "", battleType, hostEngine, legality, inviteCode);
            if (refusal != null) {
               this.service.tellPlayer(playerUuid, refusal);
            }
         });
      }
   }

   Component leaveRoom(ServerPlayer player) {
      BattleServerClient client = this.service.client();
      JsonObject leave = BattleServerClient.msg("room_leave");
      leave.addProperty("ref", client.nextRef());
      leave.add("player", playerObject(player));
      client.send(leave);
      return null;
   }

   Component startRoom(ServerPlayer player) {
      BattleServerClient client = this.service.client();
      int ref = client.nextRef();
      JsonObject start = BattleServerClient.msg("room_start");
      start.addProperty("ref", ref);
      start.add("player", playerObject(player));
      this.refOwners.put(ref, player.getUUID());
      if (!client.send(start)) {
         this.refOwners.remove(ref);
         return Msg.of(ChatFormatting.RED, "queue.send_failed");
      } else {
         return null;
      }
   }

   void onRoomClosed(JsonObject message) {
      this.claimRefOwner(message.get("ref"));
      UUID playerUuid = UUID.fromString(BattleServerClient.str(message, "player", ""));
      this.queued.remove(playerUuid);
      String why = BattleServerClient.str(message, "why", "");

      String key = switch (why) {
         case "host_left" -> "room.closed.host_left";
         case "started" -> "room.closed.started";
         case "banned" -> "room.closed.banned";
         case "finished" -> "room.closed.finished";
         case "gone" -> "room.closed.gone";
         default -> null;
      };
      if (key != null) {
         this.service.tellPlayer(playerUuid, Msg.of(ChatFormatting.YELLOW, key));
      }
   }

   private Component send(ServerPlayer player, JsonObject request, BattleQueue.Prepared prepared, String rankedId) {
      BattleServerClient client = this.service.client();
      int ref = client.nextRef();
      request.addProperty("ref", ref);
      request.add("player", playerObject(player));
      request.addProperty("team", prepared.packed());
      request.add("teamMeta", prepared.meta());
      this.queued.put(player.getUUID(), new BattleQueue.QueuedTeam(player.getUUID(), prepared.team(), prepared.packed(), rankedId));
      this.refOwners.put(ref, player.getUUID());
      if (!client.send(request)) {
         this.queued.remove(player.getUUID());
         this.refOwners.remove(ref);
         return Msg.of(ChatFormatting.RED, "queue.send_failed");
      } else {
         return null;
      }
   }

   void onRoomCreated(JsonObject message) {
      this.claimRefOwner(message.get("ref"));
      UUID playerUuid = UUID.fromString(BattleServerClient.str(message, "player", ""));
      String name = BattleServerClient.str(message, "name", "");
      this.service.tellPlayer(playerUuid, Msg.of(ChatFormatting.GREEN, "room.created", name));
      String inviteCode = BattleServerClient.str(message, "inviteCode", "");
      if (!inviteCode.isEmpty()) {
         this.service
            .tellPlayer(
               playerUuid,
               Msg.of(ChatFormatting.AQUA, "room.invite_code", inviteCode)
                  .withStyle(
                     style -> style.withClickEvent(new ClickEvent(Action.COPY_TO_CLIPBOARD, inviteCode))
                        .withHoverEvent(new HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, Msg.of("room.invite_copy_hint")))
                  )
            );
      }
   }

   Component leave(ServerPlayer player) {
      if (!this.queued.containsKey(player.getUUID())) {
         return Msg.of(ChatFormatting.YELLOW, "queue.not_in_queue");
      } else {
         BattleServerClient client = this.service.client();
         JsonObject leave = BattleServerClient.msg("queue_leave");
         leave.addProperty("ref", client.nextRef());
         leave.add("player", playerObject(player));
         client.send(leave);
         return null;
      }
   }

   void onPlayerDisconnect(ServerPlayer player) {
      if (this.queued.remove(player.getUUID()) != null) {
         BattleServerClient client = this.service.client();
         if (client != null) {
            JsonObject leave = BattleServerClient.msg("queue_leave");
            leave.add("player", playerObject(player));
            client.send(leave);
         }
      }
   }

   Component describePartyCompatibility(ServerPlayer player) {
      PartyStore party = PlayerExtensionsKt.party(player);
      List<BattlePokemon> team = party.toBattleTeam(false, false, null);
      if (team.isEmpty()) {
         return Msg.of(ChatFormatting.YELLOW, "check.no_pokemon");
      } else {
         List<RemoteDex.Rejection> rejections = new ArrayList<>();

         for (int i = 0; i < team.size(); i++) {
            rejections.addAll(this.service.dex().check(team.get(i).getEffectedPokemon(), i));
         }

         return (Component)(rejections.isEmpty() ? Msg.of(ChatFormatting.GREEN, "check.all_ok", team.size()) : describeRejections(rejections));
      }
   }

   void onQueueAck(JsonObject message) {
      this.claimRefOwner(message.get("ref"));
      UUID playerUuid = UUID.fromString(BattleServerClient.str(message, "player", ""));
      int waiting = BattleServerClient.integer(message, "waiting", 1);
      String competition = BattleServerClient.str(message, "rankedName", "");
      this.service
         .tellPlayer(
            playerUuid,
            competition.isEmpty()
               ? Msg.of(ChatFormatting.GREEN, "queue.acked", waiting)
               : Msg.of(ChatFormatting.GREEN, "queue.acked_ranked", competition, waiting)
         );
      String rankedId = BattleServerClient.str(message, "ranked", "");
      this.service.onServerThreadWithPlayer(playerUuid, player -> ApiEvents.queueJoined(player, rankedId, competition, waiting));
   }

   void onQueueLeft(JsonObject message) {
      UUID playerUuid = UUID.fromString(BattleServerClient.str(message, "player", ""));
      this.queued.remove(playerUuid);
      if (BattleServerClient.bool(message, "wasQueued", false)) {
         String why = BattleServerClient.str(message, "why", "");

         String key = switch (why) {
            case "busy" -> "queue.left_busy";
            case "banned" -> "queue.left_banned";
            default -> "queue.left";
         };
         this.service.tellPlayer(playerUuid, Msg.of(ChatFormatting.YELLOW, key));
         this.service.onServerThreadWithPlayer(playerUuid, player -> ApiEvents.queueLeft(player, why));
      }
   }

   void onQueueWait(JsonObject message) {
      UUID playerUuid = UUID.fromString(BattleServerClient.str(message, "player", ""));
      int position = message.has("position") ? message.get("position").getAsInt() : 0;
      int waiting = message.has("waiting") ? message.get("waiting").getAsInt() : 0;
      this.service.tellPlayer(playerUuid, Msg.of(ChatFormatting.YELLOW, "queue.waiting", position, waiting));
   }

   private static JsonObject playerObject(ServerPlayer player) {
      JsonObject object = new JsonObject();
      object.addProperty("uuid", player.getUUID().toString());
      object.addProperty("name", player.getGameProfile().getName());
      return object;
   }

   private static Component describeRejections(List<RemoteDex.Rejection> rejections) {
      MutableComponent root = Msg.of(ChatFormatting.RED, "reject.header").copy();

      for (RemoteDex.Rejection rejection : rejections) {
         Component body = switch (rejection.kind()) {
            case UNKNOWN_SPECIES -> Msg.compose("reject.unknown_species");
            case BASE_STAT_MISMATCH -> Msg.compose("reject.stat_mismatch", rejection.detail());
            case ILLEGAL_ABILITY -> Msg.compose("reject.illegal_ability", rejection.detail());
            case ILLEGAL_MOVE -> Msg.compose("reject.illegal_move", rejection.detail());
            case EV_OVER_CAP -> Msg.compose("reject.ev_over_cap", rejection.detail());
            case IV_OVER_CAP -> Msg.compose("reject.iv_over_cap", rejection.detail());
         };
         root.append(Msg.compose(ChatFormatting.YELLOW, "reject.slot", rejection.slot() + 1, rejection.pokemon()).append(" ").append(body));
      }

      root.append(Msg.of(ChatFormatting.GRAY, "reject.footer"));
      return root;
   }

   private record Prepared(List<BattlePokemon> team, String packed, JsonArray meta) {
   }

   record QueuedTeam(UUID playerUuid, List<BattlePokemon> team, String packed, String rankedId) {
   }

   private static final class Refused extends Exception {
      final Component message;

      Refused(Component message) {
         super(null, null, false, false);
         this.message = message;
      }
   }
}
