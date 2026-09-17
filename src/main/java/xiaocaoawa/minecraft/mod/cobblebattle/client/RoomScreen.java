package xiaocaoawa.minecraft.mod.cobblebattle.client;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.client.CobblemonClient;
import com.cobblemon.mod.common.client.CobblemonResources;
import com.cobblemon.mod.common.client.render.models.blockbench.FloatingState;
import com.cobblemon.mod.common.entity.PoseType;
import com.cobblemon.mod.common.pokemon.RenderablePokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.cobblemon.mod.common.util.math.QuaternionUtilsKt;
import io.github.rinicesiberia.shadowbattle.client.RoomInteractionState;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import xiaocaoawa.minecraft.mod.cobblebattle.network.RoomActionPayload;
import xiaocaoawa.minecraft.mod.cobblebattle.network.RoomStatePayload;

public final class RoomScreen extends Screen {
   private static final ResourceLocation FRAME_TEXTURE = ResourceLocation.fromNamespaceAndPath("cobblebattle", "textures/gui/leaderboard/base.png");
   private static final ResourceLocation ROOM_SCREEN_TEXTURE = ResourceLocation.fromNamespaceAndPath("cobblebattle", "textures/gui/leaderboard/screen_1.png");
   private static final ResourceLocation SEAT_PLATFORM_TEXTURE = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/pokedex/platform_base.png");
   private static final ResourceLocation ENTITY_SHADOW_TEXTURE = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/pokedex/platform_shadow.png");
   private static final int PANEL_WIDTH = 345;
   private static final int PANEL_HEIGHT = 207;
   private static final int CONTENT_LEFT_OFFSET = 18;
   private static final int CONTENT_TOP_OFFSET = 13;
   private static final int CONTENT_WIDTH = 309;
   private static final int CONTENT_HEIGHT = 183;
   private static final int TITLE_LEFT_OFFSET = 24;
   private static final int TITLE_TOP_OFFSET = 15;
   private static final int SEAT_WIDTH = 138;
   private static final int SEAT_TOP_OFFSET = 28;
   private static final int SEAT_HEIGHT = 128;
   private static final int HOST_SEAT_LEFT_OFFSET = 22;
   private static final int GUEST_SEAT_LEFT_OFFSET = 186;
   private static final int MEMBER_NAME_HEIGHT = 12;
   private static final int ENTITY_FLOOR_OFFSET = 138;
   private static final int SEAT_PLATFORM_HEIGHT = 36;
   private static final int SEAT_PLATFORM_TOP_OFFSET = 126;
   private static final int PLAYER_RENDER_SIZE = 34;
   private static final int ENTITY_SHADOW_WIDTH = 44;
   private static final int ENTITY_SHADOW_HEIGHT = 10;
   private static final int UNKNOWN_CREATURE_HEIGHT = 28;
   private static final int CREATURE_HORIZONTAL_OFFSET = 44;
   private static final int OBSERVER_BAR_LEFT_OFFSET = 22;
   private static final int OBSERVER_BAR_TOP_OFFSET = 160;
   private static final int OBSERVER_BAR_WIDTH = 302;
   private static final int OBSERVER_BAR_HEIGHT = 20;
   private static final int START_BUTTON_LEFT_OFFSET = 130;
   private static final int START_BUTTON_TOP_OFFSET = 183;
   private static final int START_BUTTON_WIDTH = 84;
   private static final int START_BUTTON_HEIGHT = 12;
   private static final int INVITATION_LEFT_OFFSET = 22;
   private static final int INVITATION_TOP_OFFSET = 185;
   private static final int INVITATION_WIDTH = 102;
   private static final int HOST_PANEL_COLOR = 1429903030;
   private static final int GUEST_PANEL_COLOR = 1438796619;
   private static final int EMPTY_PANEL_COLOR = 872415231;
   private static final int PANEL_BORDER_COLOR = -1426063361;
   private static final int HOST_NAME_COLOR = -869302372;
   private static final int GUEST_NAME_COLOR = -862176710;
   private static final int PRIMARY_TEXT_COLOR = -1;
   private static final int SECONDARY_TEXT_COLOR = -1770753;
   private static final int START_BUTTON_COLOR = 1728053247;
   private static final int START_BUTTON_HOVER_COLOR = -1711276033;
   private static final int START_BUTTON_DISABLED_COLOR = 872415231;
   private RoomStatePayload roomState;
   private final Map<String, PlayerPortrait> portraitCache = new HashMap<>();
   private final Map<String, RenderablePokemon> leadModelCache = new HashMap<>();
   private final Map<String, FloatingState> poseCache = new HashMap<>();
   private int panelLeft;
   private int panelTop;
   private final RoomInteractionState roomControls = new RoomInteractionState();

