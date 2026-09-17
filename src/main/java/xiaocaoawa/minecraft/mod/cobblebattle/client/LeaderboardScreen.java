package xiaocaoawa.minecraft.mod.cobblebattle.client;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.client.CobblemonResources;
import com.cobblemon.mod.common.client.render.models.blockbench.FloatingState;
import com.cobblemon.mod.common.entity.PoseType;
import com.cobblemon.mod.common.pokemon.RenderablePokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.cobblemon.mod.common.util.math.QuaternionUtilsKt;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import xiaocaoawa.minecraft.mod.cobblebattle.network.LeaderboardPayload;

public final class LeaderboardScreen extends Screen {
   private static final ResourceLocation LEADERBOARD_FRAME_TEXTURE = ResourceLocation.fromNamespaceAndPath("cobblebattle", "textures/gui/leaderboard/base.png");
   private static final ResourceLocation LEADERBOARD_CONTENT_TEXTURE = ResourceLocation.fromNamespaceAndPath("cobblebattle", "textures/gui/leaderboard/screen.png");
   private static final ResourceLocation POKEDEX_EMBLEM_TEXTURE = ResourceLocation.fromNamespaceAndPath(
      "cobblemon", "textures/gui/pokedex/pokedex_screen_poke_ball.png"
   );
   private static final ResourceLocation DISPLAY_PLATFORM_TEXTURE = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/pokedex/platform_base.png");
   private static final ResourceLocation DISPLAY_SHADOW_TEXTURE = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/pokedex/platform_shadow.png");
   private static final ResourceLocation UNKNOWN_CREATURE_TEXTURE = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/pokedex/platform_unknown.png");
   private static final ResourceLocation SCROLL_UP_TEXTURE = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/pokedex/arrow_up.png");
   private static final ResourceLocation SCROLL_DOWN_TEXTURE = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/pokedex/arrow_down.png");
   public static final int PAGE_LABEL_RIGHT = 322;
   public static final int PAGE_LABEL_Y = 14;
   private static final int PANEL_WIDTH = 345;
   private static final int PANEL_HEIGHT = 207;
   private static final int CONTENT_LEFT_OFFSET = 18;
   private static final int CONTENT_TOP_OFFSET = 13;
   private static final int CONTENT_WIDTH = 309;
   private static final int CONTENT_HEIGHT = 183;
   private static final int HEADER_TOP_OFFSET = 12;
   private static final int HEADER_HEIGHT = 12;
   private static final int PROFILE_PANE_LEFT_OFFSET = 20;
   private static final int PROFILE_PANE_WIDTH = 87;
   private static final int RANKING_PANE_LEFT_OFFSET = 110;
   private static final int RANKING_PANE_WIDTH = 215;
   private static final int PANE_TOP_OFFSET = 25;
   private static final int PANE_HEIGHT = 170;
   private static final int ODD_ROW_COLOR = -11286315;
   private static final int EVEN_ROW_COLOR = -10759718;
   private static final int SELECTED_ROW_COLOR = -12274744;
   private static final int ROW_TEXT_COLOR = -15451066;
   private static final int HEADER_TEXT_COLOR = -14721168;
   private static final int SELECTED_TEXT_COLOR = -1;
   private static final int TOP_RANK_COLOR = -868018;
   private static final int OWN_ROW_COLOR = -9773853;
   private static final int COLUMN_HEADER_TOP_OFFSET = 28;
   private static final int RANKING_ROWS_TOP_OFFSET = 40;
   private static final int RANKING_ROW_HEIGHT = 14;
   private static final int VISIBLE_RANKING_ROWS = 10;
   private static final int RANK_COLUMN_LEFT_OFFSET = 114;
   private static final int FACE_COLUMN_LEFT_OFFSET = 136;
   private static final int NAME_COLUMN_LEFT_OFFSET = 148;
   private static final int RECORD_COLUMN_LEFT_OFFSET = 256;
   private static final int SCORE_COLUMN_RIGHT_OFFSET = 319;
   private static final int PROFILE_LABEL_TOP_OFFSET = 13;
   private static final int PROFILE_LABEL_SHIFT = -3;
   private static final int PROFILE_BAR_TOP_OFFSET = 28;
   private static final int PROFILE_RANK_LEFT_OFFSET = 24;
   private static final int PROFILE_NAME_LEFT_OFFSET = 43;
   private static final int PROFILE_NAME_RIGHT_OFFSET = 104;
   private static final int PROFILE_TEXT_TOP_OFFSET = 28;
   private static final int PROFILE_NAME_COLOR = -10458002;
   private static final int DISPLAY_BOX_TOP_OFFSET = 37;
   private static final int DISPLAY_BOX_BOTTOM_OFFSET = 131;
   private static final int ROW_BACKGROUND_ALPHA = -671088640;
   private static final int DISPLAY_PLATFORM_WIDTH = 87;
   private static final int DISPLAY_PLATFORM_HEIGHT = 23;
   private static final int DISPLAY_PLATFORM_TOP_OFFSET = 110;
   private static final int DISPLAY_FLOOR_OFFSET = 121;
   private static final int DISPLAY_SHADOW_WIDTH = 45;
   private static final int DISPLAY_SHADOW_HEIGHT = 10;
   private static final int UNKNOWN_MARK_WIDTH = 26;
   private static final int UNKNOWN_MARK_HEIGHT = 30;
   private static final int PARTICIPANT_X = 40;
   private static final int PARTICIPANT_SIZE = 34;
   public static final float PLAYER_YAW = -35.0F;
   public static final float PLAYER_PITCH = -10.0F;
   private static final int CREATURE_X = 60;
   private static final int CREATURE_Y = 37;
   private static final int CREATURE_W = 46;
   private static final int CREATURE_H = 84;
   public static final float FACING = (float)(Math.atan(-0.875) * 40.0);
   public static final float POKEMON_PITCH = 5.0F;
   private final LeaderboardPayload rankingSnapshot;
   private final UUID viewerId;
   private LeaderboardPayload.Entry selectedEntry;
   private PlayerPortrait selectedPortrait;
   private final Map<Long, PlayerPortrait> portraitCache = new HashMap<>();
   private RenderablePokemon favouriteModel;
   private FloatingState creatureState = new FloatingState();
   private float creatureScale = 34.0F;
   private int firstVisibleRankOffset;
   private int panelLeft;
   private int panelTop;

