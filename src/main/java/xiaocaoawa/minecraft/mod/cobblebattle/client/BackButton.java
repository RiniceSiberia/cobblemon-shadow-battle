package xiaocaoawa.minecraft.mod.cobblebattle.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class BackButton {
   public static final int X = 280;
   public static final int Y = 184;
   public static final int W = 42;
   public static final int H = 10;
   private static final int FILL = 1728053247;
   private static final int FILL_HOVER = -1711276033;
   private static final int EDGE = -1426063361;
   private static final int TEXT = -1;

   private BackButton() {
   }

   public static void draw(GuiGraphics graphics, Font font, int originX, int originY, int mouseX, int mouseY) {
      int x = originX + 280;
      int y = originY + 184;
      boolean hover = contains(originX, originY, mouseX, mouseY);
      graphics.fill(x, y, x + 42, y + 10, hover ? -1711276033 : 1728053247);
      graphics.fill(x, y, x + 42, y + 1, -1426063361);
      graphics.fill(x, y + 10 - 1, x + 42, y + 10, -1426063361);
      graphics.fill(x, y, x + 1, y + 10, -1426063361);
      graphics.fill(x + 42 - 1, y, x + 42, y + 10, -1426063361);
      Ui.drawCentered(graphics, font, Component.translatable("cobblebattle.back"), x + 21, y + 1, -1);
   }

   public static boolean contains(int originX, int originY, double mouseX, double mouseY) {
      int x = originX + 280;
      int y = originY + 184;
      return mouseX >= x && mouseX < x + 42 && mouseY >= y && mouseY < y + 10;
   }
}
