package xiaocaoawa.minecraft.mod.cobblebattle.client;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.client.CobblemonClient;
import com.cobblemon.mod.common.client.CobblemonResources;
import com.cobblemon.mod.common.client.render.models.blockbench.FloatingState;
import com.cobblemon.mod.common.client.render.models.blockbench.PosableState;
import com.cobblemon.mod.common.entity.PoseType;
import com.cobblemon.mod.common.pokemon.RenderablePokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.cobblemon.mod.common.util.math.QuaternionUtilsKt;
import dev.architectury.networking.NetworkManager;
import io.github.rinicesiberia.shadowbattle.client.TeamPreviewSelectionState;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import xiaocaoawa.minecraft.mod.cobblebattle.network.TeamPickPayload;
import xiaocaoawa.minecraft.mod.cobblebattle.network.TeamPreviewPayload;

public final class TeamPreviewScreen extends Screen {
   private static final ResourceLocation FRAME_TEXTURE = ResourceLocation.fromNamespaceAndPath("cobblebattle", "textures/gui/leaderboard/base.png");
   private static final ResourceLocation PREVIEW_SCREEN_TEXTURE = ResourceLocation.fromNamespaceAndPath("cobblebattle", "textures/gui/leaderboard/screen_2.png");
   private static final ResourceLocation TRAINER_PLATFORM_TEXTURE = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/pokedex/platform_base.png");
   private static final ResourceLocation TRAINER_SHADOW_TEXTURE = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/pokedex/platform_shadow.png");
   private static final int PANEL_WIDTH = 345;
   private static final int PANEL_HEIGHT = 207;
   private static final int CONTENT_LEFT_OFFSET = 18;
   private static final int CONTENT_TOP_OFFSET = 13;
   private static final int CONTENT_WIDTH = 309;
   private static final int CONTENT_HEIGHT = 183;
   private static final int TITLE_LEFT_OFFSET = 24;
   private static final int TITLE_TOP_OFFSET = 15;
   private static final int TEXTURE_WIDTH = 345;
   private static final int ROSTER_SLOT_WIDTH = 72;
   private static final int ROSTER_SLOT_HEIGHT = 22;
   private static final int ROSTER_SLOT_SPACING = 24;
   private static final int ROSTER_TOP_OFFSET = 30;
   private static final int MAX_ROSTER_SLOTS = 6;
   private static final int LOCAL_ROSTER_LEFT_OFFSET = 23;
   private static final int OPPONENT_ROSTER_LEFT_OFFSET = 250;
   private static final int CREATURE_ICON_SIZE = 24;
   private static final int TRAINER_PANEL_WIDTH = 68;
   private static final int TRAINER_PANEL_TOP_OFFSET = 30;
   private static final int TRAINER_PANEL_HEIGHT = 142;
   private static final int LOCAL_TRAINER_LEFT_OFFSET = 98;
   private static final int OPPONENT_TRAINER_LEFT_OFFSET = 179;
   private static final int TRAINER_NAME_HEIGHT = 12;
   private static final int TRAINER_PLATFORM_HEIGHT = 18;
   private static final int TRAINER_PLATFORM_TOP_OFFSET = 120;
   private static final int TRAINER_FLOOR_OFFSET = 127;
   private static final int TRAINER_RENDER_SIZE = 34;
   private static final int TRAINER_SHADOW_WIDTH = 40;
   private static final int TRAINER_SHADOW_HEIGHT = 9;
   private static final int READINESS_STATUS_TOP_OFFSET = 148;
   private static final int CONFIRM_BUTTON_WIDTH = 61;
   private static final int CONFIRM_BUTTON_LEFT_OFFSET = 142;
   private static final int CONFIRM_BUTTON_TOP_OFFSET = 176;
   private static final int CONFIRM_BUTTON_HEIGHT = 15;
   private static final int EMPTY_SLOT_COLOR = 1442840575;
   private static final int LOCAL_SLOT_COLOR = 1429903030;
   private static final int OPPONENT_SLOT_COLOR = 1438796619;
   private static final int SELECTED_SLOT_COLOR = -1438665056;
   private static final int HOVERED_SLOT_COLOR = -1996488705;
   private static final int SLOT_BORDER_COLOR = -1426063361;
   private static final int SELECTED_BORDER_COLOR = -4590113;
   private static final int LOCAL_NAME_COLOR = -869302372;
   private static final int OPPONENT_NAME_COLOR = -862176710;
   private static final int PRIMARY_TEXT_COLOR = -1;
   private static final int SECONDARY_TEXT_COLOR = -1770753;
   private static final int DIM_TEXT_COLOR = -6303010;
   private static final int ORDER_BADGE_COLOR = -14718116;
   private static final int LEAD_BADGE_COLOR = -875716;
   private static final int CONFIRM_BUTTON_COLOR = 1728053247;
   private static final int CONFIRM_BUTTON_HOVER_COLOR = -1711276033;
   private static final int CONFIRM_BUTTON_DISABLED_COLOR = 872415231;
   private static final int URGENT_TEXT_COLOR = -30107;
   private TeamPreviewPayload previewState;
   private final TeamPreviewSelectionState selectionState = new TeamPreviewSelectionState();
   private final Map<String, RenderablePokemon> modelCache = new HashMap<>();
   private final Map<String, FloatingState> poseCache = new HashMap<>();
   private PlayerPortrait opponentPortrait;
   private int panelLeft;
   private int panelTop;

