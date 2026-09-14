package xiaocaoawa.minecraft.mod.cobblebattle.battle;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.battles.runner.ShowdownService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.architectury.platform.Platform;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xiaocaoawa.minecraft.mod.cobblebattle.account.AuthMode;
import xiaocaoawa.minecraft.mod.cobblebattle.account.AuthService;
import xiaocaoawa.minecraft.mod.cobblebattle.api.BattleInfo;
import xiaocaoawa.minecraft.mod.cobblebattle.api.BattleOutcome;
import xiaocaoawa.minecraft.mod.cobblebattle.api.ScoreChange;
import xiaocaoawa.minecraft.mod.cobblebattle.config.CobbleBattleConfig;
import xiaocaoawa.minecraft.mod.cobblebattle.config.ServerIdentity;
import xiaocaoawa.minecraft.mod.cobblebattle.dex.RemoteDex;
import xiaocaoawa.minecraft.mod.cobblebattle.lang.Msg;
import xiaocaoawa.minecraft.mod.cobblebattle.net.BattleServerClient;
import xiaocaoawa.minecraft.mod.cobblebattle.network.ChatLinePayload;
import xiaocaoawa.minecraft.mod.cobblebattle.network.ChatStatePayload;
import xiaocaoawa.minecraft.mod.cobblebattle.network.CobbleBattleNetwork;
import xiaocaoawa.minecraft.mod.cobblebattle.network.LeaderboardPayload;
import xiaocaoawa.minecraft.mod.cobblebattle.network.MenuActionPayload;
import xiaocaoawa.minecraft.mod.cobblebattle.network.OpenMainMenuPayload;
import xiaocaoawa.minecraft.mod.cobblebattle.network.RoomActionPayload;
import xiaocaoawa.minecraft.mod.cobblebattle.network.RoomListPayload;
import xiaocaoawa.minecraft.mod.cobblebattle.network.RoomStatePayload;
import xiaocaoawa.minecraft.mod.cobblebattle.network.ServerDexPayload;

public final class CrossServerBattleService {
   private static final Logger LOGGER = LoggerFactory.getLogger("CobbleBattle");
   private final CobbleBattleConfig config;
   private final RemoteDex dex = new RemoteDex();
   private final BattleQueue battleQueue = new BattleQueue(this);
   private final MirrorFactory mirrorFactory = new MirrorFactory(this);
   private final MirrorTeardown teardown = new MirrorTeardown(this);
   private final SpectatorFactory spectators = new SpectatorFactory(this);
   private final TeamPreviews previews = new TeamPreviews(this);
   private final AuthService auth = new AuthService();
   private final Map<String, CrossServerBattleService.Ranked> ranked = new LinkedHashMap<>();
   private final Map<Integer, UUID> boardRefs = new LinkedHashMap<Integer, UUID>() {
      @Override
      protected boolean removeEldestEntry(Entry<Integer, UUID> eldest) {
         return this.size() > 64;
      }
   };
   private final Map<Integer, UUID> chatRefs = new LinkedHashMap<Integer, UUID>() {
      @Override
      protected boolean removeEldestEntry(Entry<Integer, UUID> eldest) {
         return this.size() > 64;
      }
   };
   private BattleServerClient client;
   private MinecraftServer minecraftServer;
   private final Set<UUID> pendingAuthOpens = ConcurrentHashMap.newKeySet();
   private ScheduledExecutorService idleTimer;
   private ScheduledFuture<?> idleDisconnect;
   private static final int IDLE_DISCONNECT_MIN_SECONDS = 5;
   private final Map<Integer, UUID> menuRefs = new LinkedHashMap<>();
   private CrossServerBattleService.RoomBoard roomCache;
   private final Map<UUID, Boolean> roomWaiters = new LinkedHashMap<>();
   private boolean roomFetchInFlight;
   private final Map<UUID, String> roomBoardSent = new HashMap<>();
   private static final long ROOM_CACHE_MS = 4500L;
   private static final long ROOM_OPEN_MS = 1000L;
   private volatile int chatWatchersReported = -1;
   private volatile boolean chatEnabled = true;
   private volatile boolean emailEnabled = false;

   public Collection<CrossServerBattleService.Ranked> ranked() {
      return this.ranked.values();
   }

   public boolean hasRanked(String id) {
      return id != null && this.ranked.containsKey(id);
   }

   public CrossServerBattleService.Ranked ranked(String id) {
      return id == null ? null : this.ranked.get(id);
   }

   public CrossServerBattleService(CobbleBattleConfig config) {
      this.config = config;
   }

   public RemoteDex dex() {
      return this.dex;
   }

   public CobbleBattleConfig config() {
      return this.config;
   }

   public boolean isConnected() {
      return this.client != null && this.client.isHandshaken();
   }

   public String connectionRefusal() {
      return this.client == null ? null : this.client.refusedReason();
   }

   public Component notConnected(String key) {
      String refused = this.connectionRefusal();
      if (refused != null) {
         return Msg.of(ChatFormatting.RED, "conn.refused", refused);
      } else {
         this.ensureConnected();
         return Msg.of(ChatFormatting.RED, key);
      }
   }

   private void ensureConnected() {
      if (this.client != null && !this.client.isWanted() && this.client.refusedReason() == null) {
         this.client.connect();
      }
   }

   public boolean isQueued(UUID playerUuid) {
      return this.battleQueue.contains(playerUuid);
   }

   BattleServerClient client() {
      return this.client;
   }

   MinecraftServer server() {
      return this.minecraftServer;
   }

   BattleQueue battleQueue() {
      return this.battleQueue;
   }

   ServerPlayer playerOf(UUID playerUuid) {
      MinecraftServer server = this.minecraftServer;
      return server == null ? null : server.getPlayerList().getPlayer(playerUuid);
   }

   public void onServerStarted(MinecraftServer server) {
      this.minecraftServer = server;
      this.dex.loadFromDisk();
      CrossServerBattles.setChoiceRelay(this::relayChoice);
      CrossServerBattles.setOutputRelay(this::relayOutput);
      this.client = new BattleServerClient(this.config, this::onMessage, this::onConnected, this::onDisconnected);
      this.client.setOnConnectFailed(this::onConnectFailed);
      this.client.start();
      if (this.config.keepConnectedWhenEmpty || server.getPlayerCount() > 0) {
         this.client.connect();
      }
   }

   public void onPlayerJoin(ServerPlayer player) {
      this.cancelIdleDisconnect();
      this.ensureConnected();
      this.pushChatState(player);
   }

   private Component connectThenOpenAuth(ServerPlayer player) {
      String refused = this.connectionRefusal();
      this.pendingAuthOpens.add(player.getUUID());
      if (this.client != null) {
         this.client.connect();
      }

      return refused != null ? Msg.of(ChatFormatting.YELLOW, "auth.connecting_retry", refused) : Msg.of(ChatFormatting.YELLOW, "auth.connecting");
   }

   private void openPendingAuth() {
      for (UUID waiting : List.copyOf(this.pendingAuthOpens)) {
         this.pendingAuthOpens.remove(waiting);
         this.onServerThreadWithPlayer(waiting, player -> {
            Component refusal = this.openAuthScreen(player);
            if (refusal != null) {
               this.tellPlayer(waiting, refusal);
            }
         });
      }
   }

