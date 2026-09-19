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
   private static final ResourceLocation FRAME_TEXTURE = ResourceLocation.fromNamespaceAndPath("cobblebattle", "textures/gui/leaderboard/base.png");
   private static final ResourceLocation PANEL_TEXTURE = ResourceLocation.fromNamespaceAndPath("cobblebattle", "textures/gui/leaderboard/screen_1.png");
   private static final int PANEL_WIDTH = 345;
   private static final int PANEL_HEIGHT = 207;
   private static final int CONTENT_CLIP_OFFSET_X = 18;
   private static final int CONTENT_CLIP_OFFSET_Y = 13;
   private static final int CONTENT_CLIP_WIDTH = 309;
   private static final int CONTENT_CLIP_HEIGHT = 183;
   private static final int BACK_TAB_OFFSET_X = 24;
   private static final int BACK_TAB_OFFSET_Y = 15;
   private static final int BACK_TAB_WIDTH = 60;
   private static final int OPTION_ROW_OFFSET_X = 30;
   private static final int FIRST_OPTION_ROW_OFFSET_Y = 40;
   private static final int OPTION_ROW_WIDTH = 285;
   private static final int OPTION_ROW_HEIGHT = 22;
   private static final int OPTION_ROW_GAP = 6;
   private static final int TOGGLE_WIDTH = 22;
   private static final int OPTION_BACKGROUND_COLOR = 1728053247;
   private static final int OPTION_HOVER_COLOR = -1711276033;
   private static final int OPTION_BORDER_COLOR = -1426063361;
   private static final int PRIMARY_TEXT_COLOR = -1;
   private static final int SECONDARY_TEXT_COLOR = -1770753;
   private static final int ENABLED_TOGGLE_COLOR = -12599441;
   private static final int DISABLED_TOGGLE_COLOR = -2534577;
   private final Screen previousScreen;
   private final List<SettingsScreen.PreferenceToggle> preferenceRows = List.of(
      new SettingsScreen.PreferenceToggle(
         Component.translatable("cobblebattle.settings.chat_hud"),
         Component.translatable("cobblebattle.settings.chat_hud_sub"),
         ClientSettings::chatHud,
         ClientSettings::setChatHud
      )
   );
   private int panelLeft;
   private int panelTop;

   public SettingsScreen(Screen previousScreen) {
      super(Component.translatable("cobblebattle.settings.title"));
      this.previousScreen = previousScreen;
   }

   protected void init() {
      this.panelLeft = (this.width - 345) / 2;
      this.panelTop = (this.height - 207) / 2;
   }

   public boolean isPauseScreen() {
      return false;
   }

   public void render(GuiGraphics canvas, int pointerX, int pointerY, float frameDelta) {
      super.render(canvas, pointerX, pointerY, frameDelta);
      canvas.blit(PANEL_TEXTURE, this.panelLeft, this.panelTop, 0.0F, 0.0F, 345, 207, 345, 207);
      Backdrop.draw(canvas, this.panelLeft, this.panelTop);
      int clipLeft = this.panelLeft + 18;
      int clipTop = this.panelTop + 13;
      canvas.enableScissor(clipLeft, clipTop, clipLeft + 309, clipTop + 183);

      try {
         BackButton.draw(canvas, this.font, this.panelLeft, this.panelTop, pointerX, pointerY);
         Component heading = this.getTitle().copy().withStyle(textStyle -> textStyle.withFont(CobblemonResources.INSTANCE.getDEFAULT_LARGE()).withBold(true));
         Ui.draw(canvas, this.font, heading, this.panelLeft + 322 - Ui.width(this.font, heading), this.panelTop + 14, -1, true);

         for (int rowIndex = 0; rowIndex < this.preferenceRows.size(); rowIndex++) {
            this.renderPreferenceRow(canvas, this.preferenceRows.get(rowIndex), this.preferenceRowTop(rowIndex), this.isPointerOverPreference(pointerX, pointerY, rowIndex));
         }
      } finally {
         canvas.disableScissor();
      }

      canvas.blit(FRAME_TEXTURE, this.panelLeft, this.panelTop, 0.0F, 0.0F, 345, 207, 345, 207);
   }

   private void renderPreferenceRow(GuiGraphics canvas, SettingsScreen.PreferenceToggle preference, int rowTop, boolean hovered) {
      int rowLeft = this.panelLeft + 30;
      canvas.fill(rowLeft, rowTop, rowLeft + 285, rowTop + 22, hovered ? -1711276033 : 1728053247);
      canvas.fill(rowLeft, rowTop, rowLeft + 285, rowTop + 1, -1426063361);
      canvas.fill(rowLeft, rowTop + 22 - 1, rowLeft + 285, rowTop + 22, -1426063361);
      canvas.fill(rowLeft, rowTop, rowLeft + 1, rowTop + 22, -1426063361);
      canvas.fill(rowLeft + 285 - 1, rowTop, rowLeft + 285, rowTop + 22, -1426063361);
      Ui.draw(canvas, this.font, preference.title(), rowLeft + 8, rowTop + 3, -1, true);
      Ui.draw(canvas, this.font, preference.subtitle(), rowLeft + 8, rowTop + 13, -1770753, false);
      boolean enabled = preference.get().getAsBoolean();
      int toggleLeft = rowLeft + 285 - 8 - 22;
      int toggleTop = rowTop + 4;
      canvas.fill(toggleLeft, toggleTop, toggleLeft + 22, toggleTop + 22 - 8, enabled ? -12599441 : -2534577);
      canvas.fill(toggleLeft, toggleTop, toggleLeft + 22, toggleTop + 1, -1426063361);
      canvas.fill(toggleLeft, toggleTop + 22 - 9, toggleLeft + 22, toggleTop + 22 - 8, -1426063361);
      canvas.fill(toggleLeft, toggleTop, toggleLeft + 1, toggleTop + 22 - 8, -1426063361);
      canvas.fill(toggleLeft + 22 - 1, toggleTop, toggleLeft + 22, toggleTop + 22 - 8, -1426063361);
      Ui.drawCenteredPlain(canvas, this.font, enabled ? "✔" : "✘", toggleLeft + 11, toggleTop + 3, -1);
   }

   private int preferenceRowTop(int rowIndex) {
      return this.panelTop + 40 + rowIndex * 28;
   }

   private boolean isPointerOverPreference(double pointerX, double pointerY, int rowIndex) {
      int rowLeft = this.panelLeft + 30;
      int rowTop = this.preferenceRowTop(rowIndex);
      return pointerX >= rowLeft && pointerX < rowLeft + 285 && pointerY >= rowTop && pointerY < rowTop + 22;
   }

   private boolean isPointerOverBackButton(double pointerX, double pointerY) {
      return BackButton.contains(this.panelLeft, this.panelTop, pointerX, pointerY);
   }

   public boolean mouseClicked(double pointerX, double pointerY, int mouseButton) {
      if (mouseButton == 0) {
         if (this.isPointerOverBackButton(pointerX, pointerY)) {
            this.onClose();
            return true;
         }

         for (int rowIndex = 0; rowIndex < this.preferenceRows.size(); rowIndex++) {
            if (this.isPointerOverPreference(pointerX, pointerY, rowIndex)) {
               SettingsScreen.PreferenceToggle selectedPreference = this.preferenceRows.get(rowIndex);
               selectedPreference.set().accept(!selectedPreference.get().getAsBoolean());
               return true;
            }
         }
      }

      return super.mouseClicked(pointerX, pointerY, mouseButton);
   }

   public void onClose() {
      Minecraft.getInstance().setScreen(this.previousScreen);
   }

   private record PreferenceToggle(Component title, Component subtitle, BooleanSupplier get, Consumer<Boolean> set) {
   }
}
