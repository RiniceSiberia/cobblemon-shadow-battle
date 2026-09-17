package xiaocaoawa.minecraft.mod.cobblebattle.client;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.client.CobblemonResources;
import com.cobblemon.mod.common.client.render.models.blockbench.FloatingState;
import com.cobblemon.mod.common.entity.PoseType;
import com.cobblemon.mod.common.pokemon.RenderablePokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.cobblemon.mod.common.util.math.QuaternionUtilsKt;
import dev.architectury.networking.NetworkManager;
import io.github.rinicesiberia.shadowbattle.client.RoomCreationOptions;
import io.github.rinicesiberia.shadowbattle.client.RoomCreationRequest;
import io.github.rinicesiberia.shadowbattle.client.RoomLobbyRules;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import xiaocaoawa.minecraft.mod.cobblebattle.network.RoomActionPayload;
import xiaocaoawa.minecraft.mod.cobblebattle.network.RoomListPayload;

public final class RoomLobbyScreen extends Screen {
   private static final ResourceLocation LOBBY_FRAME_TEXTURE = ResourceLocation.fromNamespaceAndPath("cobblebattle", "textures/gui/leaderboard/base.png");
   private static final ResourceLocation LOBBY_CONTENT_TEXTURE = ResourceLocation.fromNamespaceAndPath("cobblebattle", "textures/gui/leaderboard/screen_1.png");
   private static final ResourceLocation DIALOG_BACKGROUND_TEXTURE = ResourceLocation.fromNamespaceAndPath("cobblebattle", "textures/gui/auth_background.png");
   private static final int LOBBY_WIDTH = 345;
   private static final int LOBBY_HEIGHT = 207;
   private static final int CONTENT_LEFT_OFFSET = 18;
   private static final int CONTENT_TOP_OFFSET = 13;
   private static final int CONTENT_WIDTH = 309;
   private static final int CONTENT_HEIGHT = 183;
   private static final int CREATE_LINK_LEFT_OFFSET = 24;
   private static final int HEADER_LINK_TOP_OFFSET = 14;
   private static final int HEADER_LINK_WIDTH = 52;
   private static final int HEADER_LINK_HEIGHT = 10;
   private static final int INVITE_LINK_LEFT_OFFSET = 80;
   private static final int INVITE_LINK_WIDTH = 52;
   private static final int ROOM_GRID_LEFT_OFFSET = 25;
   private static final int ROOM_GRID_TOP_OFFSET = 33;
   private static final int ROOM_GRID_COLUMNS = 2;
   private static final int VISIBLE_ROOM_ROWS = 3;
   private static final int ROOM_CARD_WIDTH = 143;
   private static final int ROOM_CARD_HEIGHT = 46;
   private static final int PORTRAIT_RENDER_SIZE = 15;
   private static final int UNKNOWN_MARK_HEIGHT = 26;
   private static final int ROOM_CARD_COLUMN_GAP = 8;
   private static final int ROOM_CARD_ROW_GAP = 5;
   private static final int SCROLLBAR_LEFT_OFFSET = 320;
   private static final int SCROLLBAR_WIDTH = 3;
   private static final int ROOM_CARD_COLOR = 1728053247;
   private static final int ROOM_CARD_HOVER_COLOR = -1996488705;
   private static final int ROOM_CARD_BORDER_COLOR = -1426063361;
   private static final int OWN_ROOM_ACCENT_COLOR = -2082246;
   private static final int OPEN_ROOM_ACCENT_COLOR = -1;
   private static final int LOCKED_ROOM_ACCENT_COLOR = -868018;
   private static final int PRIMARY_TEXT_COLOR = -1;
   private static final int SECONDARY_TEXT_COLOR = -1770753;
   private static final int SCROLL_TRACK_COLOR = -12937546;
   private static final int SCROLL_THUMB_COLOR = -197380;
   private static final int DIALOG_WIDTH = 191;
   private static final int DIALOG_HEIGHT = 207;
   private static final int DIALOG_LEFT_INSET = 20;
   private static final int DIALOG_RIGHT_INSET = 170;
   private static final int DIALOG_TITLE_TOP_OFFSET = 17;
   private static final int DIALOG_LABEL_LEFT_OFFSET = 26;
   private static final int DIALOG_FIELD_LEFT_OFFSET = 74;
   private static final int DIALOG_FIELD_WIDTH = 90;
   private static final int FIRST_DIALOG_ROW_OFFSET = 36;
   private static final int DIALOG_ROW_SPACING = 17;
   private static final int DIALOG_FIELD_HEIGHT = 13;
   private static final int PICKER_ARROW_WIDTH = 11;
   private static final int DEFAULT_DIALOG_BUTTON_TOP = 152;
   private static final int DIALOG_BUTTON_WIDTH = 64;
   private static final int DIALOG_BUTTON_HEIGHT = 14;
   private static final int DIALOG_CONTROL_COLOR = 872415231;
   private static final int DIALOG_CONTROL_HOVER_COLOR = 1728053247;
   private static final int DIALOG_CONTROL_BORDER_COLOR = -1996488705;
   private static final int DIALOG_OVERLAY_COLOR = -1728053248;
   private static final int ROOM_REFRESH_INTERVAL_TICKS = 100;
   private static final int ROOM_NAME_ROW = 0;
   private static final int BATTLE_TYPE_ROW = 1;
   private static final int TEAM_PICK_ROW = 2;
   private static final int LEVEL_CAP_ROW = 3;
   private static final int FULL_HEAL_ROW = 4;
   private static final int BATTLE_ENGINE_ROW = 5;
   private static final int LEGALITY_ROW = 6;
   private RoomListPayload directorySnapshot;
   private int firstVisibleRoomRow;
   private int refreshTickCounter;
   private int lobbyLeft;
   private int lobbyTop;
   private final Map<String, FloatingState> leadAnimationStates = new HashMap<>();
   private final Map<String, RenderablePokemon> leadModels = new HashMap<>();
   private final Map<String, PlayerPortrait> hostPortraits = new HashMap<>();
   private RoomLobbyScreen.DialogMode activeDialog = RoomLobbyScreen.DialogMode.NONE;
   private RoomListPayload.Room passwordProtectedRoom;
   private EditBox roomNameInput;
   private EditBox roomPasswordInput;
   private EditBox invitationCodeInput;
   private final RoomCreationOptions creationOptions = new RoomCreationOptions();
   private int dialogLeft;
   private int dialogTop;

