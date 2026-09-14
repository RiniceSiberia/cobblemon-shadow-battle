package xiaocaoawa.minecraft.mod.cobblebattle.shaded.snakeyaml.constructor;

import xiaocaoawa.minecraft.mod.cobblebattle.shaded.snakeyaml.error.YAMLException;
import xiaocaoawa.minecraft.mod.cobblebattle.shaded.snakeyaml.nodes.Node;

public abstract class AbstractConstruct implements Construct {
   @Override
   public void construct2ndStep(Node node, Object data) {
      if (node.isTwoStepsConstruction()) {
         throw new IllegalStateException("Not Implemented in " + this.getClass().getName());
      } else {
         throw new YAMLException("Unexpected recursive structure for Node: " + node);
      }
   }
}
