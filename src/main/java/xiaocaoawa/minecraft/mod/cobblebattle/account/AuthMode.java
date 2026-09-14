package xiaocaoawa.minecraft.mod.cobblebattle.account;

public enum AuthMode {
   LOGIN,
   REGISTER,
   BIND_EMAIL,
   CODE_REQUEST;

   public boolean isBindEmail() {
      return this == BIND_EMAIL;
   }

   public boolean isCodeRequest() {
      return this == CODE_REQUEST;
   }

   public boolean isRegister() {
      return this == REGISTER;
   }

   public static AuthMode byOrdinal(int ordinal) {
      AuthMode[] all = values();
      return ordinal >= 0 && ordinal < all.length ? all[ordinal] : LOGIN;
   }
}