   private void onConnectFailed(String why) {
      for (UUID waiting : List.copyOf(this.pendingAuthOpens)) {
         this.pendingAuthOpens.remove(waiting);
         this.tellPlayer(waiting, Msg.of(ChatFormatting.RED, "auth.connect_failed", why));
      }
   }

   private synchronized void cancelIdleDisconnect() {
      if (this.idleDisconnect != null) {
         this.idleDisconnect.cancel(false);
         this.idleDisconnect = null;
      }
   }

   private synchronized void scheduleIdleDisconnect() {
      if (!this.config.keepConnectedWhenEmpty && this.client != null) {
         this.cancelIdleDisconnect();
         if (this.idleTimer == null) {
            this.idleTimer = Executors.newSingleThreadScheduledExecutor(r -> {
               Thread t = new Thread(r, "CobbleBattle-Idle");
               t.setDaemon(true);
               return t;
            });
         }

         long seconds = Math.max(5, this.config.idleDisconnectSeconds);
         this.idleDisconnect = this.idleTimer.schedule(() -> this.onServerThread(() -> {
            MinecraftServer server = this.minecraftServer;
            if (server != null && server.getPlayerCount() <= 0 && this.client != null && this.client.isWanted()) {
               LOGGER.info("No players online for {}s, letting the battle server connection go", seconds);
               this.client.disconnect("no players online");
            }
         }), seconds, TimeUnit.SECONDS);
      }
   }

   public void reconnect(String why) {
      if (this.client != null) {
         this.client.reconnect(why);
      }
   }

   public void onServerStopping() {
      this.cancelIdleDisconnect();
      if (this.idleTimer != null) {
         this.idleTimer.shutdownNow();
         this.idleTimer = null;
      }

      if (this.client != null) {
         this.client.stop();
      }

      for (MirrorBattle mirror : CrossServerBattles.all()) {
         this.teardown.sweepEntities(mirror, 0L);
      }

      CrossServerBattles.clear();
      this.battleQueue.clear();
      this.previews.clear();
   }

   private void onConnected() {
      JsonObject hello = BattleServerClient.msg("hello");
      hello.addProperty("ref", this.client.nextRef());
      hello.addProperty("protocol", 10);
      hello.addProperty("token", this.config.authToken);
      hello.addProperty("serverId", ServerIdentity.get());
      hello.addProperty("modVersion", "1.0");
      hello.addProperty("cobblemonVersion", Platform.isModLoaded("cobblemon") ? Platform.getMod("cobblemon").getVersion() : "unknown");
      this.client.sendHandshake(hello);
   }

   private void onDisconnected(String reason) {
      if (this.client != null && !this.client.isWanted()) {
         LOGGER.info("Battle server connection closed ({}).", reason);
      } else {
         LOGGER.warn("Lost the battle server ({}). Aborting {} mirror battle(s).", reason, CrossServerBattles.size());
      }

      this.dex.suspend(reason);
      this.chatWatchersReported = -1;
      this.onServerThread(() -> {
         this.roomCache = null;
         this.roomWaiters.clear();
         this.roomBoardSent.clear();
         this.roomFetchInFlight = false;
      });
      this.battleQueue.clear();
      this.previews.clear();
      this.auth.clear();

      for (MirrorBattle mirror : CrossServerBattles.all()) {
         this.teardown.abort(mirror, Msg.of("battle.connection_lost").withStyle(ChatFormatting.RED));
      }
   }

   private void onMessage(JsonObject message) {
      String type = BattleServerClient.str(message, "t", "");
      switch (type) {
         case "ping":
            this.client.send(BattleServerClient.msg("pong"));
            break;
         case "hello_ack":
            this.onHelloAck(message);
            break;
         case "ranked_update":
            this.readRanked(message);
            break;
         case "dex_snapshot":
            this.dex.accept(message);
            break;
         case "queue_ack":
            this.battleQueue.onQueueAck(message);
            break;
         case "queue_left":
            this.battleQueue.onQueueLeft(message);
            break;
         case "queue_wait":
            this.battleQueue.onQueueWait(message);
            break;
         case "room_created":
            this.battleQueue.onRoomCreated(message);
            break;
         case "room_list":
            this.onServerThread(() -> this.onRoomList(message));
            break;
         case "room_state":
            this.onRoomState(message);
            break;
         case "room_info":
            this.battleQueue.onRoomInfo(message);
            break;
         case "room_closed":
            this.onRoomClosed(message);
            break;
         case "preview_open":
            this.onServerThread(() -> this.previews.onOpen(message));
            break;
         case "preview_state":
            this.onServerThread(() -> this.previews.onState(message));
            break;
         case "preview_closed":
            this.onServerThread(() -> this.previews.onClosed(message));
            break;
         case "match_found":
            this.onMatchFound(message);
            break;
         case "battle_start":
            this.onBattleStart(message);
            break;
         case "output":
            this.onOutput(message);
            break;
         case "battle_choice":
            this.onRelayedChoice(message);
            break;
         case "spectate_start":
            this.onServerThread(() -> this.spectators.begin(message));
            break;
         case "spectate_end":
            this.onServerThread(() -> this.spectators.end(message));
            break;
         case "battle_end":
            this.onBattleEnd(message);
            break;
         case "forfeit":
            this.onOpponentForfeit(message);
            break;
         case "chat":
            this.onChat(message);
            break;
         case "leaderboard":
            this.onLeaderboard(message);
            break;
         case "account_ok":
            this.onAccountOk(message);
            break;
         case "account_code_ok":
            this.onAccountCodeOk(message);
            break;
         case "error":
            this.onError(message);
            break;
         default:
            LOGGER.warn("Unknown message type '{}' from the battle server", type);
      }
   }

   private void onHelloAck(JsonObject message) {
      this.client.setHandshaken(true);
      boolean dexReady = BattleServerClient.bool(message, "dexReady", false);
      this.readRanked(message);
      this.dex.setStrictBaseStats(BattleServerClient.bool(message, "strictBaseStats", true));
      this.dex
         .setTeamRules(
            BattleServerClient.bool(message, "strictAbilities", false),
            BattleServerClient.bool(message, "strictMoves", false),
            BattleServerClient.integer(message, "maxEvPerStat", 0),
            BattleServerClient.integer(message, "maxEvTotal", 0),
            BattleServerClient.integer(message, "maxIv", 0)
         );
      this.chatEnabled = BattleServerClient.bool(message, "chatEnabled", true);
      if (!this.chatEnabled) {
         LOGGER.info("The battle server has chat switched off; the chat panel stays hidden");
      }

      this.emailEnabled = BattleServerClient.bool(message, "emailEnabled", false);
      if (!this.emailEnabled) {
         LOGGER.info("The battle server has no mailer; the account screen stays on account names");
      }

      LOGGER.info(
         "Handshake complete with battle server instance '{}' (dex {}, {} species)",
         new Object[]{
            BattleServerClient.str(message, "instance", "?"), dexReady ? "ready" : "NOT ready", BattleServerClient.integer(message, "speciesCount", 0)
         }
      );
      this.reportChatWatchers();
      this.openPendingAuth();
      if (!dexReady) {
         this.dex.invalidate("the battle server has no dex");
         LOGGER.warn(
            "The battle server has no dex yet. Cross-server battles stay unavailable until it has one - the Cobblemon jar belongs in its cobblemon/ folder."
         );
      } else {
         String cached = this.dex.cachedDigest();
         String theirs = BattleServerClient.str(message, "dexDigest", "");
         if (cached != null && cached.equals(theirs)) {
            this.dex.accept(unchangedDex(cached));
         } else {
            this.requestDex();
         }
      }
   }

