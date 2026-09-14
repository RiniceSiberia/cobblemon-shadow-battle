package xiaocaoawa.minecraft.mod.cobblebattle.client;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.client.CobblemonResources;
import com.cobblemon.mod.common.client.render.models.blockbench.FloatingState;
import com.cobblemon.mod.common.entity.PoseType;
import com.cobblemon.mod.common.pokemon.RenderablePokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.cobblemon.mod.common.util.math.QuaternionUtilsKt;
import dev.architectury.networking.NetworkManager;
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
   private static final ResourceLocation BASE = ResourceLocation.fromNamespaceAndPath("cobblebattle", "textures/gui/leaderboard/base.png");
   private static final ResourceLocation SCREEN = ResourceLocation.fromNamespaceAndPath("cobblebattle", "textures/gui/leaderboard/screen_1.png");
   private static final ResourceLocation FORM = ResourceLocation.fromNamespaceAndPath("cobblebattle", "textures/gui/auth_background.png");
   private static final int WIDTH = 345;
   private static final int HEIGHT = 207;
   private static final int HOLE_X = 18;
   private static final int HOLE_Y = 13;
   private static final int HOLE_W = 309;
   private static final int HOLE_H = 183;
   private static final int CREATE_X = 24;
   private static final int CREATE_Y = 14;
   private static final int CREATE_W = 52;
   private static final int CREATE_H = 10;
   private static final int INVITE_X = 80;
   private static final int INVITE_W = 52;
   private static final int GRID_X = 25;
   private static final int GRID_Y = 33;
   private static final int COLS = 2;
   private static final int ROWS = 3;
   private static final int CARD_W = 143;
   private static final int CARD_H = 46;
   private static final int FIGURE_SIZE = 15;
   private static final int UNKNOWN_H = 26;
   private static final int GAP_X = 8;
   private static final int GAP_Y = 5;
   private static final int SCROLLOFFSETS_X = 320;
   private static final int SCROLLOFFSETS_W = 3;
   private static final int CARD_FILL = 1728053247;
   private static final int CARD_FILL_HOVER = -1996488705;
   private static final int CARD_EDGE = -1426063361;
   private static final int BAR_MINE = -2082246;
   private static final int BAR_OPEN = -1;
   private static final int BAR_LOCKED = -868018;
   private static final int TEXT = -1;
   private static final int TEXT_SOFT = -1770753;
   private static final int TRACK = -12937546;
   private static final int THUMB = -197380;
   private static final int FORM_W = 191;
   private static final int FORM_H = 207;
   private static final int IN_LEFT = 20;
   private static final int IN_RIGHT = 170;
   private static final int TITLE_Y = 17;
   private static final int LABEL_X = 26;
   private static final int FIELD_X = 74;
   private static final int FIELD_W = 90;
   private static final int ROW_ONE = 36;
   private static final int ROW_GAP = 17;
   private static final int FIELD_H = 13;
   private static final int ARROW_W = 11;
   private static final int BUTTON_Y = 152;
   private static final int BUTTON_W = 64;
   private static final int BUTTON_H = 14;
   private static final int FORM_FILL = 872415231;
   private static final int FORM_FILL_HOVER = 1728053247;
   private static final int FORM_EDGE = -1996488705;
   private static final int DIM = -1728053248;
   private static final int REFRESH_TICKS = 100;
   private static final List<String> TYPES = List.of("singles", "doubles", "triples");
   private static final List<Integer> LEVELS = List.of(-1, 50, 100);
   private static final List<Integer> PICKS = List.of(6, 3, 4);
   private static final int ROW_NAME = 0;
   private static final int ROW_TYPE = 1;
   private static final int ROW_PICK = 2;
   private static final int ROW_LEVEL = 3;
   private static final int ROW_HEAL = 4;
   private static final int ROW_ENGINE = 5;
   private static final int ROW_LEGALITY = 6;
   private RoomListPayload data;
   private int firstRow;
   private int ticks;
   private int originX;
   private int originY;
   private final Map<String, FloatingState> states = new HashMap<>();
   private final Map<String, RenderablePokemon> leads = new HashMap<>();
   private final Map<String, PlayerPortrait> portraits = new HashMap<>();
   private RoomLobbyScreen.Form form = RoomLobbyScreen.Form.NONE;
   private RoomListPayload.Room locked;
   private EditBox nameBox;
   private EditBox passwordBox;
   private EditBox codeBox;
   private int type;
   private int level;
   private int pick;
   private boolean fullHeal = true;
   private boolean hostEngine;
   private boolean legality = true;
   private int formX;
   private int formY;

   private static int row(int index) {
      return 36 + 17 * index;
   }

   private static String seatType(RoomListPayload.Room room) {
      return room.fighting() ? "" : room.battleType();
   }

   private int passwordRow() {
      return this.hostEngine ? 7 : 6;
   }

   private int buttonY() {
      return this.form == RoomLobbyScreen.Form.CREATE ? row(this.passwordRow() + 1) : 152;
   }

   public RoomLobbyScreen(RoomListPayload data) {
      super(Component.translatable("cobblebattle.room.title"));
      this.data = data;
   }

   public void update(RoomListPayload data) {
      this.data = data;
      int max = Math.max(0, this.rowCount() - 3);
      if (this.firstRow > max) {
         this.firstRow = max;
      }

      Set<String> live = new HashSet<>();
      Set<String> hosts = new HashSet<>();

      for (RoomListPayload.Room room : data.rooms()) {
         live.add(room.id());
         hosts.add(hostKey(room));
      }

      this.leads.keySet().retainAll(live);
      this.states.keySet().retainAll(live);
      this.portraits.keySet().retainAll(hosts);
   }

   protected void init() {
      this.originX = (this.width - 345) / 2;
      this.originY = (this.height - 207) / 2;
      this.formX = (this.width - 191) / 2;
      this.formY = (this.height - 207) / 2;
      String was = this.nameBox == null ? this.defaultName() : this.nameBox.getValue();
      String password = this.passwordBox == null ? "" : this.passwordBox.getValue();
      this.nameBox = new EditBox(this.font, this.formX + 74, this.formY + 36, 90, 13, Component.empty());
      this.nameBox.setMaxLength(24);
      this.nameBox.setValue(was);
      this.passwordBox = new EditBox(this.font, this.formX + 74, this.formY + row(this.passwordRow()), 90, 13, Component.empty());
      this.passwordBox.setMaxLength(16);
      this.passwordBox.setValue(password);
      String code = this.codeBox == null ? "" : this.codeBox.getValue();
      this.codeBox = new EditBox(this.font, this.formX + 74, this.formY + row(1), 90, 13, Component.empty());
      this.codeBox.setMaxLength(12);
      this.codeBox.setValue(code);
      this.addWidget(this.nameBox);
      this.addWidget(this.passwordBox);
      this.addWidget(this.codeBox);
      this.applyForm();
   }

   private void applyForm() {
      boolean creating = this.form == RoomLobbyScreen.Form.CREATE;
      boolean asking = this.form == RoomLobbyScreen.Form.PASSWORD;
      boolean inviting = this.form == RoomLobbyScreen.Form.INVITE;
      this.nameBox.setVisible(creating);
      this.passwordBox.setVisible(creating || asking);
      this.codeBox.setVisible(inviting);
      if (asking) {
         this.passwordBox.setX(this.formX + 74);
         this.passwordBox.setY(this.formY + row(1));
         this.passwordBox.setValue("");
      } else {
         this.passwordBox.setX(this.formX + 74);
         this.passwordBox.setY(this.formY + row(this.passwordRow()));
      }

      this.setFocused(creating ? this.nameBox : (asking ? this.passwordBox : (inviting ? this.codeBox : null)));
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
      if (++this.ticks % 100 == 0) {
         send(RoomActionPayload.of("list"));
      }
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      super.render(graphics, mouseX, mouseY, partialTick);
      graphics.blit(SCREEN, this.originX, this.originY, 0.0F, 0.0F, 345, 207, 345, 207);
      Backdrop.draw(graphics, this.originX, this.originY);
      if (this.form == RoomLobbyScreen.Form.NONE) {
         int left = this.originX + 18;
         int top = this.originY + 13;
         graphics.enableScissor(left, top, left + 309, top + 183);

         try {
            this.drawHeader(graphics, mouseX, mouseY);
            this.drawCards(graphics, mouseX, mouseY, partialTick);
            this.drawScrollbar(graphics);
            BackButton.draw(graphics, this.font, this.originX, this.originY, mouseX, mouseY);
         } finally {
            graphics.disableScissor();
         }
      }

      graphics.blit(BASE, this.originX, this.originY, 0.0F, 0.0F, 345, 207, 345, 207);
      if (this.form != RoomLobbyScreen.Form.NONE) {
         this.drawForm(graphics, mouseX, mouseY, partialTick);
      }
   }

   private void drawHeader(GuiGraphics graphics, int mouseX, int mouseY) {
      boolean hover = this.form == RoomLobbyScreen.Form.NONE && this.inCreate(mouseX, mouseY);
      int x = this.originX + 24;
      int y = this.originY + 14;
      graphics.fill(x, y, x + 52, y + 10, hover ? 1728053247 : 872415231);
      Ui.drawCentered(graphics, this.font, Component.translatable("cobblebattle.room.create"), x + 26, y + 1, -1);
      boolean inviteHover = this.form == RoomLobbyScreen.Form.NONE && this.inInvite(mouseX, mouseY);
      int ix = this.originX + 80;
      graphics.fill(ix, y, ix + 52, y + 10, inviteHover ? 1728053247 : 872415231);
      Ui.drawCentered(graphics, this.font, Component.translatable("cobblebattle.room.invite"), ix + 26, y + 1, -1);
      Component page = Component.translatable("cobblebattle.room.title")
         .withStyle(style -> style.withFont(CobblemonResources.INSTANCE.getDEFAULT_LARGE()).withBold(true));
      Ui.draw(graphics, this.font, page, this.originX + 322 - Ui.width(this.font, page), this.originY + 14, -1, true);
   }

   private void drawCards(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      List<RoomListPayload.Room> rooms = this.data.rooms();
      if (rooms.isEmpty()) {
         Ui.drawCentered(graphics, this.font, Component.translatable("cobblebattle.room.empty"), this.originX + 172, this.originY + 33 + 60, -1770753);
      } else {
         int hovered = this.form == RoomLobbyScreen.Form.NONE ? this.cardAt(mouseX, mouseY) : -1;

         for (int slot = 0; slot < 6; slot++) {
            int index = this.firstRow * 2 + slot;
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
         PlayerPortrait portrait = this.portraits.computeIfAbsent(hostKey(room), key -> PlayerPortrait.lookup(room.host(), room.hostUid()));
         if (portrait.entity() != null) {
            LeaderboardScreen.drawEntity(graphics, x + 19, floorY, 15, -35.0F, -10.0F, portrait.entity());
         }

         RenderablePokemon lead = this.leadOf(room);
         if (lead != null) {
            FloatingState state = this.states.computeIfAbsent(room.id(), id -> new FloatingState());
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
      return room.lead().isEmpty() ? null : this.leads.computeIfAbsent(room.id(), id -> {
         Species speciesTemplate = PokemonSpecies.getByName(room.lead());
         return speciesTemplate == null ? null : new RenderablePokemon(speciesTemplate, Set.of(), ItemStack.EMPTY);
      });
   }

   private void drawScrollbar(GuiGraphics graphics) {
      int rows = this.rowCount();
      if (rows > 3) {
         int top = this.originY + 33;
         int height = 148;
         int x = this.originX + 320;
         graphics.fill(x, top, x + 3, top + height, -12937546);
         int thumb = Math.max(12, height * 3 / rows);
         int travel = height - thumb;
         int at = top + (rows - 3 == 0 ? 0 : travel * this.firstRow / (rows - 3));
         graphics.fill(x, at, x + 3, at + thumb, -197380);
      }
   }

   private void drawForm(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      graphics.fill(0, 0, this.width, this.height, -1728053248);
      graphics.blit(FORM, this.formX, this.formY, 0.0F, 0.0F, 191, 207, 191, 207);
      boolean creating = this.form == RoomLobbyScreen.Form.CREATE;
      boolean inviting = this.form == RoomLobbyScreen.Form.INVITE;
      Ui.drawCentered(
         graphics,
         this.font,
         Component.translatable(
            creating ? "cobblebattle.room.create_title" : (inviting ? "cobblebattle.room.invite_title" : "cobblebattle.room.password_title")
         ),
         this.formX + 95,
         this.formY + 17,
         -1
      );
      if (inviting) {
         Ui.drawCentered(graphics, this.font, Component.translatable("cobblebattle.room.invite_hint"), this.formX + 95, this.formY + row(0), -1770753);
         this.label(graphics, "cobblebattle.room.invite", row(1));
      } else if (creating) {
         this.label(graphics, "cobblebattle.room.name", row(0));
         this.label(graphics, "cobblebattle.room.type", row(1));
         this.label(graphics, "cobblebattle.room.pick", row(2));
         this.label(graphics, "cobblebattle.room.level", row(3));
         this.label(graphics, "cobblebattle.room.heal", row(4));
         this.label(graphics, "cobblebattle.room.engine", row(5));
         if (this.hostEngine) {
            this.label(graphics, "cobblebattle.room.legality", row(6));
         }

         this.label(graphics, "cobblebattle.room.password", row(this.passwordRow()));
         this.picker(graphics, mouseX, mouseY, row(1), Component.translatable("cobblebattle.room.type." + TYPES.get(this.type)));
         this.picker(graphics, mouseX, mouseY, row(2), Component.translatable("cobblebattle.room.pick_of", new Object[]{PICKS.get(this.pick)}));
         this.picker(
            graphics,
            mouseX,
            mouseY,
            row(3),
            LEVELS.get(this.level) > 0
               ? Component.translatable("cobblebattle.room.level_at", new Object[]{LEVELS.get(this.level)})
               : Component.translatable("cobblebattle.room.level_free")
         );
         this.picker(graphics, mouseX, mouseY, row(4), Component.translatable(this.fullHeal ? "cobblebattle.room.heal_on" : "cobblebattle.room.heal_off"));
         this.picker(
            graphics, mouseX, mouseY, row(5), Component.translatable(this.hostEngine ? "cobblebattle.room.engine_host" : "cobblebattle.room.engine_server")
         );
         if (this.hostEngine) {
            this.picker(
               graphics, mouseX, mouseY, row(6), Component.translatable(this.legality ? "cobblebattle.room.legality_on" : "cobblebattle.room.legality_off")
            );
         }
      } else {
         Ui.drawCentered(graphics, this.font, this.locked == null ? "" : this.locked.name(), this.formX + 95, this.formY + row(0), -1770753);
         this.label(graphics, "cobblebattle.room.password", row(1));
      }

      this.button(
         graphics,
         mouseX,
         mouseY,
         this.formX + 20 + 6,
         this.formY + this.buttonY(),
         Component.translatable(creating ? "cobblebattle.room.confirm" : "cobblebattle.room.join")
      );
      this.button(graphics, mouseX, mouseY, this.formX + 170 - 6 - 64, this.formY + this.buttonY(), Component.translatable("cobblebattle.back"));
      if (creating) {
         this.nameBox.render(graphics, mouseX, mouseY, partialTick);
      }

      if (inviting) {
         this.codeBox.render(graphics, mouseX, mouseY, partialTick);
      } else {
         this.passwordBox.render(graphics, mouseX, mouseY, partialTick);
      }
   }

   private void label(GuiGraphics graphics, String key, int y) {
      Ui.draw(graphics, this.font, Component.translatable(key), this.formX + 26, this.formY + y + 3, -1, true);
   }

   private void picker(GuiGraphics graphics, int mouseX, int mouseY, int y, Component value) {
      int left = this.formX + 74;
      int right = left + 90 - 11;
      this.box(graphics, mouseX, mouseY, left, this.formY + y, 11, 13, Component.literal("<"));
      this.box(graphics, mouseX, mouseY, right, this.formY + y, 11, 13, Component.literal(">"));
      graphics.enableScissor(left + 11 + 1, this.formY + y, right - 1, this.formY + y + 13);
      Ui.drawCentered(graphics, this.font, value, left + 45, this.formY + y + 3, -1);
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
      this.form = RoomLobbyScreen.Form.CREATE;
      this.nameBox.setValue(this.defaultName());
      this.applyForm();
   }

   private void openPassword(RoomListPayload.Room room) {
      this.form = RoomLobbyScreen.Form.PASSWORD;
      this.locked = room;
      this.applyForm();
   }

   private void openInvite() {
      this.form = RoomLobbyScreen.Form.INVITE;
      this.codeBox.setValue("");
      this.applyForm();
   }

   private void closeForm() {
      this.form = RoomLobbyScreen.Form.NONE;
      this.locked = null;
      this.applyForm();
   }

   private void submitForm() {
      if (this.form == RoomLobbyScreen.Form.CREATE) {
         String name = this.nameBox.getValue().trim();
         if (name.isEmpty()) {
            name = this.defaultName();
         }

         send(
            new RoomActionPayload(
               "create",
               "",
               name,
               this.passwordBox.getValue().trim(),
               TYPES.get(this.type),
               LEVELS.get(this.level),
               PICKS.get(this.pick),
               this.fullHeal,
               this.hostEngine,
               this.legality,
               ""
            )
         );
      } else if (this.form == RoomLobbyScreen.Form.PASSWORD && this.locked != null) {
         send(RoomActionPayload.join(this.locked.id(), this.passwordBox.getValue(), seatType(this.locked), this.locked.hostEngine(), this.locked.legality()));
      } else if (this.form == RoomLobbyScreen.Form.INVITE) {
         String code = this.codeBox.getValue().trim();
         if (code.isEmpty()) {
            return;
         }

         send(RoomActionPayload.joinByCode(code));
      }

      this.closeForm();
   }

   private int rowCount() {
      return (this.data.rooms().size() + 2 - 1) / 2;
   }

   private int cardX(int col) {
      return this.originX + 25 + col * 151;
   }

   private int cardY(int row) {
      return this.originY + 33 + row * 51;
   }

   private int cardAt(double mouseX, double mouseY) {
      for (int slot = 0; slot < 6; slot++) {
         int x = this.cardX(slot % 2);
         int y = this.cardY(slot / 2);
         if (mouseX >= x && mouseX < x + 143 && mouseY >= y && mouseY < y + 46) {
            int index = this.firstRow * 2 + slot;
            return index < this.data.rooms().size() ? index : -1;
         }
      }

      return -1;
   }

   private boolean inCreate(double mouseX, double mouseY) {
      int x = this.originX + 24;
      int y = this.originY + 14;
      return mouseX >= x && mouseX < x + 52 && mouseY >= y && mouseY < y + 10;
   }

   private boolean inInvite(double mouseX, double mouseY) {
      int x = this.originX + 80;
      int y = this.originY + 14;
      return mouseX >= x && mouseX < x + 52 && mouseY >= y && mouseY < y + 10;
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (this.form != RoomLobbyScreen.Form.NONE) {
         return this.formClicked(mouseX, mouseY, button);
      } else {
         if (button == 0) {
            if (BackButton.contains(this.originX, this.originY, mouseX, mouseY)) {
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
               RoomListPayload.Room room = this.data.rooms().get(index);
               if (room.locked() && !room.mine()) {
                  this.openPassword(room);
               } else {
                  send(RoomActionPayload.join(room.id(), "", seatType(room), room.hostEngine(), room.legality()));
               }

               return true;
            }
         }

         return super.mouseClicked(mouseX, mouseY, button);
      }
   }

   private boolean formClicked(double mouseX, double mouseY, int button) {
      if (button == 0) {
         if (this.form == RoomLobbyScreen.Form.CREATE) {
            int left = this.formX + 74;
            int right = left + 90 - 11;
            int typeY = this.formY + row(1);
            int pickY = this.formY + row(2);
            int levelY = this.formY + row(3);
            int healY = this.formY + row(4);
            int engineY = this.formY + row(5);
            if (hit(mouseX, mouseY, left, typeY, 11, 13)) {
               this.type = Math.floorMod(this.type - 1, TYPES.size());
               return true;
            }

            if (hit(mouseX, mouseY, right, typeY, 11, 13)) {
               this.type = Math.floorMod(this.type + 1, TYPES.size());
               return true;
            }

            if (hit(mouseX, mouseY, left, pickY, 11, 13)) {
               this.pick = Math.floorMod(this.pick - 1, PICKS.size());
               return true;
            }

            if (hit(mouseX, mouseY, right, pickY, 11, 13)) {
               this.pick = Math.floorMod(this.pick + 1, PICKS.size());
               return true;
            }

            if (hit(mouseX, mouseY, left, levelY, 11, 13)) {
               this.level = Math.floorMod(this.level - 1, LEVELS.size());
               return true;
            }

            if (hit(mouseX, mouseY, right, levelY, 11, 13)) {
               this.level = Math.floorMod(this.level + 1, LEVELS.size());
               return true;
            }

            if (hit(mouseX, mouseY, left, healY, 11, 13) || hit(mouseX, mouseY, right, healY, 11, 13)) {
               this.fullHeal = !this.fullHeal;
               return true;
            }

            if (hit(mouseX, mouseY, left, engineY, 11, 13) || hit(mouseX, mouseY, right, engineY, 11, 13)) {
               this.hostEngine = !this.hostEngine;
               this.applyForm();
               return true;
            }

            if (this.hostEngine) {
               int legalityY = this.formY + row(6);
               if (hit(mouseX, mouseY, left, legalityY, 11, 13) || hit(mouseX, mouseY, right, legalityY, 11, 13)) {
                  this.legality = !this.legality;
                  return true;
               }
            }
         }

         if (hit(mouseX, mouseY, this.formX + 20 + 6, this.formY + this.buttonY(), 64, 14)) {
            this.submitForm();
            return true;
         }

         if (hit(mouseX, mouseY, this.formX + 170 - 6 - 64, this.formY + this.buttonY(), 64, 14)) {
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
      if (this.form != RoomLobbyScreen.Form.NONE) {
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
      if (this.form != RoomLobbyScreen.Form.NONE) {
         return true;
      } else {
         int max = Math.max(0, this.rowCount() - 3);
         this.firstRow = Math.max(0, Math.min(max, this.firstRow - (int)Math.signum(amountY)));
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