   public RoomScreen(RoomStatePayload roomState) {
      super(Component.translatable("cobblebattle.room.screen_title"));
      this.roomState = roomState;
   }

   public void update(RoomStatePayload roomState) {
      this.roomState = roomState;
   }

   public String roomId() {
      return this.roomState.roomId();
   }

   protected void init() {
      this.panelLeft = (this.width - 345) / 2;
      this.panelTop = (this.height - 207) / 2;
   }

   public boolean isPauseScreen() {
      return false;
   }

   public void tick() {
      this.roomControls.tick();

      if (CobblemonClient.INSTANCE.getBattle() != null) {
         Minecraft.getInstance().setScreen(null);
      }
   }

   public void render(GuiGraphics canvas, int pointerX, int pointerY, float frameDelta) {
      super.render(canvas, pointerX, pointerY, frameDelta);
      canvas.blit(ROOM_SCREEN_TEXTURE, this.panelLeft, this.panelTop, 0.0F, 0.0F, 345, 207, 345, 207);
      Backdrop.draw(canvas, this.panelLeft, this.panelTop);
      int clipLeft = this.panelLeft + 18;
      int clipTop = this.panelTop + 13;
      canvas.enableScissor(clipLeft, clipTop, clipLeft + 309, clipTop + 183);

      try {
         this.renderRoomHeader(canvas);
         this.renderMemberSeat(canvas, 22, this.roomState.host(), true, true, frameDelta);
         this.renderMemberSeat(canvas, 186, this.roomState.guest(), this.roomState.hasGuest(), false, frameDelta);
         this.renderVersusLabel(canvas);
         this.renderObserverBar(canvas);
         this.renderInvitationCode(canvas, pointerX, pointerY);
         this.renderStartControl(canvas, pointerX, pointerY);
         BackButton.draw(canvas, this.font, this.panelLeft, this.panelTop, pointerX, pointerY);
      } finally {
         canvas.disableScissor();
      }

      canvas.blit(FRAME_TEXTURE, this.panelLeft, this.panelTop, 0.0F, 0.0F, 345, 207, 345, 207);
   }

   private void renderRoomHeader(GuiGraphics canvas) {
      Ui.draw(canvas, this.font, this.roomState.name(), this.panelLeft + 24, this.panelTop + 15, -1, true);
      Component battleTypeLabel = Component.translatable("cobblebattle.room.type." + this.roomState.battleType());
      Component levelRuleLabel = this.roomState.level() > 0
         ? Component.translatable("cobblebattle.room.level_at", new Object[]{this.roomState.level()})
         : Component.translatable("cobblebattle.room.level_free");
      List<Component> ruleLabels = new ArrayList<>(List.of(battleTypeLabel, levelRuleLabel));
      if (this.roomState.pick() > 0 && this.roomState.pick() < 6) {
         ruleLabels.add(Component.translatable("cobblebattle.room.pick_tag", new Object[]{this.roomState.pick()}));
      }

      if (this.roomState.hostEngine()) {
         ruleLabels.add(Component.translatable("cobblebattle.room.engine_host_tag"));
      }

      if (!this.roomState.legality()) {
         ruleLabels.add(Component.translatable("cobblebattle.room.legality_tag"));
      }

      Component rulesSummary = Ui.join(ruleLabels.toArray(new Component[0]));
      Ui.draw(canvas, this.font, rulesSummary, this.panelLeft + 322 - Ui.width(this.font, rulesSummary), this.panelTop + 14, -1770753, true);
   }

