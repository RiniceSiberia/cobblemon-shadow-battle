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
import io.github.rinicesiberia.shadowbattle.battle.ChatLineDecoding;
import io.github.rinicesiberia.shadowbattle.battle.ConnectionLifecycleRules;
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
import io.github.rinicesiberia.shadowbattle.showdown.OfficialShowdownClient;
import io.github.rinicesiberia.shadowbattle.showdown.ShowdownFrame;
import java.net.URI;

public final class CrossServerBattleService {
   private static final Logger SERVICE_LOGGER = LoggerFactory.getLogger("CobbleBattle");
   private final CobbleBattleConfig serviceConfig;
   private final RemoteDex remoteDex = new RemoteDex();
   private final MatchmakingQueueCoordinator matchmakingQueue = new MatchmakingQueueCoordinator(this);
   private final MirrorBattleFactory battleMirrorFactory = new MirrorBattleFactory(this);
   private final MirrorLifecycleCleanup lifecycleCleanup = new MirrorLifecycleCleanup(this);
   private final SpectatorSessionFactory spectatorSessions = new SpectatorSessionFactory(this);
   private final TeamPreviewCoordinator teamPreviewSessions = new TeamPreviewCoordinator(this);
   private final AuthService authenticationService = new AuthService();
   private final Map<String, CrossServerBattleService.Ranked> rankedCompetitions = new LinkedHashMap<>();
   private final ServiceRequestLedger requestLedger = new ServiceRequestLedger();
   private BattleServerClient battleServerClient;
   private OfficialShowdownClient officialShowdownClient;
   private MinecraftServer gameServer;
   private final Set<UUID> waitingChunksAuthOpens = ConcurrentHashMap.newKeySet();
   private ScheduledExecutorService idleDisconnectScheduler;
   private ScheduledFuture<?> pendingIdleDisconnect;
   private final RoomDirectoryState<List<RoomListPayload.Room>> roomDirectory = new RoomDirectoryState<>();
   private volatile int chatObserversReported = -1;
   private volatile boolean remoteChatEnabled = true;
   private volatile boolean emailAuthenticationEnabled = false;

   public Collection<CrossServerBattleService.Ranked> ranked() {
      return this.rankedCompetitions.values();
   }

   public boolean hasRanked(String rankedId) {
      return rankedId != null && this.rankedCompetitions.containsKey(rankedId);
   }

   public CrossServerBattleService.Ranked ranked(String rankedId) {
      return rankedId == null ? null : this.rankedCompetitions.get(rankedId);
   }

   public CrossServerBattleService(CobbleBattleConfig serviceConfig) {
      this.serviceConfig = serviceConfig;
   }

   public RemoteDex dex() {
      return this.remoteDex;
   }

   public CobbleBattleConfig config() {
      return this.serviceConfig;
   }

   public boolean isConnected() {
      return this.battleServerClient != null && this.battleServerClient.isHandshaken();
   }

   public String connectionRefusal() {
      return this.battleServerClient == null ? null : this.battleServerClient.refusedReason();
   }

   public Component notConnected(String translationKey) {
      String disconnectedMessage = this.connectionRefusal();
      if (disconnectedMessage != null) {
         return Msg.of(ChatFormatting.RED, "conn.refused", disconnectedMessage);
      } else {
         this.requestConnection();
         return Msg.of(ChatFormatting.RED, translationKey);
      }
   }

   private void requestConnection() {
      if (ConnectionLifecycleRules.shouldRequestConnection(
         this.battleServerClient != null, this.battleServerClient != null && this.battleServerClient.isWanted(), this.battleServerClient == null ? null : this.battleServerClient.refusedReason()
      )) {
         this.battleServerClient.connect();
      }
   }

   public boolean isQueued(UUID participantUuid) {
      return this.matchmakingQueue.hasWaitingTeam(participantUuid);
   }

   BattleServerClient serverClient() {
      return this.battleServerClient;
   }

   MinecraftServer runningServer() {
      return this.gameServer;
   }

   MatchmakingQueueCoordinator queueCoordinator() {
      return this.matchmakingQueue;
   }

   ServerPlayer participantOf(UUID participantUuid) {
      MinecraftServer server = this.gameServer;
      return server == null ? null : server.getPlayerList().getPlayer(participantUuid);
   }

   public void onServerStarted(MinecraftServer startedServer) {
      this.gameServer = startedServer;
      if (this.serviceConfig.backend == CobbleBattleConfig.Backend.POKEMON_SHOWDOWN) {
         try {
            this.officialShowdownClient = new OfficialShowdownClient(
               URI.create(this.serviceConfig.showdownWebSocket),
               frame -> { this.handleOfficialFrame(frame); return kotlin.Unit.INSTANCE; },
               error -> { SERVICE_LOGGER.warn("Official Pokémon Showdown connection failed", error); return kotlin.Unit.INSTANCE; }
            );
            this.officialShowdownClient.connect().exceptionally(error -> { SERVICE_LOGGER.warn("Official Pokémon Showdown connection failed", error); return null; });
         } catch (RuntimeException error) {
            SERVICE_LOGGER.warn("Invalid official Pokémon Showdown endpoint", error);
         }
      }
      if (this.serviceConfig.backend == CobbleBattleConfig.Backend.POKEMON_SHOWDOWN) return;
      this.remoteDex.loadFromDisk();
      CrossServerBattles.setChoiceRelay(this::sendBattleChoice);
      CrossServerBattles.setOutputRelay(this::sendBattleOutput);
      this.battleServerClient = new BattleServerClient(this.serviceConfig, this::onDocument, this::sendHandshakeAfterConnect, this::handleRemoteDisconnect);
      this.battleServerClient.setOnConnectFailed(this::handleConnectionFailure);
      this.battleServerClient.start();
      if (ConnectionLifecycleRules.connectOnServerStart(this.serviceConfig.keepConnectedWhenEmpty, startedServer.getPlayerCount())) {
         this.battleServerClient.connect();
      }
   }

   public void onPlayerJoin(ServerPlayer participant) {
      this.cancelScheduledDisconnect();
      this.requestConnection();
      this.pushChatState(participant);
   }

