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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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
import io.github.rinicesiberia.shadowbattle.battle.BattleResultProjection;
import io.github.rinicesiberia.shadowbattle.battle.BattleIdentifierParsing;
import io.github.rinicesiberia.shadowbattle.battle.AuthenticationErrorRules;
import io.github.rinicesiberia.shadowbattle.battle.ChatErrorRules;
import io.github.rinicesiberia.shadowbattle.battle.LeaderboardDecoding;
import io.github.rinicesiberia.shadowbattle.battle.HandshakeResponseDecoding;
import io.github.rinicesiberia.shadowbattle.battle.QueueErrorRules;
import io.github.rinicesiberia.shadowbattle.battle.RankedCompetitionDecoding;
import io.github.rinicesiberia.shadowbattle.battle.RoomDirectoryState;
import io.github.rinicesiberia.shadowbattle.battle.RoomListDecoding;
import io.github.rinicesiberia.shadowbattle.battle.RoomListPersonalization;
import io.github.rinicesiberia.shadowbattle.battle.RoomStateDecoding;
import io.github.rinicesiberia.shadowbattle.battle.ServiceRequestLedger;
import io.github.rinicesiberia.shadowbattle.transport.BattleControlMessages;
import io.github.rinicesiberia.shadowbattle.transport.ServiceRequests;
import io.github.rinicesiberia.shadowbattle.transport.ServiceProtocolMessages;

public final class CrossServerBattleService {
   private static final Logger LOGGER = LoggerFactory.getLogger("CobbleBattle");
   private final CobbleBattleConfig config;
   private final RemoteDex dex = new RemoteDex();
   private final BattleQueue battleQueue = new BattleQueue(this);
   private final MirrorFactory mirrorFactory = new MirrorFactory(this);
   private final MirrorLifecycleCleanup lifecycleCleanup = new MirrorLifecycleCleanup(this);
   private final SpectatorFactory spectators = new SpectatorFactory(this);
   private final TeamPreviews previews = new TeamPreviews(this);
   private final AuthService auth = new AuthService();
   private final Map<String, CrossServerBattleService.Ranked> ranked = new LinkedHashMap<>();
   private final ServiceRequestLedger requestLedger = new ServiceRequestLedger();
   private BattleServerClient client;
   private MinecraftServer minecraftServer;
   private final Set<UUID> waitingChunksAuthOpens = ConcurrentHashMap.newKeySet();
   private ScheduledExecutorService idleTimer;
   private ScheduledFuture<?> idleDisconnect;
   private static final int IDLE_DISCONNECT_MIN_SECONDS = 5;
   private final RoomDirectoryState<List<RoomListPayload.Room>> roomDirectory = new RoomDirectoryState<>();
   private volatile int chatObserversReported = -1;
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