   private static int dialogRowTop(int rowIndex) {
      return 36 + 17 * rowIndex;
   }

   private int passwordFieldRow() {
      return this.creationOptions.passwordRow();
   }

   private int dialogButtonTop() {
      return this.activeDialog == RoomLobbyScreen.DialogMode.CREATE ? dialogRowTop(this.passwordFieldRow() + 1) : 152;
   }

   public RoomLobbyScreen(RoomListPayload directorySnapshot) {
      super(Component.translatable("cobblebattle.room.title"));
      this.directorySnapshot = directorySnapshot;
   }

   public void update(RoomListPayload directorySnapshot) {
      this.directorySnapshot = directorySnapshot;
      int maximumFirstRow = Math.max(0, this.roomRowCount() - 3);
      if (this.firstVisibleRoomRow > maximumFirstRow) {
         this.firstVisibleRoomRow = maximumFirstRow;
      }

      Set<String> visibleRoomIds = new HashSet<>();
      Set<String> visibleHostKeys = new HashSet<>();

      for (RoomListPayload.Room roomSummary : directorySnapshot.rooms()) {
         visibleRoomIds.add(roomSummary.id());
         visibleHostKeys.add(hostPortraitKey(roomSummary));
      }

      this.leadModels.keySet().retainAll(visibleRoomIds);
      this.leadAnimationStates.keySet().retainAll(visibleRoomIds);
      this.hostPortraits.keySet().retainAll(visibleHostKeys);
   }

   protected void init() {
      this.lobbyLeft = (this.width - 345) / 2;
      this.lobbyTop = (this.height - 207) / 2;
      this.dialogLeft = (this.width - 191) / 2;
      this.dialogTop = (this.height - 207) / 2;
      String previousFocus = this.roomNameInput == null ? this.suggestedRoomName() : this.roomNameInput.getValue();
      String savedPassword = this.roomPasswordInput == null ? "" : this.roomPasswordInput.getValue();
      this.roomNameInput = new EditBox(this.font, this.dialogLeft + 74, this.dialogTop + 36, 90, 13, Component.empty());
      this.roomNameInput.setMaxLength(24);
      this.roomNameInput.setValue(previousFocus);
      this.roomPasswordInput = new EditBox(this.font, this.dialogLeft + 74, this.dialogTop + dialogRowTop(this.passwordFieldRow()), 90, 13, Component.empty());
      this.roomPasswordInput.setMaxLength(16);
      this.roomPasswordInput.setValue(savedPassword);
      String invitationCode = this.invitationCodeInput == null ? "" : this.invitationCodeInput.getValue();
      this.invitationCodeInput = new EditBox(this.font, this.dialogLeft + 74, this.dialogTop + dialogRowTop(1), 90, 13, Component.empty());
      this.invitationCodeInput.setMaxLength(12);
      this.invitationCodeInput.setValue(invitationCode);
      this.addWidget(this.roomNameInput);
      this.addWidget(this.roomPasswordInput);
      this.addWidget(this.invitationCodeInput);
      this.synchronizeDialogInputs();
   }

