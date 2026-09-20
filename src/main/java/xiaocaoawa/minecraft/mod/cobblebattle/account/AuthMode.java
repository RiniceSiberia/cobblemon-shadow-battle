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

   public static AuthMode byOrdinal(int ordinalValue) {
      AuthMode[] availableModes = values();
      return ordinalValue >= 0 && ordinalValue < availableModes.length ? availableModes[ordinalValue] : LOGIN;
   }
}
