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
import java.util.List;
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
import xiaocaoawa.minecraft.mod.cobblebattle.network.MenuActionPayload;
import xiaocaoawa.minecraft.mod.cobblebattle.network.OpenMainMenuPayload;

public final class MainMenuScreen extends Screen {
   private static final ResourceLocation BASE = ResourceLocation.fromNamespaceAndPath("cobblebattle", "textures/gui/leaderboard/base.png");
   private static final ResourceLocation SCREEN = ResourceLocation.fromNamespaceAndPath("cobblebattle", "textures/gui/leaderboard/screen_1.png");
   private static final ResourceLocation PLATFORM = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/pokedex/platform_base.png");
   private static final ResourceLocation PLATFORM_SHADOW = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/pokedex/platform_shadow.png");
   private static final ResourceLocation UNKNOWN = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/pokedex/platform_unknown.png");
   private static final int WIDTH = 345;
   private static final int HEIGHT = 207;
   private static final int HOLE_X = 18;
   private static final int HOLE_Y = 13;
   private static final int HOLE_W = 309;
   private static final int HOLE_H = 183;
   private static final int TAB_X = 24;
   private static final int TAB_Y = 15;
   private static final int TAB_W = 100;
   private static final int VIEWER_X = 22;
   private static final int VIEWER_W = 126;
   private static final int VIEWER_TOP = 30;
   private static final int FLOOR_Y = 162;
   private static final int PARTICIPANT_X = 62;
   private static final int PARTICIPANT_SIZE = 40;
   private static final int CREATURE_X = 92;
   private static final int CREATURE_W = 56;
   private static final int PLATFORM_H = 33;
   private static final int PLATFORM_Y = 150;
   private static final int SHADOW_W = 50;
   private static final int SHADOW_H = 11;
   private static final int UNKNOWN_W = 28;
   private static final int UNKNOWN_H = 32;
   private static final int COLS = 2;
   private static final int ROWS = 3;
   private static final int GRID_X = 150;
   private static final int GRID_Y = 30;
   private static final int TILE_W = 84;
   private static final int TILE_H = 50;
   private static final int GAP_X = 8;
   private static final int GAP_Y = 7;
   private static final int TILE_FILL = 1728053247;
   private static final int TILE_FILL_HOVER = -1711276033;
   private static final int TILE_EDGE = -1426063361;
   private static final int TILE_BAR = -1;
   private static final int TILE_BAR_HOVER = -2082246;
   private static final int TEXT = -1;
   private static final int TEXT_SOFT = -1770753;
   private final OpenMainMenuPayload data;
   private final List<MainMenuScreen.Tile> menu = new ArrayList<>();
   private PlayerPortrait portrait;
   private RenderablePokemon favourite;
   private final FloatingState creatureState = new FloatingState();
   private float creatureScale = 40.0F;
   private int originX;
   private int originY;

   public MainMenuScreen(OpenMainMenuPayload data) {
      super(Component.translatable("cobblebattle.menu.title"));
      this.data = data;
   }

   protected void init() {
      this.originX = (this.width - 345) / 2;
      this.originY = (this.height - 207) / 2;
      this.menu.clear();
      this.menu
         .add(
            new MainMenuScreen.Tile(
               Component.translatable("cobblebattle.menu.ranked"),
               Component.translatable("cobblebattle.menu.ranked_sub"),
               () -> Minecraft.getInstance().setScreen(new RankedScreen(this, this.data.competitions()))
            )
         );
      this.menu
         .add(
            new MainMenuScreen.Tile(
               Component.translatable("cobblebattle.menu.rooms"), Component.translatable("cobblebattle.menu.rooms_sub"), ServerDex::requestRooms
            )
         );
      this.menu
         .add(
            new MainMenuScreen.Tile(Component.translatable("cobblebattle.menu.dex"), Component.translatable("cobblebattle.menu.dex_sub"), ServerDex::requestDex)
         );
      this.menu
         .add(
            new MainMenuScreen.Tile(
               Component.translatable("cobblebattle.menu.settings"),
               Component.translatable("cobblebattle.menu.settings_sub"),
               () -> Minecraft.getInstance().setScreen(new SettingsScreen(this))
            )
         );
      this.menu
         .add(
            new MainMenuScreen.Tile(
               Component.translatable("cobblebattle.menu.chat"),
               Component.translatable("cobblebattle.menu.chat_sub"),
               () -> Minecraft.getInstance().setScreen(new ChatRoomScreen())
            )
         );
      this.menu.add(new MainMenuScreen.Tile(Component.translatable("cobblebattle.menu.logout"), Component.translatable("cobblebattle.menu.logout_sub"), () -> {
         NetworkManager.sendToServer(new MenuActionPayload("logout", ""));
         this.onClose();
      }));
      LocalPlayer blocks = Minecraft.getInstance().player;
      if (blocks instanceof AbstractClientPlayer) {
         this.portrait = PlayerPortrait.of(blocks);
      }

      this.favourite = null;
      this.creatureScale = 40.0F;
      if (!this.data.favourite().isEmpty()) {
         Species speciesTemplate = PokemonSpecies.getByName(this.data.favourite());
         if (speciesTemplate != null) {
            this.favourite = new RenderablePokemon(speciesTemplate, Set.of(), ItemStack.EMPTY);
            float blocksx = Math.max(0.1F, this.favourite.getForm().getHitbox().height());
            this.creatureScale = Math.min(40.0F, 126.0F / blocksx);
         }
      }
   }

