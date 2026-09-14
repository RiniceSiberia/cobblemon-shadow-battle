package xiaocaoawa.minecraft.mod.cobblebattle.client;

import com.cobblemon.mod.common.client.CobblemonResources;
import dev.architectury.networking.NetworkManager;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import xiaocaoawa.minecraft.mod.cobblebattle.network.MenuActionPayload;
import xiaocaoawa.minecraft.mod.cobblebattle.network.OpenMainMenuPayload;

public final class RankedScreen extends Screen {
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
   private static final int LIST_X = 22;
   private static final int LIST_Y = 30;
   private static final int LIST_W = 100;
   private static final int LIST_ROW_H = 14;
   private static final int LIST_ROWS = 11;
   private static final int PANEL_X = 128;
   private static final int PANEL_Y = 30;
   private static final int PANEL_W = 195;
   private static final int PANEL_H = 138;
   private static final int BUTTON_Y = 174;
   private static final int BUTTON_H = 16;
   private static final int BUTTON_W = 90;
   private static final int FILL = 1728053247;
   private static final int FILL_HOVER = -1711276033;
   private static final int FILL_ON = -1426063361;
   private static final int EDGE = -1426063361;
   private static final int TEXT = -1;
   private static final int TEXT_SOFT = -1770753;
   private static final int INK = -15451066;
   private final Screen parent;
   private final List<OpenMainMenuPayload.RankedInfo> competitions;
   private int chosen;
   private int firstRow;
   private int originX;
   private int originY;

   public RankedScreen(Screen parent, List<OpenMainMenuPayload.RankedInfo> competitions) {
      super(Component.translatable("cobblebattle.ranked.title"));
      this.parent = parent;
      this.competitions = competitions;
   }

   protected void init() {
      this.originX = (this.width - 345) / 2;
      this.originY = (this.height - 207) / 2;
   }

   public boolean isPauseScreen() {
      return false;
   }

   private OpenMainMenuPayload.RankedInfo current() {
      return this.competitions.isEmpty() ? null : this.competitions.get(Math.min(this.chosen, this.competitions.size() - 1));
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
         this.drawList(graphics, mouseX, mouseY);
         this.drawDetails(graphics, mouseX, mouseY);
      } finally {
         graphics.disableScissor();
      }