   private void synchronizeDialogInputs() {
      boolean isCreatingRoom = this.activeDialog == RoomLobbyScreen.DialogMode.CREATE;
      boolean isPasswordPrompt = this.activeDialog == RoomLobbyScreen.DialogMode.PASSWORD;
      boolean isJoiningByCode = this.activeDialog == RoomLobbyScreen.DialogMode.INVITE;
      this.roomNameInput.setVisible(isCreatingRoom);
      this.roomPasswordInput.setVisible(isCreatingRoom || isPasswordPrompt);
      this.invitationCodeInput.setVisible(isJoiningByCode);
      if (isPasswordPrompt) {
         this.roomPasswordInput.setX(this.dialogLeft + 74);
         this.roomPasswordInput.setY(this.dialogTop + dialogRowTop(1));
         this.roomPasswordInput.setValue("");
      } else {
         this.roomPasswordInput.setX(this.dialogLeft + 74);
         this.roomPasswordInput.setY(this.dialogTop + dialogRowTop(this.passwordFieldRow()));
      }

      this.setFocused(isCreatingRoom ? this.roomNameInput : (isPasswordPrompt ? this.roomPasswordInput : (isJoiningByCode ? this.invitationCodeInput : null)));
   }

   private String suggestedRoomName() {
      Minecraft client = Minecraft.getInstance();
      String profileName = client.player == null ? "" : client.player.getGameProfile().getName();
      return Component.translatable("cobblebattle.room.default_name", new Object[]{profileName}).getString();
   }

   public boolean isPauseScreen() {
      return false;
   }

   public void tick() {
      if (++this.refreshTickCounter % 100 == 0) {
         sendRoomAction(RoomActionPayload.of("list"));
      }
   }

   public void render(GuiGraphics canvas, int pointerX, int pointerY, float frameDelta) {
      super.render(canvas, pointerX, pointerY, frameDelta);
      canvas.blit(LOBBY_CONTENT_TEXTURE, this.lobbyLeft, this.lobbyTop, 0.0F, 0.0F, 345, 207, 345, 207);
      Backdrop.draw(canvas, this.lobbyLeft, this.lobbyTop);
      if (this.activeDialog == RoomLobbyScreen.DialogMode.NONE) {
         int contentLeft = this.lobbyLeft + 18;
         int contentTop = this.lobbyTop + 13;
         canvas.enableScissor(contentLeft, contentTop, contentLeft + 309, contentTop + 183);

         try {
            this.renderLobbyHeader(canvas, pointerX, pointerY);
            this.renderRoomGrid(canvas, pointerX, pointerY, frameDelta);
            this.renderRoomScrollbar(canvas);
            BackButton.draw(canvas, this.font, this.lobbyLeft, this.lobbyTop, pointerX, pointerY);
         } finally {
            canvas.disableScissor();
         }
      }

      canvas.blit(LOBBY_FRAME_TEXTURE, this.lobbyLeft, this.lobbyTop, 0.0F, 0.0F, 345, 207, 345, 207);
      if (this.activeDialog != RoomLobbyScreen.DialogMode.NONE) {
         this.renderActiveDialog(canvas, pointerX, pointerY, frameDelta);
      }
   }

   private void renderLobbyHeader(GuiGraphics canvas, int pointerX, int pointerY) {
      boolean isHovered = this.activeDialog == RoomLobbyScreen.DialogMode.NONE && this.isCreateLinkHovered(pointerX, pointerY);
      int createLinkLeft = this.lobbyLeft + 24;
      int headerLinkTop = this.lobbyTop + 14;
      canvas.fill(createLinkLeft, headerLinkTop, createLinkLeft + 52, headerLinkTop + 10, isHovered ? 1728053247 : 872415231);
      Ui.drawCentered(canvas, this.font, Component.translatable("cobblebattle.room.create"), createLinkLeft + 26, headerLinkTop + 1, -1);
      boolean isInvitationLinkHovered = this.activeDialog == RoomLobbyScreen.DialogMode.NONE && this.isInviteLinkHovered(pointerX, pointerY);
      int invitationLinkLeft = this.lobbyLeft + 80;
      canvas.fill(invitationLinkLeft, headerLinkTop, invitationLinkLeft + 52, headerLinkTop + 10, isInvitationLinkHovered ? 1728053247 : 872415231);
      Ui.drawCentered(canvas, this.font, Component.translatable("cobblebattle.room.invite"), invitationLinkLeft + 26, headerLinkTop + 1, -1);
      Component pageLabel = Component.translatable("cobblebattle.room.title")
         .withStyle(fontStyle -> fontStyle.withFont(CobblemonResources.INSTANCE.getDEFAULT_LARGE()).withBold(true));
      Ui.draw(canvas, this.font, pageLabel, this.lobbyLeft + 322 - Ui.width(this.font, pageLabel), this.lobbyTop + 14, -1, true);
   }

   private void renderRoomGrid(GuiGraphics canvas, int pointerX, int pointerY, float frameDelta) {
      List<RoomListPayload.Room> roomEntries = this.directorySnapshot.rooms();
      if (roomEntries.isEmpty()) {
         Ui.drawCentered(canvas, this.font, Component.translatable("cobblebattle.room.empty"), this.lobbyLeft + 172, this.lobbyTop + 33 + 60, -1770753);
      } else {
         int hoveredRoomIndex = this.activeDialog == RoomLobbyScreen.DialogMode.NONE ? this.roomIndexAt(pointerX, pointerY) : -1;

         for (int visibleSlotIndex = 0; visibleSlotIndex < 6; visibleSlotIndex++) {
            int roomIndex = this.firstVisibleRoomRow * 2 + visibleSlotIndex;
            if (roomIndex >= roomEntries.size()) {
               break;
            }

            this.renderRoomCard(canvas, roomEntries.get(roomIndex), this.roomCardLeft(visibleSlotIndex % 2), this.roomCardTop(visibleSlotIndex / 2), roomIndex == hoveredRoomIndex, frameDelta);
         }
      }
   }

