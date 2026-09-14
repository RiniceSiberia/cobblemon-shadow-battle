package xiaocaoawa.minecraft.mod.cobblebattle.client;

import com.cobblemon.mod.common.client.CobblemonResources;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class SettingsScreen extends Screen {
   private static final ResourceLocation BASE = ResourceLocation.fromNamespaceAndPath("cobblebattle", "textures/gui/leaderboard/base.png");
   private static final ResourceLocation SCREEN = ResourceLocation.fromNamespaceAndPath("cobblebattle", "textures/gui/leaderboard/screen_1.png");
   private static final int WIDTH = 345;
   private static final int HEIGHT = 207;
   private static final int HOLE_X = 18;
   private static final int HOLE_Y = 13;
   private static final int HOLE_W = 309;
   private static final int HOLE_H = 183;
   private static final int TAB_X = 24;
   private static final int TAB_Y = 15;
   private static final int TAB_W = 60;
   private static final int ROW_X = 30;
   private static final int ROW_Y = 40;
   private static final int ROW_W = 285;
   private static final int ROW_H = 22;
   private static final int ROW_GAP = 6;
   private static final int TOGGLE_W = 22;
   private static final int ROW_FILL = 1728053247;
   private static final int ROW_FILL_HOVER = -1711276033;
   private static final int ROW_EDGE = -1426063361;
   private static final int TEXT = -1;
   private static final int TEXT_SOFT = -1770753;
   private static final int ON_FILL = -12599441;
   private static final int OFF_FILL = -2534577;
   private final Screen parent;
   private final List<SettingsScreen.Row> rows = List.of(
      new SettingsScreen.Row(
         Component.translatable("cobblebattle.settings.chat_hud"),
         Component.translatable("cobblebattle.settings.chat_hud_sub"),
         ClientSettings::chatHud,
         ClientSettings::setChatHud
      )
   );
   private int originX;
   private int originY;

   public SettingsScreen(Screen parent) {
      super(Component.translatable("cobblebattle.settings.title"));
      this.parent = parent;
   }

   protected void init() {
      this.originX = (this.width - 345) / 2;
      this.originY = (this.height - 207) / 2;
   }

   public boolean isPauseScreen() {
      return false;
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      super.render(graphics, mouseX, mouseY, partialTick);
      graphics.blit(SCREEN, this.originX, this.originY, 0.0F, 0.0F, 345, 207, 345, 207);
      Backdrop.draw(graphics, this.originX, this.originY);
      int left = this.originX + 18;
      int top = this.originY + 13;
      graphics.enableScissor(left, top, left + 309, top + 183);

      try {
         BackButton.draw(graphics, this.font, this.originX, this.originY, mouseX, mouseY);
         Component page = this.getTitle().copy().withStyle(style -> style.withFont(CobblemonResources.INSTANCE.getDEFAULT_LARGE()).withBold(true));
         Ui.draw(graphics, this.font, page, this.originX + 322 - Ui.width(this.font, page), this.originY + 14, -1, true);

         for (int i = 0; i < this.rows.size(); i++) {
            this.drawRow(graphics, this.rows.get(i), this.rowY(i), this.inRow(mouseX, mouseY, i));
         }
      } finally {
         graphics.disableScissor();
      }

      graphics.blit(BASE, this.originX, this.originY, 0.0F, 0.0F, 345, 207, 345, 207);
   }

   private void drawRow(GuiGraphics graphics, SettingsScreen.Row row, int y, boolean hover) {
      int x = this.originX + 30;
      graphics.fill(x, y, x + 285, y + 22, hover ? -1711276033 : 1728053247);
      graphics.fill(x, y, x + 285, y + 1, -1426063361);
      graphics.fill(x, y + 22 - 1, x + 285, y + 22, -1426063361);
      graphics.fill(x, y, x + 1, y + 22, -1426063361);
      graphics.fill(x + 285 - 1, y, x + 285, y + 22, -1426063361);
      Ui.draw(graphics, this.font, row.title(), x + 8, y + 3, -1, true);
      Ui.draw(graphics, this.font, row.subtitle(), x + 8, y + 13, -1770753, false);
      boolean on = row.get().getAsBoolean();
      int tx = x + 285 - 8 - 22;
      int ty = y + 4;
      graphics.fill(tx, ty, tx + 22, ty + 22 - 8, on ? -12599441 : -2534577);
      graphics.fill(tx, ty, tx + 22, ty + 1, -1426063361);
      graphics.fill(tx, ty + 22 - 9, tx + 22, ty + 22 - 8, -1426063361);
      graphics.fill(tx, ty, tx + 1, ty + 22 - 8, -1426063361);
      graphics.fill(tx + 22 - 1, ty, tx + 22, ty + 22 - 8, -1426063361);
      Ui.drawCenteredPlain(graphics, this.font, on ? "✔" : "✘", tx + 11, ty + 3, -1);
   }

   private int rowY(int index) {
      return this.originY + 40 + index * 28;
   }

   private boolean inRow(double mouseX, double mouseY, int index) {
      int x = this.originX + 30;
      int y = this.rowY(index);
      return mouseX >= x && mouseX < x + 285 && mouseY >= y && mouseY < y + 22;
   }

   private boolean inBack(double mouseX, double mouseY) {
      return BackButton.contains(this.originX, this.originY, mouseX, mouseY);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0) {
         if (this.inBack(mouseX, mouseY)) {
            this.onClose();
            return true;
         }

         for (int i = 0; i < this.rows.size(); i++) {
            if (this.inRow(mouseX, mouseY, i)) {
               SettingsScreen.Row row = this.rows.get(i);
               row.set().accept(!row.get().getAsBoolean());
               return true;
            }
         }
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   public void onClose() {
      Minecraft.getInstance().setScreen(this.parent);
   }

   private record Row(Component title, Component subtitle, BooleanSupplier get, Consumer<Boolean> set) {
   }
}
