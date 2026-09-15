package xiaocaoawa.minecraft.mod.cobblebattle.client;

import xiaocaoawa.minecraft.mod.cobblebattle.account.AuthMode;

public final class AuthScreenHandler {
   private AuthScreenHandler() {
   }

   public static void init() {
      ClientHandlerRuntime.initializeAuthentication();
   }

   static void submit(AuthMode mode, String accountId, String email, String password, String code) {
      ClientHandlerRuntime.submitAuthentication(mode, accountId, email, password, code);
   }
}