   private void renderRoomCard(GuiGraphics canvas, RoomListPayload.Room roomSummary, int cardLeft, int cardTop, boolean isHovered, float frameDelta) {
      canvas.fill(cardLeft, cardTop, cardLeft + 143, cardTop + 46, isHovered ? -1996488705 : 1728053247);
      canvas.fill(cardLeft, cardTop, cardLeft + 143, cardTop + 1, -1426063361);
      canvas.fill(cardLeft, cardTop + 46 - 1, cardLeft + 143, cardTop + 46, -1426063361);
      canvas.fill(cardLeft, cardTop, cardLeft + 1, cardTop + 46, -1426063361);
      canvas.fill(cardLeft + 143 - 1, cardTop, cardLeft + 143, cardTop + 46, -1426063361);
      int accentColor = roomSummary.mine() ? -2082246 : (roomSummary.locked() ? -868018 : -1);
      canvas.fill(cardLeft + 4, cardTop + 5, cardLeft + 7, cardTop + 46 - 5, accentColor);
      this.renderHostPortraitAndLead(canvas, roomSummary, cardLeft, cardTop, frameDelta);
      int textLeft = cardLeft + 52;
      int textClipRight = cardLeft + 143 - 4;
      canvas.enableScissor(textLeft, cardTop, textClipRight, cardTop + 46);
      Ui.draw(canvas, this.font, roomSummary.name(), textLeft, cardTop + 5, -1, true);
      Ui.draw(canvas, this.font, Component.translatable("cobblebattle.room.host", new Object[]{roomSummary.host()}), textLeft, cardTop + 16, -1770753, false);
      Component battleTypeLabel = Component.translatable("cobblebattle.room.type." + roomSummary.battleType());
      Component levelLabel = roomSummary.level() > 0
         ? Component.translatable("cobblebattle.room.level_at", new Object[]{roomSummary.level()})
         : Component.translatable("cobblebattle.room.level_free");
      List<Component> ruleLabels = new ArrayList<>();
      ruleLabels.add(battleTypeLabel);
      ruleLabels.add(levelLabel);
      if (roomSummary.pick() > 0 && roomSummary.pick() < 6) {
         ruleLabels.add(Component.translatable("cobblebattle.room.pick_tag", new Object[]{roomSummary.pick()}));
      }

      if (roomSummary.hostEngine()) {
         ruleLabels.add(Component.translatable("cobblebattle.room.engine_host_tag"));
      }

      if (!roomSummary.legality()) {
         ruleLabels.add(Component.translatable("cobblebattle.room.legality_tag"));
      }

      if (roomSummary.fighting()) {
         ruleLabels.add(Component.translatable("cobblebattle.room.fighting"));
      } else if (roomSummary.locked()) {
         ruleLabels.add(Component.translatable("cobblebattle.room.needs_password"));
      }

      Component rulesLabel = Ui.join(ruleLabels.toArray(new Component[0]));
      Ui.draw(canvas, this.font, rulesLabel, textLeft, cardTop + 26, -1770753, false);
      Component occupancyLabel = (Component)(roomSummary.mine()
         ? Ui.join(Component.translatable("cobblebattle.room.mine"), Component.translatable("cobblebattle.room.mine_close"))
         : Component.translatable("cobblebattle.room.players", new Object[]{roomSummary.hasGuest() ? 2 : 1, 2, roomSummary.watchers()}));
      Ui.draw(canvas, this.font, occupancyLabel, textLeft, cardTop + 36, -1770753, false);
      canvas.disableScissor();
   }

   private void renderHostPortraitAndLead(GuiGraphics canvas, RoomListPayload.Room roomSummary, int cardLeft, int cardTop, float frameDelta) {
      int figureBaseline = cardTop + 46 - 5;
      canvas.enableScissor(cardLeft + 8, cardTop + 1, cardLeft + 50, cardTop + 46 - 1);

      try {
         PlayerPortrait hostPortrait = this.hostPortraits.computeIfAbsent(
            hostPortraitKey(roomSummary), portraitKey -> PlayerPortrait.lookup(roomSummary.host(), roomSummary.hostUid())
         );
         if (hostPortrait.entity() != null) {
            LeaderboardScreen.drawEntity(canvas, cardLeft + 19, figureBaseline, 15, -35.0F, -10.0F, hostPortrait.entity());
         }

         RenderablePokemon leadModel = this.leadModelFor(roomSummary);
         if (leadModel != null) {
            FloatingState animationState = this.leadAnimationStates.computeIfAbsent(roomSummary.id(), roomId -> new FloatingState());
            canvas.pose().pushPose();
            canvas.pose().translate(cardLeft + 38, figureBaseline, 0.0);
            Quaternionf modelRotation = QuaternionUtilsKt.fromEulerXYZDegrees(new Quaternionf(), new Vector3f(5.0F, LeaderboardScreen.FACING, 0.0F));
            float modelHeight = Math.max(0.1F, leadModel.getForm().getHitbox().height());
            float modelScale = Math.min(15.0F, 36.0F / modelHeight);
            CobblemonCompat.drawProfile(
               leadModel, canvas.pose(), modelRotation, PoseType.PROFILE, animationState, frameDelta, modelScale, true, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F, 0.0F, 13
            );
            canvas.pose().popPose();
         } else {
            UnknownMark.draw(canvas, cardLeft + 38, figureBaseline, 26);
         }
      } finally {
         canvas.disableScissor();
      }
   }

