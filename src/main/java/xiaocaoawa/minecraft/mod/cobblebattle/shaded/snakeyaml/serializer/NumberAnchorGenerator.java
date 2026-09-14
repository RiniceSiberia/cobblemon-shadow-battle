package xiaocaoawa.minecraft.mod.cobblebattle.shaded.snakeyaml.serializer;

import java.text.NumberFormat;
import xiaocaoawa.minecraft.mod.cobblebattle.shaded.snakeyaml.nodes.Node;

public class NumberAnchorGenerator implements AnchorGenerator {
   private int lastAnchorId = 0;

   public NumberAnchorGenerator(int lastAnchorId) {
      this.lastAnchorId = lastAnchorId;
   }

   @Override
   public String nextAnchor(Node node) {
      if (node.getAnchor() != null) {
         return node.getAnchor();
      } else {
         this.lastAnchorId++;
         NumberFormat format = NumberFormat.getNumberInstance();
         format.setMinimumIntegerDigits(3);
         format.setMaximumFractionDigits(0);
         format.setGroupingUsed(false);
         String anchorId = format.format((long)this.lastAnchorId);
         return "id" + anchorId;
      }
   }
}