   public LeaderboardScreen(LeaderboardPayload rankingSnapshot, UUID viewerId) {
      super(Component.translatable("cobblebattle.rank.title", new Object[]{rankingSnapshot.name()}));
      this.rankingSnapshot = rankingSnapshot;
      this.viewerId = viewerId;
   }

   protected void init() {
      this.panelLeft = (this.width - 345) / 2;
      this.panelTop = (this.height - 207) / 2;
      this.displayEntry(this.selectedEntry == null ? this.rankingSnapshot.you() : this.selectedEntry);
   }

   private void displayEntry(LeaderboardPayload.Entry rankingEntry) {
      this.selectedEntry = rankingEntry;
      this.selectedPortrait = this.portraitFor(rankingEntry);
      this.favouriteModel = null;
      this.creatureState = new FloatingState();
      this.creatureScale = 34.0F;
      String speciesTemplateId = rankingEntry.favourite();
      if (!speciesTemplateId.isEmpty()) {
         Species speciesTemplate = PokemonSpecies.getByName(speciesTemplateId);
         if (speciesTemplate != null) {
            this.favouriteModel = new RenderablePokemon(speciesTemplate, Set.of(), ItemStack.EMPTY);
            float modelHeight = Math.max(0.1F, this.favouriteModel.getForm().getHitbox().height());
            this.creatureScale = Math.min(34.0F, 78.0F / modelHeight);
         }
      }
   }

   private PlayerPortrait portraitFor(LeaderboardPayload.Entry rankingEntry) {
      Minecraft client = Minecraft.getInstance();
      if (rankingEntry == this.rankingSnapshot.you() || this.belongsToViewer(rankingEntry)) {
         LocalPlayer localPlayer = client.player;
         if (localPlayer instanceof AbstractClientPlayer) {
            return PlayerPortrait.of(localPlayer);
         }
      }

      return this.portraitCache.computeIfAbsent(rankingEntry.uid(), accountNumber -> PlayerPortrait.lookup(rankingEntry.name(), accountNumber));
   }

   private boolean belongsToViewer(LeaderboardPayload.Entry rankingEntry) {
      return rankingEntry.uid() != 0L && rankingEntry.uid() == this.rankingSnapshot.you().uid();
   }

   private boolean isCurrentEntry(LeaderboardPayload.Entry rankingEntry) {
      return this.selectedEntry != null && rankingEntry.uid() != 0L && rankingEntry.uid() == this.selectedEntry.uid();
   }

   public boolean isPauseScreen() {
      return false;
   }

   public void tick() {
   }

