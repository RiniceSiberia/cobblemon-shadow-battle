package xiaocaoawa.minecraft.mod.cobblebattle.shaded.snakeyaml.tokens;

import xiaocaoawa.minecraft.mod.cobblebattle.shaded.snakeyaml.error.Mark;
import xiaocaoawa.minecraft.mod.cobblebattle.shaded.snakeyaml.error.YAMLException;

public abstract class Token {
   private final Mark startMark;
   private final Mark endMark;

   public Token(Mark startMark, Mark endMark) {
      if (startMark != null && endMark != null) {
         this.startMark = startMark;
         this.endMark = endMark;
      } else {
         throw new YAMLException("Token requires marks.");
      }
   }

   public Mark getStartMark() {
      return this.startMark;
   }

   public Mark getEndMark() {
      return this.endMark;
   }

   public abstract Token.ID getTokenId();

   public static enum ID {
      Alias("<alias>"),
      Anchor("<anchor>"),
      BlockEnd("<block end>"),
      BlockEntry("-"),
      BlockMappingStart("<block mapping start>"),
      BlockSequenceStart("<block sequence start>"),
      Directive("<directive>"),
      DocumentEnd("<document end>"),
      DocumentStart("<document start>"),
      FlowEntry(","),
      FlowMappingEnd("}"),
      FlowMappingStart("{"),
      FlowSequenceEnd("]"),
      FlowSequenceStart("["),
      Key("?"),
      Scalar("<scalar>"),
      StreamEnd("<stream end>"),
      StreamStart("<stream start>"),
      Tag("<tag>"),
      Value(":"),
      Whitespace("<whitespace>"),
      Comment("#"),
      Error("<error>");

      private final String description;

      private ID(String s) {
         this.description = s;
      }

      @Override
      public String toString() {
         return this.description;
      }
   }
}
