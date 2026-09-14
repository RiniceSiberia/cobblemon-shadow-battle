package xiaocaoawa.minecraft.mod.cobblebattle.shaded.snakeyaml.scanner;

import xiaocaoawa.minecraft.mod.cobblebattle.shaded.snakeyaml.error.Mark;
import xiaocaoawa.minecraft.mod.cobblebattle.shaded.snakeyaml.error.MarkedYAMLException;

public class ScannerException extends MarkedYAMLException {
   private static final long serialVersionUID = 4782293188600445954L;

   public ScannerException(String context, Mark contextMark, String problem, Mark problemMark, String note) {
      super(context, contextMark, problem, problemMark, note);
   }

   public ScannerException(String context, Mark contextMark, String problem, Mark problemMark) {
      this(context, contextMark, problem, problemMark, null);
   }
}
