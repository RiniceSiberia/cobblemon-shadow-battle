package xiaocaoawa.minecraft.mod.cobblebattle.shaded.snakeyaml.inspector;

import xiaocaoawa.minecraft.mod.cobblebattle.shaded.snakeyaml.nodes.Tag;

public final class UnTrustedTagInspector implements TagInspector {
   @Override
   public boolean isGlobalTagAllowed(Tag tag) {
      return false;
   }
}
