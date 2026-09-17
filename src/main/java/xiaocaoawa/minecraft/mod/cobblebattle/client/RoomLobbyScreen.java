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
   private static final int SCROLLOFFSETS_X = 320;
   private static final int SCROLLOFFSETS_W = 3;
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
   private RoomLobbyScreen.Form activeDialog = RoomLobbyScreen.Form.NONE;
   private RoomListPayload.Room passwordProtectedRoom;
   private EditBox roomNameInput;
   private EditBox roomPasswordInput;
   private EditBox invitationCodeInput;
   private final RoomCreationOptions creationOptions = new RoomCreationOptions();
   private int dialogLeft;
   private int dialogTop;

   private static int row(int index) {
      return 36 + 17 * index;
   }

   private int passwordRow() {
      return this.creationOptions.passwordRow();
   }

   private int buttonY() {
      return this.activeDialog == RoomLobbyScreen.Form.CREATE ? row(this.passwordRow() + 1) : 152;
   }

   public RoomLobbyScreen(RoomListPayload directorySnapshot) {
      super(Component.translatable("cobblebattle.room.title"));
      this.directorySnapshot = directorySnapshot;
   }

   public void update(RoomListPayload directorySnapshot) {
      this.directorySnapshot = directorySnapshot;
      int max = Math.max(0, this.rowCount() - 3);
      if (this.firstVisibleRoomRow > max) {
         this.firstVisibleRoomRow = max;
      }

      Set<String> live = new HashSet<>();
      Set<String> hosts = new HashSet<>();

      for (RoomListPayload.Room room : directorySnapshot.rooms()) {
         live.add(room.id());
         hosts.add(hostKey(room));
      }

      this.leadModels.keySet().retainAll(live);
      this.leadAnimationStates.keySet().retainAll(live);
      this.hostPortraits.keySet().retainAll(hosts);
   }

   protected void init() {
      this.lobbyLeft = (this.width - 345) / 2;
      this.lobbyTop = (this.height - 207) / 2;
      this.dialogLeft = (this.width - 191) / 2;
      this.dialogTop = (this.height - 207) / 2;
      String was = this.roomNameInput == null ? this.defaultName() : this.roomNameInput.getValue();
      String password = this.roomPasswordInput == null ? "" : this.roomPasswordInput.getValue();
      this.roomNameInput = new EditBox(this.font, this.dialogLeft + 74, this.dialogTop + 36, 90, 13, Component.empty());
      this.roomNameInput.setMaxLength(24);
      this.roomNameInput.setValue(was);
      this.roomPasswordInput = new EditBox(this.font, this.dialogLeft + 74, this.dialogTop + row(this.passwordRow()), 90, 13, Component.empty());
      this.roomPasswordInput.setMaxLength(16);
      this.roomPasswordInput.setValue(password);
      String code = this.invitationCodeInput == null ? "" : this.invitationCodeInput.getValue();
      this.invitationCodeInput = new EditBox(this.font, this.dialogLeft + 74, this.dialogTop + row(1), 90, 13, Component.empty());
      this.invitationCodeInput.setMaxLength(12);
      this.invitationCodeInput.setValue(code);
      this.addWidget(this.roomNameInput);
      this.addWidget(this.roomPasswordInput);
      this.addWidget(this.invitationCodeInput);
      this.applyForm();
   }

   private void applyForm() {
      boolean creating = this.activeDialog == RoomLobbyScreen.Form.CREATE;
      boolean asking = this.activeDialog == RoomLobbyScreen.Form.PASSWORD;
      boolean inviting = this.activeDialog == RoomLobbyScreen.Form.INVITE;
      this.roomNameInput.setVisible(creating);
      this.roomPasswordInput.setVisible(creating || asking);
      this.invitationCodeInput.setVisible(inviting);
      if (asking) {
         this.roomPasswordInput.setX(this.dialogLeft + 74);
         this.roomPasswordInput.setY(this.dialogTop + row(1));
         this.roomPasswordInput.setValue("");
      } else {
         this.roomPasswordInput.setX(this.dialogLeft + 74);
         this.roomPasswordInput.setY(this.dialogTop + row(this.passwordRow()));
      }

      this.setFocused(creating ? this.roomNameInput : (asking ? this.roomPasswordInput : (inviting ? this.invitationCodeInput : null)));
   }

   private String defaultName() {
      Minecraft minecraft = Minecraft.getInstance();
      String who = minecraft.player == null ? "" : minecraft.player.getGameProfile().getName();
      return Component.translatable("cobblebattle.room.default_name", new Object[]{who}).getString();
   }

   public boolean isPauseScreen() {
      return false;
   }

   public void tick() {
      if (++this.refreshTickCounter % 100 == 0) {
         send(RoomActionPayload.of("list"));
      }
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      super.render(graphics, mouseX, mouseY, partialTick);
      graphics.blit(LOBBY_CONTENT_TEXTURE, this.lobbyLeft, this.lobbyTop, 0.0F, 0.0F, 345, 207, 345, 207);
      Backdrop.draw(graphics, this.lobbyLeft, this.lobbyTop);
      if (this.activeDialog == RoomLobbyScreen.Form.NONE) {
         int left = this.lobbyLeft + 18;
         int top = this.lobbyTop + 13;
         graphics.enableScissor(left, top, left + 309, top + 183);

         try {
            this.drawHeader(graphics, mouseX, mouseY);
            this.drawCards(graphics, mouseX, mouseY, partialTick);
            this.drawScrollbar(graphics);
            BackButton.draw(graphics, this.font, this.lobbyLeft, this.lobbyTop, mouseX, mouseY);
         } finally {
            graphics.disableScissor();
         }
      }

      graphics.blit(LOBBY_FRAME_TEXTURE, this.lobbyLeft, this.lobbyTop, 0.0F, 0.0F, 345, 207, 345, 207);
      if (this.activeDialog != RoomLobbyScreen.Form.NONE) {
         this.drawForm(graphics, mouseX, mouseY, partialTick);
      }
   }

   private void drawHeader(GuiGraphics graphics, int mouseX, int mouseY) {
      boolean hover = this.activeDialog == RoomLobbyScreen.Form.NONE && this.inCreate(mouseX, mouseY);
      int x = this.lobbyLeft + 24;
      int y = this.lobbyTop + 14;
      graphics.fill(x, y, x + 52, y + 10, hover ? 1728053247 : 872415231);
      Ui.drawCentered(graphics, this.font, Component.translatable("cobblebattle.room.create"), x + 26, y + 1, -1);
      boolean inviteHover = this.activeDialog == RoomLobbyScreen.Form.NONE && this.inInvite(mouseX, mouseY);
      int ix = this.lobbyLeft + 80;
      graphics.fill(ix, y, ix + 52, y + 10, inviteHover ? 1728053247 : 872415231);
      Ui.drawCentered(graphics, this.font, Component.translatable("cobblebattle.room.invite"), ix + 26, y + 1, -1);
      Component page = Component.translatable("cobblebattle.room.title")
         .withStyle(style -> style.withFont(CobblemonResources.INSTANCE.getDEFAULT_LARGE()).withBold(true));
      Ui.draw(graphics, this.font, page, this.lobbyLeft + 322 - Ui.width(this.font, page), this.lobbyTop + 14, -1, true);
   }

   private void drawCards(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      List<RoomListPayload.Room> rooms = this.directorySnapshot.rooms();
      if (rooms.isEmpty()) {
         Ui.drawCentered(graphics, this.font, Component.translatable("cobblebattle.room.empty"), this.lobbyLeft + 172, this.lobbyTop + 33 + 60, -1770753);
      } else {
         int hovered = this.activeDialog == RoomLobbyScreen.Form.NONE ? this.cardAt(mouseX, mouseY) : -1;

         for (int slot = 0; slot < 6; slot++) {
            int index = this.firstVisibleRoomRow * 2 + slot;
            if (index >= rooms.size()) {
               break;
            }

            this.drawCard(graphics, rooms.get(index), this.cardX(slot % 2), this.cardY(slot / 2), index == hovered, partialTick);
         }
      }
   }

   private void drawCard(GuiGraphics graphics, RoomListPayload.Room room, int x, int y, boolean hover, float partialTick) {
      graphics.fill(x, y, x + 143, y + 46, hover ? -1996488705 : 1728053247);
      graphics.fill(x, y, x + 143, y + 1, -1426063361);
      graphics.fill(x, y + 46 - 1, x + 143, y + 46, -1426063361);
      graphics.fill(x, y, x + 1, y + 46, -1426063361);
      graphics.fill(x + 143 - 1, y, x + 143, y + 46, -1426063361);
      int bar = room.mine() ? -2082246 : (room.locked() ? -868018 : -1);
      graphics.fill(x + 4, y + 5, x + 7, y + 46 - 5, bar);
      this.drawHostAndLead(graphics, room, x, y, partialTick);
      int textX = x + 52;
      int textRight = x + 143 - 4;
      graphics.enableScissor(textX, y, textRight, y + 46);
      Ui.draw(graphics, this.font, room.name(), textX, y + 5, -1, true);
      Ui.draw(graphics, this.font, Component.translatable("cobblebattle.room.host", new Object[]{room.host()}), textX, y + 16, -1770753, false);
      Component type = Component.translatable("cobblebattle.room.type." + room.battleType());
      Component level = room.level() > 0
         ? Component.translatable("cobblebattle.room.level_at", new Object[]{room.level()})
         : Component.translatable("cobblebattle.room.level_free");
      List<Component> parts = new ArrayList<>();
      parts.add(type);
      parts.add(level);
      if (room.pick() > 0 && room.pick() < 6) {
         parts.add(Component.translatable("cobblebattle.room.pick_tag", new Object[]{room.pick()}));
      }

      if (room.hostEngine()) {
         parts.add(Component.translatable("cobblebattle.room.engine_host_tag"));
      }

      if (!room.legality()) {
         parts.add(Component.translatable("cobblebattle.room.legality_tag"));
      }

      if (room.fighting()) {
         parts.add(Component.translatable("cobblebattle.room.fighting"));
      } else if (room.locked()) {
         parts.add(Component.translatable("cobblebattle.room.needs_password"));
      }

      Component rules = Ui.join(parts.toArray(new Component[0]));
      Ui.draw(graphics, this.font, rules, textX, y + 26, -1770753, false);
      Component players = (Component)(room.mine()
         ? Ui.join(Component.translatable("cobblebattle.room.mine"), Component.translatable("cobblebattle.room.mine_close"))
         : Component.translatable("cobblebattle.room.players", new Object[]{room.hasGuest() ? 2 : 1, 2, room.watchers()}));
      Ui.draw(graphics, this.font, players, textX, y + 36, -1770753, false);
      graphics.disableScissor();
   }

   private void drawHostAndLead(GuiGraphics graphics, RoomListPayload.Room room, int x, int y, float partialTick) {
      int floorY = y + 46 - 5;
      graphics.enableScissor(x + 8, y + 1, x + 50, y + 46 - 1);

      try {
         PlayerPortrait portrait = this.hostPortraits.computeIfAbsent(hostKey(room), key -> PlayerPortrait.lookup(room.host(), room.hostUid()));
         if (portrait.entity() != null) {
            LeaderboardScreen.drawEntity(graphics, x + 19, floorY, 15, -35.0F, -10.0F, portrait.entity());
         }

         RenderablePokemon lead = this.leadOf(room);
         if (lead != null) {
            FloatingState state = this.leadAnimationStates.computeIfAbsent(room.id(), id -> new FloatingState());
            graphics.pose().pushPose();
            graphics.pose().translate(x + 38, floorY, 0.0);
            Quaternionf rotation = QuaternionUtilsKt.fromEulerXYZDegrees(new Quaternionf(), new Vector3f(5.0F, LeaderboardScreen.FACING, 0.0F));
            float blocks = Math.max(0.1F, lead.getForm().getHitbox().height());
            float scale = Math.min(15.0F, 36.0F / blocks);
            CobblemonCompat.drawProfile(
               lead, graphics.pose(), rotation, PoseType.PROFILE, state, partialTick, scale, true, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F, 0.0F, 13
            );
            graphics.pose().popPose();
         } else {
            UnknownMark.draw(graphics, x + 38, floorY, 26);
         }
      } finally {
         graphics.disableScissor();
      }
   }

   private static String hostKey(RoomListPayload.Room room) {
      return room.hostUid() + ":" + room.host();
   }

   private RenderablePokemon leadOf(RoomListPayload.Room room) {
      return room.lead().isEmpty() ? null : this.leadModels.computeIfAbsent(room.id(), id -> {
         Species speciesTemplate = PokemonSpecies.getByName(room.lead());
         return speciesTemplate == null ? null : new RenderablePokemon(speciesTemplate, Set.of(), ItemStack.EMPTY);
      });
   }

   private void drawScrollbar(GuiGraphics graphics) {
      int rows = this.rowCount();
      if (rows > 3) {
         int top = this.lobbyTop + 33;
         int height = 148;
         int x = this.lobbyLeft + 320;
         graphics.fill(x, top, x + 3, top + height, -12937546);
         int thumb = Math.max(12, height * 3 / rows);
         int travel = height - thumb;
         int at = top + (rows - 3 == 0 ? 0 : travel * this.firstVisibleRoomRow / (rows - 3));
         graphics.fill(x, at, x + 3, at + thumb, -197380);
      }
   }

   private void drawForm(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      graphics.fill(0, 0, this.width, this.height, -1728053248);
      graphics.blit(DIALOG_BACKGROUND_TEXTURE, this.dialogLeft, this.dialogTop, 0.0F, 0.0F, 191, 207, 191, 207);
      boolean creating = this.activeDialog == RoomLobbyScreen.Form.CREATE;
      boolean inviting = this.activeDialog == RoomLobbyScreen.Form.INVITE;
      Ui.drawCentered(
         graphics,
         this.font,
         Component.translatable(
            creating ? "cobblebattle.room.create_title" : (inviting ? "cobblebattle.room.invite_title" : "cobblebattle.room.password_title")
         ),
         this.dialogLeft + 95,
         this.dialogTop + 17,
         -1
      );
      if (inviting) {
         Ui.drawCentered(graphics, this.font, Component.translatable("cobblebattle.room.invite_hint"), this.dialogLeft + 95, this.dialogTop + row(0), -1770753);
         this.label(graphics, "cobblebattle.room.invite", row(1));
      } else if (creating) {
         this.label(graphics, "cobblebattle.room.name", row(0));
         this.label(graphics, "cobblebattle.room.type", row(1));
         this.label(graphics, "cobblebattle.room.pick", row(2));
         this.label(graphics, "cobblebattle.room.level", row(3));
         this.label(graphics, "cobblebattle.room.heal", row(4));
         this.label(graphics, "cobblebattle.room.engine", row(5));
         if (this.creationOptions.hostEngine()) {
            this.label(graphics, "cobblebattle.room.legality", row(6));
         }

         this.label(graphics, "cobblebattle.room.password", row(this.passwordRow()));
         this.picker(graphics, mouseX, mouseY, row(1), Component.translatable("cobblebattle.room.type." + this.creationOptions.battleType()));
         this.picker(graphics, mouseX, mouseY, row(2), Component.translatable("cobblebattle.room.pick_of", new Object[]{this.creationOptions.pick()}));
         this.picker(
            graphics,
            mouseX,
            mouseY,
            row(3),
            this.creationOptions.level() > 0
               ? Component.translatable("cobblebattle.room.level_at", new Object[]{this.creationOptions.level()})
               : Component.translatable("cobblebattle.room.level_free")
         );
         this.picker(graphics, mouseX, mouseY, row(4), Component.translatable(this.creationOptions.fullHeal() ? "cobblebattle.room.heal_on" : "cobblebattle.room.heal_off"));
         this.picker(
            graphics, mouseX, mouseY, row(5), Component.translatable(this.creationOptions.hostEngine() ? "cobblebattle.room.engine_host" : "cobblebattle.room.engine_server")
         );
         if (this.creationOptions.hostEngine()) {
            this.picker(
               graphics,
               mouseX,
               mouseY,
               row(6),
               Component.translatable(this.creationOptions.legality() ? "cobblebattle.room.legality_on" : "cobblebattle.room.legality_off")
            );
         }
      } else {
         Ui.drawCentered(graphics, this.font, this.passwordProtectedRoom == null ? "" : this.passwordProtectedRoom.name(), this.dialogLeft + 95, this.dialogTop + row(0), -1770753);
         this.label(graphics, "cobblebattle.room.password", row(1));
      }

      this.button(
         graphics,
         mouseX,
         mouseY,
         this.dialogLeft + 20 + 6,
         this.dialogTop + this.buttonY(),
         Component.translatable(creating ? "cobblebattle.room.confirm" : "cobblebattle.room.join")
      );
      this.button(graphics, mouseX, mouseY, this.dialogLeft + 170 - 6 - 64, this.dialogTop + this.buttonY(), Component.translatable("cobblebattle.back"));
      if (creating) {
         this.roomNameInput.render(graphics, mouseX, mouseY, partialTick);
      }

      if (inviting) {
         this.invitationCodeInput.render(graphics, mouseX, mouseY, partialTick);
      } else {
         this.roomPasswordInput.render(graphics, mouseX, mouseY, partialTick);
      }
   }

   private void label(GuiGraphics graphics, String key, int y) {
      Ui.draw(graphics, this.font, Component.translatable(key), this.dialogLeft + 26, this.dialogTop + y + 3, -1, true);
   }

   private void picker(GuiGraphics graphics, int mouseX, int mouseY, int y, Component value) {
      int left = this.dialogLeft + 74;
      int right = left + 90 - 11;
      this.box(graphics, mouseX, mouseY, left, this.dialogTop + y, 11, 13, Component.literal("<"));
      this.box(graphics, mouseX, mouseY, right, this.dialogTop + y, 11, 13, Component.literal(">"));
      graphics.enableScissor(left + 11 + 1, this.dialogTop + y, right - 1, this.dialogTop + y + 13);
      Ui.drawCentered(graphics, this.font, value, left + 45, this.dialogTop + y + 3, -1);
      graphics.disableScissor();
   }

   private void button(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, Component label) {
      this.box(graphics, mouseX, mouseY, x, y, 64, 14, label);
   }

   private void box(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, int w, int h, Component label) {
      boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
      graphics.fill(x, y, x + w, y + h, hover ? 1728053247 : 872415231);
      graphics.fill(x, y, x + w, y + 1, -1996488705);
      graphics.fill(x, y + h - 1, x + w, y + h, -1996488705);
      graphics.fill(x, y, x + 1, y + h, -1996488705);
      graphics.fill(x + w - 1, y, x + w, y + h, -1996488705);
      Ui.drawCentered(graphics, this.font, label, x + w / 2, y + (h - 8) / 2, hover ? -1 : -1770753);
   }

   private void openCreate() {
      this.activeDialog = RoomLobbyScreen.Form.CREATE;
      this.roomNameInput.setValue(this.defaultName());
      this.applyForm();
   }

   private void openPassword(RoomListPayload.Room room) {
      this.activeDialog = RoomLobbyScreen.Form.PASSWORD;
      this.passwordProtectedRoom = room;
      this.applyForm();
   }

   private void openInvite() {
      this.activeDialog = RoomLobbyScreen.Form.INVITE;
      this.invitationCodeInput.setValue("");
      this.applyForm();
   }

   private void closeForm() {
      this.activeDialog = RoomLobbyScreen.Form.NONE;
      this.passwordProtectedRoom = null;
      this.applyForm();
   }

   private void submitForm() {
      if (this.activeDialog == RoomLobbyScreen.Form.CREATE) {
         RoomCreationRequest request = this.creationOptions.createRequest(this.roomNameInput.getValue(), this.roomPasswordInput.getValue(), this.defaultName());
         send(
            new RoomActionPayload(
               "create",
               "",
               request.getName(),
               request.getPassword(),
               request.getBattleType(),
               request.getLevel(),
               request.getPick(),
               request.getFullHeal(),
               request.getHostEngine(),
               request.getLegality(),
               ""
            )
         );
      } else if (this.activeDialog == RoomLobbyScreen.Form.PASSWORD && this.passwordProtectedRoom != null) {
         send(
            RoomActionPayload.join(
               this.passwordProtectedRoom.id(),
               this.roomPasswordInput.getValue(),
               RoomLobbyRules.seatType(this.passwordProtectedRoom.fighting(), this.passwordProtectedRoom.battleType()),
               this.passwordProtectedRoom.hostEngine(),
               this.passwordProtectedRoom.legality()
            )
         );
      } else if (this.activeDialog == RoomLobbyScreen.Form.INVITE) {
         String code = RoomLobbyRules.invitationCode(this.invitationCodeInput.getValue());
         if (code == null) {
            return;
         }

         send(RoomActionPayload.joinByCode(code));
      }

      this.closeForm();
   }

   private int rowCount() {
      return (this.directorySnapshot.rooms().size() + 2 - 1) / 2;
   }

   private int cardX(int col) {
      return this.lobbyLeft + 25 + col * 151;
   }

   private int cardY(int row) {
      return this.lobbyTop + 33 + row * 51;
   }

   private int cardAt(double mouseX, double mouseY) {
      for (int slot = 0; slot < 6; slot++) {
         int x = this.cardX(slot % 2);
         int y = this.cardY(slot / 2);
         if (mouseX >= x && mouseX < x + 143 && mouseY >= y && mouseY < y + 46) {
            int index = this.firstVisibleRoomRow * 2 + slot;
            return index < this.directorySnapshot.rooms().size() ? index : -1;
         }
      }

      return -1;
   }

   private boolean inCreate(double mouseX, double mouseY) {
      int x = this.lobbyLeft + 24;
      int y = this.lobbyTop + 14;
      return mouseX >= x && mouseX < x + 52 && mouseY >= y && mouseY < y + 10;
   }

   private boolean inInvite(double mouseX, double mouseY) {
      int x = this.lobbyLeft + 80;
      int y = this.lobbyTop + 14;
      return mouseX >= x && mouseX < x + 52 && mouseY >= y && mouseY < y + 10;
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (this.activeDialog != RoomLobbyScreen.Form.NONE) {
         return this.formClicked(mouseX, mouseY, button);
      } else {
         if (button == 0) {
            if (BackButton.contains(this.lobbyLeft, this.lobbyTop, mouseX, mouseY)) {
               ServerDex.requestMain();
               return true;
            }

            if (this.inCreate(mouseX, mouseY)) {
               this.openCreate();
               return true;
            }

            if (this.inInvite(mouseX, mouseY)) {
               this.openInvite();
               return true;
            }

            int index = this.cardAt(mouseX, mouseY);
            if (index >= 0) {
               RoomListPayload.Room room = this.directorySnapshot.rooms().get(index);
               if (room.locked() && !room.mine()) {
                  this.openPassword(room);
               } else {
                  send(RoomActionPayload.join(room.id(), "", RoomLobbyRules.seatType(room.fighting(), room.battleType()), room.hostEngine(), room.legality()));
               }

               return true;
            }
         }

         return super.mouseClicked(mouseX, mouseY, button);
      }
   }

   private boolean formClicked(double mouseX, double mouseY, int button) {
      if (button == 0) {
         if (this.activeDialog == RoomLobbyScreen.Form.CREATE) {
            int left = this.dialogLeft + 74;
            int right = left + 90 - 11;
            int typeY = this.dialogTop + row(1);
            int pickY = this.dialogTop + row(2);
            int levelY = this.dialogTop + row(3);
            int healY = this.dialogTop + row(4);
            int engineY = this.dialogTop + row(5);
            if (hit(mouseX, mouseY, left, typeY, 11, 13)) {
               this.creationOptions.cycleBattleType(-1);
               return true;
            }

            if (hit(mouseX, mouseY, right, typeY, 11, 13)) {
               this.creationOptions.cycleBattleType(1);
               return true;
            }

            if (hit(mouseX, mouseY, left, pickY, 11, 13)) {
               this.creationOptions.cyclePick(-1);
               return true;
            }

            if (hit(mouseX, mouseY, right, pickY, 11, 13)) {
               this.creationOptions.cyclePick(1);
               return true;
            }

            if (hit(mouseX, mouseY, left, levelY, 11, 13)) {
               this.creationOptions.cycleLevel(-1);
               return true;
            }

            if (hit(mouseX, mouseY, right, levelY, 11, 13)) {
               this.creationOptions.cycleLevel(1);
               return true;
            }

            if (hit(mouseX, mouseY, left, healY, 11, 13) || hit(mouseX, mouseY, right, healY, 11, 13)) {
               this.creationOptions.toggleFullHeal();
               return true;
            }

            if (hit(mouseX, mouseY, left, engineY, 11, 13) || hit(mouseX, mouseY, right, engineY, 11, 13)) {
               this.creationOptions.toggleHostEngine();
               this.applyForm();
               return true;
            }

            if (this.creationOptions.hostEngine()) {
               int legalityY = this.dialogTop + row(6);
               if (hit(mouseX, mouseY, left, legalityY, 11, 13) || hit(mouseX, mouseY, right, legalityY, 11, 13)) {
                  this.creationOptions.toggleLegality();
                  return true;
               }
            }
         }

         if (hit(mouseX, mouseY, this.dialogLeft + 20 + 6, this.dialogTop + this.buttonY(), 64, 14)) {
            this.submitForm();
            return true;
         }

         if (hit(mouseX, mouseY, this.dialogLeft + 170 - 6 - 64, this.dialogTop + this.buttonY(), 64, 14)) {
            this.closeForm();
            return true;
         }
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   private static boolean hit(double mx, double my, int x, int y, int w, int h) {
      return mx >= x && mx < x + w && my >= y && my < y + h;
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.activeDialog != RoomLobbyScreen.Form.NONE) {
         if (keyCode == 256) {
            this.closeForm();
            return true;
         }

         if (keyCode == 257 || keyCode == 335) {
            this.submitForm();
            return true;
         }
      }

      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double amountX, double amountY) {
      if (this.activeDialog != RoomLobbyScreen.Form.NONE) {
         return true;
      } else {
         int max = Math.max(0, this.rowCount() - 3);
         this.firstVisibleRoomRow = Math.max(0, Math.min(max, this.firstVisibleRoomRow - (int)Math.signum(amountY)));
         return true;
      }
   }

   static void send(RoomActionPayload action) {
      NetworkManager.sendToServer(action);
   }

   private static enum Form {
      NONE,
      CREATE,
      PASSWORD,
      INVITE;
   }
}