   private static String hostPortraitKey(RoomListPayload.Room roomSummary) {
      return roomSummary.hostUid() + ":" + roomSummary.host();
   }

   private RenderablePokemon leadModelFor(RoomListPayload.Room roomSummary) {
      return roomSummary.lead().isEmpty() ? null : this.leadModels.computeIfAbsent(roomSummary.id(), roomId -> {
         Species speciesTemplate = PokemonSpecies.getByName(roomSummary.lead());
         return speciesTemplate == null ? null : new RenderablePokemon(speciesTemplate, Set.of(), ItemStack.EMPTY);
      });
   }

   private void renderRoomScrollbar(GuiGraphics canvas) {
      int totalRows = this.roomRowCount();
      if (totalRows > 3) {
         int trackTop = this.lobbyTop + 33;
         int trackHeight = 148;
         int scrollbarLeft = this.lobbyLeft + 320;
         canvas.fill(scrollbarLeft, trackTop, scrollbarLeft + 3, trackTop + trackHeight, -12937546);
         int thumbHeight = Math.max(12, trackHeight * 3 / totalRows);
         int thumbTravel = trackHeight - thumbHeight;
         int thumbTop = trackTop + (totalRows - 3 == 0 ? 0 : thumbTravel * this.firstVisibleRoomRow / (totalRows - 3));
         canvas.fill(scrollbarLeft, thumbTop, scrollbarLeft + 3, thumbTop + thumbHeight, -197380);
      }
   }

   private void renderActiveDialog(GuiGraphics canvas, int pointerX, int pointerY, float frameDelta) {
      canvas.fill(0, 0, this.width, this.height, -1728053248);
      canvas.blit(DIALOG_BACKGROUND_TEXTURE, this.dialogLeft, this.dialogTop, 0.0F, 0.0F, 191, 207, 191, 207);
      boolean isCreatingRoom = this.activeDialog == RoomLobbyScreen.DialogMode.CREATE;
      boolean isJoiningByCode = this.activeDialog == RoomLobbyScreen.DialogMode.INVITE;
      Ui.drawCentered(
         canvas,
         this.font,
         Component.translatable(
            isCreatingRoom ? "cobblebattle.room.create_title" : (isJoiningByCode ? "cobblebattle.room.invite_title" : "cobblebattle.room.password_title")
         ),
         this.dialogLeft + 95,
         this.dialogTop + 17,
         -1
      );
      if (isJoiningByCode) {
         Ui.drawCentered(canvas, this.font, Component.translatable("cobblebattle.room.invite_hint"), this.dialogLeft + 95, this.dialogTop + dialogRowTop(0), -1770753);
         this.renderDialogLabel(canvas, "cobblebattle.room.invite", dialogRowTop(1));
      } else if (isCreatingRoom) {
         this.renderDialogLabel(canvas, "cobblebattle.room.name", dialogRowTop(0));
         this.renderDialogLabel(canvas, "cobblebattle.room.type", dialogRowTop(1));
         this.renderDialogLabel(canvas, "cobblebattle.room.pick", dialogRowTop(2));
         this.renderDialogLabel(canvas, "cobblebattle.room.level", dialogRowTop(3));
         this.renderDialogLabel(canvas, "cobblebattle.room.heal", dialogRowTop(4));
         this.renderDialogLabel(canvas, "cobblebattle.room.engine", dialogRowTop(5));
         if (this.creationOptions.hostEngine()) {
            this.renderDialogLabel(canvas, "cobblebattle.room.legality", dialogRowTop(6));
         }

         this.renderDialogLabel(canvas, "cobblebattle.room.password", dialogRowTop(this.passwordFieldRow()));
         this.renderPicker(canvas, pointerX, pointerY, dialogRowTop(1), Component.translatable("cobblebattle.room.type." + this.creationOptions.battleType()));
         this.renderPicker(canvas, pointerX, pointerY, dialogRowTop(2), Component.translatable("cobblebattle.room.pick_of", new Object[]{this.creationOptions.pick()}));
         this.renderPicker(
            canvas,
            pointerX,
            pointerY,
            dialogRowTop(3),
            this.creationOptions.level() > 0
               ? Component.translatable("cobblebattle.room.level_at", new Object[]{this.creationOptions.level()})
               : Component.translatable("cobblebattle.room.level_free")
         );
         this.renderPicker(canvas, pointerX, pointerY, dialogRowTop(4), Component.translatable(this.creationOptions.fullHeal() ? "cobblebattle.room.heal_on" : "cobblebattle.room.heal_off"));
         this.renderPicker(
            canvas, pointerX, pointerY, dialogRowTop(5), Component.translatable(this.creationOptions.hostEngine() ? "cobblebattle.room.engine_host" : "cobblebattle.room.engine_server")
         );
         if (this.creationOptions.hostEngine()) {
            this.renderPicker(
               canvas,
               pointerX,
               pointerY,
               dialogRowTop(6),
               Component.translatable(this.creationOptions.legality() ? "cobblebattle.room.legality_on" : "cobblebattle.room.legality_off")
            );
         }
      } else {
         Ui.drawCentered(canvas, this.font, this.passwordProtectedRoom == null ? "" : this.passwordProtectedRoom.name(), this.dialogLeft + 95, this.dialogTop + dialogRowTop(0), -1770753);
         this.renderDialogLabel(canvas, "cobblebattle.room.password", dialogRowTop(1));
      }

      this.renderDialogButton(
         canvas,
         pointerX,
         pointerY,
         this.dialogLeft + 20 + 6,
         this.dialogTop + this.dialogButtonTop(),
         Component.translatable(isCreatingRoom ? "cobblebattle.room.confirm" : "cobblebattle.room.join")
      );
      this.renderDialogButton(canvas, pointerX, pointerY, this.dialogLeft + 170 - 6 - 64, this.dialogTop + this.dialogButtonTop(), Component.translatable("cobblebattle.back"));
      if (isCreatingRoom) {
         this.roomNameInput.render(canvas, pointerX, pointerY, frameDelta);
      }

      if (isJoiningByCode) {
         this.invitationCodeInput.render(canvas, pointerX, pointerY, frameDelta);
      } else {
         this.roomPasswordInput.render(canvas, pointerX, pointerY, frameDelta);
      }
   }