   public TeamPreviewScreen(TeamPreviewPayload previewState) {
      super(Component.translatable("cobblebattle.preview.title"));
      this.previewState = previewState;
   }

   public String battleId() {
      return this.previewState.battleId();
   }

   public void update(TeamPreviewPayload previewState) {
      this.previewState = previewState;
   }

   protected void init() {
      this.panelLeft = (this.width - 345) / 2;
      this.panelTop = (this.height - 207) / 2;
   }

   public boolean isPauseScreen() {
      return false;
   }

   public boolean shouldCloseOnEsc() {
      return this.selectionClosed();
   }

   private boolean selectionClosed() {
      return this.selectionState.isOver(this.previewState.closed(), this.previewState.deadlineMs(), System.currentTimeMillis());
   }

   public void tick() {
      if (CobblemonClient.INSTANCE.getBattle() != null) {
         Minecraft.getInstance().setScreen(null);
      }
   }

   public void render(GuiGraphics canvas, int pointerX, int pointerY, float frameDelta) {
      super.render(canvas, pointerX, pointerY, frameDelta);
      canvas.blit(PREVIEW_SCREEN_TEXTURE, this.panelLeft, this.panelTop, 0.0F, 0.0F, 345, 207, 345, 207);
      Backdrop.draw(canvas, this.panelLeft, this.panelTop);
      int clipLeft = this.panelLeft + 18;
      int clipTop = this.panelTop + 13;
      canvas.enableScissor(clipLeft, clipTop, clipLeft + 309, clipTop + 183);

      try {
         this.renderHeader(canvas);
         this.renderRosterColumn(canvas, 23, this.previewState.mine(), true, pointerX, pointerY, frameDelta);
         this.renderRosterColumn(canvas, 250, this.previewState.theirs(), false, pointerX, pointerY, frameDelta);
         this.renderTrainerPanel(canvas, 98, true, frameDelta);
         this.renderTrainerPanel(canvas, 179, false, frameDelta);
         this.renderConfirmationButton(canvas, pointerX, pointerY);
      } finally {
         canvas.disableScissor();
      }

      canvas.blit(FRAME_TEXTURE, this.panelLeft, this.panelTop, 0.0F, 0.0F, 345, 207, 345, 207);
      this.renderSlotTooltip(canvas, pointerX, pointerY);
   }

   private void renderHeader(GuiGraphics canvas) {
      Ui.draw(canvas, this.font, Component.translatable("cobblebattle.preview.title"), this.panelLeft + 24, this.panelTop + 15, -1, true);
      Component selectionSummary = (Component)(this.previewState.lead() > 1
         ? Ui.join(
            Component.translatable("cobblebattle.preview.count", new Object[]{this.selectionState.count(), this.previewState.pick()}),
            Component.translatable("cobblebattle.preview.lead", new Object[]{this.previewState.lead()})
         )
         : Component.translatable("cobblebattle.preview.count", new Object[]{this.selectionState.count(), this.previewState.pick()}));
      Component headerStatus = (Component)(this.selectionClosed() && !this.previewState.closed().isEmpty()
         ? Component.translatable("cobblebattle.preview.over")
         : Ui.join(selectionSummary, this.remainingTimeLabel()));
      Ui.draw(canvas, this.font, headerStatus, this.panelLeft + 322 - Ui.width(this.font, headerStatus), this.panelTop + 14, -1770753, true);
   }

