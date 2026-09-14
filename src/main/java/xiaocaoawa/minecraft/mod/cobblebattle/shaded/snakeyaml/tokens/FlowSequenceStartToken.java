package xiaocaoawa.minecraft.mod.cobblebattle.shaded.snakeyaml.tokens;

import xiaocaoawa.minecraft.mod.cobblebattle.shaded.snakeyaml.error.Mark;

public final class FlowSequenceStartToken extends Token {
   public FlowSequenceStartToken(Mark startMark, Mark endMark) {
      super(startMark, endMark);
   }

   @Override
   public Token.ID getTokenId() {
      return Token.ID.FlowSequenceStart;
   }
}