   private void renderMemberSeat(GuiGraphics canvas, int horizontalPosition, RoomStatePayload.Member roomMember, boolean seatOccupied, boolean hostSeat, float frameDelta) {
      int seatLeft = this.panelLeft + horizontalPosition;
      int seatTop = this.panelTop + 28;
      canvas.fill(seatLeft, seatTop, seatLeft + 138, seatTop + 128, seatOccupied ? (hostSeat ? 1429903030 : 1438796619) : 872415231);
      canvas.fill(seatLeft, seatTop, seatLeft + 138, seatTop + 1, -1426063361);
      canvas.fill(seatLeft, seatTop + 128 - 1, seatLeft + 138, seatTop + 128, -1426063361);
      canvas.fill(seatLeft, seatTop, seatLeft + 1, seatTop + 128, -1426063361);
      canvas.fill(seatLeft + 138 - 1, seatTop, seatLeft + 138, seatTop + 128, -1426063361);
      canvas.fill(seatLeft + 1, seatTop + 1, seatLeft + 138 - 1, seatTop + 12, hostSeat ? -869302372 : -862176710);
      Component seatRoleLabel = Component.translatable(hostSeat ? "cobblebattle.room.host_seat" : "cobblebattle.room.guest_seat");
      if (hostSeat) {
         Ui.draw(canvas, this.font, seatRoleLabel, seatLeft + 5, seatTop + 3, -1770753, false);
         Ui.draw(canvas, this.font, roomMember.name(), seatLeft + 5 + Ui.width(this.font, seatRoleLabel) + 6, seatTop + 3, -1, true);
      } else {
         Ui.draw(canvas, this.font, seatRoleLabel, seatLeft + 138 - 5 - Ui.width(this.font, seatRoleLabel), seatTop + 3, -1770753, false);
         if (seatOccupied) {
            Component memberNameLabel = Component.literal(roomMember.name());
            Ui.draw(canvas, this.font, memberNameLabel, seatLeft + 138 - 11 - Ui.width(this.font, seatRoleLabel) - Ui.width(this.font, memberNameLabel), seatTop + 3, -1, true);
         }
      }

      if (!seatOccupied) {
         Ui.drawCentered(canvas, this.font, Component.translatable("cobblebattle.room.waiting_seat"), seatLeft + 69, seatTop + 64 - 4, -1770753);
      } else {
         canvas.enableScissor(seatLeft + 1, seatTop + 12, seatLeft + 138 - 1, this.panelTop + 126 + 36);

         try {
            canvas.blit(SEAT_PLATFORM_TEXTURE, seatLeft, this.panelTop + 126, 138, 36, 0.0F, 0.0F, 113, 30, 113, 30);
            int playerCenterX = seatLeft + 69 - 22 - 4;
            int creatureCenterX = seatLeft + 69 + 22 + 4;
            this.renderEntityShadow(canvas, playerCenterX);
            this.renderEntityShadow(canvas, creatureCenterX);
            PlayerPortrait playerPortrait = this.portraitFor(roomMember, hostSeat);
            if (playerPortrait != null && playerPortrait.entity() != null) {
               LeaderboardScreen.drawEntity(canvas, playerCenterX, this.panelTop + 138, 34, -35.0F, -10.0F, playerPortrait.entity());
            }

            RenderablePokemon leadModel = this.leadModelFor(roomMember);
            if (leadModel != null) {
               FloatingState modelPose = this.poseCache.computeIfAbsent(memberCacheKey(roomMember), unusedCacheKey -> new FloatingState());
               float modelHeightBlocks = Math.max(0.1F, leadModel.getForm().getHitbox().height());
               float modelScale = Math.min(34.0F, 94.0F / modelHeightBlocks);
               canvas.pose().pushPose();
               canvas.pose().translate(creatureCenterX, this.panelTop + 138, 0.0);
               Quaternionf modelRotation = QuaternionUtilsKt.fromEulerXYZDegrees(new Quaternionf(), new Vector3f(5.0F, LeaderboardScreen.FACING, 0.0F));
               CobblemonCompat.drawProfile(
                  leadModel, canvas.pose(), modelRotation, PoseType.PROFILE, modelPose, frameDelta, modelScale, true, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F, 0.0F, 13
               );
               canvas.pose().popPose();
            } else {
               UnknownMark.draw(canvas, creatureCenterX, this.panelTop + 138, 28);
            }
         } finally {
            canvas.disableScissor();
         }

         Ui.drawCentered(
            canvas, this.font, Component.translatable("cobblebattle.room.team", new Object[]{roomMember.teamSize()}), seatLeft + 69, seatTop + 128 - 12, -1770753
         );
      }
   }

   private void renderEntityShadow(GuiGraphics canvas, int centerX) {
      canvas.blit(ENTITY_SHADOW_TEXTURE, centerX - 22, this.panelTop + 138 - 5, 44, 10, 0.0F, 0.0F, 90, 20, 90, 20);
   }

