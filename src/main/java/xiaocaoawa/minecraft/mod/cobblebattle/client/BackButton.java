package xiaocaoawa.minecraft.mod.cobblebattle.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class BackButton {
   public static final int X = 280;
   public static final int Y = 184;
   public static final int W = 42;
   public static final int H = 10;
   private static final int NORMAL_FILL_COLOR = 1728053247;
   private static final int HOVER_FILL_COLOR = -1711276033;
   private static final int BORDER_COLOR = -1426063361;
   private static final int LABEL_COLOR = -1;

   private BackButton() {
   }

   public static void draw(GuiGraphics canvas, Font textRenderer, int screenOriginX, int screenOriginY, int pointerX, int pointerY) {
      int buttonLeft = screenOriginX + X;
      int buttonTop = screenOriginY + Y;
      boolean isHovered = contains(screenOriginX, screenOriginY, pointerX, pointerY);
      canvas.fill(buttonLeft, buttonTop, buttonLeft + W, buttonTop + H, isHovered ? HOVER_FILL_COLOR : NORMAL_FILL_COLOR);
      canvas.fill(buttonLeft, buttonTop, buttonLeft + W, buttonTop + 1, BORDER_COLOR);
      canvas.fill(buttonLeft, buttonTop + H - 1, buttonLeft + W, buttonTop + H, BORDER_COLOR);
      canvas.fill(buttonLeft, buttonTop, buttonLeft + 1, buttonTop + H, BORDER_COLOR);
      canvas.fill(buttonLeft + W - 1, buttonTop, buttonLeft + W, buttonTop + H, BORDER_COLOR);
      Ui.drawCentered(canvas, textRenderer, Component.translatable("cobblebattle.back"), buttonLeft + W / 2, buttonTop + 1, LABEL_COLOR);
   }

   public static boolean contains(int screenOriginX, int screenOriginY, double pointerX, double pointerY) {
      int buttonLeft = screenOriginX + X;
      int buttonTop = screenOriginY + Y;
      return pointerX >= buttonLeft && pointerX < buttonLeft + W && pointerY >= buttonTop && pointerY < buttonTop + H;
   }
}
