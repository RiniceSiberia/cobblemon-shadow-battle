package xiaocaoawa.minecraft.mod.cobblebattle.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class UnknownMark {
   private static final ResourceLocation UNKNOWN_TEXTURE = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/pokedex/platform_unknown.png");
   private static final int UNKNOWN_TEXTURE_WIDTH = 390;
   private static final int UNKNOWN_TEXTURE_HEIGHT = 444;

   private UnknownMark() {
   }

   public static void draw(GuiGraphics graphics, int centerX, int baseY, int targetHeight) {
      int targetWidth = Math.max(1, targetHeight * UNKNOWN_TEXTURE_WIDTH / UNKNOWN_TEXTURE_HEIGHT);
      graphics.blit(UNKNOWN_TEXTURE, centerX - targetWidth / 2, baseY - targetHeight, targetWidth, targetHeight, 0.0F, 0.0F, UNKNOWN_TEXTURE_WIDTH, UNKNOWN_TEXTURE_HEIGHT, UNKNOWN_TEXTURE_WIDTH, UNKNOWN_TEXTURE_HEIGHT);
   }
}
