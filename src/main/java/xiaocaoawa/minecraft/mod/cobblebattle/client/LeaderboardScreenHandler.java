package xiaocaoawa.minecraft.mod.cobblebattle.client;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.NetworkManager.Side;
import net.minecraft.client.Minecraft;
import xiaocaoawa.minecraft.mod.cobblebattle.network.LeaderboardPayload;

public final class LeaderboardScreenHandler {
   private LeaderboardScreenHandler() {
   }

   public static void init() {
      NetworkManager.registerReceiver(Side.S2C, LeaderboardPayload.TYPE, LeaderboardPayload.CODEC, (body, context) -> context.queue(() -> {
         Minecraft minecraft = Minecraft.getInstance();
         if (minecraft.player != null) {
            ServerDex.rememberRanked(body.rankedId());
            minecraft.setScreen(new LeaderboardScreen(body, minecraft.player.getUUID()));
         }
      }));
   }
}