   public void render(GuiGraphics canvas, int pointerX, int pointerY, float frameDelta) {
      super.render(canvas, pointerX, pointerY, frameDelta);
      canvas.blit(LEADERBOARD_CONTENT_TEXTURE, this.panelLeft, this.panelTop, 0.0F, 0.0F, 345, 207, 345, 207);
      Backdrop.draw(canvas, this.panelLeft, this.panelTop);
      this.renderRankingContent(canvas, pointerX, pointerY, frameDelta);
      BackButton.draw(canvas, this.font, this.panelLeft, this.panelTop, pointerX, pointerY);
      canvas.blit(LEADERBOARD_FRAME_TEXTURE, this.panelLeft, this.panelTop, 0.0F, 0.0F, 345, 207, 345, 207);
   }

   private void renderRankingContent(GuiGraphics canvas, int pointerX, int pointerY, float frameDelta) {
      int regionLeft = this.panelLeft + 18;
      int regionTop = this.panelTop + 13;
      canvas.enableScissor(regionLeft, regionTop, regionLeft + 309, regionTop + 183);

      try {
         Ui.drawCentered(canvas, this.font, this.rankingSnapshot.name(), this.panelLeft + 172, this.panelTop + 12 + 2, -1);
         Component pageLabel = Component.translatable("cobblebattle.rank.label")
            .withStyle(fontStyle -> fontStyle.withFont(CobblemonResources.INSTANCE.getDEFAULT_LARGE()).withBold(true));
         Ui.draw(canvas, this.font, pageLabel, this.panelLeft + 322 - Ui.width(this.font, pageLabel), this.panelTop + 14, -1, true);
         this.renderSelectedProfile(canvas, pointerX, pointerY, frameDelta);
         this.renderRankingTable(canvas);
      } finally {
         canvas.disableScissor();
      }
   }

   private void renderSelectedProfile(GuiGraphics canvas, int pointerX, int pointerY, float frameDelta) {
      Minecraft client = Minecraft.getInstance();
      int paneLeft = this.panelLeft + 20;
      int displayCenterX = paneLeft + 43;
      Ui.drawCentered(canvas, this.font, Component.translatable("cobblebattle.rank.label"), displayCenterX + -3, this.panelTop + 13, -1);
      this.renderProfileBackdrop(canvas);
      if (this.selectedPortrait != null && this.selectedPortrait.entity() != null) {
         drawEntity(canvas, this.panelLeft + 40, this.panelTop + 121, 34, -35.0F, -10.0F, this.selectedPortrait.entity());
      }

      if (this.favouriteModel != null) {
         int creatureClipLeft = this.panelLeft + 60;
         int creatureClipTop = this.panelTop + 37;
         canvas.enableScissor(creatureClipLeft, creatureClipTop, creatureClipLeft + 46, this.panelTop + 121 + 2);
         canvas.pose().pushPose();
         canvas.pose().translate(creatureClipLeft + 23.0, this.panelTop + 121, 0.0);
         Quaternionf modelRotation = QuaternionUtilsKt.fromEulerXYZDegrees(new Quaternionf(), new Vector3f(5.0F, FACING, 0.0F));
         CobblemonCompat.drawProfile(
            this.favouriteModel,
            canvas.pose(),
            modelRotation,
            PoseType.PROFILE,
            this.creatureState,
            frameDelta,
            this.creatureScale,
            true,
            1.0F,
            1.0F,
            1.0F,
            1.0F,
            0.0F,
            0.0F,
            13
         );
         canvas.pose().popPose();
         canvas.disableScissor();
      } else {
         UnknownMark.draw(canvas, this.panelLeft + 60 + 23, this.panelTop + 121, 30);
      }

      LeaderboardPayload.Entry displayedEntry = this.selectedEntry == null ? this.rankingSnapshot.you() : this.selectedEntry;
      String displayName = !displayedEntry.name().isEmpty() ? displayedEntry.name() : (client.player == null ? "" : client.player.getGameProfile().getName());
      String rankLabel = displayedEntry.rank() != 0 ? "#" + displayedEntry.rank() : "#-";
      Ui.draw(canvas, this.font, styledProfileText(rankLabel), this.panelLeft + 24, this.panelTop + 28, -1, true);
      canvas.enableScissor(this.panelLeft + 43, this.panelTop + 28, this.panelLeft + 104, this.panelTop + 28 + 10);
      Ui.draw(canvas, this.font, styledProfileText(displayName), this.panelLeft + 43, this.panelTop + 28, -10458002, false);
      canvas.disableScissor();
   }

   private static Component styledProfileText(String content) {
      return Component.literal(content).withStyle(fontStyle -> fontStyle.withFont(CobblemonResources.INSTANCE.getDEFAULT_LARGE()).withBold(true));
   }

