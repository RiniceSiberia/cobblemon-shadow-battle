package xiaocaoawa.minecraft.mod.cobblebattle.client;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.NetworkManager.Side;
import net.minecraft.client.Minecraft;
import xiaocaoawa.minecraft.mod.cobblebattle.network.TeamPreviewPayload;

public final class TeamPreviewHandler {
   private TeamPreviewHandler() {
   }

   public static void init() {
      NetworkManager.registerReceiver(Side.S2C, TeamPreviewPayload.TYPE, TeamPreviewPayload.CODEC, (payload, context) -> context.queue(() -> {
         Minecraft minecraft = Minecraft.getInstance();
         if (minecraft.player != null) {
            if (minecraft.screen instanceof TeamPreviewScreen open && open.battleId().equals(payload.battleId())) {
               open.update(payload);
            } else if (payload.closed().isEmpty()) {
               minecraft.setScreen(new TeamPreviewScreen(payload));
            }
         }
      }));
   }
}
