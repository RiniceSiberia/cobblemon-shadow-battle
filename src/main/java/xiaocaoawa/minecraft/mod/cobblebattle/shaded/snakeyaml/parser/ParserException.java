package xiaocaoawa.minecraft.mod.cobblebattle.shaded.snakeyaml.parser;

import xiaocaoawa.minecraft.mod.cobblebattle.shaded.snakeyaml.error.Mark;
import xiaocaoawa.minecraft.mod.cobblebattle.shaded.snakeyaml.error.MarkedYAMLException;

public class ParserException extends MarkedYAMLException {
   private static final long serialVersionUID = -2349253802798398038L;

   public ParserException(String context, Mark contextMark, String problem, Mark problemMark) {
      super(context, contextMark, problem, problemMark, null, null);
   }
}
