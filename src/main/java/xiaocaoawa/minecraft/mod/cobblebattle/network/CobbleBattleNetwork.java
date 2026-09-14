package xiaocaoawa.minecraft.mod.cobblebattle.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.NetworkManager.Side;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import xiaocaoawa.minecraft.mod.cobblebattle.CobbleBattle;
import xiaocaoawa.minecraft.mod.cobblebattle.account.AuthMode;

public final class CobbleBattleNetwork {
   private CobbleBattleNetwork() {
   }

   public static void init() {
      NetworkManager.registerReceiver(
         Side.C2S,
         SubmitAuthPayload.TYPE,
         SubmitAuthPayload.CODEC,
         (payload, context) -> {
            if (context.getPlayer() instanceof ServerPlayer player) {
               context.queue(
                  () -> CobbleBattle.service()
                     .onCredentialsSubmitted(player, payload.authMode(), payload.accountId(), payload.email(), payload.password(), payload.verificationCode())
               );
            }
         }
      );
      NetworkManager.registerReceiver(Side.C2S, SendChatPayload.TYPE, SendChatPayload.CODEC, (payload, context) -> {
         if (context.getPlayer() instanceof ServerPlayer player) {
            context.queue(() -> CobbleBattle.service().onChatSubmitted(player, payload.channel(), payload.text()));
         }
      });
      NetworkManager.registerReceiver(Side.C2S, OpenPagePayload.TYPE, OpenPagePayload.CODEC, (payload, context) -> {
         if (context.getPlayer() instanceof ServerPlayer player) {
            context.queue(() -> CobbleBattle.service().openPage(player, payload.page(), payload.ranked(), payload.have()));
         }
      });
      NetworkManager.registerReceiver(Side.C2S, RoomActionPayload.TYPE, RoomActionPayload.CODEC, (payload, context) -> {
         if (context.getPlayer() instanceof ServerPlayer player) {
            context.queue(() -> CobbleBattle.service().onRoomAction(player, payload));
         }
      });
      NetworkManager.registerReceiver(Side.C2S, TeamPickPayload.TYPE, TeamPickPayload.CODEC, (payload, context) -> {
         if (context.getPlayer() instanceof ServerPlayer player) {
            context.queue(() -> CobbleBattle.service().onTeamPicked(player, payload.battleId(), payload.picks()));
         }
      });
      NetworkManager.registerReceiver(Side.C2S, MenuActionPayload.TYPE, MenuActionPayload.CODEC, (payload, context) -> {
         if (context.getPlayer() instanceof ServerPlayer player) {
            context.queue(() -> CobbleBattle.service().onMenuAction(player, payload));
         }
      });
      if (Platform.getEnvironment() == Env.SERVER) {
         NetworkManager.registerS2CPayloadType(OpenAuthScreenPayload.TYPE, OpenAuthScreenPayload.CODEC);
         NetworkManager.registerS2CPayloadType(AuthResultPayload.TYPE, AuthResultPayload.CODEC);
         NetworkManager.registerS2CPayloadType(ChatLinePayload.TYPE, ChatLinePayload.CODEC);
         NetworkManager.registerS2CPayloadType(ChatStatePayload.TYPE, ChatStatePayload.CODEC);
         NetworkManager.registerS2CPayloadType(LeaderboardPayload.TYPE, LeaderboardPayload.CODEC);
         NetworkManager.registerS2CPayloadType(ServerDexPayload.TYPE, ServerDexPayload.CODEC);
         NetworkManager.registerS2CPayloadType(RoomListPayload.TYPE, RoomListPayload.CODEC);
         NetworkManager.registerS2CPayloadType(RoomStatePayload.TYPE, RoomStatePayload.CODEC);
         NetworkManager.registerS2CPayloadType(OpenMainMenuPayload.TYPE, OpenMainMenuPayload.CODEC);
         NetworkManager.registerS2CPayloadType(TeamPreviewPayload.TYPE, TeamPreviewPayload.CODEC);
      }
   }

   public static boolean sendTeamPreview(ServerPlayer player, TeamPreviewPayload preview) {
      if (!NetworkManager.canPlayerReceive(player, TeamPreviewPayload.TYPE)) {
         return false;
      } else {
         NetworkManager.sendToPlayer(player, preview);
         return true;
      }
   }

   public static boolean sendMainMenu(ServerPlayer player, OpenMainMenuPayload menu) {
      if (!NetworkManager.canPlayerReceive(player, OpenMainMenuPayload.TYPE)) {
         return false;
      } else {
         NetworkManager.sendToPlayer(player, menu);
         return true;
      }
   }

   public static boolean sendRooms(ServerPlayer player, RoomListPayload rooms) {
      if (!NetworkManager.canPlayerReceive(player, RoomListPayload.TYPE)) {
         return false;
      } else {
         NetworkManager.sendToPlayer(player, rooms);
         return true;
      }
   }

   public static boolean sendRoomState(ServerPlayer player, RoomStatePayload state) {
      if (!NetworkManager.canPlayerReceive(player, RoomStatePayload.TYPE)) {
         return false;
      } else {
         NetworkManager.sendToPlayer(player, state);
         return true;
      }
   }

   public static boolean sendServerDex(ServerPlayer player, ServerDexPayload dex) {
      if (!NetworkManager.canPlayerReceive(player, ServerDexPayload.TYPE)) {
         return false;
      } else {
         NetworkManager.sendToPlayer(player, dex);
         return true;
      }
   }

   public static boolean openScreen(ServerPlayer player, AuthMode mode, String suggestedId, boolean emailEnabled) {
      if (!NetworkManager.canPlayerReceive(player, OpenAuthScreenPayload.TYPE)) {
         return false;
      } else {
         NetworkManager.sendToPlayer(player, OpenAuthScreenPayload.of(mode, suggestedId, emailEnabled));
         return true;
      }
   }

   public static void sendResult(ServerPlayer player, boolean ok, Component message) {
      if (NetworkManager.canPlayerReceive(player, AuthResultPayload.TYPE)) {
         NetworkManager.sendToPlayer(player, new AuthResultPayload(ok, message.getString()));
      }
   }

   public static void sendChatLine(ServerPlayer player, ChatLinePayload line) {
      if (NetworkManager.canPlayerReceive(player, ChatLinePayload.TYPE)) {
         NetworkManager.sendToPlayer(player, line);
      }
   }

   public static void sendLeaderboard(ServerPlayer player, LeaderboardPayload board) {
      if (NetworkManager.canPlayerReceive(player, LeaderboardPayload.TYPE)) {
         NetworkManager.sendToPlayer(player, board);
      }
   }

   public static void sendChatState(ServerPlayer player, ChatStatePayload state) {
      if (NetworkManager.canPlayerReceive(player, ChatStatePayload.TYPE)) {
         NetworkManager.sendToPlayer(player, state);
      }
   }
}
