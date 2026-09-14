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
   private static final ResourceLocation BASE = ResourceLocation.fromNamespaceAndPath("cobblebattle", "textures/gui/leaderboard/base.png");
   private static final ResourceLocation SCREEN = ResourceLocation.fromNamespaceAndPath("cobblebattle", "textures/gui/leaderboard/screen.png");
   private static final ResourceLocation POKE_BALL_BACKGROUND = ResourceLocation.fromNamespaceAndPath(
      "cobblemon", "textures/gui/pokedex/pokedex_screen_poke_ball.png"
   );
   private static final ResourceLocation PLATFORM = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/pokedex/platform_base.png");
   private static final ResourceLocation PLATFORM_SHADOW = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/pokedex/platform_shadow.png");
   private static final ResourceLocation UNKNOWN = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/pokedex/platform_unknown.png");
   private static final ResourceLocation ARROW_UP = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/pokedex/arrow_up.png");
   private static final ResourceLocation ARROW_DOWN = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/pokedex/arrow_down.png");
   public static final int PAGE_LABEL_RIGHT = 322;
   public static final int PAGE_LABEL_Y = 14;
   private static final int WIDTH = 345;
   private static final int HEIGHT = 207;
   private static final int HOLE_X = 18;
   private static final int HOLE_Y = 13;
   private static final int HOLE_W = 309;
   private static final int HOLE_H = 183;
   private static final int HEADER_Y = 12;
   private static final int HEADER_H = 12;
   private static final int LEFT_X = 20;
   private static final int LEFT_W = 87;
   private static final int RIGHT_X = 110;
   private static final int RIGHT_W = 215;
   private static final int PANE_Y = 25;
   private static final int PANE_H = 170;
   private static final int BAND_A = -11286315;
   private static final int BAND_B = -10759718;
   private static final int BAND_DEEP = -12274744;
   private static final int INK = -15451066;
   private static final int INK_SOFT = -14721168;
   private static final int WHITE = -1;
   private static final int GOLD = -868018;
   private static final int BAND_MINE = -9773853;
   private static final int COLUMNS_Y = 28;
   private static final int ROWS_Y = 40;
   private static final int ROW_H = 14;
   private static final int ROWS = 10;
   private static final int COL_RANK = 114;
   private static final int COL_FACE = 136;
   private static final int COL_NAME = 148;
   private static final int COL_RECORD = 256;
   private static final int COL_SCORE_RIGHT = 319;
   private static final int LABEL_Y = 13;
   private static final int LABEL_SHIFT_X = -3;
   private static final int BAR_Y = 28;
   private static final int BAR_NUMBER_X = 24;
   private static final int BAR_NAME_X = 43;
   private static final int BAR_NAME_RIGHT = 104;
   private static final int BAR_TEXT_Y = 28;
   private static final int DEX_NAME = -10458002;
   private static final int BOX_Y = 37;
   private static final int BOX_BOTTOM = 131;
   private static final int PLATE_ALPHA = -671088640;
   private static final int PLATFORM_W = 87;
   private static final int PLATFORM_H = 23;
   private static final int PLATFORM_Y = 110;
   private static final int FLOOR_Y = 121;
   private static final int SHADOW_W = 45;
   private static final int SHADOW_H = 10;
   private static final int UNKNOWN_W = 26;
   private static final int UNKNOWN_H = 30;
   private static final int PLAYER_X = 40;
   private static final int PLAYER_SIZE = 34;
   public static final float PLAYER_YAW = -35.0F;
   public static final float PLAYER_PITCH = -10.0F;
   private static final int POKEMON_X = 60;
   private static final int POKEMON_Y = 37;
   private static final int POKEMON_W = 46;
   private static final int POKEMON_H = 84;
   public static final float FACING = (float)(Math.atan(-0.875) * 40.0);
   public static final float POKEMON_PITCH = 5.0F;
   private final LeaderboardPayload board;
   private final UUID viewer;
   private LeaderboardPayload.Entry selected;
   private PlayerPortrait portrait;
   private final Map<Long, PlayerPortrait> portraits = new HashMap<>();
   private RenderablePokemon favourite;
   private FloatingState pokemonState = new FloatingState();
   private float pokemonScale = 34.0F;
   private int offset;
   private int originX;
   private int originY;

   public LeaderboardScreen(LeaderboardPayload board, UUID viewer) {
      super(Component.translatable("cobblebattle.rank.title", new Object[]{board.name()}));
      this.board = board;
      this.viewer = viewer;
   }

   protected void init() {
      this.originX = (this.width - 345) / 2;
      this.originY = (this.height - 207) / 2;
      this.select(this.selected == null ? this.board.you() : this.selected);
   }

   private void select(LeaderboardPayload.Entry entry) {
      this.selected = entry;
      this.portrait = this.portraitOf(entry);
      this.favourite = null;
      this.pokemonState = new FloatingState();
      this.pokemonScale = 34.0F;
      String speciesId = entry.favourite();
      if (!speciesId.isEmpty()) {
         Species species = PokemonSpecies.getByName(speciesId);
         if (species != null) {
            this.favourite = new RenderablePokemon(species, Set.of(), ItemStack.EMPTY);
            float blocks = Math.max(0.1F, this.favourite.getForm().getHitbox().height());
            this.pokemonScale = Math.min(34.0F, 78.0F / blocks);
         }
      }
   }

   private PlayerPortrait portraitOf(LeaderboardPayload.Entry entry) {
      Minecraft minecraft = Minecraft.getInstance();
      if (entry == this.board.you() || this.isMine(entry)) {
         LocalPlayer var4 = minecraft.player;
         if (var4 instanceof AbstractClientPlayer) {
            return PlayerPortrait.of(var4);
         }
      }

      return this.portraits.computeIfAbsent(entry.uid(), uid -> PlayerPortrait.lookup(entry.name(), uid));
   }

   private boolean isMine(LeaderboardPayload.Entry entry) {
      return entry.uid() != 0L && entry.uid() == this.board.you().uid();
   }

   private boolean isSelected(LeaderboardPayload.Entry entry) {
      return this.selected != null && entry.uid() != 0L && entry.uid() == this.selected.uid();
   }

   public boolean isPauseScreen() {
      return false;
   }

   public void tick() {
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      super.render(graphics, mouseX, mouseY, partialTick);
      graphics.blit(SCREEN, this.originX, this.originY, 0.0F, 0.0F, 345, 207, 345, 207);
      Backdrop.draw(graphics, this.originX, this.originY);
      this.drawContents(graphics, mouseX, mouseY, partialTick);
      BackButton.draw(graphics, this.font, this.originX, this.originY, mouseX, mouseY);
      graphics.blit(BASE, this.originX, this.originY, 0.0F, 0.0F, 345, 207, 345, 207);
   }

   private void drawContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      int left = this.originX + 18;
      int top = this.originY + 13;
      graphics.enableScissor(left, top, left + 309, top + 183);

      try {
         Ui.drawCentered(graphics, this.font, this.board.name(), this.originX + 172, this.originY + 12 + 2, -1);
         Component page = Component.translatable("cobblebattle.rank.label")
            .withStyle(style -> style.withFont(CobblemonResources.INSTANCE.getDEFAULT_LARGE()).withBold(true));
         Ui.draw(graphics, this.font, page, this.originX + 322 - Ui.width(this.font, page), this.originY + 14, -1, true);
         this.drawViewer(graphics, mouseX, mouseY, partialTick);
         this.drawLadder(graphics);
      } finally {
         graphics.disableScissor();
      }
   }

   private void drawViewer(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      Minecraft minecraft = Minecraft.getInstance();
      int paneLeft = this.originX + 20;
      int centreX = paneLeft + 43;
      Ui.drawCentered(graphics, this.font, Component.translatable("cobblebattle.rank.label"), centreX + -3, this.originY + 13, -1);
      this.drawBackdrop(graphics);
      if (this.portrait != null && this.portrait.entity() != null) {
         drawEntity(graphics, this.originX + 40, this.originY + 121, 34, -35.0F, -10.0F, this.portrait.entity());
      }

      if (this.favourite != null) {
         int boxLeft = this.originX + 60;
         int boxTop = this.originY + 37;
         graphics.enableScissor(boxLeft, boxTop, boxLeft + 46, this.originY + 121 + 2);
         graphics.pose().pushPose();
         graphics.pose().translate(boxLeft + 23.0, this.originY + 121, 0.0);
         Quaternionf rotation = QuaternionUtilsKt.fromEulerXYZDegrees(new Quaternionf(), new Vector3f(5.0F, FACING, 0.0F));
         CobblemonCompat.drawProfile(
            this.favourite,
            graphics.pose(),
            rotation,
            PoseType.PROFILE,
            this.pokemonState,
            partialTick,
            this.pokemonScale,
            true,
            1.0F,
            1.0F,
            1.0F,
            1.0F,
            0.0F,
            0.0F,
            13
         );
         graphics.pose().popPose();
         graphics.disableScissor();
      } else {
         UnknownMark.draw(graphics, this.originX + 60 + 23, this.originY + 121, 30);
      }

      LeaderboardPayload.Entry shown = this.selected == null ? this.board.you() : this.selected;
      String name = !shown.name().isEmpty() ? shown.name() : (minecraft.player == null ? "" : minecraft.player.getGameProfile().getName());
      String rank = shown.rank() != 0 ? "#" + shown.rank() : "#-";
      Ui.draw(graphics, this.font, dexText(rank), this.originX + 24, this.originY + 28, -1, true);
      graphics.enableScissor(this.originX + 43, this.originY + 28, this.originX + 104, this.originY + 28 + 10);
      Ui.draw(graphics, this.font, dexText(name), this.originX + 43, this.originY + 28, -10458002, false);
      graphics.disableScissor();
   }

   private static Component dexText(String text) {
      return Component.literal(text).withStyle(style -> style.withFont(CobblemonResources.INSTANCE.getDEFAULT_LARGE()).withBold(true));
   }

   private void drawBackdrop(GuiGraphics graphics) {
      int left = this.originX + 20;
      int top = this.originY + 37;
      int bottom = this.originY + Math.max(131, 133);
      graphics.enableScissor(left, top, left + 87, bottom);

      try {
         graphics.blit(PLATFORM, left, this.originY + 110, 87, 23, 0.0F, 0.0F, 113, 30, 113, 30);
         this.drawShadow(graphics, this.originX + 40);
         this.drawShadow(graphics, this.originX + 60 + 23);
      } finally {
         graphics.disableScissor();
      }
   }

   private void drawShadow(GuiGraphics graphics, int centreX) {
      graphics.blit(PLATFORM_SHADOW, centreX - 22, this.originY + 121 - 5, 45, 10, 0.0F, 0.0F, 90, 20, 90, 20);
   }

   public static void drawEntity(GuiGraphics graphics, int x, int y, int size, float yaw, float pitch, LivingEntity entity) {
      float turn = (float)Math.atan(yaw / 40.0F);
      float tilt = (float)Math.atan(pitch / 40.0F);
      Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);
      Quaternionf camera = new Quaternionf().rotateX(tilt * 20.0F * (float) (Math.PI / 180.0));
      pose.mul(camera);
      float bodyRot = entity.yBodyRot;
      float yRot = entity.getYRot();
      float xRot = entity.getXRot();
      float headRotO = entity.yHeadRotO;
      float headRot = entity.yHeadRot;
      entity.yBodyRot = 180.0F + turn * 20.0F;
      entity.setYRot(180.0F + turn * 40.0F);
      entity.setXRot(-tilt * 20.0F);
      entity.yHeadRot = entity.getYRot();
      entity.yHeadRotO = entity.getYRot();

      try {
         InventoryScreen.renderEntityInInventory(graphics, x, y, size, new Vector3f(), pose, camera, entity);
      } finally {
         entity.yBodyRot = bodyRot;
         entity.setYRot(yRot);
         entity.setXRot(xRot);
         entity.yHeadRotO = headRotO;
         entity.yHeadRot = headRot;
      }
   }

   private void drawLadder(GuiGraphics graphics) {
      Font font = this.font;
      int cy = this.originY + 28;
      Ui.draw(graphics, font, Component.translatable("cobblebattle.rank.col.rank"), this.originX + 114, cy, -14721168, false);
      Ui.draw(graphics, font, Component.translatable("cobblebattle.rank.col.player"), this.originX + 148, cy, -14721168, false);
      Ui.draw(graphics, font, Component.translatable("cobblebattle.rank.col.record"), this.originX + 256, cy, -14721168, false);
      Component scoreHead = Component.translatable("cobblebattle.rank.col.score");
      Ui.draw(graphics, font, scoreHead, this.originX + 319 - Ui.width(font, scoreHead), cy, -14721168, false);
      List<LeaderboardPayload.Entry> entries = this.board.top();
      if (entries.isEmpty()) {
         Ui.drawCentered(graphics, font, Component.translatable("cobblebattle.rank.empty"), this.originX + 110 + 107, this.originY + 40 + 56, -14721168);
      } else {
         for (int i = 0; i < 10; i++) {
            int index = this.offset + i;
            if (index >= entries.size()) {
               break;
            }

            this.drawRow(graphics, font, this.originY + 40 + i * 14, entries.get(index), i % 2 == 0);
         }
      }
   }

   private void drawRow(GuiGraphics graphics, Font font, int y, LeaderboardPayload.Entry entry, boolean even) {
      boolean picked = this.isSelected(entry);
      int plate = picked ? -12274744 : (this.isMine(entry) ? -9773853 : (even ? -11286315 : -10759718));
      int ink = picked ? -1 : -15451066;
      graphics.fill(this.originX + 110, y, this.originX + 110 + 215, y + 14, plate & 16777215 | -671088640);
      int textY = y + 3;
      int rankColour = entry.rank() <= 3 && !picked ? -868018 : ink;
      Ui.draw(graphics, font, "#" + entry.rank(), this.originX + 114, textY, rankColour, false);
      this.drawFace(graphics, entry, this.originX + 136, y + 3);
      int nameWidth = 104;
      graphics.enableScissor(this.originX + 148, y, this.originX + 148 + nameWidth, y + 14);
      Ui.draw(graphics, font, entry.name(), this.originX + 148, textY, ink, false);
      graphics.disableScissor();
      String record = entry.wins() + " / " + entry.losses() + (entry.streak() >= 3 ? " ↑" + entry.streak() : "");
      Ui.draw(graphics, font, record, this.originX + 256, textY, ink, false);
      String score = String.valueOf(entry.score());
      Ui.draw(graphics, font, score, this.originX + 319 - Ui.width(font, score), textY, ink, false);
   }

   private void drawFace(GuiGraphics graphics, LeaderboardPayload.Entry entry, int x, int y) {
      ResourceLocation skin;
      label20: {
         Minecraft minecraft = Minecraft.getInstance();
         skin = null;
         if (this.isMine(entry)) {
            LocalPlayer known = minecraft.player;
            if (known instanceof AbstractClientPlayer && known.getUUID().equals(this.viewer)) {
               skin = known.getSkin().texture();
               break label20;
            }
         }

         PlayerPortrait known = this.portraits.get(entry.uid());
         if (known != null) {
            skin = known.skin().texture();
         }
      }

      if (skin == null) {
         skin = DefaultPlayerSkin.get(new UUID(0L, entry.uid())).texture();
      }

      graphics.blit(skin, x, y, 8, 8, 8.0F, 8.0F, 8, 8, 64, 64);
      graphics.blit(skin, x, y, 8, 8, 40.0F, 8.0F, 8, 8, 64, 64);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0) {
         if (BackButton.contains(this.originX, this.originY, mouseX, mouseY)) {
            ServerDex.requestMain();
            return true;
         }

         int row = this.rowAt(mouseX, mouseY);
         if (row >= 0) {
            this.select(this.board.top().get(row));
            return true;
         }
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   private int rowAt(double mouseX, double mouseY) {
      int left = this.originX + 110;
      int top = this.originY + 40;
      if (!(mouseX < left) && !(mouseX >= left + 215) && !(mouseY < top) && !(mouseY >= top + 140)) {
         int index = this.offset + (int)((mouseY - top) / 14.0);
         return index < this.board.top().size() ? index : -1;
      } else {
         return -1;
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double amountX, double amountY) {
      int max = Math.max(0, this.board.top().size() - 10);
      this.offset = Math.max(0, Math.min(max, this.offset - (int)Math.signum(amountY)));
      return true;
   }
}
