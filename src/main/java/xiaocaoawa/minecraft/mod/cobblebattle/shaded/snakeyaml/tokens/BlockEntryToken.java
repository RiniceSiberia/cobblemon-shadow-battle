package xiaocaoawa.minecraft.mod.cobblebattle.shaded.snakeyaml.tokens;

import xiaocaoawa.minecraft.mod.cobblebattle.shaded.snakeyaml.error.Mark;

public final class BlockEntryToken extends Token {
   public BlockEntryToken(Mark startMark, Mark endMark) {
      super(startMark, endMark);
   }

   @Override
   public Token.ID getTokenId() {
      return Token.ID.BlockEntry;
   }
}
