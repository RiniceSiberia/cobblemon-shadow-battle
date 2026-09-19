package xiaocaoawa.minecraft.mod.cobblebattle.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class Backdrop {
   private static final ResourceLocation POKE_BALL_TEXTURE = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/pokedex/pokedex_screen_poke_ball.png");
   private static final int ANIMATION_FRAME_COUNT = 16;
   private static final int FRAME_WIDTH = 109;
   private static final int FRAME_HEIGHT = 109;
   private static final int SPRITE_SHEET_HEIGHT = 1744;
   private static final long FRAME_DURATION_MILLIS = 150L;
   private static final int VISIBLE_FRAME_WIDTH = 82;
   private static final int VISIBLE_FRAME_HEIGHT = 67;
   private static final int CLIP_OFFSET_X = 18;
   private static final int CLIP_OFFSET_Y = 13;
   private static final int CLIP_WIDTH = 309;
   private static final int CLIP_HEIGHT = 183;
   private static final int SPRITE_ORIGIN_X = 243;
   private static final int SPRITE_ORIGIN_Y = 128;

   private Backdrop() {
   }

   public static void draw(GuiGraphics canvas, int backdropX, int backdropY) {
      int animationFrameIndex = (int)(System.currentTimeMillis() / 150L % 16L);
      int clipLeft = backdropX + 18;
      int clipTop = backdropY + 13;
      canvas.enableScissor(clipLeft, clipTop, clipLeft + 309, clipTop + 183);
      canvas.blit(POKE_BALL_TEXTURE, backdropX + 243, backdropY + 128, 109, 109, 0.0F, animationFrameIndex * 109, 109, 109, 109, 1744);
      canvas.disableScissor();
   }
}
