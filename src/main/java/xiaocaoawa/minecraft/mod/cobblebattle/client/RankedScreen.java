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
   private static final ResourceLocation RANKED_FRAME_TEXTURE = ResourceLocation.fromNamespaceAndPath("cobblebattle", "textures/gui/leaderboard/base.png");
   private static final ResourceLocation RANKED_CONTENT_TEXTURE = ResourceLocation.fromNamespaceAndPath("cobblebattle", "textures/gui/leaderboard/screen_1.png");
   private static final int SCREEN_WIDTH = 345;
   private static final int SCREEN_HEIGHT = 207;
   private static final int CONTENT_LEFT_OFFSET = 18;
   private static final int CONTENT_TOP_OFFSET = 13;
   private static final int CONTENT_WIDTH = 309;
   private static final int CONTENT_HEIGHT = 183;
   private static final int TAB_LEFT_OFFSET = 24;
   private static final int TAB_TOP_OFFSET = 15;
   private static final int TAB_WIDTH = 60;
   private static final int LIST_LEFT_OFFSET = 22;
   private static final int LIST_TOP_OFFSET = 30;
   private static final int LIST_WIDTH = 100;
   private static final int LIST_ROW_HEIGHT = 14;
   private static final int VISIBLE_LIST_ROWS = 11;
   private static final int DETAIL_PANEL_LEFT_OFFSET = 128;
   private static final int DETAIL_PANEL_TOP_OFFSET = 30;
   private static final int DETAIL_PANEL_WIDTH = 195;
   private static final int DETAIL_PANEL_HEIGHT = 138;
   private static final int ACTION_BUTTON_TOP_OFFSET = 174;
   private static final int ACTION_BUTTON_HEIGHT = 16;
   private static final int ACTION_BUTTON_WIDTH = 90;
   private static final int BACKGROUND_FILL_COLOR = 1728053247;
   private static final int HOVER_FILL_COLOR = -1711276033;
   private static final int SELECTED_FILL_COLOR = -1426063361;
   private static final int BORDER_COLOR = -1426063361;
   private static final int PRIMARY_TEXT_COLOR = -1;
   private static final int SECONDARY_TEXT_COLOR = -1770753;
   private static final int EMPHASIS_TEXT_COLOR = -15451066;
   private final Screen previousScreen;
   private final List<OpenMainMenuPayload.RankedInfo> rankedCompetitions;
   private int selectedCompetitionIndex;
   private int visibleListStart;
   private int screenLeft;
   private int screenTop;

   public RankedScreen(Screen previousScreen, List<OpenMainMenuPayload.RankedInfo> rankedCompetitions) {
      super(Component.translatable("cobblebattle.ranked.title"));
      this.previousScreen = previousScreen;
      this.rankedCompetitions = rankedCompetitions;
   }

   protected void init() {
      this.screenLeft = (this.width - 345) / 2;
      this.screenTop = (this.height - 207) / 2;
   }

   public boolean isPauseScreen() {
      return false;
   }

   private OpenMainMenuPayload.RankedInfo selectedCompetition() {
      return this.rankedCompetitions.isEmpty() ? null : this.rankedCompetitions.get(Math.min(this.selectedCompetitionIndex, this.rankedCompetitions.size() - 1));
   }

   public void render(GuiGraphics canvas, int pointerX, int pointerY, float frameDelta) {
      super.render(canvas, pointerX, pointerY, frameDelta);
      canvas.blit(RANKED_CONTENT_TEXTURE, this.screenLeft, this.screenTop, 0.0F, 0.0F, 345, 207, 345, 207);
      Backdrop.draw(canvas, this.screenLeft, this.screenTop);
      int regionLeft = this.screenLeft + 18;
      int regionTop = this.screenTop + 13;
      canvas.enableScissor(regionLeft, regionTop, regionLeft + 309, regionTop + 183);

      try {
         BackButton.draw(canvas, this.font, this.screenLeft, this.screenTop, pointerX, pointerY);
         Component titleLabel = this.getTitle().copy().withStyle(textStyle -> textStyle.withFont(CobblemonResources.INSTANCE.getDEFAULT_LARGE()).withBold(true));
         Ui.draw(canvas, this.font, titleLabel, this.screenLeft + 322 - Ui.width(this.font, titleLabel), this.screenTop + 14, -1, true);
         this.renderCompetitionList(canvas, pointerX, pointerY);
         this.renderCompetitionDetails(canvas, pointerX, pointerY);
      } finally {
         canvas.disableScissor();
      }

      canvas.blit(RANKED_FRAME_TEXTURE, this.screenLeft, this.screenTop, 0.0F, 0.0F, 345, 207, 345, 207);
   }

   private void renderCompetitionList(GuiGraphics canvas, int pointerX, int pointerY) {
      int coordinateX = this.screenLeft + 22;
      if (this.rankedCompetitions.isEmpty()) {
         Ui.draw(canvas, this.font, Component.translatable("cobblebattle.room.no_rules"), coordinateX + 4, this.screenTop + 30 + 4, -1770753, false);
      } else {
         for (int visibleRowIndex = 0; visibleRowIndex < 11; visibleRowIndex++) {
            int competitionIndex = this.visibleListStart + visibleRowIndex;
            if (competitionIndex >= this.rankedCompetitions.size()) {
               break;
            }

            int coordinateY = this.screenTop + 30 + visibleRowIndex * 14;
            boolean isSelected = competitionIndex == this.selectedCompetitionIndex;
            boolean isHovered = pointerX >= coordinateX && pointerX < coordinateX + 100 && pointerY >= coordinateY && pointerY < coordinateY + 14;
            canvas.fill(coordinateX, coordinateY, coordinateX + 100, coordinateY + 14 - 1, isSelected ? -1426063361 : (isHovered ? -1711276033 : 1728053247));
            OpenMainMenuPayload.RankedInfo battleDetails = this.rankedCompetitions.get(competitionIndex);
            String displayedName = battleDetails.name().isEmpty() ? battleDetails.id() : battleDetails.name();
            canvas.enableScissor(coordinateX + 3, coordinateY, coordinateX + 100 - 3, coordinateY + 14);
            Ui.draw(canvas, this.font, displayedName, coordinateX + 4, coordinateY + 3, isSelected ? -15451066 : -1, !isSelected);
            canvas.disableScissor();
         }
      }
   }

   private void renderCompetitionDetails(GuiGraphics canvas, int pointerX, int pointerY) {
      int detailPanelLeft = this.screenLeft + 128;
      int detailPanelTop = this.screenTop + 30;
      canvas.fill(detailPanelLeft, detailPanelTop, detailPanelLeft + 195, detailPanelTop + 138, 1728053247);
      canvas.fill(detailPanelLeft, detailPanelTop, detailPanelLeft + 195, detailPanelTop + 1, -1426063361);
      canvas.fill(detailPanelLeft, detailPanelTop + 138 - 1, detailPanelLeft + 195, detailPanelTop + 138, -1426063361);
      canvas.fill(detailPanelLeft, detailPanelTop, detailPanelLeft + 1, detailPanelTop + 138, -1426063361);
      canvas.fill(detailPanelLeft + 195 - 1, detailPanelTop, detailPanelLeft + 195, detailPanelTop + 138, -1426063361);
      OpenMainMenuPayload.RankedInfo battleDetails = this.selectedCompetition();
      if (battleDetails != null) {
         int coordinateX = detailPanelLeft + 8;
         int coordinateY = detailPanelTop + 6;
         Component competitionName = Component.literal(battleDetails.name().isEmpty() ? battleDetails.id() : battleDetails.name())
            .withStyle(textStyle -> textStyle.withFont(CobblemonResources.INSTANCE.getDEFAULT_LARGE()).withBold(true));
         Ui.draw(canvas, this.font, competitionName, coordinateX, coordinateY, -1, true);
         coordinateY += 14;
         Ui.draw(canvas, this.font, Component.translatable("cobblebattle.ranked.id", new Object[]{battleDetails.id()}), coordinateX, coordinateY, -1770753, false);
         coordinateY += 12;
         Ui.draw(canvas, this.font, Component.translatable("cobblebattle.ranked.type", new Object[]{battleDetails.battleType(), battleDetails.slots()}), coordinateX, coordinateY, -1770753, false);
         coordinateY += 10;
         Component levelLabel = battleDetails.adjustLevel() > 0
            ? Component.translatable("cobblebattle.ranked.level", new Object[]{battleDetails.adjustLevel()})
            : Component.translatable("cobblebattle.ranked.level_free");
         Ui.draw(canvas, this.font, levelLabel, coordinateX, coordinateY, -1770753, false);
         coordinateY += 10;
         Ui.draw(
            canvas,
            this.font,
            Component.translatable(battleDetails.fullHeal() ? "cobblebattle.ranked.full_heal" : "cobblebattle.ranked.no_heal"),
            coordinateX,
            coordinateY,
            -1770753,
            false
         );
         coordinateY += 10;
         Ui.draw(
            canvas, this.font, Component.translatable("cobblebattle.ranked.score", new Object[]{battleDetails.winScore(), battleDetails.failScore()}), coordinateX, coordinateY, -1770753, false
         );
         coordinateY += 12;
         Ui.draw(canvas, this.font, Component.translatable("cobblebattle.ranked.rules"), coordinateX, coordinateY, -1, true);
         coordinateY += 10;
         String rulesText = battleDetails.rules().isEmpty() ? Component.translatable("cobblebattle.ranked.no_rules").getString() : String.join(", ", battleDetails.rules());
         canvas.enableScissor(detailPanelLeft, coordinateY, detailPanelLeft + 195, detailPanelTop + 138 - 2);

         for (String wrappedRule : ChatPanel.wrap(this.font, rulesText, 179)) {
            Ui.draw(canvas, this.font, wrappedRule, coordinateX, coordinateY, -1770753, false);
            coordinateY += 10;
         }

         canvas.disableScissor();
         this.renderActionButton(canvas, pointerX, pointerY, detailPanelLeft, this.screenTop + 174, Component.translatable("cobblebattle.ranked.queue"));
         this.renderActionButton(canvas, pointerX, pointerY, detailPanelLeft + 195 - 90, this.screenTop + 174, Component.translatable("cobblebattle.ranked.leaderboard"));
      }
   }

   private void renderActionButton(GuiGraphics canvas, int pointerX, int pointerY, int coordinateX, int coordinateY, Component buttonLabel) {
      boolean isHovered = pointerX >= coordinateX && pointerX < coordinateX + 90 && pointerY >= coordinateY && pointerY < coordinateY + 16;
      canvas.fill(coordinateX, coordinateY, coordinateX + 90, coordinateY + 16, isHovered ? -1711276033 : 1728053247);
      canvas.fill(coordinateX, coordinateY, coordinateX + 90, coordinateY + 1, -1426063361);
      canvas.fill(coordinateX, coordinateY + 16 - 1, coordinateX + 90, coordinateY + 16, -1426063361);
      canvas.fill(coordinateX, coordinateY, coordinateX + 1, coordinateY + 16, -1426063361);
      canvas.fill(coordinateX + 90 - 1, coordinateY, coordinateX + 90, coordinateY + 16, -1426063361);
      Ui.drawCentered(canvas, this.font, buttonLabel, coordinateX + 45, coordinateY + 4, -1);
   }

   private boolean isBackButtonHit(double pointerX, double pointerY) {
      return BackButton.contains(this.screenLeft, this.screenTop, pointerX, pointerY);
   }

   public boolean mouseClicked(double pointerX, double pointerY, int clickButton) {
      if (clickButton == 0) {
         if (this.isBackButtonHit(pointerX, pointerY)) {
            this.onClose();
            return true;
         }

         int listRegionLeft = this.screenLeft + 22;

         for (int visibleRowIndex = 0; visibleRowIndex < 11; visibleRowIndex++) {
            int competitionIndex = this.visibleListStart + visibleRowIndex;
            if (competitionIndex >= this.rankedCompetitions.size()) {
               break;
            }

            int coordinateY = this.screenTop + 30 + visibleRowIndex * 14;
            if (pointerX >= listRegionLeft && pointerX < listRegionLeft + 100 && pointerY >= coordinateY && pointerY < coordinateY + 14) {
               this.selectedCompetitionIndex = competitionIndex;
               return true;
            }
         }

         OpenMainMenuPayload.RankedInfo battleDetails = this.selectedCompetition();
         if (battleDetails != null) {
            int detailPanelLeft = this.screenLeft + 128;
            int actionButtonTop = this.screenTop + 174;
            if (pointerX >= detailPanelLeft && pointerX < detailPanelLeft + 90 && pointerY >= actionButtonTop && pointerY < actionButtonTop + 16) {
               NetworkManager.sendToServer(new MenuActionPayload("queue", battleDetails.id()));
               Minecraft.getInstance().setScreen(null);
               return true;
            }

            int leaderboardButtonLeft = detailPanelLeft + 195 - 90;
            if (pointerX >= leaderboardButtonLeft && pointerX < leaderboardButtonLeft + 90 && pointerY >= actionButtonTop && pointerY < actionButtonTop + 16) {
               ServerDex.rememberRanked(battleDetails.id());
               ServerDex.requestLeaderboard();
               return true;
            }
         }
      }

      return super.mouseClicked(pointerX, pointerY, clickButton);
   }

   public boolean mouseScrolled(double pointerX, double pointerY, double horizontalScrollAmount, double verticalScrollAmount) {
      int maximumOffset = Math.max(0, this.rankedCompetitions.size() - 11);
      this.visibleListStart = Math.max(0, Math.min(maximumOffset, this.visibleListStart - (int)Math.signum(verticalScrollAmount)));
      return true;
   }

   public void onClose() {
      Minecraft.getInstance().setScreen(this.previousScreen);
   }
}