   private static JsonObject unchangedDex(String digest) {
      JsonObject same = new JsonObject();
      same.addProperty("digest", digest);
      same.addProperty("unchanged", true);
      return same;
   }

   private void readRanked(JsonObject message) {
      this.ranked.clear();
      JsonArray offered = message.has("ranked") && message.get("ranked").isJsonArray() ? message.getAsJsonArray("ranked") : null;
      if (offered != null) {
         for (JsonElement el : offered) {
            if (el.isJsonObject()) {
               JsonObject o = el.getAsJsonObject();
               String id = BattleServerClient.str(o, "id", "");
               if (!id.isEmpty()) {
                  List<String> rules = new ArrayList<>();
                  if (o.has("ruleSet") && o.get("ruleSet").isJsonArray()) {
                     for (JsonElement rule : o.getAsJsonArray("ruleSet")) {
                        if (rule.isJsonPrimitive()) {
                           rules.add(rule.getAsString());
                        }
                     }
                  }

                  this.ranked
                     .put(
                        id,
                        new CrossServerBattleService.Ranked(
                           id,
                           BattleServerClient.str(o, "name", id),
                           BattleServerClient.str(o, "battleType", "singles"),
                           BattleServerClient.integer(o, "slotsPerActor", 1),
                           BattleServerClient.integer(o, "adjustLevel", -1),
                           BattleServerClient.bool(o, "fullHeal", false),
                           BattleServerClient.str(o, "winScore", "1"),
                           BattleServerClient.str(o, "failScore", "1"),
                           List.copyOf(rules)
                        )
                     );
               }
            }
         }
      }

      if (this.ranked.isEmpty()) {
         LOGGER.warn("The battle server offers no ranked competitions - nobody can queue. Its ranked/ folder is empty, or every file in it was refused.");
      } else {
         LOGGER.info("Ranked competitions offered: {}", String.join(", ", this.ranked.keySet()));
      }
   }

   private void requestDex() {
      JsonObject query = BattleServerClient.msg("dex_query");
      query.addProperty("ref", this.client.nextRef());
      String cached = this.dex.cachedDigest();
      if (cached != null) {
         query.addProperty("have", cached);
      }

      this.client.send(query);
   }

   private void onError(JsonObject message) {
      String code = BattleServerClient.str(message, "code", "?");
      String text = BattleServerClient.str(message, "message", "");
      LOGGER.warn("Battle server error [{}]: {}", code, text);
      if (!"hello".equals(BattleServerClient.str(message, "about", ""))) {
         if ("room_list".equals(BattleServerClient.str(message, "about", ""))) {
            this.onServerThread(() -> {
               this.roomFetchInFlight = false;
               this.roomWaiters.clear();
            });
         }

         MirrorBattle mirror = CrossServerBattles.byRemoteId(BattleServerClient.str(message, "battleId", ""));
         if (mirror != null && !mirror.isFinished()) {
            LOGGER.error("Battle server refused something for battle {} ({}: {}) - closing the local mirror", new Object[]{mirror.remoteBattleId(), code, text});
            this.teardown.abort(mirror, Msg.of("battle.ended", text).withStyle(ChatFormatting.RED));
         } else if (this.auth.isAwaiting(message)) {
            UUID waiting = this.auth.onAccountError(message);
            if (waiting != null) {
               Component refusal = describeAuthFailure(code, text);
               this.tellPlayer(waiting, refusal);
               this.withPlayer(waiting, player -> CobbleBattleNetwork.sendResult(player, false, refusal));
            }
         } else {
            UUID talker = this.claimChatRef(message.get("ref"));
            if (talker != null) {
               this.tellPlayer(talker, describeChatFailure(message, code, text));
            } else {
               UUID forMenu = claimRef(this.menuRefs, message.get("ref"));
               if (forMenu != null) {
                  this.withPlayer(forMenu, player -> this.sendMainMenu(player, ""));
               } else {
                  UUID asker = claimRef(this.boardRefs, message.get("ref"));
                  if (asker != null) {
                     this.tellPlayer(asker, Component.literal(text).withStyle(ChatFormatting.RED));
                  } else {
                     UUID looker = this.battleQueue.claimLookupRef(message.get("ref"));
                     if (looker != null) {
                        this.tellPlayer(looker, describeLookupFailure(code, text));
                     } else {
                        UUID owner = this.battleQueue.claimRefOwner(message.get("ref"));
                        if (owner != null) {
                           this.battleQueue.drop(owner);

                           String roomKey = switch (code) {
                              case "NO_SUCH_ROOM" -> "room.err.no_such";
                              case "ROOM_LOCKED" -> "room.err.locked";
                              case "BAD_INVITE_CODE" -> "room.err.bad_invite";
                              case "OWN_ROOM" -> "room.err.own";
                              case "NOT_ROOM_HOST" -> "room.err.not_host";
                              case "ROOM_NOT_READY" -> "room.err.not_ready";
                              default -> null;
                           };
                           if (roomKey != null) {
                              this.tellPlayer(owner, Msg.of(ChatFormatting.RED, roomKey));
                              this.refreshRooms(owner);
                           } else if ("BANNED".equals(code)) {
                              long left = message.has("left") ? message.get("left").getAsLong() : 0L;
                              String ranked = BattleServerClient.str(message, "ranked", "?");
                              this.tellPlayer(
                                 owner,
                                 (left == 0L ? Msg.of("queue.banned_permanent", ranked) : Msg.of("queue.banned_for", ranked, describeDuration(left)))
                                    .withStyle(ChatFormatting.RED)
                              );
                           } else {
                              this.tellPlayer(owner, Msg.of("queue.refused", text).withStyle(ChatFormatting.RED));
                           }
                        }
                     }
                  }
               }
            }
         }
      } else {
         String why = describeHandshakeRefusal(code, text);
         LOGGER.error(
            "The battle server refused this server's handshake: {}. Not reconnecting until somebody opens the sign-in screen or runs /cbattle reload.", why
         );
         this.client.suspend(why);

         for (UUID waiting : List.copyOf(this.pendingAuthOpens)) {
            this.pendingAuthOpens.remove(waiting);
            this.tellPlayer(waiting, Msg.of(ChatFormatting.RED, "conn.refused", why));
         }
      }
   }

   private static Component describeLookupFailure(String code, String fallback) {
      String key = switch (code) {
         case "BAD_INVITE_CODE" -> "room.err.bad_invite";
         case "NOT_LOGGED_IN" -> "queue.not_signed_in";
         case "ALREADY_QUEUED" -> "queue.already_queued";
         case "ALREADY_IN_BATTLE" -> "queue.already_in_battle";
         case "DEX_NOT_READY" -> "queue.dex_not_ready";
         default -> null;
      };
      Component text = key == null ? Msg.of("queue.refused", fallback) : Msg.of(key);
      return text.copy().withStyle(ChatFormatting.RED);
   }

   private static String describeHandshakeRefusal(String code, String text) {
      return switch (code) {
         case "BAD_PROTOCOL" -> Msg.raw("conn.err.protocol", text);
         case "BAD_AUTH" -> Msg.raw("conn.err.auth");
         default -> text.isEmpty() ? code : text;
      };
   }

