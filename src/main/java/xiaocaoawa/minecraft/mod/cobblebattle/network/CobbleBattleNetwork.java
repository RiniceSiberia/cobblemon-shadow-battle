package xiaocaoawa.minecraft.mod.cobblebattle.network;

import io.github.rinicesiberia.shadowbattle.network.PayloadChannelCoordinator;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import xiaocaoawa.minecraft.mod.cobblebattle.account.AuthMode;

public final class CobbleBattleNetwork {
   private CobbleBattleNetwork() {
   }

   public static void init() {
      PayloadChannelCoordinator.initialize();
   }

   public static boolean sendTeamPreview(ServerPlayer participant, TeamPreviewPayload previewPayload) {
      return PayloadChannelCoordinator.sendWhenSupported(participant, TeamPreviewPayload.TYPE, () -> previewPayload);
   }

   public static boolean sendMainMenu(ServerPlayer participant, OpenMainMenuPayload menuPayload) {
      return PayloadChannelCoordinator.sendWhenSupported(participant, OpenMainMenuPayload.TYPE, () -> menuPayload);
   }

   public static boolean sendRooms(ServerPlayer participant, RoomListPayload roomListPayload) {
      return PayloadChannelCoordinator.sendWhenSupported(participant, RoomListPayload.TYPE, () -> roomListPayload);
   }

   public static boolean sendRoomState(ServerPlayer participant, RoomStatePayload roomStatePayload) {
      return PayloadChannelCoordinator.sendWhenSupported(participant, RoomStatePayload.TYPE, () -> roomStatePayload);
   }

   public static boolean sendServerDex(ServerPlayer participant, ServerDexPayload dexPayload) {
      return PayloadChannelCoordinator.sendWhenSupported(participant, ServerDexPayload.TYPE, () -> dexPayload);
   }

   public static boolean openScreen(ServerPlayer participant, AuthMode authMode, String suggestedId, boolean emailEnabled) {
      return PayloadChannelCoordinator.sendWhenSupported(
         participant, OpenAuthScreenPayload.TYPE, () -> OpenAuthScreenPayload.of(authMode, suggestedId, emailEnabled)
      );
   }

   public static void sendResult(ServerPlayer participant, boolean succeeded, Component resultComponent) {
      PayloadChannelCoordinator.sendWhenSupported(participant, AuthResultPayload.TYPE, () -> new AuthResultPayload(succeeded, resultComponent.getString()));
   }

   public static void sendChatLine(ServerPlayer participant, ChatLinePayload chatLinePayload) {
      PayloadChannelCoordinator.sendWhenSupported(participant, ChatLinePayload.TYPE, () -> chatLinePayload);
   }

   public static void sendLeaderboard(ServerPlayer participant, LeaderboardPayload leaderboardPayload) {
      PayloadChannelCoordinator.sendWhenSupported(participant, LeaderboardPayload.TYPE, () -> leaderboardPayload);
   }

   public static void sendChatState(ServerPlayer participant, ChatStatePayload chatStatePayload) {
      PayloadChannelCoordinator.sendWhenSupported(participant, ChatStatePayload.TYPE, () -> chatStatePayload);
   }
}