   public boolean isQueued(UUID participantUuid) {
      return this.battleQueue.contains(participantUuid);
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

   ServerPlayer participantOf(UUID participantUuid) {
      MinecraftServer server = this.minecraftServer;
      return server == null ? null : server.getPlayerList().getPlayer(participantUuid);
   }

   public void onServerStarted(MinecraftServer server) {
      this.minecraftServer = server;
      this.dex.loadFromDisk();
      CrossServerBattles.setChoiceRelay(this::relayChoice);
      CrossServerBattles.setOutputRelay(this::relayOutput);
      this.client = new BattleServerClient(this.config, this::onDocument, this::onConnected, this::onDisconnected);
      this.client.setOnConnectFailed(this::onConnectFailed);
      this.client.start();
      if (this.config.keepConnectedWhenEmpty || server.getPlayerCount() > 0) {
         this.client.connect();
      }
   }

   public void onPlayerJoin(ServerPlayer participant) {
      this.cancelIdleDisconnect();
      this.ensureConnected();
      this.pushChatState(participant);
   }

   private Component connectThenOpenAuth(ServerPlayer participant) {
      String refused = this.connectionRefusal();
      this.waitingChunksAuthOpens.add(participant.getUUID());
      if (this.client != null) {
         this.client.connect();
      }

      return refused != null ? Msg.of(ChatFormatting.YELLOW, "auth.connecting_retry", refused) : Msg.of(ChatFormatting.YELLOW, "auth.connecting");
   }

   private void openWaitingChunksAuth() {
      for (UUID waiting : List.copyOf(this.waitingChunksAuthOpens)) {
         this.waitingChunksAuthOpens.remove(waiting);
         this.onServerThreadWithParticipant(waiting, participant -> {
            Component refusal = this.openAuthScreen(participant);
            if (refusal != null) {
               this.tellParticipant(waiting, refusal);
            }
         });
      }
   }

   private void onConnectFailed(String why) {
      for (UUID waiting : List.copyOf(this.waitingChunksAuthOpens)) {
         this.waitingChunksAuthOpens.remove(waiting);
         this.tellParticipant(waiting, Msg.of(ChatFormatting.RED, "auth.connect_failed", why));
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
         this.lifecycleCleanup.sweepEntities(mirror, 0L);
      }

      CrossServerBattles.clear();
      this.battleQueue.clear();
      this.previews.clear();
   }

   private void onConnected() {
      JsonObject hello = ServiceProtocolMessages.hello(
         this.client.nextRef(),
         this.config.authToken,
         ServerIdentity.get(),
         Platform.isModLoaded("cobblemon") ? Platform.getMod("cobblemon").getVersion() : "unknown"
      );
      this.client.sendHandshake(hello);
   }

   private void onDisconnected(String reason) {
      if (this.client != null && !this.client.isWanted()) {
         LOGGER.info("Battle server connection closed ({}).", reason);
      } else {
         LOGGER.warn("Lost the battle server ({}). Aborting {} mirror battle(s).", reason, CrossServerBattles.size());
      }

      this.dex.suspend(reason);
      this.chatObserversReported = -1;
      this.onServerThread(this.roomDirectory::clearSession);
      this.battleQueue.clear();
      this.previews.clear();
      this.auth.clear();

      for (MirrorBattle mirror : CrossServerBattles.all()) {
         this.lifecycleCleanup.abort(mirror, Msg.of("battle.connection_lost").withStyle(ChatFormatting.RED));
      }
   }

   private void onDocument(JsonObject document) {
      String type = BattleServerClient.str(document, "t", "");
      switch (type) {
         case "ping":
            this.client.send(BattleServerClient.msg("pong"));
            break;
         case "hello_ack":
            this.onHelloAck(document);
            break;
         case "ranked_update":
            this.readRanked(document);
            break;
         case "dex_snapshot":
            this.dex.accept(document);
            break;
         case "queue_ack":
            this.battleQueue.onQueueAck(document);
            break;
         case "queue_left":
            this.battleQueue.onQueueLeft(document);
            break;
         case "queue_wait":
            this.battleQueue.onQueueWait(document);
            break;
         case "room_created":
            this.battleQueue.onRoomCreated(document);
            break;
         case "room_list":
            this.onServerThread(() -> this.onRoomList(document));
            break;
         case "room_state":
            this.onRoomState(document);
            break;
         case "room_info":
            this.battleQueue.onRoomBattleDetails(document);
            break;
         case "room_closed":
            this.onRoomClosed(document);
            break;
         case "preview_open":
            this.onServerThread(() -> this.previews.onOpen(document));
            break;
         case "preview_state":
            this.onServerThread(() -> this.previews.onState(document));
            break;
         case "preview_closed":
            this.onServerThread(() -> this.previews.onClosed(document));
            break;
         case "match_found":
            this.onMatchFound(document);
            break;
         case "battle_start":
            this.onBattleStart(document);
            break;
         case "output":
            this.onOutput(document);
            break;
         case "battle_choice":
            this.onRelayedChoice(document);
            break;
         case "spectate_start":
            this.onServerThread(() -> this.spectators.begin(document));
            break;
         case "spectate_end":
            this.onServerThread(() -> this.spectators.end(document));
            break;
         case "battle_end":
            this.onBattleEnd(document);
            break;
         case "forfeit":
            this.onOpponentForfeit(document);
            break;
         case "chat":
            this.onChat(document);
            break;
         case "leaderboard":
            this.onLeaderboard(document);
            break;
         case "account_ok":
            this.onAccountOk(document);
            break;
         case "account_code_ok":
            this.onAccountCodeOk(document);
            break;
         case "error":
            this.onError(document);
            break;
         default:
            LOGGER.warn("Unknown message type '{}' from the battle server", type);
      }
   }

   private void onHelloAck(JsonObject document) {
      this.client.setHandshaken(true);
      HandshakeResponseDecoding.Settings settings = HandshakeResponseDecoding.decode(document);
      this.readRanked(document);
      this.dex.setStrictBaseStats(settings.getStrictBaseStats());
      this.dex
         .setTeamRules(
            settings.getStrictAbilities(), settings.getStrictMoves(), settings.getMaxEvPerStat(), settings.getMaxEvTotal(), settings.getMaxIv()
         );
      this.chatEnabled = settings.getChatEnabled();
      if (!this.chatEnabled) {
         LOGGER.info("The battle server has chat switched off; the chat panel stays hidden");
      }

      this.emailEnabled = settings.getEmailEnabled();
      if (!this.emailEnabled) {
         LOGGER.info("The battle server has no mailer; the account screen stays on account names");
      }

      LOGGER.info(
         "Handshake complete with battle server instance '{}' (dex {}, {} species)",
         new Object[]{
            settings.getInstance(), settings.getDexReady() ? "ready" : "NOT ready", settings.getSpeciesCount()
         }
      );
      this.reportChatObservers();
      this.openWaitingChunksAuth();
      HandshakeResponseDecoding.DexAction dexAction = HandshakeResponseDecoding.decideDex(settings, this.dex.cachedDigest());
      if (dexAction == HandshakeResponseDecoding.DexAction.Invalidate.INSTANCE) {
         this.dex.invalidate("the battle server has no dex");
         LOGGER.warn(
            "The battle server has no dex yet. Cross-server battles stay unavailable until it has one - the Cobblemon jar belongs in its cobblemon/ folder."
         );
      } else if (dexAction instanceof HandshakeResponseDecoding.DexAction.AcceptCached cached) {
         this.dex.accept(unchangedDex(cached.getDigest()));
      } else {
         this.requestDex();
      }
   }

   private static JsonObject unchangedDex(String digest) {
      JsonObject same = new JsonObject();
      same.addProperty("digest", digest);
      same.addProperty("unchanged", true);
      return same;
   }

   private void readRanked(JsonObject document) {
      this.ranked.clear();
      this.ranked.putAll(RankedCompetitionDecoding.decode(document));

      if (this.ranked.isEmpty()) {
         LOGGER.warn("The battle server offers no ranked competitions - nobody can queue. Its ranked/ folder is empty, or every file in it was refused.");
      } else {
         LOGGER.info("Ranked competitions offered: {}", String.join(", ", this.ranked.keySet()));
      }
   }

   private void requestDex() {
      String cached = this.dex.cachedDigest();
      JsonObject query = ServiceProtocolMessages.dexQuery(this.client.nextRef(), cached);
      this.client.send(query);
   }

   private void onError(JsonObject document) {
      String code = BattleServerClient.str(document, "code", "?");
      String text = BattleServerClient.str(document, "message", "");
      LOGGER.warn("Battle server error [{}]: {}", code, text);
      if (!"hello".equals(BattleServerClient.str(document, "about", ""))) {
         if ("room_list".equals(BattleServerClient.str(document, "about", ""))) {
            this.onServerThread(this.roomDirectory::cancelRequests);
         }

         MirrorBattle mirror = CrossServerBattles.byRemoteId(BattleServerClient.str(document, "battleId", ""));
         if (mirror != null && !mirror.isFinished()) {
            LOGGER.error("Battle server refused something for battle {} ({}: {}) - closing the local mirror", new Object[]{mirror.remoteBattleId(), code, text});
            this.lifecycleCleanup.abort(mirror, Msg.of("battle.ended", text).withStyle(ChatFormatting.RED));
         } else if (this.auth.isAwaiting(document)) {
            UUID waiting = this.auth.onAccountError(document);
            if (waiting != null) {
               Component refusal = describeAuthFailure(code, text);
               this.tellParticipant(waiting, refusal);
               this.withParticipant(waiting, participant -> CobbleBattleNetwork.sendResult(participant, false, refusal));
            }
         } else {
            UUID talker = this.claimChatRef(document.get("ref"));
            if (talker != null) {
               this.tellParticipant(talker, describeChatFailure(document, code, text));
            } else {
               UUID forMenu = this.claimMenuRef(document.get("ref"));
               if (forMenu != null) {
                  this.withParticipant(forMenu, player -> this.sendMainMenu(player, ""));
               } else {
                  UUID asker = this.claimLeaderboardRef(document.get("ref"));
                  if (asker != null) {
                     this.tellParticipant(asker, Component.literal(text).withStyle(ChatFormatting.RED));
                  } else {
                     UUID looker = this.battleQueue.claimLookupRef(document.get("ref"));
                     if (looker != null) {
                        this.tellParticipant(looker, describeLookupFailure(code, text));
                     } else {
                        UUID owner = this.battleQueue.claimRefOwner(document.get("ref"));
                        if (owner != null) {
                           this.battleQueue.drop(owner);

                           String roomKey = QueueErrorRules.roomRefusalKey(code);
                           if (roomKey != null) {
                              this.tellParticipant(owner, Msg.of(ChatFormatting.RED, roomKey));
                              this.refreshRooms(owner);
                           } else if ("BANNED".equals(code)) {
                              long left = document.has("left") ? document.get("left").getAsLong() : 0L;
                              String ranked = BattleServerClient.str(document, "ranked", "?");
                              this.tellParticipant(
                                 owner,
                                 (left == 0L ? Msg.of("queue.banned_permanent", ranked) : Msg.of("queue.banned_for", ranked, describeDuration(left)))
                                    .withStyle(ChatFormatting.RED)
                              );
                           } else {
                              this.tellParticipant(owner, Msg.of("queue.refused", text).withStyle(ChatFormatting.RED));
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

         for (UUID waiting : List.copyOf(this.waitingChunksAuthOpens)) {
            this.waitingChunksAuthOpens.remove(waiting);
            this.tellParticipant(waiting, Msg.of(ChatFormatting.RED, "conn.refused", why));
         }
      }
   }

   private static Component describeLookupFailure(String code, String fallback) {
      String key = QueueErrorRules.lookupFailureKey(code);
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
      StringBuilder outputStream = new StringBuilder();
      int parts = 0;
      if (days > 0L) {
         outputStream.append(Msg.of("time.days", days).getString());
         parts++;
      }

      if (hours > 0L && parts < 2) {
         outputStream.append(Msg.of("time.hours", hours).getString());
         parts++;
      }

      if (minutes > 0L && parts < 2) {
         outputStream.append(Msg.of("time.minutes", minutes).getString());
         parts++;
      }

      if (parts == 0) {
         outputStream.append(Msg.of("time.seconds", Math.max(1L, secs)).getString());
      }

      return outputStream.toString().trim();
   }

   private static Component describeAuthFailure(String code, String fallback) {
      String key = AuthenticationErrorRules.translationKey(code);
      Component text = key == null ? Component.literal(fallback) : Msg.of(key);
      return text.copy().withStyle(ChatFormatting.RED);
   }

   private void onServerThread(Runnable work) {
      MinecraftServer server = this.minecraftServer;
      if (server != null) {
         server.execute(work);
      }
   }

   void closeReplayViewMirror(MirrorBattle mirror) {
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
         this.lifecycleCleanup.sweepEntities(mirror, 0L);
      }
   }

   private void onMatchFound(JsonObject document) {
      MinecraftServer server = this.minecraftServer;
      if (server != null) {
         server.execute(() -> this.mirrorFactory.build(document));
      }
   }

   private void onBattleStart(JsonObject document) {
      MirrorBattle mirror = CrossServerBattles.byRemoteId(BattleServerClient.str(document, "battleId", ""));
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

         JsonObject outputStream = ServiceProtocolMessages.battleOutput(relay.remoteBattleId(), relay.line());
         this.client.send(outputStream);
      }
   }

   private void onRelayedChoice(JsonObject document) {
      String remoteBattleId = BattleServerClient.str(document, "battleId", "");
      String line = BattleServerClient.str(document, "data", null);
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
                  } catch (RuntimeException failure) {
                     LOGGER.error("Battle {}: could not feed the relayed choice to showdown", remoteBattleId, failure);
                  }
               });
            }
         }
      }
   }