   private static String describeDuration(long ms) {
      long seconds = (ms + 999L) / 1000L;
      long days = seconds / 86400L;
      long hours = seconds % 86400L / 3600L;
      long minutes = seconds % 3600L / 60L;
      long secs = seconds % 60L;
      StringBuilder out = new StringBuilder();
      int parts = 0;
      if (days > 0L) {
         out.append(Msg.of("time.days", days).getString());
         parts++;
      }

      if (hours > 0L && parts < 2) {
         out.append(Msg.of("time.hours", hours).getString());
         parts++;
      }

      if (minutes > 0L && parts < 2) {
         out.append(Msg.of("time.minutes", minutes).getString());
         parts++;
      }

      if (parts == 0) {
         out.append(Msg.of("time.seconds", Math.max(1L, secs)).getString());
      }

      return out.toString().trim();
   }

   private static Component describeAuthFailure(String code, String fallback) {
      String key = switch (code) {
         case "BAD_CREDENTIALS" -> "auth.err.bad_credentials";
         case "BAD_EMAIL" -> "auth.err.bad_email";
         case "BAD_CODE" -> "auth.err.bad_code";
         case "EMAIL_EXISTS" -> "auth.err.email_taken";
         case "CODE_TOO_SOON" -> "auth.err.code_too_soon";
         case "SMTP_FAILED" -> "auth.err.smtp_failed";
         case "BAD_ACCOUNT_ID" -> "auth.err.bad_id";
         case "ACCOUNT_EXISTS" -> "auth.err.taken";
         case "BAD_PASSWORD" -> "auth.err.bad_password";
         case "ACCOUNT_IN_USE" -> "auth.err.in_use";
         case "NOT_ACCOUNT_OWNER" -> "auth.err.not_owner";
         case "TOO_MANY_ATTEMPTS" -> "auth.err.too_many";
         default -> null;
      };
      Component text = key == null ? Component.literal(fallback) : Msg.of(key);
      return text.copy().withStyle(ChatFormatting.RED);
   }

   private void onServerThread(Runnable work) {
      MinecraftServer server = this.minecraftServer;
      if (server != null) {
         server.execute(work);
      }
   }

   void closeSpectatorMirror(MirrorBattle mirror) {
      mirror.markFinished();
      UUID localBattleId = mirror.localBattleId();
      if (localBattleId != null) {
         CrossServerBattles.forget(localBattleId);
         this.onServerThread(() -> {
            PokemonBattle battle = BattleRegistry.getBattle(localBattleId);
            if (battle != null && !battle.getEnded()) {
               battle.end();
               BattleRegistry.closeBattle(battle);
            }
         });
         this.teardown.sweepEntities(mirror, 0L);
      }
   }

   private void onMatchFound(JsonObject message) {
      MinecraftServer server = this.minecraftServer;
      if (server != null) {
         server.execute(() -> this.mirrorFactory.build(message));
      }
   }

   private void onBattleStart(JsonObject message) {
      MirrorBattle mirror = CrossServerBattles.byRemoteId(BattleServerClient.str(message, "battleId", ""));
      if (mirror != null) {
         mirror.release();

         for (UUID seated : mirror.localPlayers()) {
            this.pushChatState(seated);
         }
      }
   }

   private void relayOutput(CrossServerBattles.ChoiceRelay relay) {
      if (this.client != null) {
         if (this.config.debug) {
            LOGGER.info("[{}] >> output {}", relay.remoteBattleId(), relay.line().replace("\n", " \\n "));
         }

         JsonObject out = BattleServerClient.msg("battle_output");
         out.addProperty("battleId", relay.remoteBattleId());
         out.addProperty("data", relay.line());
         this.client.send(out);
      }
   }

   private void onRelayedChoice(JsonObject message) {
      String remoteBattleId = BattleServerClient.str(message, "battleId", "");
      String line = BattleServerClient.str(message, "data", null);
      MirrorBattle mirror = CrossServerBattles.byRemoteId(remoteBattleId);
      if (mirror == null || line == null) {
         LOGGER.warn("Relayed choice for unknown battle {}", remoteBattleId);
      } else if (!mirror.isAuthoritative()) {
         LOGGER.warn("Battle {} sent us a choice to run, but we are only mirroring it", remoteBattleId);
      } else {
         UUID localBattleId = mirror.localBattleId();
         if (localBattleId == null) {
            LOGGER.warn("Relayed choice for battle {} arrived before the local battle existed", remoteBattleId);
         } else {
            if (this.config.debug) {
               LOGGER.info("[{}] << choice {}", remoteBattleId, line);
            }

            MinecraftServer server = this.server();
            if (server == null) {
               LOGGER.warn("Relayed choice for battle {} arrived with no server to run it on", remoteBattleId);
            } else {
               server.execute(() -> {
                  try {
                     CrossServerBattles.injecting(() -> ShowdownService.Companion.getService().send(localBattleId, new String[]{line}));
                  } catch (RuntimeException var4x) {
                     LOGGER.error("Battle {}: could not feed the relayed choice to showdown", remoteBattleId, var4x);
                  }
               });
            }
         }
      }
   }

   private void onOutput(JsonObject message) {
      String remoteBattleId = BattleServerClient.str(message, "battleId", "");
      MirrorBattle mirror = CrossServerBattles.byRemoteId(remoteBattleId);
      if (mirror == null) {
         LOGGER.warn("Output for unknown battle {}", remoteBattleId);
      } else {
         mirror.accept(message.get("seq").getAsLong(), message.get("data").getAsString());
      }
   }

   private void onOpponentForfeit(JsonObject message) {
      MirrorBattle mirror = CrossServerBattles.byRemoteId(BattleServerClient.str(message, "battleId", ""));
      MinecraftServer server = this.minecraftServer;
      if (mirror != null && server != null) {
         String seat = BattleServerClient.str(message, "seat", "");
         String reportedName = BattleServerClient.str(message, "name", "?");
         server.execute(
            () -> {
               PokemonBattle battle = mirror.battle();
               if (battle != null && !battle.getEnded()) {
                  battle.broadcastChatMessage(
                     Component.translatable("cobblemon.battle.forfeit", new Object[]{actorName(battle, seat, reportedName)}).withStyle(ChatFormatting.RED)
                  );
               }
            }
         );
      }
   }

   private static Component actorName(PokemonBattle battle, String seat, String fallback) {
      for (BattleActor actor : battle.getActors()) {
         if (actor.isInitialized() && actor.getShowdownId().equals(seat)) {
            return actor.getName();
         }
      }

      return Component.literal(fallback);
   }

   private void onBattleEnd(JsonObject message) {
      String remoteBattleId = BattleServerClient.str(message, "battleId", "");
      String reason = BattleServerClient.str(message, "reason", "unknown");
      MirrorBattle mirror = CrossServerBattles.byRemoteId(remoteBattleId);
      if (mirror != null) {
         mirror.markFinished();
         if (!mirror.isSpectator()) {
            for (UUID seated : mirror.localPlayers()) {
               this.pushChatState(seated);
            }

            this.announceScores(message);
            this.fireBattleEnded(mirror, message, reason);
         } else {
            for (UUID watcher : mirror.watchers()) {
               this.tellPlayer(watcher, Msg.of(ChatFormatting.YELLOW, "battle.spectate_over"));
            }
         }

         if (!"win".equals(reason) && !"tie".equals(reason)) {
            LOGGER.warn("Battle {} ended abnormally ({}) - forcing the local mirror closed", remoteBattleId, reason);
            this.teardown.abort(mirror, Msg.of("battle.ended", reason).withStyle(ChatFormatting.RED));
         } else {
            CrossServerBattles.forget(mirror.localBattleId());
            this.teardown.sweepEntities(mirror, 2000L);
         }
      }
   }