   public boolean isPauseScreen() {
      return false;
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      super.render(graphics, mouseX, mouseY, partialTick);
      graphics.blit(SCREEN, this.originX, this.originY, 0.0F, 0.0F, 345, 207, 345, 207);
      Backdrop.draw(graphics, this.originX, this.originY);
      int left = this.originX + 18;
      int top = this.originY + 13;
      graphics.enableScissor(left, top, left + 309, top + 183);

      try {
         this.drawHeader(graphics);
         this.drawViewer(graphics, partialTick);

         for (int i = 0; i < this.menu.size() && i < 6; i++) {
            int x = this.tileX(i % 2);
            int y = this.tileY(i / 2);
            boolean hover = mouseX >= x && mouseX < x + 84 && mouseY >= y && mouseY < y + 50;
            this.drawTile(graphics, this.menu.get(i), x, y, hover);
         }
      } finally {
         graphics.disableScissor();
      }

      graphics.blit(BASE, this.originX, this.originY, 0.0F, 0.0F, 345, 207, 345, 207);
   }

   private void drawHeader(GuiGraphics graphics) {
      graphics.enableScissor(this.originX + 24, this.originY + 15 - 2, this.originX + 24 + 100, this.originY + 15 + 10);
      Ui.draw(graphics, this.font, Component.literal(this.data.nickname()), this.originX + 24, this.originY + 15, -1, true);
      graphics.disableScissor();
      Component page = Component.translatable("cobblebattle.menu.title")
         .withStyle(style -> style.withFont(CobblemonResources.INSTANCE.getDEFAULT_LARGE()).withBold(true));
      Ui.draw(graphics, this.font, page, this.originX + 322 - Ui.width(this.font, page), this.originY + 14, -1, true);
   }

   private void drawViewer(GuiGraphics graphics, float partialTick) {
      int paneLeft = this.originX + 22;
      int paneTop = this.originY + 30;
      graphics.enableScissor(paneLeft, paneTop, paneLeft + 126, this.originY + 150 + 33);

      try {
         graphics.blit(PLATFORM, paneLeft, this.originY + 150, 126, 33, 0.0F, 0.0F, 113, 30, 113, 30);
         this.drawShadow(graphics, this.originX + 62);
         this.drawShadow(graphics, this.originX + 92 + 28);
         if (this.portrait != null && this.portrait.entity() != null) {
            LeaderboardScreen.drawEntity(graphics, this.originX + 62, this.originY + 162, 40, -35.0F, -10.0F, this.portrait.entity());
         }

         if (this.favourite != null) {
            graphics.pose().pushPose();
            graphics.pose().translate(this.originX + 92 + 28.0, this.originY + 162, 0.0);
            Quaternionf rotation = QuaternionUtilsKt.fromEulerXYZDegrees(new Quaternionf(), new Vector3f(5.0F, LeaderboardScreen.FACING, 0.0F));
            CobblemonCompat.drawProfile(
               this.favourite,
               graphics.pose(),
               rotation,
               PoseType.PROFILE,
               this.creatureState,
               partialTick,
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
            graphics.pose().popPose();
         } else {
            UnknownMark.draw(graphics, this.originX + 92 + 28, this.originY + 162, 32);
         }
      } finally {
         graphics.disableScissor();
      }
   }

   private void drawShadow(GuiGraphics graphics, int centreX) {
      graphics.blit(PLATFORM_SHADOW, centreX - 25, this.originY + 162 - 5, 50, 11, 0.0F, 0.0F, 90, 20, 90, 20);
   }

   private void drawTile(GuiGraphics graphics, MainMenuScreen.Tile tile, int x, int y, boolean hover) {
      graphics.fill(x, y, x + 84, y + 50, hover ? -1711276033 : 1728053247);
      graphics.fill(x, y, x + 84, y + 1, -1426063361);
      graphics.fill(x, y + 50 - 1, x + 84, y + 50, -1426063361);
      graphics.fill(x, y, x + 1, y + 50, -1426063361);
      graphics.fill(x + 84 - 1, y, x + 84, y + 50, -1426063361);
      graphics.fill(x + 5, y + 5, x + 8, y + 50 - 5, hover ? -2082246 : -1);
      int centre = x + 8 + 38;
      Component title = tile.title().copy().withStyle(style -> style.withFont(CobblemonResources.INSTANCE.getDEFAULT_LARGE()).withBold(true));
      graphics.enableScissor(x + 9, y, x + 84 - 2, y + 50);
      Ui.drawCentered(graphics, this.font, title, centre, y + 14, -1);
      Ui.drawCentered(graphics, this.font, tile.subtitle(), centre, y + 30, -1770753);
      graphics.disableScissor();
   }

   private int tileX(int col) {
      return this.originX + 150 + col * 92;
   }

   private int tileY(int row) {
      return this.originY + 30 + row * 57;
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0) {
         for (int i = 0; i < this.menu.size() && i < 6; i++) {
            int x = this.tileX(i % 2);
            int y = this.tileY(i / 2);
            if (mouseX >= x && mouseX < x + 84 && mouseY >= y && mouseY < y + 50) {
               this.menu.get(i).action().run();
               return true;
            }
         }
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   private record Tile(Component title, Component subtitle, Runnable action) {
   }
}
