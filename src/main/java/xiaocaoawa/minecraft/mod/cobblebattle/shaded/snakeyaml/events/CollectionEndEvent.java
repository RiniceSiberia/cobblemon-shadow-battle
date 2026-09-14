package xiaocaoawa.minecraft.mod.cobblebattle.shaded.snakeyaml.events;

import xiaocaoawa.minecraft.mod.cobblebattle.shaded.snakeyaml.error.Mark;

public abstract class CollectionEndEvent extends Event {
   public CollectionEndEvent(Mark startMark, Mark endMark) {
      super(startMark, endMark);
   }
}