   private void relayChoice(CrossServerBattles.ChoiceRelay relay) {
      if (this.client != null) {
         if (this.config.debug) {
            LOGGER.info("[{}] >> {}", relay.remoteBattleId(), relay.line());
         }

         JsonObject choice = BattleServerClient.msg("choice");
         choice.addProperty("battleId", relay.remoteBattleId());
         choice.addProperty("line", relay.line());
         this.client.send(choice);
      }
   }

   void sendAbort(String remoteBattleId, String reason) {
      JsonObject abort = BattleServerClient.msg("battle_abort");
      abort.addProperty("battleId", remoteBattleId);
      abort.addProperty("reason", reason);
      this.client.send(abort);
   }

   public AuthService auth() {
      return this.auth;
   }

   public Component openMainMenu(ServerPlayer player) {
      if (!this.isConnected()) {
         return this.notConnected("auth.not_connected");
      } else {
         List<CrossServerBattleService.Ranked> competitions = new ArrayList<>(this.ranked());
         if (competitions.isEmpty()) {
            return this.sendMainMenu(player, "");
         } else {
            String chosen = this.hasRanked(this.config().defaultRanked) ? this.config().defaultRanked : competitions.get(0).id();
            int ref = this.client.nextRef();
            JsonObject request = BattleServerClient.msg("leaderboard_query");
            request.addProperty("ref", ref);
            request.addProperty("ranked", chosen);
            JsonObject who = new JsonObject();
            who.addProperty("uuid", player.getUUID().toString());
            who.addProperty("name", player.getGameProfile().getName());
            request.add("player", who);
            this.menuRefs.put(ref, player.getUUID());
            if (!this.client.send(request)) {
               this.menuRefs.remove(ref);
               return this.sendMainMenu(player, "");
            } else {
               return null;
            }
         }
      }
   }

   private Component sendMainMenu(ServerPlayer player, String favourite) {
      List<OpenMainMenuPayload.RankedInfo> competitions = new ArrayList<>();

      for (CrossServerBattleService.Ranked ranked : this.ranked()) {
         competitions.add(
            new OpenMainMenuPayload.RankedInfo(
               ranked.id(),
               ranked.name(),
               ranked.battleType(),
               ranked.slots(),
               ranked.adjustLevel(),
               ranked.fullHeal(),
               ranked.winScore(),
               ranked.failScore(),
               ranked.rules()
            )
         );
      }

      String nickname = player.getGameProfile().getName();
      return !CobbleBattleNetwork.sendMainMenu(player, new OpenMainMenuPayload(nickname, favourite, competitions))
         ? Msg.of(ChatFormatting.RED, "cmd.dex.no_client")
         : null;
   }

   public void onTeamPicked(ServerPlayer player, String battleId, List<Integer> picks) {
      this.previews.onPicked(player, battleId, picks);
   }

   public void onMenuAction(ServerPlayer player, MenuActionPayload action) {
      String var3 = action.action();
      switch (var3) {
         case "queue":
            String chosen = !action.arg().isEmpty() && this.hasRanked(action.arg()) ? action.arg() : this.config().defaultRanked;
            Component refusal = this.queue(player, chosen);
            if (refusal != null) {
               this.tellPlayer(player.getUUID(), refusal);
            }
            break;
         case "logout":
            if (this.auth.isSignedIn(player.getUUID())) {
               this.signOut(player);
            }
      }
   }

   public void openPage(ServerPlayer player, String page, String ranked, String have) {
      Component refusal;
      if ("dex".equals(page)) {
         refusal = this.openServerDex(player, have);
      } else if ("rooms".equals(page)) {
         refusal = this.requestRooms(player);
      } else if ("main".equals(page)) {
         refusal = this.auth.isSignedIn(player.getUUID()) ? this.openMainMenu(player) : this.openAuthScreen(player);
      } else {
         String chosen = ranked != null && !ranked.isEmpty() && this.hasRanked(ranked) ? ranked : this.config().defaultRanked;
         refusal = this.requestLeaderboard(player, chosen);
      }

      if (refusal != null) {
         this.tellPlayer(player.getUUID(), refusal);
      }
   }

   public void onRoomAction(ServerPlayer player, RoomActionPayload action) {
      String var4 = action.action();

      Component refusal = switch (var4) {
         case "list" -> this.requestRooms(player, true);
         case "create" -> {
            String name = action.name().isBlank() ? Msg.raw("room.default_name", player.getGameProfile().getName()) : action.name();
            yield this.battleQueue
               .createRoom(
                  player,
                  name,
                  action.password(),
                  action.battleType(),
                  action.level(),
                  action.pick(),
                  action.fullHeal(),
                  action.hostEngine(),
                  action.legality()
               );
         }
         case "join" -> this.battleQueue.joinRoom(player, action.roomId(), action.password(), action.battleType(), action.hostEngine(), action.legality(), "");
         case "join_code" -> this.battleQueue.lookupRoom(player, action.inviteCode());
         case "leave" -> this.battleQueue.leaveRoom(player);
         case "start" -> this.battleQueue.startRoom(player);
         default -> null;
      };
      if (refusal != null) {
         this.tellPlayer(player.getUUID(), refusal);
      }
   }

   private void onRoomState(JsonObject message) {
      UUID who = uuidOrNull(BattleServerClient.str(message, "player", ""));
      if (who != null) {
         RoomStatePayload.Member host = readMember(message, "host");
         boolean hasGuest = message.has("guest") && message.get("guest").isJsonObject();
         RoomStatePayload.Member guest = hasGuest ? readMember(message, "guest") : RoomStatePayload.Member.NOBODY;
         List<RoomStatePayload.Member> watchers = new ArrayList<>();
         if (message.has("watchers") && message.get("watchers").isJsonArray()) {
            for (JsonElement el : message.getAsJsonArray("watchers")) {
               if (el.isJsonObject()) {
                  watchers.add(memberOf(el.getAsJsonObject()));
               }
            }
         }

         RoomStatePayload state = new RoomStatePayload(
            BattleServerClient.str(message, "roomId", ""),
            BattleServerClient.str(message, "inviteCode", ""),
            BattleServerClient.str(message, "name", ""),
            BattleServerClient.bool(message, "locked", false),
            BattleServerClient.str(message, "battleType", "singles"),
            BattleServerClient.integer(message, "level", -1),
            BattleServerClient.integer(message, "pick", 6),
            BattleServerClient.bool(message, "fullHeal", true),
            "host".equals(BattleServerClient.str(message, "engine", "server")),
            BattleServerClient.bool(message, "legality", true),
            BattleServerClient.bool(message, "fighting", false),
            host,
            hasGuest,
            guest,
            watchers,
            BattleServerClient.str(message, "youAre", "watcher")
         );
         this.withPlayer(who, player -> CobbleBattleNetwork.sendRoomState(player, state));
      }
   }

   private static RoomStatePayload.Member readMember(JsonObject message, String key) {
      return message.has(key) && message.get(key).isJsonObject() ? memberOf(message.getAsJsonObject(key)) : RoomStatePayload.Member.NOBODY;
   }

