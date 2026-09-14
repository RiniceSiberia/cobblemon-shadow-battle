package xiaocaoawa.minecraft.mod.cobblebattle.client;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.NetworkManager.Side;
import net.minecraft.client.Minecraft;
import xiaocaoawa.minecraft.mod.cobblebattle.account.AuthMode;
import xiaocaoawa.minecraft.mod.cobblebattle.network.AuthResultPayload;
import xiaocaoawa.minecraft.mod.cobblebattle.network.OpenAuthScreenPayload;
import xiaocaoawa.minecraft.mod.cobblebattle.network.SubmitAuthPayload;

public final class AuthScreenHandler {
   private AuthScreenHandler() {
   }

   public static void init() {
      NetworkManager.registerReceiver(
         Side.S2C,
         OpenAuthScreenPayload.TYPE,
         OpenAuthScreenPayload.CODEC,
         (body, context) -> context.queue(
            () -> Minecraft.getInstance().setScreen(new AuthScreen(body.authMode(), body.suggestedId(), body.emailEnabled()))
         )
      );
      NetworkManager.registerReceiver(Side.S2C, AuthResultPayload.TYPE, AuthResultPayload.CODEC, (payload, context) -> context.queue(() -> {
         if (Minecraft.getInstance().screen instanceof AuthScreen screen) {
            screen.onResult(payload.ok(), payload.message());
         }
      }));
   }

   static void submit(AuthMode mode, String accountId, String email, String password, String code) {
      NetworkManager.sendToServer(SubmitAuthPayload.of(mode, accountId, email, password, code));
   }
}