   private void renderDialogLabel(GuiGraphics canvas, String translationKey, int rowTopOffset) {
      Ui.draw(canvas, this.font, Component.translatable(translationKey), this.dialogLeft + 26, this.dialogTop + rowTopOffset + 3, -1, true);
   }

   private void renderPicker(GuiGraphics canvas, int pointerX, int pointerY, int rowTopOffset, Component displayValue) {
      int pickerLeft = this.dialogLeft + 74;
      int pickerRight = pickerLeft + 90 - 11;
      this.renderControlBox(canvas, pointerX, pointerY, pickerLeft, this.dialogTop + rowTopOffset, 11, 13, Component.literal("<"));
      this.renderControlBox(canvas, pointerX, pointerY, pickerRight, this.dialogTop + rowTopOffset, 11, 13, Component.literal(">"));
      canvas.enableScissor(pickerLeft + 11 + 1, this.dialogTop + rowTopOffset, pickerRight - 1, this.dialogTop + rowTopOffset + 13);
      Ui.drawCentered(canvas, this.font, displayValue, pickerLeft + 45, this.dialogTop + rowTopOffset + 3, -1);
      canvas.disableScissor();
   }

   private void renderDialogButton(GuiGraphics canvas, int pointerX, int pointerY, int buttonLeft, int buttonTop, Component caption) {
      this.renderControlBox(canvas, pointerX, pointerY, buttonLeft, buttonTop, 64, 14, caption);
   }

   private void renderControlBox(
      GuiGraphics canvas, int pointerX, int pointerY, int controlLeft, int controlTop, int controlWidth, int controlHeight, Component caption
   ) {
      boolean isHovered = pointerX >= controlLeft && pointerX < controlLeft + controlWidth && pointerY >= controlTop && pointerY < controlTop + controlHeight;
      canvas.fill(controlLeft, controlTop, controlLeft + controlWidth, controlTop + controlHeight, isHovered ? 1728053247 : 872415231);
      canvas.fill(controlLeft, controlTop, controlLeft + controlWidth, controlTop + 1, -1996488705);
      canvas.fill(controlLeft, controlTop + controlHeight - 1, controlLeft + controlWidth, controlTop + controlHeight, -1996488705);
      canvas.fill(controlLeft, controlTop, controlLeft + 1, controlTop + controlHeight, -1996488705);
      canvas.fill(controlLeft + controlWidth - 1, controlTop, controlLeft + controlWidth, controlTop + controlHeight, -1996488705);
      Ui.drawCentered(canvas, this.font, caption, controlLeft + controlWidth / 2, controlTop + (controlHeight - 8) / 2, isHovered ? -1 : -1770753);
   }

   private void openCreationDialog() {
      this.activeDialog = RoomLobbyScreen.DialogMode.CREATE;
      this.roomNameInput.setValue(this.suggestedRoomName());
      this.synchronizeDialogInputs();
   }