   private static RoomStatePayload.Member memberOf(JsonObject o) {
      return new RoomStatePayload.Member(
         BattleServerClient.str(o, "name", "?"),
         o.has("uid") ? o.get("uid").getAsLong() : 0L,
         BattleServerClient.str(o, "lead", ""),
         BattleServerClient.integer(o, "teamSize", 0)
      );
   }

   private void onRoomClosed(JsonObject message) {
      this.battleQueue.onRoomClosed(message);
      UUID who = uuidOrNull(BattleServerClient.str(message, "player", ""));
      if (who != null) {
         this.refreshRooms(who);
      }
   }

   public Component requestRooms(ServerPlayer player) {
      return this.requestRooms(player, false);
   }

   public Component requestRooms(ServerPlayer player, boolean refresh) {
      if (!this.isConnected()) {
         return this.notConnected("auth.not_connected");
      } else {
         CrossServerBattleService.RoomBoard board = this.roomCache;
         long age = board == null ? Long.MAX_VALUE : System.currentTimeMillis() - board.at();
         if (board != null && age < (refresh ? 4500L : 1000L)) {
            this.deliverRooms(player.getUUID(), board, refresh);
            return null;
         } else {
            this.roomWaiters.merge(player.getUUID(), refresh, (existing, added) -> existing && added);
            if (this.roomFetchInFlight) {
               return null;
            } else {
               JsonObject request = BattleServerClient.msg("room_list");
               request.addProperty("ref", this.client.nextRef());
               if (board != null) {
                  request.addProperty("hash", board.hash());
               }

               if (!this.client.send(request)) {
                  this.roomWaiters.remove(player.getUUID());
                  return Msg.of(ChatFormatting.RED, "auth.send_failed");
               } else {
                  this.roomFetchInFlight = true;
                  return null;
               }
            }
         }
      }
   }

   void refreshRooms(UUID playerUuid) {
      if (CrossServerBattles.byLocalPlayer(playerUuid) == null) {
         this.withPlayer(playerUuid, player -> {
            this.roomCache = null;
            this.requestRooms(player);
         });
      }
   }

   private void onRoomList(JsonObject message) {
      this.roomFetchInFlight = false;
      CrossServerBattleService.RoomBoard board;
      if (BattleServerClient.bool(message, "unchanged", false) && this.roomCache != null) {
         board = new CrossServerBattleService.RoomBoard(this.roomCache.rooms(), this.roomCache.hash(), System.currentTimeMillis());
      } else {
         List<RoomListPayload.Room> rooms = new ArrayList<>();

         for (JsonElement el : message.has("rooms") && message.get("rooms").isJsonArray() ? message.getAsJsonArray("rooms") : new JsonArray()) {
            if (el.isJsonObject()) {
               JsonObject o = el.getAsJsonObject();
               rooms.add(
                  new RoomListPayload.Room(
                     BattleServerClient.str(o, "id", ""),
                     BattleServerClient.str(o, "name", ""),
                     BattleServerClient.str(o, "host", "?"),
                     o.has("hostUid") && !o.get("hostUid").isJsonNull() ? o.get("hostUid").getAsLong() : 0L,
                     BattleServerClient.str(o, "battleType", "singles"),
                     BattleServerClient.integer(o, "level", -1),
                     BattleServerClient.integer(o, "pick", 6),
                     BattleServerClient.bool(o, "fullHeal", true),
                     BattleServerClient.bool(o, "locked", false),
                     BattleServerClient.str(o, "lead", ""),
                     BattleServerClient.bool(o, "hasGuest", false),
                     BattleServerClient.integer(o, "watchers", 0),
                     false,
                     "host".equals(BattleServerClient.str(o, "engine", "server")),
                     BattleServerClient.bool(o, "fighting", false),
                     BattleServerClient.bool(o, "legality", true)
                  )
               );
            }
         }

         board = new CrossServerBattleService.RoomBoard(List.copyOf(rooms), BattleServerClient.str(message, "hash", ""), System.currentTimeMillis());
      }

      this.roomCache = board;
      Map<UUID, Boolean> waiting = Map.copyOf(this.roomWaiters);
      this.roomWaiters.clear();

      for (Entry<UUID, Boolean> entry : waiting.entrySet()) {
         this.deliverRooms(entry.getKey(), board, entry.getValue());
      }
   }

   private void deliverRooms(UUID playerUuid, CrossServerBattleService.RoomBoard board, boolean refresh) {
      this.withPlayer(
         playerUuid,
         player -> {
            long uid = this.auth.uidOf(player.getUUID());
            List<RoomListPayload.Room> mine = new ArrayList<>(board.rooms().size());
            boolean own = false;

            for (RoomListPayload.Room room : board.rooms()) {
               boolean ours = uid != 0L && room.hostUid() == uid;
               own |= ours;
               mine.add(
                  ours
                     ? new RoomListPayload.Room(
                        room.id(),
                        room.name(),
                        room.host(),
                        room.hostUid(),
                        room.battleType(),
                        room.level(),
                        room.pick(),
                        room.fullHeal(),
                        room.locked(),
                        room.lead(),
                        room.hasGuest(),
                        room.watchers(),
                        true,
                        room.hostEngine(),
                        room.fighting(),
                        room.legality()
                     )
                     : room
               );
            }

            String stamp = board.hash() + (own ? ":own" : "");
            if (!refresh || !stamp.equals(this.roomBoardSent.get(player.getUUID()))) {
               if (CobbleBattleNetwork.sendRooms(player, new RoomListPayload(mine, refresh))) {
                  this.roomBoardSent.put(player.getUUID(), stamp);
               }
            }
         }
      );
   }

   public Component openServerDex(ServerPlayer player) {
      return this.openServerDex(player, "");
   }

   public Component openServerDex(ServerPlayer player, String have) {
      if (!this.isConnected()) {
         return this.notConnected("auth.not_connected");
      } else if (!this.dex.isReady()) {
         return Msg.of(ChatFormatting.RED, "cmd.dex.not_ready");
      } else {
         String digest = this.dex.digest() == null ? "" : this.dex.digest();
         if (have.isEmpty() || !have.equals(digest)) {
            List<ServerDexPayload.Entry> entries = new ArrayList<>();

            for (RemoteDex.Entry entry : this.dex.entries()) {
               Map<String, Integer> stats = entry.baseStats();
               entries.add(
                  new ServerDexPayload.Entry(
                     entry.id(),
                     stats.getOrDefault("hp", 0),
                     stats.getOrDefault("atk", 0),
                     stats.getOrDefault("def", 0),
                     stats.getOrDefault("spa", 0),
                     stats.getOrDefault("spd", 0),
                     stats.getOrDefault("spe", 0)
                  )
               );
            }

            return !CobbleBattleNetwork.sendServerDex(player, ServerDexPayload.of(digest, entries)) ? Msg.of(ChatFormatting.RED, "cmd.dex.no_client") : null;
         } else {
            return !CobbleBattleNetwork.sendServerDex(player, ServerDexPayload.unchanged(digest)) ? Msg.of(ChatFormatting.RED, "cmd.dex.no_client") : null;
         }
      }
   }