   private void renderProfileBackdrop(GuiGraphics canvas) {
      int regionLeft = this.panelLeft + 20;
      int regionTop = this.panelTop + 37;
      int clipBottom = this.panelTop + Math.max(131, 133);
      canvas.enableScissor(regionLeft, regionTop, regionLeft + 87, clipBottom);

      try {
         canvas.blit(DISPLAY_PLATFORM_TEXTURE, regionLeft, this.panelTop + 110, 87, 23, 0.0F, 0.0F, 113, 30, 113, 30);
         this.renderDisplayShadow(canvas, this.panelLeft + 40);
         this.renderDisplayShadow(canvas, this.panelLeft + 60 + 23);
      } finally {
         canvas.disableScissor();
      }
   }

   private void renderDisplayShadow(GuiGraphics canvas, int displayCenterX) {
      canvas.blit(DISPLAY_SHADOW_TEXTURE, displayCenterX - 22, this.panelTop + 121 - 5, 45, 10, 0.0F, 0.0F, 90, 20, 90, 20);
   }

   public static void drawEntity(GuiGraphics canvas, int anchorX, int anchorY, int renderSize, float horizontalLook, float verticalLook, LivingEntity renderedEntity) {
      float yawRadians = (float)Math.atan(horizontalLook / 40.0F);
      float pitchRadians = (float)Math.atan(verticalLook / 40.0F);
      Quaternionf entityPose = new Quaternionf().rotateZ((float) Math.PI);
      Quaternionf cameraRotation = new Quaternionf().rotateX(pitchRadians * 20.0F * (float) (Math.PI / 180.0));
      entityPose.mul(cameraRotation);
      float previousBodyYaw = renderedEntity.yBodyRot;
      float previousYaw = renderedEntity.getYRot();
      float previousPitch = renderedEntity.getXRot();
      float previousHeadYawBeforeTick = renderedEntity.yHeadRotO;
      float previousHeadYaw = renderedEntity.yHeadRot;
      renderedEntity.yBodyRot = 180.0F + yawRadians * 20.0F;
      renderedEntity.setYRot(180.0F + yawRadians * 40.0F);
      renderedEntity.setXRot(-pitchRadians * 20.0F);
      renderedEntity.yHeadRot = renderedEntity.getYRot();
      renderedEntity.yHeadRotO = renderedEntity.getYRot();

      try {
         InventoryScreen.renderEntityInInventory(canvas, anchorX, anchorY, renderSize, new Vector3f(), entityPose, cameraRotation, renderedEntity);
      } finally {
         renderedEntity.yBodyRot = previousBodyYaw;
         renderedEntity.setYRot(previousYaw);
         renderedEntity.setXRot(previousPitch);
         renderedEntity.yHeadRotO = previousHeadYawBeforeTick;
         renderedEntity.yHeadRot = previousHeadYaw;
      }
   }

   private void renderRankingTable(GuiGraphics canvas) {
      Font textRenderer = this.font;
      int columnHeaderTop = this.panelTop + 28;
      Ui.draw(canvas, textRenderer, Component.translatable("cobblebattle.rank.col.rank"), this.panelLeft + 114, columnHeaderTop, -14721168, false);
      Ui.draw(canvas, textRenderer, Component.translatable("cobblebattle.rank.col.player"), this.panelLeft + 148, columnHeaderTop, -14721168, false);
      Ui.draw(canvas, textRenderer, Component.translatable("cobblebattle.rank.col.record"), this.panelLeft + 256, columnHeaderTop, -14721168, false);
      Component scoreHeader = Component.translatable("cobblebattle.rank.col.score");
      Ui.draw(canvas, textRenderer, scoreHeader, this.panelLeft + 319 - Ui.width(textRenderer, scoreHeader), columnHeaderTop, -14721168, false);
      List<LeaderboardPayload.Entry> rankedEntries = this.rankingSnapshot.top();
      if (rankedEntries.isEmpty()) {
         Ui.drawCentered(canvas, textRenderer, Component.translatable("cobblebattle.rank.empty"), this.panelLeft + 110 + 107, this.panelTop + 40 + 56, -14721168);
      } else {
         for (int visibleRowIndex = 0; visibleRowIndex < 10; visibleRowIndex++) {
            int rankingIndex = this.firstVisibleRankOffset + visibleRowIndex;
            if (rankingIndex >= rankedEntries.size()) {
               break;
            }

            this.renderRankingRow(canvas, textRenderer, this.panelTop + 40 + visibleRowIndex * 14, rankedEntries.get(rankingIndex), visibleRowIndex % 2 == 0);
         }
      }
   }