   private void openPasswordDialog(RoomListPayload.Room roomSummary) {
      this.activeDialog = RoomLobbyScreen.DialogMode.PASSWORD;
      this.passwordProtectedRoom = roomSummary;
      this.synchronizeDialogInputs();
   }

   private void openInvitationDialog() {
      this.activeDialog = RoomLobbyScreen.DialogMode.INVITE;
      this.invitationCodeInput.setValue("");
      this.synchronizeDialogInputs();
   }

   private void closeDialog() {
      this.activeDialog = RoomLobbyScreen.DialogMode.NONE;
      this.passwordProtectedRoom = null;
      this.synchronizeDialogInputs();
   }

   private void submitActiveDialog() {
      if (this.activeDialog == RoomLobbyScreen.DialogMode.CREATE) {
         RoomCreationRequest creationRequest = this.creationOptions.createRequest(
            this.roomNameInput.getValue(), this.roomPasswordInput.getValue(), this.suggestedRoomName()
         );
         sendRoomAction(
            new RoomActionPayload(
               "create",
               "",
               creationRequest.getName(),
               creationRequest.getPassword(),
               creationRequest.getBattleType(),
               creationRequest.getLevel(),
               creationRequest.getPick(),
               creationRequest.getFullHeal(),
               creationRequest.getHostEngine(),
               creationRequest.getLegality(),
               ""
            )
         );
      } else if (this.activeDialog == RoomLobbyScreen.DialogMode.PASSWORD && this.passwordProtectedRoom != null) {
         sendRoomAction(
            RoomActionPayload.join(
               this.passwordProtectedRoom.id(),
               this.roomPasswordInput.getValue(),
               RoomLobbyRules.seatType(this.passwordProtectedRoom.fighting(), this.passwordProtectedRoom.battleType()),
               this.passwordProtectedRoom.hostEngine(),
               this.passwordProtectedRoom.legality()
            )
         );
      } else if (this.activeDialog == RoomLobbyScreen.DialogMode.INVITE) {
         String invitationCode = RoomLobbyRules.invitationCode(this.invitationCodeInput.getValue());
         if (invitationCode == null) {
            return;
         }

         sendRoomAction(RoomActionPayload.joinByCode(invitationCode));
      }

      this.closeDialog();
   }

   private int roomRowCount() {
      return (this.directorySnapshot.rooms().size() + 2 - 1) / 2;
   }

   private int roomCardLeft(int columnIndex) {
      return this.lobbyLeft + 25 + columnIndex * 151;
   }

   private int roomCardTop(int rowIndex) {
      return this.lobbyTop + 33 + rowIndex * 51;
   }

   private int roomIndexAt(double pointerX, double pointerY) {
      for (int visibleSlotIndex = 0; visibleSlotIndex < 6; visibleSlotIndex++) {
         int cardLeft = this.roomCardLeft(visibleSlotIndex % 2);
         int cardTop = this.roomCardTop(visibleSlotIndex / 2);
         if (pointerX >= cardLeft && pointerX < cardLeft + 143 && pointerY >= cardTop && pointerY < cardTop + 46) {
            int roomIndex = this.firstVisibleRoomRow * 2 + visibleSlotIndex;
            return roomIndex < this.directorySnapshot.rooms().size() ? roomIndex : -1;
         }
      }

      return -1;
   }

   private boolean isCreateLinkHovered(double pointerX, double pointerY) {
      int linkLeft = this.lobbyLeft + 24;
      int linkTop = this.lobbyTop + 14;
      return pointerX >= linkLeft && pointerX < linkLeft + 52 && pointerY >= linkTop && pointerY < linkTop + 10;
   }

   private boolean isInviteLinkHovered(double pointerX, double pointerY) {
      int linkLeft = this.lobbyLeft + 80;
      int linkTop = this.lobbyTop + 14;
      return pointerX >= linkLeft && pointerX < linkLeft + 52 && pointerY >= linkTop && pointerY < linkTop + 10;
   }

   public boolean mouseClicked(double pointerX, double pointerY, int mouseButton) {
      if (this.activeDialog != RoomLobbyScreen.DialogMode.NONE) {
         return this.handleDialogClick(pointerX, pointerY, mouseButton);
      } else {
         if (mouseButton == 0) {
            if (BackButton.contains(this.lobbyLeft, this.lobbyTop, pointerX, pointerY)) {
               ServerDex.requestMain();
               return true;
            }

            if (this.isCreateLinkHovered(pointerX, pointerY)) {
               this.openCreationDialog();
               return true;
            }

            if (this.isInviteLinkHovered(pointerX, pointerY)) {
               this.openInvitationDialog();
               return true;
            }

            int selectedRoomIndex = this.roomIndexAt(pointerX, pointerY);
            if (selectedRoomIndex >= 0) {
               RoomListPayload.Room roomSummary = this.directorySnapshot.rooms().get(selectedRoomIndex);
               if (roomSummary.locked() && !roomSummary.mine()) {
                  this.openPasswordDialog(roomSummary);
               } else {
                  sendRoomAction(RoomActionPayload.join(roomSummary.id(), "", RoomLobbyRules.seatType(roomSummary.fighting(), roomSummary.battleType()), roomSummary.hostEngine(), roomSummary.legality()));
               }

               return true;
            }
         }

         return super.mouseClicked(pointerX, pointerY, mouseButton);
      }
   }

