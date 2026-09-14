package xiaocaoawa.minecraft.mod.cobblebattle.shaded.snakeyaml.composer;

import xiaocaoawa.minecraft.mod.cobblebattle.shaded.snakeyaml.error.Mark;
import xiaocaoawa.minecraft.mod.cobblebattle.shaded.snakeyaml.error.MarkedYAMLException;

public class ComposerException extends MarkedYAMLException {
   private static final long serialVersionUID = 2146314636913113935L;

   protected ComposerException(String context, Mark contextMark, String problem, Mark problemMark) {
      super(context, contextMark, problem, problemMark);
   }

   protected ComposerException(String problem, Mark problemMark) {
      this(null, null, problem, problemMark);
   }
}