   private Component remainingTimeLabel() {
      long remainingSeconds = this.selectionState.remainingSeconds(this.previewState.deadlineMs(), System.currentTimeMillis());
      return Component.literal(String.format("%d:%02d", remainingSeconds / 60L, remainingSeconds % 60L));
   }

   private void renderRosterColumn(GuiGraphics canvas, int horizontalPosition, List<TeamPreviewPayload.Slot> rosterEntries, boolean localSide, int pointerX, int pointerY, float frameDelta) {
      for (int slotIndex = 0; slotIndex < 6; slotIndex++) {
         int contentLeft = this.panelLeft + horizontalPosition;
         int contentTop = this.panelTop + 30 + slotIndex * 24;
         TeamPreviewPayload.Slot rosterSlot = slotIndex < rosterEntries.size() ? rosterEntries.get(slotIndex) : null;
         int selectionOrder = localSide ? this.selectionState.orderOf(slotIndex) : -1;
         boolean pointerOver = rosterSlot != null && localSide && !this.selectionLocked() && pointerX >= contentLeft && pointerX < contentLeft + 72 && pointerY >= contentTop && pointerY < contentTop + 22;
         int backgroundColor;
         if (rosterSlot == null) {
            backgroundColor = 1442840575;
         } else if (selectionOrder >= 0) {
            backgroundColor = -1438665056;
         } else if (pointerOver) {
            backgroundColor = -1996488705;
         } else {
            backgroundColor = localSide ? 1429903030 : 1438796619;
         }

         canvas.fill(contentLeft, contentTop, contentLeft + 72, contentTop + 22, backgroundColor);
         int borderColor = selectionOrder >= 0 ? -4590113 : -1426063361;
         canvas.fill(contentLeft, contentTop, contentLeft + 72, contentTop + 1, borderColor);
         canvas.fill(contentLeft, contentTop + 22 - 1, contentLeft + 72, contentTop + 22, borderColor);
         canvas.fill(contentLeft, contentTop, contentLeft + 1, contentTop + 22, borderColor);
         canvas.fill(contentLeft + 72 - 1, contentTop, contentLeft + 72, contentTop + 22, borderColor);
         if (rosterSlot != null) {
            int iconLeft = localSide ? contentLeft : contentLeft + 72 - 24;
            this.renderCreatureIcon(canvas, rosterSlot, iconLeft, contentTop, frameDelta);
            int labelLeft = localSide ? contentLeft + 24 + 2 : contentLeft + 3;
            int labelRight = localSide ? contentLeft + 72 - 3 : contentLeft + 72 - 24 - 2;
            canvas.enableScissor(labelLeft, contentTop, labelRight, contentTop + 22);

            try {
               Component speciesNameLabel = localizedSpeciesName(rosterSlot);
               Ui.draw(canvas, this.font, speciesNameLabel, labelLeft, contentTop + 3, -1, false);
               Component detailsLabel = Component.translatable("cobblebattle.preview.level", new Object[]{rosterSlot.level()}).copy().append(genderMarker(rosterSlot));
               Ui.draw(canvas, this.font, detailsLabel, labelLeft, contentTop + 12, -6303010, false);
            } finally {
               canvas.disableScissor();
            }

            if (selectionOrder >= 0) {
               boolean isLeadSlot = selectionOrder < this.previewState.lead();
               int badgeLeft = localSide ? contentLeft + 72 - 9 : contentLeft + 1;
               canvas.fill(badgeLeft, contentTop + 1, badgeLeft + 8, contentTop + 9, isLeadSlot ? -875716 : -14718116);
               Ui.drawCentered(canvas, this.font, String.valueOf(selectionOrder + 1), badgeLeft + 4, contentTop + 1, isLeadSlot ? -1 : -6303010);
            }
         }
      }
   }

