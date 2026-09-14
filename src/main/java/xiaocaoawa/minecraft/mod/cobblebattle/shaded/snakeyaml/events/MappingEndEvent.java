package xiaocaoawa.minecraft.mod.cobblebattle.shaded.snakeyaml.events;

import xiaocaoawa.minecraft.mod.cobblebattle.shaded.snakeyaml.error.Mark;

public final class MappingEndEvent extends CollectionEndEvent {
   public MappingEndEvent(Mark startMark, Mark endMark) {
      super(startMark, endMark);
   }

   @Override
   public Event.ID getEventId() {
      return Event.ID.MappingEnd;
   }
}
