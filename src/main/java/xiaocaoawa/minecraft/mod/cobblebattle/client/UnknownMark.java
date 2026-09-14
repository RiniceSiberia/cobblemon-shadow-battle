package xiaocaoawa.minecraft.mod.cobblebattle.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class UnknownMark {
   private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/pokedex/platform_unknown.png");
   private static final int TEXTURE_W = 390;
   private static final int TEXTURE_H = 444;

   private UnknownMark() {
   }

   public static void draw(GuiGraphics graphics, int centreX, int floorY, int height) {
      int width = Math.max(1, height * 390 / 444);
      graphics.blit(TEXTURE, centreX - width / 2, floorY - height, width, height, 0.0F, 0.0F, 390, 444, 390, 444);
   }
}