   private void renderCreatureIcon(GuiGraphics canvas, TeamPreviewPayload.Slot rosterSlot, int horizontalPosition, int verticalPosition, float frameDelta) {
      RenderablePokemon renderModel = this.modelFor(rosterSlot);
      int centerX = horizontalPosition + 12;
      int iconFloorY = verticalPosition + 22 - 2;
      if (renderModel == null) {
         UnknownMark.draw(canvas, centerX, iconFloorY, 18);
      } else {
         canvas.enableScissor(horizontalPosition, verticalPosition, horizontalPosition + 24, verticalPosition + 22);

         try {
            float modelHeightBlocks = Math.max(0.1F, renderModel.getForm().getHitbox().height());
            float modelScale = Math.min(18.0F, 19.0F / modelHeightBlocks);
            canvas.pose().pushPose();
            canvas.pose().translate(centerX, iconFloorY, 0.0);
            Quaternionf modelRotation = QuaternionUtilsKt.fromEulerXYZDegrees(new Quaternionf(), new Vector3f(5.0F, LeaderboardScreen.FACING, 0.0F));
            CobblemonCompat.drawProfile(
               renderModel,
               canvas.pose(),
               modelRotation,
               PoseType.PROFILE,
               (PosableState)this.poseCache.computeIfAbsent(modelCacheKey(rosterSlot), unusedCacheKey -> new FloatingState()),
               frameDelta,
               modelScale,
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
         } finally {
            canvas.disableScissor();
         }
      }
   }

   private void renderTrainerPanel(GuiGraphics canvas, int horizontalPosition, boolean localSide, float frameDelta) {
      int contentLeft = this.panelLeft + horizontalPosition;
      int contentTop = this.panelTop + 30;
      canvas.fill(contentLeft, contentTop, contentLeft + 68, contentTop + 142, localSide ? 1429903030 : 1438796619);
      canvas.fill(contentLeft, contentTop, contentLeft + 68, contentTop + 1, -1426063361);
      canvas.fill(contentLeft, contentTop + 142 - 1, contentLeft + 68, contentTop + 142, -1426063361);
      canvas.fill(contentLeft, contentTop, contentLeft + 1, contentTop + 142, -1426063361);
      canvas.fill(contentLeft + 68 - 1, contentTop, contentLeft + 68, contentTop + 142, -1426063361);
      canvas.fill(contentLeft + 1, contentTop + 1, contentLeft + 68 - 1, contentTop + 12, localSide ? -869302372 : -862176710);
      String trainerName = localSide ? this.previewState.you() : this.previewState.opponent();
      canvas.enableScissor(contentLeft + 2, contentTop + 1, contentLeft + 68 - 2, contentTop + 12);
      Ui.drawCentered(canvas, this.font, trainerName, contentLeft + 34, contentTop + 3, -1);
      canvas.disableScissor();
      canvas.enableScissor(contentLeft + 1, contentTop + 12, contentLeft + 68 - 1, this.panelTop + 120 + 18);

      try {
         canvas.blit(TRAINER_PLATFORM_TEXTURE, contentLeft, this.panelTop + 120, 68, 18, 0.0F, 0.0F, 113, 30, 113, 30);
         int centerX = contentLeft + 34;
         canvas.blit(TRAINER_SHADOW_TEXTURE, centerX - 20, this.panelTop + 127 - 4, 40, 9, 0.0F, 0.0F, 90, 20, 90, 20);
         PlayerPortrait trainerPortrait = this.portraitFor(localSide);
         if (trainerPortrait != null && trainerPortrait.entity() != null) {
            LeaderboardScreen.drawEntity(canvas, centerX, this.panelTop + 127, 34, localSide ? -35.0F : 35.0F, -10.0F, trainerPortrait.entity());
         }
      } finally {
         canvas.disableScissor();
      }

      boolean readyState = localSide ? this.previewState.mineReady() : this.previewState.theirsReady();
      Component readinessLabel = readyState ? Component.translatable("cobblebattle.preview.ready") : Component.translatable("cobblebattle.preview.choosing");
      Ui.drawCentered(canvas, this.font, readinessLabel, contentLeft + 34, this.panelTop + 148, readyState ? -4590113 : -6303010);
      if (!localSide) {
         Ui.drawCentered(
            canvas,
            this.font,
            Component.translatable("cobblebattle.preview.from", new Object[]{this.previewState.opponentServer()}),
            contentLeft + 34,
            this.panelTop + 148 + 10,
            -6303010
         );
      }
   }

   private PlayerPortrait portraitFor(boolean localSide) {
      Minecraft client = Minecraft.getInstance();
      if (localSide) {
         LocalPlayer localPlayer = client.player;
         return localPlayer instanceof AbstractClientPlayer ? PlayerPortrait.of(localPlayer) : null;
      } else {
         if (this.opponentPortrait == null) {
            this.opponentPortrait = PlayerPortrait.lookup(this.previewState.opponent(), 0L);
         }

         return this.opponentPortrait;
      }
   }

   private boolean selectionLocked() {
      return this.selectionState.isLocked(this.previewState.mineReady(), this.previewState.closed(), this.previewState.deadlineMs(), System.currentTimeMillis());
   }

   private boolean canConfirmSelection() {
      return this.selectionState.canConfirm(
         this.previewState.pick(), this.previewState.mineReady(), this.previewState.closed(), this.previewState.deadlineMs(), System.currentTimeMillis()
      );
   }

   private void renderConfirmationButton(GuiGraphics canvas, int pointerX, int pointerY) {
      int horizontalPosition = this.panelLeft + 142;
      int verticalPosition = this.panelTop + 176;
      boolean readyState = this.canConfirmSelection();
      boolean pointerOver = readyState && this.confirmationContains(pointerX, pointerY);
      canvas.fill(horizontalPosition, verticalPosition, horizontalPosition + 61, verticalPosition + 15, readyState ? (pointerOver ? -1711276033 : 1728053247) : 872415231);
      canvas.fill(horizontalPosition, verticalPosition, horizontalPosition + 61, verticalPosition + 1, -1426063361);
      canvas.fill(horizontalPosition, verticalPosition + 15 - 1, horizontalPosition + 61, verticalPosition + 15, -1426063361);
      canvas.fill(horizontalPosition, verticalPosition, horizontalPosition + 1, verticalPosition + 15, -1426063361);
      canvas.fill(horizontalPosition + 61 - 1, verticalPosition, horizontalPosition + 61, verticalPosition + 15, -1426063361);
      Component buttonLabel;
      if (!this.previewState.closed().isEmpty()) {
         buttonLabel = Component.translatable("cobblebattle.preview.closed");
      } else if (this.previewState.mineReady()) {
         buttonLabel = Component.translatable(this.previewState.theirsReady() ? "cobblebattle.preview.starting" : "cobblebattle.preview.waiting");
      } else if (this.selectionState.count() < this.previewState.pick()) {
         buttonLabel = Component.translatable("cobblebattle.preview.pick_more", new Object[]{this.previewState.pick() - this.selectionState.count()});
      } else {
         buttonLabel = Component.translatable("cobblebattle.preview.confirm");
      }

      Ui.drawCentered(canvas, this.font, buttonLabel, horizontalPosition + 30, verticalPosition + 4, readyState ? -1 : -1770753);
   }

   private boolean confirmationContains(double pointerX, double pointerY) {
      int horizontalPosition = this.panelLeft + 142;
      int verticalPosition = this.panelTop + 176;
      return pointerX >= horizontalPosition && pointerX < horizontalPosition + 61 && pointerY >= verticalPosition && pointerY < verticalPosition + 15;
   }

   private void renderSlotTooltip(GuiGraphics canvas, int pointerX, int pointerY) {
      TeamPreviewPayload.Slot rosterSlot = this.slotUnderPointer(pointerX, pointerY);
      if (rosterSlot != null) {
         List<Component> tooltipLines = new ArrayList<>(3);
         tooltipLines.add(localizedSpeciesName(rosterSlot));
         tooltipLines.add(Component.translatable("cobblebattle.preview.level", new Object[]{rosterSlot.level()}).copy().append(genderMarker(rosterSlot)));
         if (rosterSlot.shiny()) {
            tooltipLines.add(Component.translatable("cobblebattle.preview.shiny"));
         }

         if (!rosterSlot.item().isEmpty()) {
            tooltipLines.add(Component.translatable("cobblebattle.preview.item", new Object[]{localizedItemName(rosterSlot.item())}));
         }

         canvas.renderComponentTooltip(this.font, tooltipLines, pointerX, pointerY);
      }
   }

   private TeamPreviewPayload.Slot slotUnderPointer(double pointerX, double pointerY) {
      for (int slotIndex = 0; slotIndex < 6; slotIndex++) {
         int contentTop = this.panelTop + 30 + slotIndex * 24;
         if (!(pointerY < contentTop) && !(pointerY >= contentTop + 22)) {
            int localRosterLeft = this.panelLeft + 23;
            if (pointerX >= localRosterLeft && pointerX < localRosterLeft + 72 && slotIndex < this.previewState.mine().size()) {
               return this.previewState.mine().get(slotIndex);
            }

            int opponentRosterLeft = this.panelLeft + 250;
            if (pointerX >= opponentRosterLeft && pointerX < opponentRosterLeft + 72 && slotIndex < this.previewState.theirs().size()) {
               return this.previewState.theirs().get(slotIndex);
            }
         }
      }

      return null;
   }

   public boolean mouseClicked(double pointerX, double pointerY, int mouseButton) {
      if (mouseButton != 0) {
         return super.mouseClicked(pointerX, pointerY, mouseButton);
      } else if (this.canConfirmSelection() && this.confirmationContains(pointerX, pointerY)) {
         this.submitSelection();
         return true;
      } else if (this.selectionLocked()) {
         return super.mouseClicked(pointerX, pointerY, mouseButton);
      } else {
         for (int slotIndex = 0; slotIndex < Math.min(6, this.previewState.mine().size()); slotIndex++) {
            int contentLeft = this.panelLeft + 23;
            int contentTop = this.panelTop + 30 + slotIndex * 24;
            if (!(pointerX < contentLeft) && !(pointerX >= contentLeft + 72) && !(pointerY < contentTop) && !(pointerY >= contentTop + 22)) {
               this.toggleSelection(slotIndex);
               return true;
            }
         }

         return super.mouseClicked(pointerX, pointerY, mouseButton);
      }
   }

   private void toggleSelection(int rosterSlot) {
      this.selectionState.toggle(rosterSlot, this.previewState.pick());
   }

   private void submitSelection() {
      this.previewState = new TeamPreviewPayload(
         this.previewState.battleId(),
         this.previewState.you(),
         this.previewState.opponent(),
         this.previewState.opponentServer(),
         this.previewState.pick(),
         this.previewState.lead(),
         this.previewState.deadlineMs(),
         this.previewState.mine(),
         this.previewState.theirs(),
         true,
         this.previewState.theirsReady(),
         this.previewState.closed()
      );
      NetworkManager.sendToServer(new TeamPickPayload(this.previewState.battleId(), this.selectionState.snapshot()));
   }

   private static String modelCacheKey(TeamPreviewPayload.Slot rosterSlot) {
      return rosterSlot.species() + "/" + rosterSlot.shiny() + "/" + rosterSlot.gender();
   }

   private static Component localizedSpeciesName(TeamPreviewPayload.Slot rosterSlot) {
      Species speciesDefinition = PokemonSpecies.getByName(rosterSlot.species());
      return speciesDefinition == null ? Component.literal(rosterSlot.species()) : speciesDefinition.getTranslatedName();
   }

   private static Component genderMarker(TeamPreviewPayload.Slot rosterSlot) {
      if ("M".equalsIgnoreCase(rosterSlot.gender())) {
         return Ui.plain(" ♂");
      } else {
         return (Component)("F".equalsIgnoreCase(rosterSlot.gender()) ? Ui.plain(" ♀") : Component.empty());
      }
   }

   private static Component localizedItemName(String itemPath) {
      ResourceLocation cobblemonItemId = ResourceLocation.tryBuild("cobblemon", itemPath);
      if (cobblemonItemId != null && BuiltInRegistries.ITEM.containsKey(cobblemonItemId)) {
         Item resolvedItem = (Item)BuiltInRegistries.ITEM.get(cobblemonItemId);
         return new ItemStack(resolvedItem).getHoverName();
      } else {
         ResourceLocation vanillaItemId = ResourceLocation.tryBuild("minecraft", itemPath);
         return (Component)(vanillaItemId != null && BuiltInRegistries.ITEM.containsKey(vanillaItemId)
            ? new ItemStack((ItemLike)BuiltInRegistries.ITEM.get(vanillaItemId)).getHoverName()
            : Component.literal(itemPath));
      }
   }

   private RenderablePokemon modelFor(TeamPreviewPayload.Slot rosterSlot) {
      return this.modelCache.computeIfAbsent(modelCacheKey(rosterSlot), unusedCacheKey -> {
         Species speciesDefinition = PokemonSpecies.getByName(rosterSlot.species());
         if (speciesDefinition == null) {
            return null;
         } else {
            Set<String> modelAspects = new LinkedHashSet<>();
            if (rosterSlot.shiny()) {
               modelAspects.add("shiny");
            }

            if ("M".equalsIgnoreCase(rosterSlot.gender())) {
               modelAspects.add("male");
            }

            if ("F".equalsIgnoreCase(rosterSlot.gender())) {
               modelAspects.add("female");
            }

            return new RenderablePokemon(speciesDefinition, modelAspects, ItemStack.EMPTY);
         }
      });
   }

   private static Component styledLargeText(String content) {
      return Component.literal(content).withStyle(textStyle -> textStyle.withFont(CobblemonResources.INSTANCE.getDEFAULT_LARGE()).withBold(true));
   }
}
