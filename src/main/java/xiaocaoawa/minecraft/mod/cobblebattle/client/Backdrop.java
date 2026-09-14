package xiaocaoawa.minecraft.mod.cobblebattle.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class Backdrop {
   private static final ResourceLocation POKE_BALL = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/pokedex/pokedex_screen_poke_ball.png");
   private static final int FRAMES = 16;
   private static final int FRAME_W = 109;
   private static final int FRAME_H = 109;
   private static final int SHEET_H = 1744;
   private static final long MS_PER_FRAME = 150L;
   private static final int SHOWN_W = 82;
   private static final int SHOWN_H = 67;
   private static final int HOLE_X = 18;
   private static final int HOLE_Y = 13;
   private static final int HOLE_W = 309;
   private static final int HOLE_H = 183;
   private static final int X = 243;
   private static final int Y = 128;

   private Backdrop() {
   }

   public static void draw(GuiGraphics graphics, int originX, int originY) {
      int frame = (int)(System.currentTimeMillis() / 150L % 16L);
      int left = originX + 18;
      int top = originY + 13;
      graphics.enableScissor(left, top, left + 309, top + 183);
      graphics.blit(POKE_BALL, originX + 243, originY + 128, 109, 109, 0.0F, frame * 109, 109, 109, 109, 1744);
      graphics.disableScissor();
   }
}