   private Component connectAndOpenAuthentication(ServerPlayer participant) {
      String connectionMessage = this.connectionRefusal();
      this.waitingChunksAuthOpens.add(participant.getUUID());
      if (this.battleServerClient != null) {
         this.battleServerClient.connect();
      }

      return connectionMessage != null ? Msg.of(ChatFormatting.YELLOW, "auth.connecting_retry", connectionMessage) : Msg.of(ChatFormatting.YELLOW, "auth.connecting");
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

   private void handleConnectionFailure(String failureReason) {
      for (UUID authenticationWaiter : List.copyOf(this.waitingChunksAuthOpens)) {
         this.waitingChunksAuthOpens.remove(authenticationWaiter);
         this.tellParticipant(authenticationWaiter, Msg.of(ChatFormatting.RED, "auth.connect_failed", failureReason));
      }
   }

   private synchronized void cancelScheduledDisconnect() {
      if (this.pendingIdleDisconnect != null) {
         this.pendingIdleDisconnect.cancel(false);
         this.pendingIdleDisconnect = null;
      }
   }

   private synchronized void scheduleDisconnectWhenUnused() {
      if (!this.serviceConfig.keepConnectedWhenEmpty && this.battleServerClient != null) {
         this.cancelScheduledDisconnect();
         if (this.idleDisconnectScheduler == null) {
            this.idleDisconnectScheduler = Executors.newSingleThreadScheduledExecutor(scheduledTask -> {
               Thread idleThread = new Thread(scheduledTask, "CobbleBattle-Idle");
               idleThread.setDaemon(true);
               return idleThread;
            });
         }

         long delaySeconds = ConnectionLifecycleRules.idleDelaySeconds(this.serviceConfig.idleDisconnectSeconds);
         this.pendingIdleDisconnect = this.idleDisconnectScheduler.schedule(() -> this.executeOnServerThread(() -> {
            MinecraftServer activeServer = this.gameServer;
            if (activeServer != null && this.battleServerClient != null && ConnectionLifecycleRules.shouldReleaseIdleConnection(activeServer.getPlayerCount(), this.battleServerClient.isWanted())) {
               SERVICE_LOGGER.info("No players online for {}s, letting the battle server connection go", delaySeconds);
               this.battleServerClient.disconnect("no players online");
            }
         }), delaySeconds, TimeUnit.SECONDS);
      }
   }

   public void reconnect(String reconnectReason) {
      if (this.battleServerClient != null) {
         this.battleServerClient.reconnect(reconnectReason);
      }
   }

   public void onServerStopping() {
      this.cancelScheduledDisconnect();
      if (this.idleDisconnectScheduler != null) {
         this.idleDisconnectScheduler.shutdownNow();
         this.idleDisconnectScheduler = null;
      }

      if (this.battleServerClient != null) {
         this.battleServerClient.stop();
      }
      if (this.officialShowdownClient != null) {
         this.officialShowdownClient.close();
         this.officialShowdownClient = null;
      }

      for (MirrorBattle activeMirror : CrossServerBattles.all()) {
         this.lifecycleCleanup.sweepEntities(activeMirror, 0L);
      }

      CrossServerBattles.clear();
      this.matchmakingQueue.clearQueueState();
      this.teamPreviewSessions.clearSessions();
   }

   private void handleOfficialFrame(ShowdownFrame frame) {
      SERVICE_LOGGER.debug("Official PS frame room={} lines={}", frame.getRoomId(), frame.getLines().size());
   }

   private void sendHandshakeAfterConnect() {
      JsonObject handshake = ServiceProtocolMessages.hello(
         this.battleServerClient.nextRef(),
         this.serviceConfig.authToken,
         ServerIdentity.get(),
         Platform.isModLoaded("cobblemon") ? Platform.getMod("cobblemon").getVersion() : "unknown"
      );
      this.battleServerClient.sendHandshake(handshake);
   }

   private void handleRemoteDisconnect(String disconnectReason) {
      if (this.battleServerClient != null && !this.battleServerClient.isWanted()) {
         SERVICE_LOGGER.info("Battle server connection closed ({}).", disconnectReason);
      } else {
         SERVICE_LOGGER.warn("Lost the battle server ({}). Aborting {} mirror battle(s).", disconnectReason, CrossServerBattles.size());
      }

      this.remoteDex.suspend(disconnectReason);
      this.chatObserversReported = -1;
      this.executeOnServerThread(this.roomDirectory::clearSession);
      this.matchmakingQueue.clearQueueState();
      this.teamPreviewSessions.clearSessions();
      this.authenticationService.clear();

      for (MirrorBattle activeMirror : CrossServerBattles.all()) {
         this.lifecycleCleanup.abort(activeMirror, Msg.of("battle.connection_lost").withStyle(ChatFormatting.RED));
      }
   }

   private void onDocument(JsonObject document) {
      String type = BattleServerClient.str(document, "t", "");
      switch (type) {
         case "ping":
            this.battleServerClient.send(BattleServerClient.msg("pong"));
            break;
         case "hello_ack":
            this.handleHandshakeAccepted(document);
            break;
         case "ranked_update":
            this.replaceRankedCompetitions(document);
            break;
         case "dex_snapshot":
            this.remoteDex.accept(document);
            break;
         case "queue_ack":
            this.matchmakingQueue.handleQueueAccepted(document);
            break;
         case "queue_left":
            this.matchmakingQueue.handleQueueDeparted(document);
            break;
         case "queue_wait":
            this.matchmakingQueue.handleQueuePosition(document);
            break;
         case "room_created":
            this.matchmakingQueue.handleRoomCreated(document);
            break;
         case "room_list":
            this.executeOnServerThread(() -> this.handleRoomList(document));
            break;
         case "room_state":
            this.handleRoomState(document);
            break;
         case "room_info":
            this.matchmakingQueue.onRoomBattleDetails(document);
            break;
         case "room_closed":
            this.handleRoomClosed(document);
            break;
         case "preview_open":
            this.executeOnServerThread(() -> this.teamPreviewSessions.handlePreviewOpened(document));
            break;
         case "preview_state":
            this.executeOnServerThread(() -> this.teamPreviewSessions.handlePreviewState(document));
            break;
         case "preview_closed":
            this.executeOnServerThread(() -> this.teamPreviewSessions.handlePreviewClosed(document));
            break;
         case "match_found":
            this.handleMatchFound(document);
            break;
         case "battle_start":
            this.handleBattleStarted(document);
            break;
         case "output":
            this.handleBattleOutput(document);
            break;
         case "battle_choice":
            this.handleRelayedChoice(document);
            break;
         case "spectate_start":
            this.executeOnServerThread(() -> this.spectatorSessions.begin(document));
            break;
         case "spectate_end":
            this.executeOnServerThread(() -> this.spectatorSessions.end(document));
            break;
         case "battle_end":
            this.handleBattleEnded(document);
            break;
         case "forfeit":
            this.handleOpponentForfeit(document);
            break;
         case "chat":
            this.handleChatLine(document);
            break;
         case "leaderboard":
            this.handleLeaderboard(document);
            break;
         case "account_ok":
            this.handleAuthenticationAccepted(document);
            break;
         case "account_code_ok":
            this.handleVerificationCodeAccepted(document);
            break;
         case "error":
            this.handleServiceError(document);
            break;
         default:
            SERVICE_LOGGER.warn("Unknown message type '{}' from the battle server", type);
      }
   }

   private void handleHandshakeAccepted(JsonObject document) {
      this.battleServerClient.setHandshaken(true);
      HandshakeResponseDecoding.Settings settings = HandshakeResponseDecoding.decode(document);
      this.replaceRankedCompetitions(document);
      this.remoteDex.setStrictBaseStats(settings.getStrictBaseStats());
      this.remoteDex
         .setTeamRules(
            settings.getStrictAbilities(), settings.getStrictMoves(), settings.getMaxEvPerStat(), settings.getMaxEvTotal(), settings.getMaxIv()
         );
      this.remoteChatEnabled = settings.getChatEnabled();
      if (!this.remoteChatEnabled) {
         SERVICE_LOGGER.info("The battle server has chat switched off; the chat panel stays hidden");
      }

      this.emailAuthenticationEnabled = settings.getEmailEnabled();
      if (!this.emailAuthenticationEnabled) {
         SERVICE_LOGGER.info("The battle server has no mailer; the account screen stays on account names");
      }

      SERVICE_LOGGER.info(
         "Handshake complete with battle server instance '{}' (dex {}, {} species)",
         new Object[]{
            settings.getInstance(), settings.getDexReady() ? "ready" : "NOT ready", settings.getSpeciesCount()
         }
      );
      this.reportChatObservers();
      this.openWaitingChunksAuth();
      HandshakeResponseDecoding.DexAction dexAction = HandshakeResponseDecoding.decideDex(settings, this.remoteDex.cachedDigest());
      if (dexAction == HandshakeResponseDecoding.DexAction.Invalidate.INSTANCE) {
         this.remoteDex.invalidate("the battle server has no dex");
         SERVICE_LOGGER.warn(
            "The battle server has no dex yet. Cross-server battles stay unavailable until it has one - the Cobblemon jar belongs in its cobblemon/ folder."
         );
      } else if (dexAction instanceof HandshakeResponseDecoding.DexAction.AcceptCached cached) {
         this.remoteDex.accept(unchangedDexDocument(cached.getDigest()));
      } else {
         this.requestRemoteDex();
      }
   }

   private static JsonObject unchangedDexDocument(String expectedDigest) {
      JsonObject responseDocument = new JsonObject();
      responseDocument.addProperty("digest", expectedDigest);
      responseDocument.addProperty("unchanged", true);
      return responseDocument;
   }

   private void replaceRankedCompetitions(JsonObject document) {
      this.rankedCompetitions.clear();
      this.rankedCompetitions.putAll(RankedCompetitionDecoding.decode(document));

      if (this.rankedCompetitions.isEmpty()) {
         SERVICE_LOGGER.warn("The battle server offers no ranked competitions - nobody can queue. Its ranked/ folder is empty, or every file in it was refused.");
      } else {
         SERVICE_LOGGER.info("Ranked competitions offered: {}", String.join(", ", this.rankedCompetitions.keySet()));
      }
   }

   private void requestRemoteDex() {
      String cachedDex = this.remoteDex.cachedDigest();
      JsonObject requestFrame = ServiceProtocolMessages.dexQuery(this.battleServerClient.nextRef(), cachedDex);
      this.battleServerClient.send(requestFrame);
   }

   private void handleServiceError(JsonObject document) {
      String errorCode = BattleServerClient.str(document, "code", "?");
      String errorText = BattleServerClient.str(document, "message", "");
      SERVICE_LOGGER.warn("Battle server error [{}]: {}", errorCode, errorText);
      if (!"hello".equals(BattleServerClient.str(document, "about", ""))) {
         if ("room_list".equals(BattleServerClient.str(document, "about", ""))) {
            this.executeOnServerThread(this.roomDirectory::cancelRequests);
         }

         MirrorBattle affectedBattle = CrossServerBattles.byRemoteId(BattleServerClient.str(document, "battleId", ""));
         if (affectedBattle != null && !affectedBattle.isFinished()) {
            SERVICE_LOGGER.error("Battle server refused something for battle {} ({}: {}) - closing the local mirror", new Object[]{affectedBattle.remoteBattleId(), errorCode, errorText});
            this.lifecycleCleanup.abort(affectedBattle, Msg.of("battle.ended", errorText).withStyle(ChatFormatting.RED));
         } else if (this.authenticationService.isAwaiting(document)) {
            UUID connectionWaiter = this.authenticationService.onAccountError(document);
            if (connectionWaiter != null) {
               Component failureMessage = formatAuthenticationFailure(errorCode, errorText);
               this.tellParticipant(connectionWaiter, failureMessage);
               this.withParticipant(connectionWaiter, participant -> CobbleBattleNetwork.sendResult(participant, false, failureMessage));
            }
         } else {
            UUID chatRequester = this.claimChatRequest(document.get("ref"));
            if (chatRequester != null) {
               this.tellParticipant(chatRequester, formatChatFailure(document, errorCode, errorText));
            } else {
               UUID menuRequester = this.claimMenuRef(document.get("ref"));
               if (menuRequester != null) {
                  this.withParticipant(menuRequester, waitingPlayer -> this.deliverMainMenu(waitingPlayer, ""));
               } else {
                  UUID leaderboardRequester = this.claimLeaderboardRef(document.get("ref"));
                  if (leaderboardRequester != null) {
                     this.tellParticipant(leaderboardRequester, Component.literal(errorText).withStyle(ChatFormatting.RED));
                  } else {
                     UUID roomLookupRequester = this.matchmakingQueue.claimLookupRequester(document.get("ref"));
                     if (roomLookupRequester != null) {
                        this.tellParticipant(roomLookupRequester, formatLookupFailure(errorCode, errorText));
                     } else {
                        UUID roomOwner = this.matchmakingQueue.claimRequestOwner(document.get("ref"));
                        if (roomOwner != null) {
                           this.matchmakingQueue.discardWaitingTeam(roomOwner);

                           String roomReference = QueueErrorRules.roomRefusalKey(errorCode);
                           if (roomReference != null) {
                              this.tellParticipant(roomOwner, Msg.of(ChatFormatting.RED, roomReference));
                              this.refreshRoomDirectory(roomOwner);
                           } else if ("BANNED".equals(errorCode)) {
                              long queueLeaver = document.has("left") ? document.get("left").getAsLong() : 0L;
                              String rankedId = BattleServerClient.str(document, "ranked", "?");
                              this.tellParticipant(
                                 roomOwner,
                                 (queueLeaver == 0L ? Msg.of("queue.banned_permanent", rankedId) : Msg.of("queue.banned_for", rankedId, formatDuration(queueLeaver)))
                                    .withStyle(ChatFormatting.RED)
                              );
                           } else {
                              this.tellParticipant(roomOwner, Msg.of("queue.refused", errorText).withStyle(ChatFormatting.RED));
                           }
                        }
                     }
                  }
               }
            }
         }
      } else {
         String rejectionReason = formatHandshakeRejection(errorCode, errorText);
         SERVICE_LOGGER.error(
            "The battle server refused this server's handshake: {}. Not reconnecting until somebody opens the sign-in screen or runs /cbattle reload.", rejectionReason
         );
         this.battleServerClient.suspend(rejectionReason);

         for (UUID authenticationWaiter : List.copyOf(this.waitingChunksAuthOpens)) {
            this.waitingChunksAuthOpens.remove(authenticationWaiter);
            this.tellParticipant(authenticationWaiter, Msg.of(ChatFormatting.RED, "conn.refused", rejectionReason));
         }
      }
   }

   private static Component formatLookupFailure(String errorCode, String fallbackText) {
      String translationKey = QueueErrorRules.lookupFailureKey(errorCode);
      Component failureMessage = translationKey == null ? Msg.of("queue.refused", fallbackText) : Msg.of(translationKey);
      return failureMessage.copy().withStyle(ChatFormatting.RED);
   }

   private static String formatHandshakeRejection(String errorCode, String serverText) {
      return switch (errorCode) {
         case "BAD_PROTOCOL" -> Msg.raw("conn.err.protocol", serverText);
         case "BAD_AUTH" -> Msg.raw("conn.err.auth");
         default -> serverText.isEmpty() ? errorCode : serverText;
      };
   }

   private static String formatDuration(long durationMillis) {
      long totalSeconds = (durationMillis + 999L) / 1000L;
      long wholeDays = totalSeconds / 86400L;
      long remainingHours = totalSeconds % 86400L / 3600L;
      long remainingMinutes = totalSeconds % 3600L / 60L;
      long remainingSeconds = totalSeconds % 60L;
      StringBuilder outputStream = new StringBuilder();
      int durationParts = 0;
      if (wholeDays > 0L) {
         outputStream.append(Msg.of("time.days", wholeDays).getString());
         durationParts++;
      }

      if (remainingHours > 0L && durationParts < 2) {
         outputStream.append(Msg.of("time.hours", remainingHours).getString());
         durationParts++;
      }

      if (remainingMinutes > 0L && durationParts < 2) {
         outputStream.append(Msg.of("time.minutes", remainingMinutes).getString());
         durationParts++;
      }

      if (durationParts == 0) {
         outputStream.append(Msg.of("time.seconds", Math.max(1L, remainingSeconds)).getString());
      }

      return outputStream.toString().trim();
   }

   private static Component formatAuthenticationFailure(String errorCode, String fallbackText) {
      String translationKey = AuthenticationErrorRules.translationKey(errorCode);
      Component failureMessage = translationKey == null ? Component.literal(fallbackText) : Msg.of(translationKey);
      return failureMessage.copy().withStyle(ChatFormatting.RED);
   }

   private void executeOnServerThread(Runnable action) {
      MinecraftServer activeServer = this.gameServer;
      if (activeServer != null) {
         activeServer.execute(action);
      }
   }

   void closeReplayViewMirror(MirrorBattle mirror) {
      mirror.markFinished();
      UUID localBattleId = mirror.localBattleId();
      if (localBattleId != null) {
         CrossServerBattles.forget(localBattleId);
         this.executeOnServerThread(() -> {
            PokemonBattle battle = BattleRegistry.getBattle(localBattleId);
            if (battle != null && !battle.getEnded()) {
               battle.end();
               BattleRegistry.closeBattle(battle);
            }
         });
         this.lifecycleCleanup.sweepEntities(mirror, 0L);
      }
   }

   private void handleMatchFound(JsonObject document) {
      MinecraftServer activeServer = this.gameServer;
      if (activeServer != null) {
         activeServer.execute(() -> this.battleMirrorFactory.build(document));
      }
   }

   private void handleBattleStarted(JsonObject document) {
      MirrorBattle activeMirror = CrossServerBattles.byRemoteId(BattleServerClient.str(document, "battleId", ""));
      if (activeMirror != null) {
         activeMirror.release();

         for (UUID battleParticipantUuid : activeMirror.localPlayers()) {
            this.pushChatState(battleParticipantUuid);
         }
      }
   }

   private void sendBattleOutput(CrossServerBattles.ChoiceRelay outputRelay) {
      if (this.battleServerClient != null) {
         if (this.serviceConfig.debug) {
            SERVICE_LOGGER.info("[{}] >> output {}", outputRelay.remoteBattleId(), outputRelay.line().replace("\n", " \\n "));
         }

         JsonObject outputStream = ServiceProtocolMessages.battleOutput(outputRelay.remoteBattleId(), outputRelay.line());
         this.battleServerClient.send(outputStream);
      }
   }

   private void handleRelayedChoice(JsonObject document) {
      String externalBattleId = BattleServerClient.str(document, "battleId", "");
      String choiceLine = BattleServerClient.str(document, "data", null);
      MirrorBattle activeMirror = CrossServerBattles.byRemoteId(externalBattleId);
      if (activeMirror == null || choiceLine == null) {
         SERVICE_LOGGER.warn("Relayed choice for unknown battle {}", externalBattleId);
      } else if (!activeMirror.isAuthoritative()) {
         SERVICE_LOGGER.warn("Battle {} sent us a choice to run, but we are only mirroring it", externalBattleId);
      } else {
         UUID internalBattleId = activeMirror.localBattleId();
         if (internalBattleId == null) {
            SERVICE_LOGGER.warn("Relayed choice for battle {} arrived before the local battle existed", externalBattleId);
         } else {
            if (this.serviceConfig.debug) {
               SERVICE_LOGGER.info("[{}] << choice {}", externalBattleId, choiceLine);
            }

            MinecraftServer activeServer = this.runningServer();
            if (activeServer == null) {
               SERVICE_LOGGER.warn("Relayed choice for battle {} arrived with no server to run it on", externalBattleId);
            } else {
               activeServer.execute(() -> {
                  try {
                     CrossServerBattles.injecting(() -> ShowdownService.Companion.getService().send(internalBattleId, new String[]{choiceLine}));
                  } catch (RuntimeException failure) {
                     SERVICE_LOGGER.error("Battle {}: could not feed the relayed choice to showdown", externalBattleId, failure);
                  }
               });
            }
         }
      }
   }

   private void handleBattleOutput(JsonObject document) {
      String externalBattleId = BattleServerClient.str(document, "battleId", "");
      MirrorBattle activeMirror = CrossServerBattles.byRemoteId(externalBattleId);
      if (activeMirror == null) {
         SERVICE_LOGGER.warn("Output for unknown battle {}", externalBattleId);
      } else {
         activeMirror.accept(document.get("seq").getAsLong(), document.get("data").getAsString());
      }
   }

   private void handleOpponentForfeit(JsonObject document) {
      MirrorBattle activeMirror = CrossServerBattles.byRemoteId(BattleServerClient.str(document, "battleId", ""));
      MinecraftServer activeServer = this.gameServer;
      if (activeMirror != null && activeServer != null) {
         String forfeitingSeat = BattleServerClient.str(document, "seat", "");
         String fallbackName = BattleServerClient.str(document, "name", "?");
         activeServer.execute(
            () -> {
               PokemonBattle localBattle = activeMirror.battle();
               if (localBattle != null && !localBattle.getEnded()) {
                  localBattle.broadcastChatMessage(
                     Component.translatable("cobblemon.battle.forfeit", new Object[]{resolveActorName(localBattle, forfeitingSeat, fallbackName)}).withStyle(ChatFormatting.RED)
                  );
               }
            }
         );
      }
   }

   private static Component resolveActorName(PokemonBattle localBattle, String actorSeat, String fallbackName) {
      for (BattleActor resolvedActor : localBattle.getActors()) {
         if (resolvedActor.isInitialized() && resolvedActor.getShowdownId().equals(actorSeat)) {
            return resolvedActor.getName();
         }
      }

      return Component.literal(fallbackName);
   }

   private void handleBattleEnded(JsonObject document) {
      String externalBattleId = BattleServerClient.str(document, "battleId", "");
      String endReason = BattleServerClient.str(document, "reason", "unknown");
      MirrorBattle activeMirror = CrossServerBattles.byRemoteId(externalBattleId);
      if (activeMirror != null) {
         activeMirror.markFinished();
         if (!activeMirror.isSpectator()) {
            for (UUID battleParticipantUuid : activeMirror.localPlayers()) {
               this.pushChatState(battleParticipantUuid);
            }

            this.announceScoreChanges(document);
            this.publishBattleEnded(activeMirror, document, endReason);
         } else {
            for (UUID spectatorUuid : activeMirror.watchers()) {
               this.tellParticipant(spectatorUuid, Msg.of(ChatFormatting.YELLOW, "battle.spectate_over"));
            }
         }

         if (!"win".equals(endReason) && !"tie".equals(endReason)) {
            SERVICE_LOGGER.warn("Battle {} ended abnormally ({}) - forcing the local mirror closed", externalBattleId, endReason);
            this.lifecycleCleanup.abort(activeMirror, Msg.of("battle.ended", endReason).withStyle(ChatFormatting.RED));
         } else {
            CrossServerBattles.forget(activeMirror.localBattleId());
            this.lifecycleCleanup.sweepEntities(activeMirror, MirrorLifecycleCleanup.RECALL_GRACE_MS);
         }
      }
   }

   private void sendBattleChoice(CrossServerBattles.ChoiceRelay choiceRelay) {
      if (this.battleServerClient != null) {
         if (this.serviceConfig.debug) {
            SERVICE_LOGGER.info("[{}] >> {}", choiceRelay.remoteBattleId(), choiceRelay.line());
         }

         JsonObject choiceText = ServiceProtocolMessages.choice(choiceRelay.remoteBattleId(), choiceRelay.line());
         this.battleServerClient.send(choiceText);
      }
   }

   void sendBattleAbort(String externalBattleId, String abortReason) {
      this.battleServerClient.send(BattleControlMessages.abort(externalBattleId, abortReason));
   }

   public AuthService auth() {
      return this.authenticationService;
   }

   public Component openMainMenu(ServerPlayer participant) {
      if (!this.isConnected()) {
         return this.notConnected("auth.not_connected");
      } else {
         List<CrossServerBattleService.Ranked> visibleCompetitions = new ArrayList<>(this.ranked());
         if (visibleCompetitions.isEmpty()) {
            return this.deliverMainMenu(participant, "");
         } else {
            String selectedRankedId = this.hasRanked(this.config().defaultRanked) ? this.config().defaultRanked : visibleCompetitions.get(0).id();
            int requestReference = this.battleServerClient.nextRef();
            JsonObject requestFrame = ServiceRequests.leaderboard(requestReference, selectedRankedId, participant.getUUID(), participant.getGameProfile().getName());
            if (!this.requestLedger.sendMenu(requestReference, participant.getUUID(), () -> this.battleServerClient.send(requestFrame))) {
               return this.deliverMainMenu(participant, "");
            } else {
               return null;
            }
         }
      }
   }

   private Component deliverMainMenu(ServerPlayer participant, String favouriteRankedId) {
      List<OpenMainMenuPayload.RankedInfo> visibleCompetitions = new ArrayList<>();

      for (CrossServerBattleService.Ranked competition : this.ranked()) {
         visibleCompetitions.add(
            new OpenMainMenuPayload.RankedInfo(
               competition.id(),
               competition.name(),
               competition.battleType(),
               competition.slots(),
               competition.adjustLevel(),
               competition.fullHeal(),
               competition.winScore(),
               competition.failScore(),
               competition.rules()
            )
         );
      }

      String displayLabel = participant.getGameProfile().getName();
      return !CobbleBattleNetwork.sendMainMenu(participant, new OpenMainMenuPayload(displayLabel, favouriteRankedId, visibleCompetitions))
         ? Msg.of(ChatFormatting.RED, "cmd.dex.no_client")
         : null;
   }

   public void onTeamPicked(ServerPlayer participant, String externalBattleId, List<Integer> selectedSlots) {
      this.teamPreviewSessions.handleTeamPicked(participant, externalBattleId, selectedSlots);
   }

   public void onMenuAction(ServerPlayer participant, MenuActionPayload menuAction) {
      String actionName = menuAction.action();
      switch (actionName) {
         case "queue":
            String selectedRankedId = !menuAction.arg().isEmpty() && this.hasRanked(menuAction.arg()) ? menuAction.arg() : this.config().defaultRanked;
            Component failureMessage = this.queue(participant, selectedRankedId);
            if (failureMessage != null) {
               this.tellParticipant(participant.getUUID(), failureMessage);
            }
            break;
         case "logout":
            if (this.authenticationService.isSignedIn(participant.getUUID())) {
               this.signOut(participant);
            }
      }
   }

   public void openPage(ServerPlayer participant, String pageId, String rankedId, String clientDexDigest) {
      Component failureMessage;
      if ("dex".equals(pageId)) {
         failureMessage = this.openServerDex(participant, clientDexDigest);
      } else if ("rooms".equals(pageId)) {
         failureMessage = this.requestRooms(participant);
      } else if ("main".equals(pageId)) {
         failureMessage = this.authenticationService.isSignedIn(participant.getUUID()) ? this.openMainMenu(participant) : this.openAuthScreen(participant);
      } else {
         String selectedRankedId = rankedId != null && !rankedId.isEmpty() && this.hasRanked(rankedId) ? rankedId : this.config().defaultRanked;
         failureMessage = this.requestLeaderboard(participant, selectedRankedId);
      }

      if (failureMessage != null) {
         this.tellParticipant(participant.getUUID(), failureMessage);
      }
   }

   public void onRoomAction(ServerPlayer participant, RoomActionPayload roomAction) {
      String actionName = roomAction.action();

      Component failureMessage = switch (actionName) {
         case "list" -> this.requestRooms(participant, true);
         case "create" -> {
            String roomName = roomAction.name().isBlank() ? Msg.raw("room.default_name", participant.getGameProfile().getName()) : roomAction.name();
            yield this.matchmakingQueue
               .createBattleRoom(
                  participant,
                  roomName,
                  roomAction.password(),
                  roomAction.battleType(),
                  roomAction.level(),
                  roomAction.pick(),
                  roomAction.fullHeal(),
                  roomAction.hostEngine(),
                  roomAction.legality()
               );
         }
         case "join" -> this.matchmakingQueue.joinBattleRoom(participant, roomAction.roomId(), roomAction.password(), roomAction.battleType(), roomAction.hostEngine(), roomAction.legality(), "");
         case "join_code" -> this.matchmakingQueue.findRoomByInvite(participant, roomAction.inviteCode());
         case "leave" -> this.matchmakingQueue.leaveBattleRoom(participant);
         case "start" -> this.matchmakingQueue.startBattleRoom(participant);
         default -> null;
      };
      if (failureMessage != null) {
         this.tellParticipant(participant.getUUID(), failureMessage);
      }
   }

   private void handleRoomState(JsonObject document) {
      RoomStateDecoding.Result decoded = RoomStateDecoding.decode(document);
      if (decoded != null) {
         this.withParticipant(decoded.getParticipant(), participant -> CobbleBattleNetwork.sendRoomState(participant, decoded.getPayload()));
      }
   }

   private void handleRoomClosed(JsonObject document) {
      this.matchmakingQueue.handleRoomClosed(document);
      UUID participantUuid = BattleIdentifierParsing.uuidOrNull(BattleServerClient.str(document, "player", ""));
      if (participantUuid != null) {
         this.refreshRoomDirectory(participantUuid);
      }
   }

   public Component requestRooms(ServerPlayer participant) {
      return this.requestRooms(participant, false);
   }

   public Component requestRooms(ServerPlayer participant, boolean forceRefresh) {
      if (!this.isConnected()) {
         return this.notConnected("auth.not_connected");
      } else {
         RoomDirectoryState.Snapshot<List<RoomListPayload.Room>> snapshot = this.roomDirectory.reusable(System.currentTimeMillis(), forceRefresh);
         if (snapshot != null) {
            this.deliverRoomDirectory(participant.getUUID(), snapshot, forceRefresh);
            return null;
         } else {
            this.roomDirectory.enqueue(participant.getUUID(), forceRefresh);
            if (!this.roomDirectory.needsFetch()) {
               return null;
            } else {
               RoomDirectoryState.Snapshot<List<RoomListPayload.Room>> current = this.roomDirectory.current();
               JsonObject requestFrame = ServiceProtocolMessages.roomList(this.battleServerClient.nextRef(), current == null ? null : current.getHash());

               if (!this.battleServerClient.send(requestFrame)) {
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

   void refreshRoomDirectory(UUID participantUuid) {
      if (CrossServerBattles.byLocalPlayer(participantUuid) == null) {
         this.withParticipant(participantUuid, participant -> {
            this.roomDirectory.invalidate();
            this.requestRooms(participant);
         });
      }
   }

   private void handleRoomList(JsonObject document) {
      List<RoomListPayload.Room> decodedRooms;
      String hash;
      RoomDirectoryState.Snapshot<List<RoomListPayload.Room>> current = this.roomDirectory.current();
      if (BattleServerClient.bool(document, "unchanged", false) && current != null) {
         decodedRooms = current.getContent();
         hash = current.getHash();
      } else {
         decodedRooms = RoomListDecoding.decodeRooms(document);
         hash = BattleServerClient.str(document, "hash", "");
      }

      RoomDirectoryState.Completion<List<RoomListPayload.Room>> completion = this.roomDirectory.complete(decodedRooms, hash, System.currentTimeMillis());
      for (RoomDirectoryState.Waiter waiter : completion.getWaiters()) {
         this.deliverRoomDirectory(waiter.getParticipant(), completion.getSnapshot(), waiter.getRefresh());
      }
   }

   private void deliverRoomDirectory(UUID participantUuid, RoomDirectoryState.Snapshot<List<RoomListPayload.Room>> snapshot, boolean refresh) {
      this.withParticipant(
         participantUuid,
         participant -> {
            long accountNumber = this.authenticationService.uidOf(participant.getUUID());
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

   public Component openServerDex(ServerPlayer participant, String clientDigest) {
      if (!this.isConnected()) {
         return this.notConnected("auth.not_connected");
      } else if (!this.remoteDex.isReady()) {
         return Msg.of(ChatFormatting.RED, "cmd.dex.not_ready");
      } else {
         String serverDigest = this.remoteDex.digest() == null ? "" : this.remoteDex.digest();
         if (clientDigest.isEmpty() || !clientDigest.equals(serverDigest)) {
            List<ServerDexPayload.Entry> dexEntries = new ArrayList<>();

            for (RemoteDex.Entry dexEntry : this.remoteDex.entries()) {
               Map<String, Integer> baseStats = dexEntry.baseStats();
               dexEntries.add(
                  new ServerDexPayload.Entry(
                     dexEntry.id(),
                     baseStats.getOrDefault("hp", 0),
                     baseStats.getOrDefault("atk", 0),
                     baseStats.getOrDefault("def", 0),
                     baseStats.getOrDefault("spa", 0),
                     baseStats.getOrDefault("spd", 0),
                     baseStats.getOrDefault("spe", 0)
                  )
               );
            }

            return !CobbleBattleNetwork.sendServerDex(participant, ServerDexPayload.of(serverDigest, dexEntries)) ? Msg.of(ChatFormatting.RED, "cmd.dex.no_client") : null;
         } else {
            return !CobbleBattleNetwork.sendServerDex(participant, ServerDexPayload.unchanged(serverDigest)) ? Msg.of(ChatFormatting.RED, "cmd.dex.no_client") : null;
         }
      }
   }

   public Component openAuthScreen(ServerPlayer participant) {
      if (!this.isConnected()) {
         return this.connectAndOpenAuthentication(participant);
      } else {
         String suggestedAccount = this.authenticationService.accountOf(participant.getUUID());
         if (suggestedAccount == null) {
            suggestedAccount = participant.getGameProfile().getName();
         }

         return !CobbleBattleNetwork.openScreen(participant, AuthMode.LOGIN, suggestedAccount, this.emailAuthenticationEnabled)
            ? Msg.of(ChatFormatting.RED, "auth.client_required")
            : null;
      }
   }

   public void onCredentialsSubmitted(ServerPlayer participant, AuthMode authenticationMode, String submittedAccountId, String emailAddress, String submittedPassword, String submittedVerificationCode) {
      Component failureMessage = this.authenticationService.submit(this.battleServerClient, participant, authenticationMode, submittedAccountId, emailAddress, submittedPassword, submittedVerificationCode, this.emailAuthenticationEnabled);
      if (failureMessage != null) {
         CobbleBattleNetwork.sendResult(participant, false, failureMessage);
      }
   }

   public void signOut(ServerPlayer participant) {
      this.announceSignOutputStream(participant);
      this.authenticationService.signOut(this.battleServerClient, participant);
      this.pushChatState(participant);
   }

   private void announceSignOutputStream(ServerPlayer participant) {
      if (this.authenticationService.isSignedIn(participant.getUUID())) {
         ApiEvents.signedOut(participant, this.authenticationService.accountOf(participant.getUUID()), this.authenticationService.nicknameOf(participant.getUUID()), this.authenticationService.uidOf(participant.getUUID()));
      }
   }

   private void handleChatLine(JsonObject document) {
      ChatLineDecoding.Result decoded = ChatLineDecoding.decode(document);
      if (decoded != null) {
         ChatLinePayload chatLine = decoded.getPayload();
         MinecraftServer activeServer = this.runningServer();
         if (activeServer != null) {
            if ("battle".equals(decoded.getChannel())) {
               MirrorBattle activeMirror = CrossServerBattles.byRemoteId(decoded.getBattleId());
               if (activeMirror != null) {
                  for (UUID battleParticipantUuid : activeMirror.localPlayers()) {
                     this.withParticipant(battleParticipantUuid, participant -> CobbleBattleNetwork.sendChatLine(participant, chatLine));
                  }
               }
            } else {
               for (UUID participantUuid : this.authenticationService.signedInPlayers()) {
                  this.withParticipant(participantUuid, chatRecipient -> CobbleBattleNetwork.sendChatLine(chatRecipient, chatLine));
               }
            }
         }
      }
   }

   private UUID claimChatRequest(JsonElement reference) {
      return reference != null && !reference.isJsonNull() ? this.requestLedger.claimChat(reference.getAsInt()) : null;
   }

   private UUID claimMenuRef(JsonElement ref) {
      return ref != null && !ref.isJsonNull() ? this.requestLedger.claimMenu(ref.getAsInt()) : null;
   }

   private UUID claimLeaderboardRef(JsonElement ref) {
      return ref != null && !ref.isJsonNull() ? this.requestLedger.claimLeaderboard(ref.getAsInt()) : null;
   }

   public Component requestLeaderboard(ServerPlayer participant, String competitionId) {
      if (!this.isConnected()) {
         return this.notConnected("auth.not_connected");
      } else {
         int requestReference = this.battleServerClient.nextRef();
         JsonObject requestFrame = ServiceRequests.leaderboard(requestReference, competitionId, participant.getUUID(), participant.getGameProfile().getName());
         if (!this.requestLedger.sendLeaderboard(requestReference, participant.getUUID(), () -> this.battleServerClient.send(requestFrame))) {
            return Msg.of(ChatFormatting.RED, "auth.send_failed");
         } else {
            return null;
         }
      }
   }

   private void handleLeaderboard(JsonObject document) {
      UUID menuRequester = this.claimMenuRef(document.get("ref"));
      if (menuRequester != null) {
         String favouriteRankedId = document.has("you") && document.get("you").isJsonObject()
            ? BattleServerClient.str(document.getAsJsonObject("you"), "favourite", "")
            : "";
         this.withParticipant(menuRequester, participant -> {
            Component failureMessage = this.deliverMainMenu(participant, favouriteRankedId);
            if (failureMessage != null) {
               this.tellParticipant(menuRequester, failureMessage);
            }
         });
      } else {
         UUID leaderboardRequester = this.claimLeaderboardRef(document.get("ref"));
         if (leaderboardRequester != null) {
            LeaderboardPayload leaderboard = LeaderboardDecoding.decode(document);
            this.withParticipant(leaderboardRequester, leaderboardRecipient -> CobbleBattleNetwork.sendLeaderboard(leaderboardRecipient, leaderboard));
         }
      }
   }

   private void publishBattleEnded(MirrorBattle endedBattle, JsonObject document, String endReason) {
      String winningSeat = BattleServerClient.str(document, "winnerSeat", "");

      for (UUID participantUuid : endedBattle.localPlayers()) {
         BattleInfo battleDetails = endedBattle.infoFor(participantUuid);
         if (battleDetails != null) {
            BattleOutcome battleOutcome = BattleResultProjection.outcome(endReason, winningSeat, endedBattle.seatOf(participantUuid));
            ScoreChange scoreChange = BattleResultProjection.scoreFor(document, participantUuid);
            this.withParticipant(participantUuid, participant -> ApiEvents.battleEnded(participant, battleDetails, battleOutcome, endReason, scoreChange));
         }
      }
   }

   private void announceScoreChanges(JsonObject document) {
      for (BattleResultProjection.ParticipantScore score : BattleResultProjection.participantScores(document)) {
         long scoreDelta = score.getAfter() - score.getBefore();
         String signedDelta = (scoreDelta >= 0L ? "+" : "") + scoreDelta;
         this.tellParticipant(
            score.getParticipant(),
            Msg.of(
               score.getWon() ? ChatFormatting.GREEN : ChatFormatting.RED,
               score.getWon() ? "rank.won" : "rank.lost",
               signedDelta,
               score.getBefore(),
               score.getAfter()
            )
         );
      }
   }

   private static Component formatChatFailure(JsonObject document, String errorCode, String fallbackText) {
      if ("MUTED".equals(errorCode)) {
         long remainingMillis = document.has("left") ? document.get("left").getAsLong() : 0L;
         Component mutedMessage = remainingMillis == 0L ? Msg.of("chat.err.muted_permanent") : Msg.of("chat.err.muted_for", formatDuration(remainingMillis));
         return mutedMessage.copy().withStyle(ChatFormatting.RED);
      } else {
         String translationKey = ChatErrorRules.translationKey(errorCode);
         Component failureMessage = translationKey == null ? Component.literal(fallbackText) : Msg.of(translationKey);
         return failureMessage.copy().withStyle(ChatFormatting.RED);
      }
   }

   public void onChatSubmitted(ServerPlayer participant, String selectedConversation, String messageText) {
      if (messageText != null && !messageText.isBlank()) {
         if (!this.authenticationService.isSignedIn(participant.getUUID())) {
            this.tellParticipant(participant.getUUID(), Msg.of(ChatFormatting.YELLOW, "queue.not_signed_in"));
         } else if (!this.isConnected()) {
            this.tellParticipant(participant.getUUID(), this.notConnected("chat.not_connected"));
         } else if (!this.remoteChatEnabled) {
            this.tellParticipant(participant.getUUID(), Msg.of(ChatFormatting.RED, "chat.err.disabled"));
         } else {
            int requestReference = this.battleServerClient.nextRef();
            JsonObject requestFrame = ServiceRequests.chat(requestReference, selectedConversation, messageText, participant.getUUID(), participant.getGameProfile().getName());
            this.requestLedger.sendChat(requestReference, participant.getUUID(), () -> this.battleServerClient.send(requestFrame));
         }
      }
   }

   public void pushChatState(ServerPlayer participant) {
      UUID participantUuid = participant.getUUID();
      if (!this.authenticationService.isSignedIn(participantUuid)) {
         CobbleBattleNetwork.sendChatState(participant, ChatStatePayload.signedOut());
         this.reportChatObservers();
      } else {
         CobbleBattleNetwork.sendChatState(
            participant,
            new ChatStatePayload(
               true, CrossServerBattles.byLocalPlayer(participantUuid) != null, this.authenticationService.uidOf(participantUuid), String.valueOf(this.authenticationService.nicknameOf(participantUuid)), this.remoteChatEnabled
            )
         );
         this.reportChatObservers();
      }
   }

   private void reportChatObservers() {
      if (this.battleServerClient != null && this.battleServerClient.isHandshaken()) {
         int observers = this.authenticationService.signedInPlayers().size();
         if (observers != this.chatObserversReported) {
            JsonObject frame = ServiceProtocolMessages.chatWatch(observers);
            if (this.battleServerClient.send(frame)) {
               this.chatObserversReported = observers;
            }
         }
      }
   }

   public boolean chatEnabled() {
      return this.remoteChatEnabled;
   }

   public boolean emailEnabled() {
      return this.emailAuthenticationEnabled;
   }

   public void pushChatState(UUID participantUuid) {
      this.withParticipant(participantUuid, this::pushChatState);
   }

   private void handleVerificationCodeAccepted(JsonObject document) {
      UUID participant = this.authenticationService.onAccountCodeOk(document);
      this.withParticipant(participant, accountPlayer -> CobbleBattleNetwork.sendResult(accountPlayer, true, Component.translatable("auth.code_sent")));
   }

   private void handleAuthenticationAccepted(JsonObject document) {
      AuthService.Outcome authenticationOutcome = this.authenticationService.onAccountOk(document);
      if (authenticationOutcome != null) {
         Component successMessage = Msg.of(
            ChatFormatting.GREEN, authenticationOutcome.registered() ? "auth.registered" : "auth.logged_in", authenticationOutcome.accountId(), authenticationOutcome.nickname(), authenticationOutcome.uid()
         );
         this.tellParticipant(authenticationOutcome.playerUuid(), successMessage);
         this.withParticipant(authenticationOutcome.playerUuid(), participant -> {
            CobbleBattleNetwork.sendResult(participant, true, successMessage);
            this.pushChatState(participant);
            ApiEvents.signedIn(participant, authenticationOutcome.accountId(), authenticationOutcome.nickname(), authenticationOutcome.uid(), authenticationOutcome.registered());
            Component menuFailure = this.openMainMenu(participant);
            if (menuFailure != null) {
               this.tellParticipant(authenticationOutcome.playerUuid(), menuFailure);
            }
         });
      }
   }

   void onServerThreadWithParticipant(UUID participantUuid, Consumer<ServerPlayer> action) {
      this.withParticipant(participantUuid, action);
   }

   private void withParticipant(UUID participantUuid, Consumer<ServerPlayer> action) {
      MinecraftServer server = this.gameServer;
      if (server != null) {
         server.execute(() -> {
            ServerPlayer participant = server.getPlayerList().getPlayer(participantUuid);
            if (participant != null) {
               action.accept(participant);
            }
         });
      }
   }

   public Component queue(ServerPlayer participant, String competitionId) {
      return this.matchmakingQueue.joinCompetitionQueue(participant, competitionId);
   }

   public Component leaveQueue(ServerPlayer participant) {
      return this.matchmakingQueue.leaveMatchmakingQueue(participant);
   }

   public Component describePartyCompatibility(ServerPlayer participant) {
      return this.matchmakingQueue.evaluatePartyCompatibility(participant);
   }

   public void onPlayerDisconnect(ServerPlayer participant) {
      this.waitingChunksAuthOpens.remove(participant.getUUID());
      this.scheduleDisconnectWhenUnused();
      this.matchmakingQueue.onParticipantDisconnect(participant);
      this.teamPreviewSessions.forgetParticipant(participant.getUUID());
      MirrorBattle activeMirror = CrossServerBattles.byLocalPlayer(participant.getUUID());
      if (activeMirror != null && !activeMirror.isFinished()) {
         SERVICE_LOGGER.info("{} disconnected during battle {} - closing this side and telling the host", participant.getGameProfile().getName(), activeMirror.remoteBattleId());
         this.sendBattleAbort(activeMirror.remoteBattleId(), "player disconnected");
         this.lifecycleCleanup.abort(activeMirror, Msg.of("battle.player_left").withStyle(ChatFormatting.RED));
      }

      this.announceSignOutputStream(participant);
      this.authenticationService.signOut(this.battleServerClient, participant);
      this.reportChatObservers();
      this.roomDirectory.forgetDelivery(participant.getUUID());
   }

   void tellParticipant(UUID participantUuid, Component document) {
      MinecraftServer server = this.gameServer;
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
