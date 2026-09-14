package xiaocaoawa.minecraft.mod.cobblebattle.client;

import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.event.events.client.ClientTickEvent.Client;
import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.NetworkManager.Side;
import xiaocaoawa.minecraft.mod.cobblebattle.network.ServerDexPayload;

public final class ServerDexHandler {
   private ServerDexHandler() {
   }

   public static void init() {
      NetworkManager.registerReceiver(
         Side.S2C, ServerDexPayload.TYPE, ServerDexPayload.CODEC, (payload, context) -> context.queue(() -> ServerDex.open(payload))
      );
      ClientTickEvent.CLIENT_POST.register((Client)minecraft -> ServerDex.tick());
   }
}
