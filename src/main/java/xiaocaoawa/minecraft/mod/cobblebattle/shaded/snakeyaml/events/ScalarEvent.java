package xiaocaoawa.minecraft.mod.cobblebattle.shaded.snakeyaml.events;

import xiaocaoawa.minecraft.mod.cobblebattle.shaded.snakeyaml.DumperOptions;
import xiaocaoawa.minecraft.mod.cobblebattle.shaded.snakeyaml.error.Mark;

public final class ScalarEvent extends NodeEvent {
   private final String tag;
   private final DumperOptions.ScalarStyle style;
   private final String value;
   private final ImplicitTuple implicit;

   public ScalarEvent(String anchor, String tag, ImplicitTuple implicit, String value, Mark startMark, Mark endMark, DumperOptions.ScalarStyle style) {
      super(anchor, startMark, endMark);
      this.tag = tag;
      this.implicit = implicit;
      if (value == null) {
         throw new NullPointerException("Value must be provided.");
      } else {
         this.value = value;
         if (style == null) {
            throw new NullPointerException("Style must be provided.");
         } else {
            this.style = style;
         }
      }
   }

   public String getTag() {
      return this.tag;
   }

   public DumperOptions.ScalarStyle getScalarStyle() {
      return this.style;
   }

   public String getValue() {
      return this.value;
   }

   public ImplicitTuple getImplicit() {
      return this.implicit;
   }

   @Override
   protected String getArguments() {
      return super.getArguments() + ", tag=" + this.tag + ", style=" + this.style + "," + this.implicit + ", value=" + this.value;
   }

   @Override
   public Event.ID getEventId() {
      return Event.ID.Scalar;
   }

   public boolean isPlain() {
      return this.style == DumperOptions.ScalarStyle.PLAIN;
   }

   public boolean isLiteral() {
      return this.style == DumperOptions.ScalarStyle.LITERAL;
   }

   public boolean isSQuoted() {
      return this.style == DumperOptions.ScalarStyle.SINGLE_QUOTED;
   }

   public boolean isDQuoted() {
      return this.style == DumperOptions.ScalarStyle.DOUBLE_QUOTED;
   }

   public boolean isFolded() {
      return this.style == DumperOptions.ScalarStyle.FOLDED;
   }

   public boolean isJson() {
      return this.style == DumperOptions.ScalarStyle.JSON_SCALAR_STYLE;
   }
}