   private void renderRankingRow(GuiGraphics canvas, Font textRenderer, int anchorY, LeaderboardPayload.Entry rankingEntry, boolean isEvenRow) {
      boolean isSelectedEntry = this.isCurrentEntry(rankingEntry);
      int rowBackground = isSelectedEntry ? -12274744 : (this.belongsToViewer(rankingEntry) ? -9773853 : (isEvenRow ? -11286315 : -10759718));
      int rowTextColor = isSelectedEntry ? -1 : -15451066;
      canvas.fill(this.panelLeft + 110, anchorY, this.panelLeft + 110 + 215, anchorY + 14, rowBackground & 16777215 | -671088640);
      int rowTextTop = anchorY + 3;
      int rankTextColor = rankingEntry.rank() <= 3 && !isSelectedEntry ? -868018 : rowTextColor;
      Ui.draw(canvas, textRenderer, "#" + rankingEntry.rank(), this.panelLeft + 114, rowTextTop, rankTextColor, false);
      this.renderEntryFace(canvas, rankingEntry, this.panelLeft + 136, anchorY + 3);
      int nameClipWidth = 104;
      canvas.enableScissor(this.panelLeft + 148, anchorY, this.panelLeft + 148 + nameClipWidth, anchorY + 14);
      Ui.draw(canvas, textRenderer, rankingEntry.name(), this.panelLeft + 148, rowTextTop, rowTextColor, false);
      canvas.disableScissor();
      String recordLabel = rankingEntry.wins() + " / " + rankingEntry.losses() + (rankingEntry.streak() >= 3 ? " ↑" + rankingEntry.streak() : "");
      Ui.draw(canvas, textRenderer, recordLabel, this.panelLeft + 256, rowTextTop, rowTextColor, false);
      String scoreLabel = String.valueOf(rankingEntry.score());
      Ui.draw(canvas, textRenderer, scoreLabel, this.panelLeft + 319 - Ui.width(textRenderer, scoreLabel), rowTextTop, rowTextColor, false);
   }

   private void renderEntryFace(GuiGraphics canvas, LeaderboardPayload.Entry rankingEntry, int anchorX, int anchorY) {
      ResourceLocation faceTexture;
      label20: {
         Minecraft client = Minecraft.getInstance();
         faceTexture = null;
         if (this.belongsToViewer(rankingEntry)) {
            LocalPlayer localViewer = client.player;
            if (localViewer instanceof AbstractClientPlayer && localViewer.getUUID().equals(this.viewerId)) {
               faceTexture = localViewer.getSkin().texture();
               break label20;
            }
         }

         PlayerPortrait cachedPortrait = this.portraitCache.get(rankingEntry.uid());
         if (cachedPortrait != null) {
            faceTexture = cachedPortrait.skin().texture();
         }
      }

      if (faceTexture == null) {
         faceTexture = DefaultPlayerSkin.get(new UUID(0L, rankingEntry.uid())).texture();
      }

      canvas.blit(faceTexture, anchorX, anchorY, 8, 8, 8.0F, 8.0F, 8, 8, 64, 64);
      canvas.blit(faceTexture, anchorX, anchorY, 8, 8, 40.0F, 8.0F, 8, 8, 64, 64);
   }

   public boolean mouseClicked(double pointerX, double pointerY, int mouseButton) {
      if (mouseButton == 0) {
         if (BackButton.contains(this.panelLeft, this.panelTop, pointerX, pointerY)) {
            ServerDex.requestMain();
            return true;
         }

         int selectedRowIndex = this.rankingIndexAt(pointerX, pointerY);
         if (selectedRowIndex >= 0) {
            this.displayEntry(this.rankingSnapshot.top().get(selectedRowIndex));
            return true;
         }
      }

      return super.mouseClicked(pointerX, pointerY, mouseButton);
   }

   private int rankingIndexAt(double pointerX, double pointerY) {
      int regionLeft = this.panelLeft + 110;
      int regionTop = this.panelTop + 40;
      if (!(pointerX < regionLeft) && !(pointerX >= regionLeft + 215) && !(pointerY < regionTop) && !(pointerY >= regionTop + 140)) {
         int rankingIndex = this.firstVisibleRankOffset + (int)((pointerY - regionTop) / 14.0);
         return rankingIndex < this.rankingSnapshot.top().size() ? rankingIndex : -1;
      } else {
         return -1;
      }
   }

   public boolean mouseScrolled(double pointerX, double pointerY, double horizontalScrollAmount, double verticalScrollAmount) {
      int maximumOffset = Math.max(0, this.rankingSnapshot.top().size() - 10);
      this.firstVisibleRankOffset = Math.max(0, Math.min(maximumOffset, this.firstVisibleRankOffset - (int)Math.signum(verticalScrollAmount)));
      return true;
   }
}
