package xiaocaoawa.minecraft.mod.cobblebattle.client;

import com.cobblemon.mod.common.client.CobblemonClient;
import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.NetworkManager.Side;
import net.minecraft.client.Minecraft;
import xiaocaoawa.minecraft.mod.cobblebattle.network.RoomListPayload;
import xiaocaoawa.minecraft.mod.cobblebattle.network.RoomStatePayload;

public final class RoomScreenHandler {
   private RoomScreenHandler() {
   }

   public static void init() {
      NetworkManager.registerReceiver(Side.S2C, RoomListPayload.TYPE, RoomListPayload.CODEC, (body, context) -> context.queue(() -> {
         Minecraft minecraft = Minecraft.getInstance();
         if (minecraft.player != null) {
            if (minecraft.screen instanceof RoomLobbyScreen lobby) {
               lobby.update(body);
            } else if (!body.refresh()) {
               if (CobblemonClient.INSTANCE.getBattle() == null) {
                  minecraft.setScreen(new RoomLobbyScreen(body));
               }
            }
         }
      }));
      NetworkManager.registerReceiver(Side.S2C, RoomStatePayload.TYPE, RoomStatePayload.CODEC, (payload, context) -> context.queue(() -> {
         Minecraft minecraft = Minecraft.getInstance();
         if (minecraft.player != null) {
            if (minecraft.screen instanceof RoomScreen room && room.roomId().equals(payload.roomId())) {
               room.update(payload);
            } else if (CobblemonClient.INSTANCE.getBattle() == null) {
               minecraft.setScreen(new RoomScreen(payload));
            }
         }
      }));
   }
}
