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
   private static final ResourceLocation MENU_FRAME_TEXTURE = ResourceLocation.fromNamespaceAndPath("cobblebattle", "textures/gui/leaderboard/base.png");
   private static final ResourceLocation MENU_CONTENT_TEXTURE = ResourceLocation.fromNamespaceAndPath("cobblebattle", "textures/gui/leaderboard/screen_1.png");
   private static final ResourceLocation CREATURE_PLATFORM_TEXTURE = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/pokedex/platform_base.png");
   private static final ResourceLocation CREATURE_PLATFORM_SHADOW_TEXTURE = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/pokedex/platform_shadow.png");
   private static final ResourceLocation UNKNOWN_CREATURE_TEXTURE = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/pokedex/platform_unknown.png");
   private static final int SCREEN_WIDTH = 345;
   private static final int SCREEN_HEIGHT = 207;
   private static final int CONTENT_LEFT_OFFSET = 18;
   private static final int CONTENT_TOP_OFFSET = 13;
   private static final int CONTENT_WIDTH = 309;
   private static final int CONTENT_HEIGHT = 183;
   private static final int ACCOUNT_LABEL_LEFT_OFFSET = 24;
   private static final int ACCOUNT_LABEL_TOP_OFFSET = 15;
   private static final int ACCOUNT_LABEL_WIDTH = 100;
   private static final int PROFILE_VIEWER_LEFT_OFFSET = 22;
   private static final int PROFILE_VIEWER_WIDTH = 126;
   private static final int PROFILE_VIEWER_TOP_OFFSET = 30;
   private static final int PLATFORM_TOP_OFFSET = 162;
   private static final int PLAYER_PORTRAIT_LEFT_OFFSET = 62;
   private static final int PLAYER_PORTRAIT_SIZE = 40;
   private static final int FEATURED_CREATURE_LEFT_OFFSET = 92;
   private static final int FEATURED_CREATURE_WIDTH = 56;
   private static final int PLATFORM_HEIGHT = 33;
   private static final int PLATFORM_DRAW_TOP_OFFSET = 150;
   private static final int PLATFORM_SHADOW_WIDTH = 50;
   private static final int PLATFORM_SHADOW_HEIGHT = 11;
   private static final int UNKNOWN_MARK_WIDTH = 28;
   private static final int UNKNOWN_MARK_HEIGHT = 32;
   private static final int MENU_COLUMN_COUNT = 2;
   private static final int MENU_ROW_COUNT = 3;
   private static final int MENU_GRID_LEFT_OFFSET = 150;
   private static final int MENU_GRID_TOP_OFFSET = 30;
   private static final int MENU_TILE_WIDTH = 84;
   private static final int MENU_TILE_HEIGHT = 50;
   private static final int MENU_TILE_HORIZONTAL_GAP = 8;
   private static final int MENU_TILE_VERTICAL_GAP = 7;
   private static final int MENU_TILE_FILL_COLOR = 1728053247;
   private static final int MENU_TILE_HOVER_COLOR = -1711276033;
   private static final int MENU_TILE_BORDER_COLOR = -1426063361;
   private static final int MENU_TILE_ACCENT_COLOR = -1;
   private static final int MENU_TILE_HOVER_ACCENT_COLOR = -2082246;
   private static final int PRIMARY_TEXT_COLOR = -1;
   private static final int SECONDARY_TEXT_COLOR = -1770753;
   private final OpenMainMenuPayload menuPayload;
   private final List<MainMenuScreen.MenuActionTile> actionTiles = new ArrayList<>();
   private PlayerPortrait playerPortrait;
   private RenderablePokemon featuredPokemon;
   private final FloatingState featuredPokemonPose = new FloatingState();
   private float featuredPokemonScale = 40.0F;
   private int screenLeft;
   private int screenTop;

   public MainMenuScreen(OpenMainMenuPayload menuPayload) {
      super(Component.translatable("cobblebattle.menu.title"));
      this.menuPayload = menuPayload;
   }

   protected void init() {
      this.screenLeft = (this.width - 345) / 2;
      this.screenTop = (this.height - 207) / 2;
      this.actionTiles.clear();
      this.actionTiles
         .add(
            new MainMenuScreen.MenuActionTile(
               Component.translatable("cobblebattle.menu.ranked"),
               Component.translatable("cobblebattle.menu.ranked_sub"),
               () -> Minecraft.getInstance().setScreen(new RankedScreen(this, this.menuPayload.competitions()))
            )
         );
      this.actionTiles
         .add(
            new MainMenuScreen.MenuActionTile(
               Component.translatable("cobblebattle.menu.rooms"), Component.translatable("cobblebattle.menu.rooms_sub"), ServerDex::requestRooms
            )
         );
      this.actionTiles
         .add(
            new MainMenuScreen.MenuActionTile(Component.translatable("cobblebattle.menu.dex"), Component.translatable("cobblebattle.menu.dex_sub"), ServerDex::requestDex)
         );
      this.actionTiles
         .add(
            new MainMenuScreen.MenuActionTile(
               Component.translatable("cobblebattle.menu.settings"),
               Component.translatable("cobblebattle.menu.settings_sub"),
               () -> Minecraft.getInstance().setScreen(new SettingsScreen(this))
            )
         );
      this.actionTiles
         .add(
            new MainMenuScreen.MenuActionTile(
               Component.translatable("cobblebattle.menu.chat"),
               Component.translatable("cobblebattle.menu.chat_sub"),
               () -> Minecraft.getInstance().setScreen(new ChatRoomScreen())
            )
         );
      this.actionTiles.add(new MainMenuScreen.MenuActionTile(Component.translatable("cobblebattle.menu.logout"), Component.translatable("cobblebattle.menu.logout_sub"), () -> {
         NetworkManager.sendToServer(new MenuActionPayload("logout", ""));
         this.onClose();
      }));
      LocalPlayer localPlayer = Minecraft.getInstance().player;
      if (localPlayer instanceof AbstractClientPlayer) {
         this.playerPortrait = PlayerPortrait.of(localPlayer);
      }

      this.featuredPokemon = null;
      this.featuredPokemonScale = 40.0F;
      if (!this.menuPayload.favourite().isEmpty()) {
         Species speciesTemplate = PokemonSpecies.getByName(this.menuPayload.favourite());
         if (speciesTemplate != null) {
            this.featuredPokemon = new RenderablePokemon(speciesTemplate, Set.of(), ItemStack.EMPTY);
            float featuredHitboxHeight = Math.max(0.1F, this.featuredPokemon.getForm().getHitbox().height());
            this.featuredPokemonScale = Math.min(40.0F, 126.0F / featuredHitboxHeight);
         }
      }
   }

   public boolean isPauseScreen() {
      return false;
   }

   public void render(GuiGraphics canvas, int pointerX, int pointerY, float frameDelta) {
      super.render(canvas, pointerX, pointerY, frameDelta);
      canvas.blit(MENU_CONTENT_TEXTURE, this.screenLeft, this.screenTop, 0.0F, 0.0F, 345, 207, 345, 207);
      Backdrop.draw(canvas, this.screenLeft, this.screenTop);
      int contentLeft = this.screenLeft + 18;
      int contentTop = this.screenTop + 13;
      canvas.enableScissor(contentLeft, contentTop, contentLeft + 309, contentTop + 183);

      try {
         this.renderHeader(canvas);
         this.renderProfileViewer(canvas, frameDelta);

         for (int actionIndex = 0; actionIndex < this.actionTiles.size() && actionIndex < 6; actionIndex++) {
            int tileLeft = this.tileLeftFor(actionIndex % 2);
            int tileTop = this.tileTopFor(actionIndex / 2);
            boolean isHovered = pointerX >= tileLeft && pointerX < tileLeft + 84 && pointerY >= tileTop && pointerY < tileTop + 50;
            this.renderMenuTile(canvas, this.actionTiles.get(actionIndex), tileLeft, tileTop, isHovered);
         }
      } finally {
         canvas.disableScissor();
      }

      canvas.blit(MENU_FRAME_TEXTURE, this.screenLeft, this.screenTop, 0.0F, 0.0F, 345, 207, 345, 207);
   }

   private void renderHeader(GuiGraphics canvas) {
      canvas.enableScissor(this.screenLeft + 24, this.screenTop + 15 - 2, this.screenLeft + 24 + 100, this.screenTop + 15 + 10);
      Ui.draw(canvas, this.font, Component.literal(this.menuPayload.nickname()), this.screenLeft + 24, this.screenTop + 15, -1, true);
      canvas.disableScissor();
      Component titleLabel = Component.translatable("cobblebattle.menu.title")
         .withStyle(textStyle -> textStyle.withFont(CobblemonResources.INSTANCE.getDEFAULT_LARGE()).withBold(true));
      Ui.draw(canvas, this.font, titleLabel, this.screenLeft + 322 - Ui.width(this.font, titleLabel), this.screenTop + 14, -1, true);
   }

   private void renderProfileViewer(GuiGraphics canvas, float frameDelta) {
      int viewerLeft = this.screenLeft + 22;
      int viewerTop = this.screenTop + 30;
      canvas.enableScissor(viewerLeft, viewerTop, viewerLeft + 126, this.screenTop + 150 + 33);

      try {
         canvas.blit(CREATURE_PLATFORM_TEXTURE, viewerLeft, this.screenTop + 150, 126, 33, 0.0F, 0.0F, 113, 30, 113, 30);
         this.renderPlatformShadow(canvas, this.screenLeft + 62);
         this.renderPlatformShadow(canvas, this.screenLeft + 92 + 28);
         if (this.playerPortrait != null && this.playerPortrait.entity() != null) {
            LeaderboardScreen.drawEntity(canvas, this.screenLeft + 62, this.screenTop + 162, 40, -35.0F, -10.0F, this.playerPortrait.entity());
         }

         if (this.featuredPokemon != null) {
            canvas.pose().pushPose();
            canvas.pose().translate(this.screenLeft + 92 + 28.0, this.screenTop + 162, 0.0);
            Quaternionf profileRotation = QuaternionUtilsKt.fromEulerXYZDegrees(new Quaternionf(), new Vector3f(5.0F, LeaderboardScreen.FACING, 0.0F));
            CobblemonCompat.drawProfile(
               this.featuredPokemon,
               canvas.pose(),
               profileRotation,
               PoseType.PROFILE,
               this.featuredPokemonPose,
               frameDelta,
               this.featuredPokemonScale,
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
         } else {
            UnknownMark.draw(canvas, this.screenLeft + 92 + 28, this.screenTop + 162, 32);
         }
      } finally {
         canvas.disableScissor();
      }
   }

   private void renderPlatformShadow(GuiGraphics canvas, int centerX) {
      canvas.blit(CREATURE_PLATFORM_SHADOW_TEXTURE, centerX - 25, this.screenTop + 162 - 5, 50, 11, 0.0F, 0.0F, 90, 20, 90, 20);
   }

   private void renderMenuTile(GuiGraphics canvas, MainMenuScreen.MenuActionTile menuTile, int tileLeft, int tileTop, boolean isHovered) {
      canvas.fill(tileLeft, tileTop, tileLeft + 84, tileTop + 50, isHovered ? -1711276033 : 1728053247);
      canvas.fill(tileLeft, tileTop, tileLeft + 84, tileTop + 1, -1426063361);
      canvas.fill(tileLeft, tileTop + 50 - 1, tileLeft + 84, tileTop + 50, -1426063361);
      canvas.fill(tileLeft, tileTop, tileLeft + 1, tileTop + 50, -1426063361);
      canvas.fill(tileLeft + 84 - 1, tileTop, tileLeft + 84, tileTop + 50, -1426063361);
      canvas.fill(tileLeft + 5, tileTop + 5, tileLeft + 8, tileTop + 50 - 5, isHovered ? -2082246 : -1);
      int textCenter = tileLeft + 8 + 38;
      Component tileTitle = menuTile.title().copy().withStyle(textStyle -> textStyle.withFont(CobblemonResources.INSTANCE.getDEFAULT_LARGE()).withBold(true));
      canvas.enableScissor(tileLeft + 9, tileTop, tileLeft + 84 - 2, tileTop + 50);
      Ui.drawCentered(canvas, this.font, tileTitle, textCenter, tileTop + 14, -1);
      Ui.drawCentered(canvas, this.font, menuTile.subtitle(), textCenter, tileTop + 30, -1770753);
      canvas.disableScissor();
   }

   private int tileLeftFor(int columnIndex) {
      return this.screenLeft + 150 + columnIndex * 92;
   }

   private int tileTopFor(int rowIndex) {
      return this.screenTop + 30 + rowIndex * 57;
   }

   public boolean mouseClicked(double pointerX, double pointerY, int clickButton) {
      if (clickButton == 0) {
         for (int actionIndex = 0; actionIndex < this.actionTiles.size() && actionIndex < 6; actionIndex++) {
            int tileLeft = this.tileLeftFor(actionIndex % 2);
            int tileTop = this.tileTopFor(actionIndex / 2);
            if (pointerX >= tileLeft && pointerX < tileLeft + 84 && pointerY >= tileTop && pointerY < tileTop + 50) {
               this.actionTiles.get(actionIndex).action().run();
               return true;
            }
         }
      }

      return super.mouseClicked(pointerX, pointerY, clickButton);
   }

   private record MenuActionTile(Component title, Component subtitle, Runnable action) {
   }
}