   private void renderVersusLabel(GuiGraphics canvas) {
      Component versusLabel = Component.literal("VS").withStyle(textStyle -> textStyle.withFont(CobblemonResources.INSTANCE.getDEFAULT_LARGE()).withBold(true));
      Ui.drawCentered(canvas, this.font, versusLabel, this.panelLeft + 172, this.panelTop + 28 + 64 - 8, -1);
   }

   private void renderObserverBar(GuiGraphics canvas) {
      int horizontalPosition = this.panelLeft + 22;
      int verticalPosition = this.panelTop + 160;
      canvas.fill(horizontalPosition, verticalPosition, horizontalPosition + 302, verticalPosition + 20, 872415231);
      canvas.fill(horizontalPosition, verticalPosition, horizontalPosition + 302, verticalPosition + 1, -1426063361);
      canvas.fill(horizontalPosition, verticalPosition + 20 - 1, horizontalPosition + 302, verticalPosition + 20, -1426063361);
      List<RoomStatePayload.Member> observerList = this.roomState.watchers();
      Component displayLabel = Component.translatable("cobblebattle.room.watchers", new Object[]{observerList.size()});
      Ui.draw(canvas, this.font, displayLabel, horizontalPosition + 5, verticalPosition + 6, -1770753, false);
      int nextNameLeft = horizontalPosition + 8 + Ui.width(this.font, displayLabel);
      canvas.enableScissor(nextNameLeft, verticalPosition, horizontalPosition + 302 - 4, verticalPosition + 20);

      for (RoomStatePayload.Member observer : observerList) {
         String memberNameLabel = observer.name();
         if (nextNameLeft > horizontalPosition + 302) {
            break;
         }

         Ui.draw(canvas, this.font, memberNameLabel, nextNameLeft, verticalPosition + 6, -1, false);
         nextNameLeft += Ui.width(this.font, memberNameLabel) + 8;
      }

      if (observerList.isEmpty()) {
         Ui.draw(canvas, this.font, Component.translatable("cobblebattle.room.no_watchers"), nextNameLeft, verticalPosition + 6, -1770753, false);
      }

      canvas.disableScissor();
   }

   private void renderInvitationCode(GuiGraphics canvas, int pointerX, int pointerY) {
      String invitationCode = this.roomState.inviteCode();
      if (!invitationCode.isEmpty()) {
         int horizontalPosition = this.panelLeft + 22;
         int verticalPosition = this.panelTop + 185;
         boolean pointerOver = this.invitationContains(pointerX, pointerY);
         Component displayLabel = Component.translatable("cobblebattle.room.invite_show", new Object[]{invitationCode});
         Ui.draw(canvas, this.font, displayLabel, horizontalPosition, verticalPosition, pointerOver ? -1 : -1770753, true);
         Component interactionHint = this.roomControls.copied()
            ? Component.translatable("cobblebattle.room.invite_copied")
            : (pointerOver ? Component.translatable("cobblebattle.room.invite_copy") : null);
         if (interactionHint != null) {
            canvas.enableScissor(horizontalPosition, verticalPosition - 2, horizontalPosition + 102, verticalPosition + 10);
            Ui.draw(canvas, this.font, interactionHint, horizontalPosition + Ui.width(this.font, displayLabel) + 6, verticalPosition, -1770753, false);
            canvas.disableScissor();
         }
      }
   }

   private boolean invitationContains(double pointerX, double pointerY) {
      int horizontalPosition = this.panelLeft + 22;
      int verticalPosition = this.panelTop + 185;
      return pointerX >= horizontalPosition && pointerX < horizontalPosition + 102 && pointerY >= verticalPosition - 2 && pointerY < verticalPosition + 10;
   }

