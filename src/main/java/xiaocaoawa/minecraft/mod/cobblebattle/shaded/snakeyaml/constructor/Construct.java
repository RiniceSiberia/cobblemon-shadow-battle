package xiaocaoawa.minecraft.mod.cobblebattle.shaded.snakeyaml.constructor;

import xiaocaoawa.minecraft.mod.cobblebattle.shaded.snakeyaml.nodes.Node;

public interface Construct {
   Object construct(Node var1);

   void construct2ndStep(Node var1, Object var2);
}
