package xiaocaoawa.minecraft.mod.cobblebattle.shaded.snakeyaml.scanner;

import xiaocaoawa.minecraft.mod.cobblebattle.shaded.snakeyaml.tokens.Token;

public interface Scanner {
   boolean checkToken(Token.ID... var1);

   default boolean checkToken(Token.ID choice) {
      return this.checkToken(choice);
   }

   Token peekToken();

   Token getToken();

   void resetDocumentIndex();
}