   public Component openAuthScreen(ServerPlayer player) {
      if (!this.isConnected()) {
         return this.connectThenOpenAuth(player);
      } else {
         String suggested = this.auth.accountOf(player.getUUID());
         if (suggested == null) {
            suggested = player.getGameProfile().getName();
         }

         return !CobbleBattleNetwork.openScreen(player, AuthMode.LOGIN, suggested, this.emailEnabled)
            ? Msg.of(ChatFormatting.RED, "auth.client_required")
            : null;
      }
   }

   public void onCredentialsSubmitted(ServerPlayer player, AuthMode mode, String accountId, String email, String password, String verificationCode) {
      Component refusal = this.auth.submit(this.client, player, mode, accountId, email, password, verificationCode, this.emailEnabled);
      if (refusal != null) {
         CobbleBattleNetwork.sendResult(player, false, refusal);
      }
   }

   public void signOut(ServerPlayer player) {
      this.announceSignOut(player);
      this.auth.signOut(this.client, player);
      this.pushChatState(player);
   }

   private void announceSignOut(ServerPlayer player) {
      if (this.auth.isSignedIn(player.getUUID())) {
         ApiEvents.signedOut(player, this.auth.accountOf(player.getUUID()), this.auth.nicknameOf(player.getUUID()), this.auth.uidOf(player.getUUID()));
      }
   }

   private void onChat(JsonObject message) {
      String channel = BattleServerClient.str(message, "channel", "global");
      String text = BattleServerClient.str(message, "text", "");
      if (!text.isEmpty()) {
         UUID sender = uuidOrNull(BattleServerClient.str(message, "player", ""));
         ChatLinePayload line = new ChatLinePayload(
            channel,
            message.has("uid") ? message.get("uid").getAsLong() : 0L,
            BattleServerClient.str(message, "id", ""),
            BattleServerClient.str(message, "name", "?"),
            sender == null ? new UUID(0L, 0L) : sender,
            text
         );
         MinecraftServer server = this.server();
         if (server != null) {
            if ("battle".equals(channel)) {
               MirrorBattle mirror = CrossServerBattles.byRemoteId(BattleServerClient.str(message, "battleId", ""));
               if (mirror != null) {
                  for (UUID seated : mirror.localPlayers()) {
                     this.withPlayer(seated, player -> CobbleBattleNetwork.sendChatLine(player, line));
                  }
               }
            } else {
               for (UUID playerUuid : this.auth.signedInPlayers()) {
                  this.withPlayer(playerUuid, player -> CobbleBattleNetwork.sendChatLine(player, line));
               }
            }
         }
      }
   }

   private static UUID uuidOrNull(String raw) {
      try {
         return raw != null && !raw.isBlank() ? UUID.fromString(raw) : null;
      } catch (IllegalArgumentException var2) {
         return null;
      }
   }

   private UUID claimChatRef(JsonElement ref) {
      return claimRef(this.chatRefs, ref);
   }

   private static UUID claimRef(Map<Integer, UUID> refs, JsonElement ref) {
      return ref != null && !ref.isJsonNull() ? refs.remove(ref.getAsInt()) : null;
   }

   public Component requestLeaderboard(ServerPlayer player, String rankedId) {
      if (!this.isConnected()) {
         return this.notConnected("auth.not_connected");
      } else {
         int ref = this.client.nextRef();
         JsonObject request = BattleServerClient.msg("leaderboard_query");
         request.addProperty("ref", ref);
         request.addProperty("ranked", rankedId);
         JsonObject who = new JsonObject();
         who.addProperty("uuid", player.getUUID().toString());
         who.addProperty("name", player.getGameProfile().getName());
         request.add("player", who);
         this.boardRefs.put(ref, player.getUUID());
         if (!this.client.send(request)) {
            this.boardRefs.remove(ref);
            return Msg.of(ChatFormatting.RED, "auth.send_failed");
         } else {
            return null;
         }
      }
   }

   private void onLeaderboard(JsonObject message) {
      UUID forMenu = claimRef(this.menuRefs, message.get("ref"));
      if (forMenu != null) {
         String favourite = message.has("you") && message.get("you").isJsonObject()
            ? BattleServerClient.str(message.getAsJsonObject("you"), "favourite", "")
            : "";
         this.withPlayer(forMenu, player -> {
            Component refusal = this.sendMainMenu(player, favourite);
            if (refusal != null) {
               this.tellPlayer(forMenu, refusal);
            }
         });
      } else {
         UUID asker = claimRef(this.boardRefs, message.get("ref"));
         if (asker != null) {
            List<LeaderboardPayload.Entry> top = new ArrayList<>();

            for (JsonElement el : message.has("top") && message.get("top").isJsonArray() ? message.getAsJsonArray("top") : new JsonArray()) {
               if (el.isJsonObject()) {
                  top.add(entryOf(el.getAsJsonObject()));
               }
            }

            LeaderboardPayload.Entry you = message.has("you") && message.get("you").isJsonObject()
               ? entryOf(message.getAsJsonObject("you"))
               : LeaderboardPayload.Entry.NONE;
            LeaderboardPayload board = new LeaderboardPayload(
               BattleServerClient.str(message, "ranked", ""),
               BattleServerClient.str(message, "name", ""),
               BattleServerClient.integer(message, "players", 0),
               top,
               you
            );
            this.withPlayer(asker, player -> CobbleBattleNetwork.sendLeaderboard(player, board));
         }
      }
   }

   private static LeaderboardPayload.Entry entryOf(JsonObject o) {
      return new LeaderboardPayload.Entry(
         BattleServerClient.integer(o, "rank", 0),
         o.has("uid") ? o.get("uid").getAsLong() : 0L,
         BattleServerClient.str(o, "name", "?"),
         o.has("score") ? o.get("score").getAsLong() : 0L,
         BattleServerClient.integer(o, "wins", 0),
         BattleServerClient.integer(o, "losses", 0),
         BattleServerClient.integer(o, "streak", 0),
         BattleServerClient.str(o, "favourite", "")
      );
   }

   private void fireBattleEnded(MirrorBattle mirror, JsonObject message, String reason) {
      String winnerSeat = BattleServerClient.str(message, "winnerSeat", "");

      for (UUID who : mirror.localPlayers()) {
         BattleInfo info = mirror.infoFor(who);
         if (info != null) {
            BattleOutcome outcome;
            if ("win".equals(reason) && !winnerSeat.isEmpty()) {
               outcome = winnerSeat.equals(mirror.seatOf(who)) ? BattleOutcome.WIN : BattleOutcome.LOSS;
            } else if ("tie".equals(reason)) {
               outcome = BattleOutcome.TIE;
            } else {
               outcome = BattleOutcome.ABORTED;
            }

            ScoreChange score = this.scoreFor(message, who);
            this.withPlayer(who, player -> ApiEvents.battleEnded(player, info, outcome, reason, score));
         }
      }
   }

