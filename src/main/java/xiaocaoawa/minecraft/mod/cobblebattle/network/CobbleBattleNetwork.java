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

   public static boolean sendTeamPreview(ServerPlayer participant, TeamPreviewPayload preview) {
      return PayloadChannelCoordinator.sendWhenSupported(participant, TeamPreviewPayload.TYPE, () -> preview);
   }

   public static boolean sendMainMenu(ServerPlayer participant, OpenMainMenuPayload menu) {
      return PayloadChannelCoordinator.sendWhenSupported(participant, OpenMainMenuPayload.TYPE, () -> menu);
   }

   public static boolean sendRooms(ServerPlayer participant, RoomListPayload rooms) {
      return PayloadChannelCoordinator.sendWhenSupported(participant, RoomListPayload.TYPE, () -> rooms);
   }

   public static boolean sendRoomState(ServerPlayer participant, RoomStatePayload state) {
      return PayloadChannelCoordinator.sendWhenSupported(participant, RoomStatePayload.TYPE, () -> state);
   }

   public static boolean sendServerDex(ServerPlayer participant, ServerDexPayload dex) {
      return PayloadChannelCoordinator.sendWhenSupported(participant, ServerDexPayload.TYPE, () -> dex);
   }

   public static boolean openScreen(ServerPlayer participant, AuthMode mode, String suggestedId, boolean emailEnabled) {
      return PayloadChannelCoordinator.sendWhenSupported(
         participant, OpenAuthScreenPayload.TYPE, () -> OpenAuthScreenPayload.of(mode, suggestedId, emailEnabled)
      );
   }

   public static void sendResult(ServerPlayer participant, boolean ok, Component document) {
      PayloadChannelCoordinator.sendWhenSupported(participant, AuthResultPayload.TYPE, () -> new AuthResultPayload(ok, document.getString()));
   }

   public static void sendChatLine(ServerPlayer participant, ChatLinePayload line) {
      PayloadChannelCoordinator.sendWhenSupported(participant, ChatLinePayload.TYPE, () -> line);
   }

   public static void sendLeaderboard(ServerPlayer participant, LeaderboardPayload board) {
      PayloadChannelCoordinator.sendWhenSupported(participant, LeaderboardPayload.TYPE, () -> board);
   }

   public static void sendChatState(ServerPlayer participant, ChatStatePayload state) {
      PayloadChannelCoordinator.sendWhenSupported(participant, ChatStatePayload.TYPE, () -> state);
   }
}