      graphics.blit(BASE, this.originX, this.originY, 0.0F, 0.0F, 345, 207, 345, 207);
   }

   private void drawList(GuiGraphics graphics, int mouseX, int mouseY) {
      int x = this.originX + 22;
      if (this.competitions.isEmpty()) {
         Ui.draw(graphics, this.font, Component.translatable("cobblebattle.room.no_rules"), x + 4, this.originY + 30 + 4, -1770753, false);
      } else {
         for (int i = 0; i < 11; i++) {
            int index = this.firstRow + i;
            if (index >= this.competitions.size()) {
               break;
            }

            int y = this.originY + 30 + i * 14;
            boolean on = index == this.chosen;
            boolean hover = mouseX >= x && mouseX < x + 100 && mouseY >= y && mouseY < y + 14;
            graphics.fill(x, y, x + 100, y + 14 - 1, on ? -1426063361 : (hover ? -1711276033 : 1728053247));
            OpenMainMenuPayload.RankedInfo info = this.competitions.get(index);
            String shown = info.name().isEmpty() ? info.id() : info.name();
            graphics.enableScissor(x + 3, y, x + 100 - 3, y + 14);
            Ui.draw(graphics, this.font, shown, x + 4, y + 3, on ? -15451066 : -1, !on);
            graphics.disableScissor();
         }
      }
   }

   private void drawDetails(GuiGraphics graphics, int mouseX, int mouseY) {
      int px = this.originX + 128;
      int py = this.originY + 30;
      graphics.fill(px, py, px + 195, py + 138, 1728053247);
      graphics.fill(px, py, px + 195, py + 1, -1426063361);
      graphics.fill(px, py + 138 - 1, px + 195, py + 138, -1426063361);
      graphics.fill(px, py, px + 1, py + 138, -1426063361);
      graphics.fill(px + 195 - 1, py, px + 195, py + 138, -1426063361);
      OpenMainMenuPayload.RankedInfo info = this.current();
      if (info != null) {
         int x = px + 8;
         int y = py + 6;
         Component name = Component.literal(info.name().isEmpty() ? info.id() : info.name())
            .withStyle(style -> style.withFont(CobblemonResources.INSTANCE.getDEFAULT_LARGE()).withBold(true));
         Ui.draw(graphics, this.font, name, x, y, -1, true);
         y += 14;
         Ui.draw(graphics, this.font, Component.translatable("cobblebattle.ranked.id", new Object[]{info.id()}), x, y, -1770753, false);
         y += 12;
         Ui.draw(graphics, this.font, Component.translatable("cobblebattle.ranked.type", new Object[]{info.battleType(), info.slots()}), x, y, -1770753, false);
         y += 10;
         Component level = info.adjustLevel() > 0
            ? Component.translatable("cobblebattle.ranked.level", new Object[]{info.adjustLevel()})
            : Component.translatable("cobblebattle.ranked.level_free");
         Ui.draw(graphics, this.font, level, x, y, -1770753, false);
         y += 10;
         Ui.draw(
            graphics,
            this.font,
            Component.translatable(info.fullHeal() ? "cobblebattle.ranked.full_heal" : "cobblebattle.ranked.no_heal"),
            x,
            y,
            -1770753,
            false
         );
         y += 10;
         Ui.draw(
            graphics, this.font, Component.translatable("cobblebattle.ranked.score", new Object[]{info.winScore(), info.failScore()}), x, y, -1770753, false
         );
         y += 12;
         Ui.draw(graphics, this.font, Component.translatable("cobblebattle.ranked.rules"), x, y, -1, true);
         y += 10;
         String rules = info.rules().isEmpty() ? Component.translatable("cobblebattle.ranked.no_rules").getString() : String.join(", ", info.rules());
         graphics.enableScissor(px, y, px + 195, py + 138 - 2);

         for (String row : ChatPanel.wrap(this.font, rules, 179)) {
            Ui.draw(graphics, this.font, row, x, y, -1770753, false);
            y += 10;
         }

         graphics.disableScissor();
         this.drawButton(graphics, mouseX, mouseY, px, this.originY + 174, Component.translatable("cobblebattle.ranked.queue"));
         this.drawButton(graphics, mouseX, mouseY, px + 195 - 90, this.originY + 174, Component.translatable("cobblebattle.ranked.leaderboard"));
      }
   }

   private void drawButton(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, Component label) {
      boolean hover = mouseX >= x && mouseX < x + 90 && mouseY >= y && mouseY < y + 16;
      graphics.fill(x, y, x + 90, y + 16, hover ? -1711276033 : 1728053247);
      graphics.fill(x, y, x + 90, y + 1, -1426063361);
      graphics.fill(x, y + 16 - 1, x + 90, y + 16, -1426063361);
      graphics.fill(x, y, x + 1, y + 16, -1426063361);
      graphics.fill(x + 90 - 1, y, x + 90, y + 16, -1426063361);
      Ui.drawCentered(graphics, this.font, label, x + 45, y + 4, -1);
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

         int lx = this.originX + 22;

         for (int i = 0; i < 11; i++) {
            int index = this.firstRow + i;
            if (index >= this.competitions.size()) {
               break;
            }

            int y = this.originY + 30 + i * 14;
            if (mouseX >= lx && mouseX < lx + 100 && mouseY >= y && mouseY < y + 14) {
               this.chosen = index;
               return true;
            }
         }

         OpenMainMenuPayload.RankedInfo info = this.current();
         if (info != null) {
            int px = this.originX + 128;
            int by = this.originY + 174;
            if (mouseX >= px && mouseX < px + 90 && mouseY >= by && mouseY < by + 16) {
               NetworkManager.sendToServer(new MenuActionPayload("queue", info.id()));
               Minecraft.getInstance().setScreen(null);
               return true;
            }

            int bx = px + 195 - 90;
            if (mouseX >= bx && mouseX < bx + 90 && mouseY >= by && mouseY < by + 16) {
               ServerDex.rememberRanked(info.id());
               ServerDex.requestLeaderboard();
               return true;
            }
         }
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double amountX, double amountY) {
      int max = Math.max(0, this.competitions.size() - 11);
      this.firstRow = Math.max(0, Math.min(max, this.firstRow - (int)Math.signum(amountY)));
      return true;
   }

   public void onClose() {
      Minecraft.getInstance().setScreen(this.parent);
   }
}
