package xiaocaoawa.minecraft.mod.cobblebattle.shaded.snakeyaml.parser;

import xiaocaoawa.minecraft.mod.cobblebattle.shaded.snakeyaml.events.Event;

public interface Parser {
   boolean checkEvent(Event.ID var1);

   Event peekEvent();

   Event getEvent();
}