   private ScoreChange scoreFor(JsonObject message, UUID who) {
      if (message.has("scores") && message.get("scores").isJsonArray()) {
         for (JsonElement el : message.getAsJsonArray("scores")) {
            if (el.isJsonObject()) {
               JsonObject o = el.getAsJsonObject();
               if (who.equals(uuidOrNull(BattleServerClient.str(o, "player", "")))) {
                  return new ScoreChange(o.has("before") ? o.get("before").getAsLong() : 0L, o.has("after") ? o.get("after").getAsLong() : 0L);
               }
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private void announceScores(JsonObject message) {
      if (message.has("scores") && message.get("scores").isJsonArray()) {
         for (JsonElement el : message.getAsJsonArray("scores")) {
            if (el.isJsonObject()) {
               JsonObject o = el.getAsJsonObject();
               UUID who = uuidOrNull(BattleServerClient.str(o, "player", ""));
               if (who != null) {
                  long before = o.has("before") ? o.get("before").getAsLong() : 0L;
                  long after = o.has("after") ? o.get("after").getAsLong() : 0L;
                  long delta = after - before;
                  boolean won = BattleServerClient.bool(o, "won", false);
                  String signed = (delta >= 0L ? "+" : "") + delta;
                  this.tellPlayer(who, Msg.of(won ? ChatFormatting.GREEN : ChatFormatting.RED, won ? "rank.won" : "rank.lost", signed, before, after));
               }
            }
         }
      }
   }

   private static Component describeChatFailure(JsonObject message, String code, String fallback) {
      if ("MUTED".equals(code)) {
         long left = message.has("left") ? message.get("left").getAsLong() : 0L;
         Component muted = left == 0L ? Msg.of("chat.err.muted_permanent") : Msg.of("chat.err.muted_for", describeDuration(left));
         return muted.copy().withStyle(ChatFormatting.RED);
      } else {
         String key = switch (code) {
            case "CHAT_TOO_FAST" -> "chat.err.too_fast";
            case "NOT_IN_BATTLE" -> "chat.err.not_in_battle";
            case "CHAT_DISABLED" -> "chat.err.disabled";
            case "NOT_LOGGED_IN" -> "queue.not_signed_in";
            default -> null;
         };
         Component text = key == null ? Component.literal(fallback) : Msg.of(key);
         return text.copy().withStyle(ChatFormatting.RED);
      }
   }

   public void onChatSubmitted(ServerPlayer player, String channel, String text) {
      if (text != null && !text.isBlank()) {
         if (!this.auth.isSignedIn(player.getUUID())) {
            this.tellPlayer(player.getUUID(), Msg.of(ChatFormatting.YELLOW, "queue.not_signed_in"));
         } else if (!this.isConnected()) {
            this.tellPlayer(player.getUUID(), this.notConnected("chat.not_connected"));
         } else if (!this.chatEnabled) {
            this.tellPlayer(player.getUUID(), Msg.of(ChatFormatting.RED, "chat.err.disabled"));
         } else {
            JsonObject request = BattleServerClient.msg("chat_send");
            int ref = this.client.nextRef();
            request.addProperty("ref", ref);
            request.addProperty("channel", "battle".equals(channel) ? "battle" : "global");
            request.addProperty("text", text);
            JsonObject who = new JsonObject();
            who.addProperty("uuid", player.getUUID().toString());
            who.addProperty("name", player.getGameProfile().getName());
            request.add("player", who);
            this.chatRefs.put(ref, player.getUUID());
            this.client.send(request);
         }
      }
   }

   public void pushChatState(ServerPlayer player) {
      UUID uuid = player.getUUID();
      if (!this.auth.isSignedIn(uuid)) {
         CobbleBattleNetwork.sendChatState(player, ChatStatePayload.signedOut());
         this.reportChatWatchers();
      } else {
         CobbleBattleNetwork.sendChatState(
            player,
            new ChatStatePayload(
               true, CrossServerBattles.byLocalPlayer(uuid) != null, this.auth.uidOf(uuid), String.valueOf(this.auth.nicknameOf(uuid)), this.chatEnabled
            )
         );
         this.reportChatWatchers();
      }
   }

   private void reportChatWatchers() {
      if (this.client != null && this.client.isHandshaken()) {
         int watchers = this.auth.signedInPlayers().size();
         if (watchers != this.chatWatchersReported) {
            JsonObject frame = BattleServerClient.msg("chat_watch");
            frame.addProperty("watchers", watchers);
            if (this.client.send(frame)) {
               this.chatWatchersReported = watchers;
            }
         }
      }
   }

   public boolean chatEnabled() {
      return this.chatEnabled;
   }

   public boolean emailEnabled() {
      return this.emailEnabled;
   }

   public void pushChatState(UUID playerUuid) {
      this.withPlayer(playerUuid, this::pushChatState);
   }

   private void onAccountCodeOk(JsonObject message) {
      UUID player = this.auth.onAccountCodeOk(message);
      this.withPlayer(player, p -> CobbleBattleNetwork.sendResult(p, true, Component.translatable("auth.code_sent")));
   }

   private void onAccountOk(JsonObject message) {
      AuthService.Outcome outcome = this.auth.onAccountOk(message);
      if (outcome != null) {
         Component text = Msg.of(
            ChatFormatting.GREEN, outcome.registered() ? "auth.registered" : "auth.logged_in", outcome.accountId(), outcome.nickname(), outcome.uid()
         );
         this.tellPlayer(outcome.playerUuid(), text);
         this.withPlayer(outcome.playerUuid(), player -> {
            CobbleBattleNetwork.sendResult(player, true, text);
            this.pushChatState(player);
            ApiEvents.signedIn(player, outcome.accountId(), outcome.nickname(), outcome.uid(), outcome.registered());
            Component refusal = this.openMainMenu(player);
            if (refusal != null) {
               this.tellPlayer(outcome.playerUuid(), refusal);
            }
         });
      }
   }

   void onServerThreadWithPlayer(UUID playerUuid, Consumer<ServerPlayer> action) {
      this.withPlayer(playerUuid, action);
   }

   private void withPlayer(UUID playerUuid, Consumer<ServerPlayer> action) {
      MinecraftServer server = this.minecraftServer;
      if (server != null) {
         server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
            if (player != null) {
               action.accept(player);
            }
         });
      }
   }

   public Component queue(ServerPlayer player, String rankedId) {
      return this.battleQueue.join(player, rankedId);
   }

   public Component leaveQueue(ServerPlayer player) {
      return this.battleQueue.leave(player);
   }

   public Component describePartyCompatibility(ServerPlayer player) {
      return this.battleQueue.describePartyCompatibility(player);
   }

   public void onPlayerDisconnect(ServerPlayer player) {
      this.pendingAuthOpens.remove(player.getUUID());
      this.scheduleIdleDisconnect();
      this.battleQueue.onPlayerDisconnect(player);
      this.previews.forget(player.getUUID());
      MirrorBattle mirror = CrossServerBattles.byLocalPlayer(player.getUUID());
      if (mirror != null && !mirror.isFinished()) {
         LOGGER.info("{} disconnected during battle {} - closing this side and telling the host", player.getGameProfile().getName(), mirror.remoteBattleId());
         this.sendAbort(mirror.remoteBattleId(), "player disconnected");
         this.teardown.abort(mirror, Msg.of("battle.player_left").withStyle(ChatFormatting.RED));
      }

      this.announceSignOut(player);
      this.auth.signOut(this.client, player);
      this.reportChatWatchers();
      this.roomBoardSent.remove(player.getUUID());
   }

   void tellPlayer(UUID playerUuid, Component message) {
      MinecraftServer server = this.minecraftServer;
      if (server != null) {
         server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
            if (player != null) {
               player.sendSystemMessage(message);
            }
         });
      }
   }

   public record Ranked(
      String id, String name, String battleType, int slots, int adjustLevel, boolean fullHeal, String winScore, String failScore, List<String> rules
   ) {
   }

   private record RoomBoard(List<RoomListPayload.Room> rooms, String hash, long at) {
   }
}