   private void onOutput(JsonObject document) {
      String remoteBattleId = BattleServerClient.str(document, "battleId", "");
      MirrorBattle mirror = CrossServerBattles.byRemoteId(remoteBattleId);
      if (mirror == null) {
         LOGGER.warn("Output for unknown battle {}", remoteBattleId);
      } else {
         mirror.accept(document.get("seq").getAsLong(), document.get("data").getAsString());
      }
   }

   private void onOpponentForfeit(JsonObject document) {
      MirrorBattle mirror = CrossServerBattles.byRemoteId(BattleServerClient.str(document, "battleId", ""));
      MinecraftServer server = this.minecraftServer;
      if (mirror != null && server != null) {
         String seat = BattleServerClient.str(document, "seat", "");
         String reportedName = BattleServerClient.str(document, "name", "?");
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

   private void onBattleEnd(JsonObject document) {
      String remoteBattleId = BattleServerClient.str(document, "battleId", "");
      String reason = BattleServerClient.str(document, "reason", "unknown");
      MirrorBattle mirror = CrossServerBattles.byRemoteId(remoteBattleId);
      if (mirror != null) {
         mirror.markFinished();
         if (!mirror.isSpectator()) {
            for (UUID seated : mirror.localPlayers()) {
               this.pushChatState(seated);
            }

            this.announceScores(document);
            this.fireBattleEnded(mirror, document, reason);
         } else {
            for (UUID watcher : mirror.watchers()) {
               this.tellParticipant(watcher, Msg.of(ChatFormatting.YELLOW, "battle.spectate_over"));
            }
         }

         if (!"win".equals(reason) && !"tie".equals(reason)) {
            LOGGER.warn("Battle {} ended abnormally ({}) - forcing the local mirror closed", remoteBattleId, reason);
            this.lifecycleCleanup.abort(mirror, Msg.of("battle.ended", reason).withStyle(ChatFormatting.RED));
         } else {
            CrossServerBattles.forget(mirror.localBattleId());
            this.lifecycleCleanup.sweepEntities(mirror, MirrorLifecycleCleanup.RECALL_GRACE_MS);
         }
      }
   }

   private void relayChoice(CrossServerBattles.ChoiceRelay relay) {
      if (this.client != null) {
         if (this.config.debug) {
            LOGGER.info("[{}] >> {}", relay.remoteBattleId(), relay.line());
         }

         JsonObject choice = ServiceProtocolMessages.choice(relay.remoteBattleId(), relay.line());
         this.client.send(choice);
      }
   }

   void sendAbort(String remoteBattleId, String reason) {
      this.client.send(BattleControlMessages.abort(remoteBattleId, reason));
   }

   public AuthService auth() {
      return this.auth;
   }

   public Component openMainMenu(ServerPlayer participant) {
      if (!this.isConnected()) {
         return this.notConnected("auth.not_connected");
      } else {
         List<CrossServerBattleService.Ranked> competitions = new ArrayList<>(this.ranked());
         if (competitions.isEmpty()) {
            return this.sendMainMenu(participant, "");
         } else {
            String chosen = this.hasRanked(this.config().defaultRanked) ? this.config().defaultRanked : competitions.get(0).id();
            int ref = this.client.nextRef();
            JsonObject request = ServiceRequests.leaderboard(ref, chosen, participant.getUUID(), participant.getGameProfile().getName());
            if (!this.requestLedger.sendMenu(ref, participant.getUUID(), () -> this.client.send(request))) {
               return this.sendMainMenu(participant, "");
            } else {
               return null;
            }
         }
      }
   }

   private Component sendMainMenu(ServerPlayer participant, String favourite) {
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

      String displayLabel = participant.getGameProfile().getName();
      return !CobbleBattleNetwork.sendMainMenu(participant, new OpenMainMenuPayload(displayLabel, favourite, competitions))
         ? Msg.of(ChatFormatting.RED, "cmd.dex.no_client")
         : null;
   }

   public void onTeamPicked(ServerPlayer participant, String battleId, List<Integer> picks) {
      this.previews.onPicked(participant, battleId, picks);
   }

   public void onMenuAction(ServerPlayer participant, MenuActionPayload action) {
      String var3 = action.action();
      switch (var3) {
         case "queue":
            String chosen = !action.arg().isEmpty() && this.hasRanked(action.arg()) ? action.arg() : this.config().defaultRanked;
            Component refusal = this.queue(participant, chosen);
            if (refusal != null) {
               this.tellParticipant(participant.getUUID(), refusal);
            }
            break;
         case "logout":
            if (this.auth.isSignedIn(participant.getUUID())) {
               this.signOut(participant);
            }
      }
   }

   public void openPage(ServerPlayer participant, String page, String ranked, String have) {
      Component refusal;
      if ("dex".equals(page)) {
         refusal = this.openServerDex(participant, have);
      } else if ("rooms".equals(page)) {
         refusal = this.requestRooms(participant);
      } else if ("main".equals(page)) {
         refusal = this.auth.isSignedIn(participant.getUUID()) ? this.openMainMenu(participant) : this.openAuthScreen(participant);
      } else {
         String chosen = ranked != null && !ranked.isEmpty() && this.hasRanked(ranked) ? ranked : this.config().defaultRanked;
         refusal = this.requestLeaderboard(participant, chosen);
      }

      if (refusal != null) {
         this.tellParticipant(participant.getUUID(), refusal);
      }
   }

   public void onRoomAction(ServerPlayer participant, RoomActionPayload action) {
      String var4 = action.action();

      Component refusal = switch (var4) {
         case "list" -> this.requestRooms(participant, true);
         case "create" -> {
            String name = action.name().isBlank() ? Msg.raw("room.default_name", participant.getGameProfile().getName()) : action.name();
            yield this.battleQueue
               .createRoom(
                  participant,
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
         case "join" -> this.battleQueue.joinRoom(participant, action.roomId(), action.password(), action.battleType(), action.hostEngine(), action.legality(), "");
         case "join_code" -> this.battleQueue.lookupRoom(participant, action.inviteCode());
         case "leave" -> this.battleQueue.leaveRoom(participant);
         case "start" -> this.battleQueue.startRoom(participant);
         default -> null;
      };
      if (refusal != null) {
         this.tellParticipant(participant.getUUID(), refusal);
      }
   }

   private void onRoomState(JsonObject document) {
      RoomStateDecoding.Result decoded = RoomStateDecoding.decode(document);
      if (decoded != null) {
         this.withParticipant(decoded.getParticipant(), participant -> CobbleBattleNetwork.sendRoomState(participant, decoded.getPayload()));
      }
   }

   private void onRoomClosed(JsonObject document) {
      this.battleQueue.onRoomClosed(document);
      UUID who = BattleIdentifierParsing.uuidOrNull(BattleServerClient.str(document, "player", ""));
      if (who != null) {
         this.refreshRooms(who);
      }
   }

   public Component requestRooms(ServerPlayer participant) {
      return this.requestRooms(participant, false);
   }

   public Component requestRooms(ServerPlayer participant, boolean refresh) {
      if (!this.isConnected()) {
         return this.notConnected("auth.not_connected");
      } else {
         RoomDirectoryState.Snapshot<List<RoomListPayload.Room>> snapshot = this.roomDirectory.reusable(System.currentTimeMillis(), refresh);
         if (snapshot != null) {
            this.deliverRooms(participant.getUUID(), snapshot, refresh);
            return null;
         } else {
            this.roomDirectory.enqueue(participant.getUUID(), refresh);
            if (!this.roomDirectory.needsFetch()) {
               return null;
            } else {
               RoomDirectoryState.Snapshot<List<RoomListPayload.Room>> current = this.roomDirectory.current();
               JsonObject request = ServiceProtocolMessages.roomList(this.client.nextRef(), current == null ? null : current.getHash());

               if (!this.client.send(request)) {
                  this.roomDirectory.abandon(participant.getUUID());
                  return Msg.of(ChatFormatting.RED, "auth.send_failed");
               } else {
                  this.roomDirectory.markFetchStarted();
                  return null;
               }
            }
         }
      }
   }

   void refreshRooms(UUID participantUuid) {
      if (CrossServerBattles.byLocalPlayer(participantUuid) == null) {
         this.withParticipant(participantUuid, participant -> {
            this.roomDirectory.invalidate();
            this.requestRooms(participant);
         });
      }
   }

   private void onRoomList(JsonObject document) {
      List<RoomListPayload.Room> rooms;
      String hash;
      RoomDirectoryState.Snapshot<List<RoomListPayload.Room>> current = this.roomDirectory.current();
      if (BattleServerClient.bool(document, "unchanged", false) && current != null) {
         rooms = current.getContent();
         hash = current.getHash();
      } else {
         rooms = RoomListDecoding.decodeRooms(document);
         hash = BattleServerClient.str(document, "hash", "");
      }

      RoomDirectoryState.Completion<List<RoomListPayload.Room>> completion = this.roomDirectory.complete(rooms, hash, System.currentTimeMillis());
      for (RoomDirectoryState.Waiter waiter : completion.getWaiters()) {
         this.deliverRooms(waiter.getParticipant(), completion.getSnapshot(), waiter.getRefresh());
      }
   }

   private void deliverRooms(UUID participantUuid, RoomDirectoryState.Snapshot<List<RoomListPayload.Room>> snapshot, boolean refresh) {
      this.withParticipant(
         participantUuid,
         participant -> {
            long accountNumber = this.auth.uidOf(participant.getUUID());
            RoomListPersonalization.Result personalized = RoomListPersonalization.apply(snapshot.getContent(), snapshot.getHash(), accountNumber);
            String stamp = personalized.getDeliveryStamp();
            if (this.roomDirectory.shouldDeliver(participant.getUUID(), stamp, refresh)) {
               if (CobbleBattleNetwork.sendRooms(participant, new RoomListPayload(personalized.getRooms(), refresh))) {
                  this.roomDirectory.recordDelivery(participant.getUUID(), stamp);
               }
            }
         }
      );
   }

   public Component openServerDex(ServerPlayer participant) {
      return this.openServerDex(participant, "");
   }

   public Component openServerDex(ServerPlayer participant, String have) {
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

            return !CobbleBattleNetwork.sendServerDex(participant, ServerDexPayload.of(digest, entries)) ? Msg.of(ChatFormatting.RED, "cmd.dex.no_client") : null;
         } else {
            return !CobbleBattleNetwork.sendServerDex(participant, ServerDexPayload.unchanged(digest)) ? Msg.of(ChatFormatting.RED, "cmd.dex.no_client") : null;
         }
      }
   }

   public Component openAuthScreen(ServerPlayer participant) {
      if (!this.isConnected()) {
         return this.connectThenOpenAuth(participant);
      } else {
         String suggested = this.auth.accountOf(participant.getUUID());
         if (suggested == null) {
            suggested = participant.getGameProfile().getName();
         }

         return !CobbleBattleNetwork.openScreen(participant, AuthMode.LOGIN, suggested, this.emailEnabled)
            ? Msg.of(ChatFormatting.RED, "auth.client_required")
            : null;
      }
   }

   public void onCredentialsSubmitted(ServerPlayer participant, AuthMode mode, String accountId, String email, String password, String verificationCode) {
      Component refusal = this.auth.submit(this.client, participant, mode, accountId, email, password, verificationCode, this.emailEnabled);
      if (refusal != null) {
         CobbleBattleNetwork.sendResult(participant, false, refusal);
      }
   }

   public void signOut(ServerPlayer participant) {
      this.announceSignOutputStream(participant);
      this.auth.signOut(this.client, participant);
      this.pushChatState(participant);
   }

   private void announceSignOutputStream(ServerPlayer participant) {
      if (this.auth.isSignedIn(participant.getUUID())) {
         ApiEvents.signedOut(participant, this.auth.accountOf(participant.getUUID()), this.auth.nicknameOf(participant.getUUID()), this.auth.uidOf(participant.getUUID()));
      }
   }

   private void onChat(JsonObject document) {
      String selectedConversation = BattleServerClient.str(document, "channel", "global");
      String text = BattleServerClient.str(document, "text", "");
      if (!text.isEmpty()) {
         UUID sender = BattleIdentifierParsing.uuidOrNull(BattleServerClient.str(document, "player", ""));
         ChatLinePayload line = new ChatLinePayload(
            selectedConversation,
            document.has("uid") ? document.get("uid").getAsLong() : 0L,
            BattleServerClient.str(document, "id", ""),
            BattleServerClient.str(document, "name", "?"),
            sender == null ? new UUID(0L, 0L) : sender,
            text
         );
         MinecraftServer server = this.server();
         if (server != null) {
            if ("battle".equals(selectedConversation)) {
               MirrorBattle mirror = CrossServerBattles.byRemoteId(BattleServerClient.str(document, "battleId", ""));
               if (mirror != null) {
                  for (UUID seated : mirror.localPlayers()) {
                     this.withParticipant(seated, participant -> CobbleBattleNetwork.sendChatLine(participant, line));
                  }
               }
            } else {
               for (UUID participantUuid : this.auth.signedInPlayers()) {
                  this.withParticipant(participantUuid, player -> CobbleBattleNetwork.sendChatLine(player, line));
               }
            }
         }
      }
   }

   private UUID claimChatRef(JsonElement ref) {
      return ref != null && !ref.isJsonNull() ? this.requestLedger.claimChat(ref.getAsInt()) : null;
   }

   private UUID claimMenuRef(JsonElement ref) {
      return ref != null && !ref.isJsonNull() ? this.requestLedger.claimMenu(ref.getAsInt()) : null;
   }

   private UUID claimLeaderboardRef(JsonElement ref) {
      return ref != null && !ref.isJsonNull() ? this.requestLedger.claimLeaderboard(ref.getAsInt()) : null;
   }

   public Component requestLeaderboard(ServerPlayer participant, String rankedId) {
      if (!this.isConnected()) {
         return this.notConnected("auth.not_connected");
      } else {
         int ref = this.client.nextRef();
         JsonObject request = ServiceRequests.leaderboard(ref, rankedId, participant.getUUID(), participant.getGameProfile().getName());
         if (!this.requestLedger.sendLeaderboard(ref, participant.getUUID(), () -> this.client.send(request))) {
            return Msg.of(ChatFormatting.RED, "auth.send_failed");
         } else {
            return null;
         }
      }
   }

   private void onLeaderboard(JsonObject document) {
      UUID forMenu = this.claimMenuRef(document.get("ref"));
      if (forMenu != null) {
         String favourite = document.has("you") && document.get("you").isJsonObject()
            ? BattleServerClient.str(document.getAsJsonObject("you"), "favourite", "")
            : "";
         this.withParticipant(forMenu, participant -> {
            Component refusal = this.sendMainMenu(participant, favourite);
            if (refusal != null) {
               this.tellParticipant(forMenu, refusal);
            }
         });
      } else {
         UUID asker = this.claimLeaderboardRef(document.get("ref"));
         if (asker != null) {
            LeaderboardPayload board = LeaderboardDecoding.decode(document);
            this.withParticipant(asker, player -> CobbleBattleNetwork.sendLeaderboard(player, board));
         }
      }
   }

   private void fireBattleEnded(MirrorBattle mirror, JsonObject document, String reason) {
      String winnerSeat = BattleServerClient.str(document, "winnerSeat", "");

      for (UUID who : mirror.localPlayers()) {
         BattleInfo battleDetails = mirror.infoFor(who);
         if (battleDetails != null) {
            BattleOutcome outcome = BattleResultProjection.outcome(reason, winnerSeat, mirror.seatOf(who));
            ScoreChange score = BattleResultProjection.scoreFor(document, who);
            this.withParticipant(who, participant -> ApiEvents.battleEnded(participant, battleDetails, outcome, reason, score));
         }
      }
   }

   private void announceScores(JsonObject document) {
      for (BattleResultProjection.ParticipantScore score : BattleResultProjection.participantScores(document)) {
         long delta = score.getAfter() - score.getBefore();
         String signed = (delta >= 0L ? "+" : "") + delta;
         this.tellParticipant(
            score.getParticipant(),
            Msg.of(
               score.getWon() ? ChatFormatting.GREEN : ChatFormatting.RED,
               score.getWon() ? "rank.won" : "rank.lost",
               signed,
               score.getBefore(),
               score.getAfter()
            )
         );
      }
   }

   private static Component describeChatFailure(JsonObject document, String code, String fallback) {
      if ("MUTED".equals(code)) {
         long left = document.has("left") ? document.get("left").getAsLong() : 0L;
         Component muted = left == 0L ? Msg.of("chat.err.muted_permanent") : Msg.of("chat.err.muted_for", describeDuration(left));
         return muted.copy().withStyle(ChatFormatting.RED);
      } else {
         String key = ChatErrorRules.translationKey(code);
         Component text = key == null ? Component.literal(fallback) : Msg.of(key);
         return text.copy().withStyle(ChatFormatting.RED);
      }
   }

   public void onChatSubmitted(ServerPlayer participant, String selectedConversation, String text) {
      if (text != null && !text.isBlank()) {
         if (!this.auth.isSignedIn(participant.getUUID())) {
            this.tellParticipant(participant.getUUID(), Msg.of(ChatFormatting.YELLOW, "queue.not_signed_in"));
         } else if (!this.isConnected()) {
            this.tellParticipant(participant.getUUID(), this.notConnected("chat.not_connected"));
         } else if (!this.chatEnabled) {
            this.tellParticipant(participant.getUUID(), Msg.of(ChatFormatting.RED, "chat.err.disabled"));
         } else {
            int ref = this.client.nextRef();
            JsonObject request = ServiceRequests.chat(ref, selectedConversation, text, participant.getUUID(), participant.getGameProfile().getName());
            this.requestLedger.sendChat(ref, participant.getUUID(), () -> this.client.send(request));
         }
      }
   }

   public void pushChatState(ServerPlayer participant) {
      UUID uuid = participant.getUUID();
      if (!this.auth.isSignedIn(uuid)) {
         CobbleBattleNetwork.sendChatState(participant, ChatStatePayload.signedOut());
         this.reportChatObservers();
      } else {
         CobbleBattleNetwork.sendChatState(
            participant,
            new ChatStatePayload(
               true, CrossServerBattles.byLocalPlayer(uuid) != null, this.auth.uidOf(uuid), String.valueOf(this.auth.nicknameOf(uuid)), this.chatEnabled
            )
         );
         this.reportChatObservers();
      }
   }

   private void reportChatObservers() {
      if (this.client != null && this.client.isHandshaken()) {
         int observers = this.auth.signedInPlayers().size();
         if (observers != this.chatObserversReported) {
            JsonObject frame = ServiceProtocolMessages.chatWatch(observers);
            if (this.client.send(frame)) {
               this.chatObserversReported = observers;
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

   public void pushChatState(UUID participantUuid) {
      this.withParticipant(participantUuid, this::pushChatState);
   }

   private void onAccountCodeOk(JsonObject document) {
      UUID participant = this.auth.onAccountCodeOk(document);
      this.withParticipant(participant, p -> CobbleBattleNetwork.sendResult(p, true, Component.translatable("auth.code_sent")));
   }

   private void onAccountOk(JsonObject document) {
      AuthService.Outcome outcome = this.auth.onAccountOk(document);
      if (outcome != null) {
         Component text = Msg.of(
            ChatFormatting.GREEN, outcome.registered() ? "auth.registered" : "auth.logged_in", outcome.accountId(), outcome.nickname(), outcome.uid()
         );
         this.tellParticipant(outcome.playerUuid(), text);
         this.withParticipant(outcome.playerUuid(), participant -> {
            CobbleBattleNetwork.sendResult(participant, true, text);
            this.pushChatState(participant);
            ApiEvents.signedIn(participant, outcome.accountId(), outcome.nickname(), outcome.uid(), outcome.registered());
            Component refusal = this.openMainMenu(participant);
            if (refusal != null) {
               this.tellParticipant(outcome.playerUuid(), refusal);
            }
         });
      }
   }

   void onServerThreadWithParticipant(UUID participantUuid, Consumer<ServerPlayer> action) {
      this.withParticipant(participantUuid, action);
   }

   private void withParticipant(UUID participantUuid, Consumer<ServerPlayer> action) {
      MinecraftServer server = this.minecraftServer;
      if (server != null) {
         server.execute(() -> {
            ServerPlayer participant = server.getPlayerList().getPlayer(participantUuid);
            if (participant != null) {
               action.accept(participant);
            }
         });
      }
   }

   public Component queue(ServerPlayer participant, String rankedId) {
      return this.battleQueue.join(participant, rankedId);
   }

   public Component leaveQueue(ServerPlayer participant) {
      return this.battleQueue.leave(participant);
   }

   public Component describePartyCompatibility(ServerPlayer participant) {
      return this.battleQueue.describePartyCompatibility(participant);
   }

   public void onPlayerDisconnect(ServerPlayer participant) {
      this.waitingChunksAuthOpens.remove(participant.getUUID());
      this.scheduleIdleDisconnect();
      this.battleQueue.onParticipantDisconnect(participant);
      this.previews.forget(participant.getUUID());
      MirrorBattle mirror = CrossServerBattles.byLocalPlayer(participant.getUUID());
      if (mirror != null && !mirror.isFinished()) {
         LOGGER.info("{} disconnected during battle {} - closing this side and telling the host", participant.getGameProfile().getName(), mirror.remoteBattleId());
         this.sendAbort(mirror.remoteBattleId(), "player disconnected");
         this.lifecycleCleanup.abort(mirror, Msg.of("battle.player_left").withStyle(ChatFormatting.RED));
      }

      this.announceSignOutputStream(participant);
      this.auth.signOut(this.client, participant);
      this.reportChatObservers();
      this.roomDirectory.forgetDelivery(participant.getUUID());
   }

   void tellParticipant(UUID participantUuid, Component document) {
      MinecraftServer server = this.minecraftServer;
      if (server != null) {
         server.execute(() -> {
            ServerPlayer participant = server.getPlayerList().getPlayer(participantUuid);
            if (participant != null) {
               participant.sendSystemMessage(document);
            }
         });
      }
   }

   public record Ranked(
      String id, String name, String battleType, int slots, int adjustLevel, boolean fullHeal, String winScore, String failScore, List<String> rules
   ) {
   }
}
