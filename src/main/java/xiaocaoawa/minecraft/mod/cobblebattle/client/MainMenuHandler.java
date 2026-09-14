package xiaocaoawa.minecraft.mod.cobblebattle.client;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientRawInputEvent;
import dev.architectury.event.events.client.ClientRawInputEvent.KeyPressed;
import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.NetworkManager.Side;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.Minecraft;
import xiaocaoawa.minecraft.mod.cobblebattle.network.OpenMainMenuPayload;

public final class MainMenuHandler {
   private MainMenuHandler() {
   }

   public static void init() {
      KeyMappingRegistry.register(MenuKeys.OPEN);
      NetworkManager.registerReceiver(Side.S2C, OpenMainMenuPayload.TYPE, OpenMainMenuPayload.CODEC, (body, context) -> context.queue(() -> {
         Minecraft minecraft = Minecraft.getInstance();
         if (minecraft.player != null) {
            minecraft.setScreen(new MainMenuScreen(body));
         }
      }));
      ClientRawInputEvent.KEY_PRESSED.register((KeyPressed)(minecraft, keyCode, scanCode, action, modifiers) -> {
         if (action != 1 || minecraft.screen != null || minecraft.player == null) {
            return EventResult.pass();
         } else if (!MenuKeys.OPEN.isUnbound() && MenuKeys.OPEN.matches(keyCode, scanCode)) {
            ServerDex.requestMain();
            return EventResult.interruptTrue();
         } else {
            return EventResult.pass();
         }
      });
   }
}
