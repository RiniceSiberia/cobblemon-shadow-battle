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
         (body, context) -> {
            if (context.getPlayer() instanceof ServerPlayer participant) {
               context.queue(
                  () -> CobbleBattle.service()
                     .onCredentialsSubmitted(participant, body.authMode(), body.accountId(), body.email(), body.password(), body.verificationCode())
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

   public static boolean sendTeamPreview(ServerPlayer participant, TeamPreviewPayload preview) {
      if (!NetworkManager.canPlayerReceive(participant, TeamPreviewPayload.TYPE)) {
         return false;
      } else {
         NetworkManager.sendToPlayer(participant, preview);
         return true;
      }
   }

   public static boolean sendMainMenu(ServerPlayer participant, OpenMainMenuPayload menu) {
      if (!NetworkManager.canPlayerReceive(participant, OpenMainMenuPayload.TYPE)) {
         return false;
      } else {
         NetworkManager.sendToPlayer(participant, menu);
         return true;
      }
   }

   public static boolean sendRooms(ServerPlayer participant, RoomListPayload rooms) {
      if (!NetworkManager.canPlayerReceive(participant, RoomListPayload.TYPE)) {
         return false;
      } else {
         NetworkManager.sendToPlayer(participant, rooms);
         return true;
      }
   }

   public static boolean sendRoomState(ServerPlayer participant, RoomStatePayload state) {
      if (!NetworkManager.canPlayerReceive(participant, RoomStatePayload.TYPE)) {
         return false;
      } else {
         NetworkManager.sendToPlayer(participant, state);
         return true;
      }
   }

   public static boolean sendServerDex(ServerPlayer participant, ServerDexPayload dex) {
      if (!NetworkManager.canPlayerReceive(participant, ServerDexPayload.TYPE)) {
         return false;
      } else {
         NetworkManager.sendToPlayer(participant, dex);
         return true;
      }
   }

   public static boolean openScreen(ServerPlayer participant, AuthMode mode, String suggestedId, boolean emailEnabled) {
      if (!NetworkManager.canPlayerReceive(participant, OpenAuthScreenPayload.TYPE)) {
         return false;
      } else {
         NetworkManager.sendToPlayer(participant, OpenAuthScreenPayload.of(mode, suggestedId, emailEnabled));
         return true;
      }
   }

   public static void sendResult(ServerPlayer participant, boolean ok, Component document) {
      if (NetworkManager.canPlayerReceive(participant, AuthResultPayload.TYPE)) {
         NetworkManager.sendToPlayer(participant, new AuthResultPayload(ok, document.getString()));
      }
   }

   public static void sendChatLine(ServerPlayer participant, ChatLinePayload line) {
      if (NetworkManager.canPlayerReceive(participant, ChatLinePayload.TYPE)) {
         NetworkManager.sendToPlayer(participant, line);
      }
   }

   public static void sendLeaderboard(ServerPlayer participant, LeaderboardPayload board) {
      if (NetworkManager.canPlayerReceive(participant, LeaderboardPayload.TYPE)) {
         NetworkManager.sendToPlayer(participant, board);
      }
   }

   public static void sendChatState(ServerPlayer participant, ChatStatePayload state) {
      if (NetworkManager.canPlayerReceive(participant, ChatStatePayload.TYPE)) {
         NetworkManager.sendToPlayer(participant, state);
      }
   }
}
