package xiaocaoawa.minecraft.mod.cobblebattle.client;

import xiaocaoawa.minecraft.mod.cobblebattle.account.AuthMode;

public final class AuthScreenHandler {
   private AuthScreenHandler() {
   }

   public static void init() {
      ClientHandlerRuntime.initializeAuthentication();
   }

   static void submit(AuthMode authMode, String accountId, String email, String password, String verificationCode) {
      ClientHandlerRuntime.submitAuthentication(authMode, accountId, email, password, verificationCode);
   }
}