   private boolean handleDialogClick(double pointerX, double pointerY, int mouseButton) {
      if (mouseButton == 0) {
         if (this.activeDialog == RoomLobbyScreen.DialogMode.CREATE) {
            int pickerLeft = this.dialogLeft + 74;
            int pickerRight = pickerLeft + 90 - 11;
            int typeRowTop = this.dialogTop + dialogRowTop(1);
            int pickRowTop = this.dialogTop + dialogRowTop(2);
            int levelRowTop = this.dialogTop + dialogRowTop(3);
            int healRowTop = this.dialogTop + dialogRowTop(4);
            int engineRowTop = this.dialogTop + dialogRowTop(5);
            if (containsPoint(pointerX, pointerY, pickerLeft, typeRowTop, 11, 13)) {
               this.creationOptions.cycleBattleType(-1);
               return true;
            }

            if (containsPoint(pointerX, pointerY, pickerRight, typeRowTop, 11, 13)) {
               this.creationOptions.cycleBattleType(1);
               return true;
            }

            if (containsPoint(pointerX, pointerY, pickerLeft, pickRowTop, 11, 13)) {
               this.creationOptions.cyclePick(-1);
               return true;
            }

            if (containsPoint(pointerX, pointerY, pickerRight, pickRowTop, 11, 13)) {
               this.creationOptions.cyclePick(1);
               return true;
            }

            if (containsPoint(pointerX, pointerY, pickerLeft, levelRowTop, 11, 13)) {
               this.creationOptions.cycleLevel(-1);
               return true;
            }

            if (containsPoint(pointerX, pointerY, pickerRight, levelRowTop, 11, 13)) {
               this.creationOptions.cycleLevel(1);
               return true;
            }

            if (containsPoint(pointerX, pointerY, pickerLeft, healRowTop, 11, 13) || containsPoint(pointerX, pointerY, pickerRight, healRowTop, 11, 13)) {
               this.creationOptions.toggleFullHeal();
               return true;
            }

            if (containsPoint(pointerX, pointerY, pickerLeft, engineRowTop, 11, 13) || containsPoint(pointerX, pointerY, pickerRight, engineRowTop, 11, 13)) {
               this.creationOptions.toggleHostEngine();
               this.synchronizeDialogInputs();
               return true;
            }

            if (this.creationOptions.hostEngine()) {
               int legalityRowTop = this.dialogTop + dialogRowTop(6);
               if (containsPoint(pointerX, pointerY, pickerLeft, legalityRowTop, 11, 13) || containsPoint(pointerX, pointerY, pickerRight, legalityRowTop, 11, 13)) {
                  this.creationOptions.toggleLegality();
                  return true;
               }
            }
         }

         if (containsPoint(pointerX, pointerY, this.dialogLeft + 20 + 6, this.dialogTop + this.dialogButtonTop(), 64, 14)) {
            this.submitActiveDialog();
            return true;
         }

         if (containsPoint(pointerX, pointerY, this.dialogLeft + 170 - 6 - 64, this.dialogTop + this.dialogButtonTop(), 64, 14)) {
            this.closeDialog();
            return true;
         }
      }

      return super.mouseClicked(pointerX, pointerY, mouseButton);
   }

   private static boolean containsPoint(double pointX, double pointY, int regionLeft, int regionTop, int regionWidth, int regionHeight) {
      return pointX >= regionLeft && pointX < regionLeft + regionWidth && pointY >= regionTop && pointY < regionTop + regionHeight;
   }

   public boolean keyPressed(int inputKeyCode, int hardwareScanCode, int keyModifiers) {
      if (this.activeDialog != RoomLobbyScreen.DialogMode.NONE) {
         if (inputKeyCode == 256) {
            this.closeDialog();
            return true;
         }

         if (inputKeyCode == 257 || inputKeyCode == 335) {
            this.submitActiveDialog();
            return true;
         }
      }

      return super.keyPressed(inputKeyCode, hardwareScanCode, keyModifiers);
   }

   public boolean mouseScrolled(double pointerX, double pointerY, double horizontalScrollAmount, double verticalScrollAmount) {
      if (this.activeDialog != RoomLobbyScreen.DialogMode.NONE) {
         return true;
      } else {
         int maximumFirstRow = Math.max(0, this.roomRowCount() - 3);
         this.firstVisibleRoomRow = Math.max(0, Math.min(maximumFirstRow, this.firstVisibleRoomRow - (int)Math.signum(verticalScrollAmount)));
         return true;
      }
   }

   static void sendRoomAction(RoomActionPayload roomAction) {
      NetworkManager.sendToServer(roomAction);
   }

   private static enum DialogMode {
      NONE,
      CREATE,
      PASSWORD,
      INVITE;
   }
}