   private void renderStartControl(GuiGraphics canvas, int pointerX, int pointerY) {
      int horizontalPosition = this.panelLeft + 130;
      int verticalPosition = this.panelTop + 183;
      if (this.roomState.fighting()) {
         Ui.drawCentered(canvas, this.font, Component.translatable("cobblebattle.room.in_battle"), horizontalPosition + 42, verticalPosition + 2, -1770753);
      } else {
         boolean hostSeat = "host".equals(this.roomState.youAre());
         boolean startAllowed = this.roomControls.canStart(this.roomState.youAre(), this.roomState.hasGuest(), this.roomState.fighting());
         boolean pointerOver = startAllowed && pointerX >= horizontalPosition && pointerX < horizontalPosition + 84 && pointerY >= verticalPosition && pointerY < verticalPosition + 12;
         if (!hostSeat) {
            Ui.drawCentered(canvas, this.font, Component.translatable("cobblebattle.room.wait_host"), horizontalPosition + 42, verticalPosition + 2, -1770753);
         } else {
            canvas.fill(horizontalPosition, verticalPosition, horizontalPosition + 84, verticalPosition + 12, startAllowed ? (pointerOver ? -1711276033 : 1728053247) : 872415231);
            canvas.fill(horizontalPosition, verticalPosition, horizontalPosition + 84, verticalPosition + 1, -1426063361);
            canvas.fill(horizontalPosition, verticalPosition + 12 - 1, horizontalPosition + 84, verticalPosition + 12, -1426063361);
            canvas.fill(horizontalPosition, verticalPosition, horizontalPosition + 1, verticalPosition + 12, -1426063361);
            canvas.fill(horizontalPosition + 84 - 1, verticalPosition, horizontalPosition + 84, verticalPosition + 12, -1426063361);
            String displayLabel = this.roomControls.starting()
               ? "cobblebattle.room.starting"
               : (this.roomState.hasGuest() ? "cobblebattle.room.start" : "cobblebattle.room.need_opponent");
            Ui.drawCentered(canvas, this.font, Component.translatable(displayLabel), horizontalPosition + 42, verticalPosition + 2, startAllowed ? -1 : -1770753);
         }
      }
   }

   private static String memberCacheKey(RoomStatePayload.Member roomMember) {
      return roomMember.uid() + ":" + roomMember.name();
   }

   private PlayerPortrait portraitFor(RoomStatePayload.Member roomMember, boolean hostSeat) {
      boolean localSeat = hostSeat ? "host".equals(this.roomState.youAre()) : "guest".equals(this.roomState.youAre());
      Minecraft client = Minecraft.getInstance();
      if (localSeat) {
         LocalPlayer localPlayer = client.player;
         if (localPlayer instanceof AbstractClientPlayer) {
            return PlayerPortrait.of(localPlayer);
         }
      }

      return this.portraitCache.computeIfAbsent(memberCacheKey(roomMember), unusedCacheKey -> PlayerPortrait.lookup(roomMember.name(), roomMember.uid()));
   }

   private RenderablePokemon leadModelFor(RoomStatePayload.Member roomMember) {
      return roomMember.lead().isEmpty() ? null : this.leadModelCache.computeIfAbsent(memberCacheKey(roomMember), unusedCacheKey -> {
         Species speciesDefinition = PokemonSpecies.getByName(roomMember.lead());
         return speciesDefinition == null ? null : new RenderablePokemon(speciesDefinition, Set.of(), ItemStack.EMPTY);
      });
   }

   public boolean mouseClicked(double pointerX, double pointerY, int mouseButton) {
      if (mouseButton == 0) {
         if (BackButton.contains(this.panelLeft, this.panelTop, pointerX, pointerY)) {
            RoomLobbyScreen.sendRoomAction(RoomActionPayload.of("leave"));
            return true;
         }

         if (!this.roomState.inviteCode().isEmpty() && this.invitationContains(pointerX, pointerY)) {
            Minecraft.getInstance().keyboardHandler.setClipboard(this.roomState.inviteCode());
            this.roomControls.markInvitationCopied();
            return true;
         }

         int horizontalPosition = this.panelLeft + 130;
         int verticalPosition = this.panelTop + 183;
         if (this.roomControls.canStart(this.roomState.youAre(), this.roomState.hasGuest(), this.roomState.fighting())
            && pointerX >= horizontalPosition
            && pointerX < horizontalPosition + 84
            && pointerY >= verticalPosition
            && pointerY < verticalPosition + 12) {
            this.roomControls.beginStart(this.roomState.youAre(), this.roomState.hasGuest(), this.roomState.fighting());
            RoomLobbyScreen.sendRoomAction(RoomActionPayload.of("start"));
            return true;
         }
      }

      return super.mouseClicked(pointerX, pointerY, mouseButton);
   }

   public void onClose() {
      RoomLobbyScreen.sendRoomAction(RoomActionPayload.of("leave"));
      Minecraft.getInstance().setScreen(null);
   }
}
