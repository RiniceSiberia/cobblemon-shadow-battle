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
   private static final ResourceLocation BASE = ResourceLocation.fromNamespaceAndPath("cobblebattle", "textures/gui/leaderboard/base.png");
   private static final ResourceLocation SCREEN = ResourceLocation.fromNamespaceAndPath("cobblebattle", "textures/gui/leaderboard/screen_2.png");
   private static final ResourceLocation PLATFORM = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/pokedex/platform_base.png");
   private static final ResourceLocation PLATFORM_SHADOW = ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/pokedex/platform_shadow.png");
   private static final int WIDTH = 345;
   private static final int HEIGHT = 207;
   private static final int HOLE_X = 18;
   private static final int HOLE_Y = 13;
   private static final int HOLE_W = 309;
   private static final int HOLE_H = 183;
   private static final int TITLE_X = 24;
   private static final int TITLE_Y = 15;
   private static final int MIRROR = 345;
   private static final int SLOT_W = 72;
   private static final int SLOT_H = 22;
   private static final int SLOT_PITCH = 24;
   private static final int SLOT_Y = 30;
   private static final int SLOTS = 6;
   private static final int MINE_X = 23;
   private static final int THEIRS_X = 250;
   private static final int SLOT_ICON = 24;
   private static final int PANE_W = 68;
   private static final int PANE_Y = 30;
   private static final int PANE_H = 142;
   private static final int MINE_PANE_X = 98;
   private static final int THEIRS_PANE_X = 179;
   private static final int NAME_H = 12;
   private static final int PLATFORM_H = 18;
   private static final int PLATFORM_Y = 120;
   private static final int FLOOR_Y = 127;
   private static final int PLAYER_SIZE = 34;
   private static final int SHADOW_W = 40;
   private static final int SHADOW_H = 9;
   private static final int STATUS_Y = 148;
   private static final int CONFIRM_W = 61;
   private static final int CONFIRM_X = 142;
   private static final int CONFIRM_Y = 176;
   private static final int CONFIRM_H = 15;
   private static final int PLATE = 1442840575;
   private static final int PLATE_MINE = 1429903030;
   private static final int PLATE_THEIRS = 1438796619;
   private static final int PLATE_PICKED = -1438665056;
   private static final int PLATE_HOVER = -1996488705;
   private static final int EDGE = -1426063361;
   private static final int EDGE_PICKED = -4590113;
   private static final int NAME_MINE = -869302372;
   private static final int NAME_THEIRS = -862176710;
   private static final int TEXT = -1;
   private static final int TEXT_SOFT = -1770753;
   private static final int TEXT_DIM = -6303010;
   private static final int BADGE = -14718116;
   private static final int BADGE_LEAD = -875716;
   private static final int FILL = 1728053247;
   private static final int FILL_HOVER = -1711276033;
   private static final int FILL_OFF = 872415231;
   private static final int URGENT = -30107;
   private TeamPreviewPayload preview;
   private final List<Integer> picks = new ArrayList<>();
   private final Map<String, RenderablePokemon> models = new HashMap<>();
   private final Map<String, FloatingState> poses = new HashMap<>();
   private PlayerPortrait theirPortrait;
   private int originX;
   private int originY;

   public TeamPreviewScreen(TeamPreviewPayload preview) {
      super(Component.translatable("cobblebattle.preview.title"));
      this.preview = preview;
   }

   public String battleId() {
      return this.preview.battleId();
   }

   public void update(TeamPreviewPayload preview) {
      this.preview = preview;
   }

   protected void init() {
      this.originX = (this.width - 345) / 2;
      this.originY = (this.height - 207) / 2;
   }

   public boolean isPauseScreen() {
      return false;
   }

   public boolean shouldCloseOnEsc() {
      return this.over();
   }

   private boolean over() {
      return !this.preview.closed().isEmpty() || System.currentTimeMillis() >= this.preview.deadlineMs();
   }

   public void tick() {
      if (CobblemonClient.INSTANCE.getBattle() != null) {
         Minecraft.getInstance().setScreen(null);
      }
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
         this.drawColumn(graphics, 23, this.preview.mine(), true, mouseX, mouseY, partialTick);
         this.drawColumn(graphics, 250, this.preview.theirs(), false, mouseX, mouseY, partialTick);
         this.drawTrainer(graphics, 98, true, partialTick);
         this.drawTrainer(graphics, 179, false, partialTick);
         this.drawConfirm(graphics, mouseX, mouseY);
      } finally {
         graphics.disableScissor();
      }

      graphics.blit(BASE, this.originX, this.originY, 0.0F, 0.0F, 345, 207, 345, 207);
      this.drawTooltip(graphics, mouseX, mouseY);
   }

   private void drawHeader(GuiGraphics graphics) {
      Ui.draw(graphics, this.font, Component.translatable("cobblebattle.preview.title"), this.originX + 24, this.originY + 15, -1, true);
      Component counts = (Component)(this.preview.lead() > 1
         ? Ui.join(
            Component.translatable("cobblebattle.preview.count", new Object[]{this.picks.size(), this.preview.pick()}),
            Component.translatable("cobblebattle.preview.lead", new Object[]{this.preview.lead()})
         )
         : Component.translatable("cobblebattle.preview.count", new Object[]{this.picks.size(), this.preview.pick()}));
      Component right = (Component)(this.over() && !this.preview.closed().isEmpty()
         ? Component.translatable("cobblebattle.preview.over")
         : Ui.join(counts, this.clock()));
      Ui.draw(graphics, this.font, right, this.originX + 322 - Ui.width(this.font, right), this.originY + 14, -1770753, true);
   }

   private Component clock() {
      long left = Math.max(0L, this.preview.deadlineMs() - System.currentTimeMillis());
      long seconds = (left + 999L) / 1000L;
      return Component.literal(String.format("%d:%02d", seconds / 60L, seconds % 60L));
   }

   private void drawColumn(GuiGraphics graphics, int x, List<TeamPreviewPayload.Slot> team, boolean mine, int mouseX, int mouseY, float partialTick) {
      for (int i = 0; i < 6; i++) {
         int px = this.originX + x;
         int py = this.originY + 30 + i * 24;
         TeamPreviewPayload.Slot slot = i < team.size() ? team.get(i) : null;
         int order = mine ? this.picks.indexOf(i) : -1;
         boolean hover = slot != null && mine && !this.locked() && mouseX >= px && mouseX < px + 72 && mouseY >= py && mouseY < py + 22;
         int fill;
         if (slot == null) {
            fill = 1442840575;
         } else if (order >= 0) {
            fill = -1438665056;
         } else if (hover) {
            fill = -1996488705;
         } else {
            fill = mine ? 1429903030 : 1438796619;
         }

         graphics.fill(px, py, px + 72, py + 22, fill);
         int edge = order >= 0 ? -4590113 : -1426063361;
         graphics.fill(px, py, px + 72, py + 1, edge);
         graphics.fill(px, py + 22 - 1, px + 72, py + 22, edge);
         graphics.fill(px, py, px + 1, py + 22, edge);
         graphics.fill(px + 72 - 1, py, px + 72, py + 22, edge);
         if (slot != null) {
            int iconX = mine ? px : px + 72 - 24;
            this.drawPokemon(graphics, slot, iconX, py, partialTick);
            int textLeft = mine ? px + 24 + 2 : px + 3;
            int textRight = mine ? px + 72 - 3 : px + 72 - 24 - 2;
            graphics.enableScissor(textLeft, py, textRight, py + 22);

            try {
               Component name = speciesName(slot);
               Ui.draw(graphics, this.font, name, textLeft, py + 3, -1, false);
               Component sub = Component.translatable("cobblebattle.preview.level", new Object[]{slot.level()}).copy().append(gender(slot));
               Ui.draw(graphics, this.font, sub, textLeft, py + 12, -6303010, false);
            } finally {
               graphics.disableScissor();
            }

            if (order >= 0) {
               boolean leads = order < this.preview.lead();
               int badgeX = mine ? px + 72 - 9 : px + 1;
               graphics.fill(badgeX, py + 1, badgeX + 8, py + 9, leads ? -875716 : -14718116);
               Ui.drawCentered(graphics, this.font, String.valueOf(order + 1), badgeX + 4, py + 1, leads ? -1 : -6303010);
            }
         }
      }
   }

   private void drawPokemon(GuiGraphics graphics, TeamPreviewPayload.Slot slot, int x, int y, float partialTick) {
      RenderablePokemon model = this.modelOf(slot);
      int centreX = x + 12;
      int floor = y + 22 - 2;
      if (model == null) {
         UnknownMark.draw(graphics, centreX, floor, 18);
      } else {
         graphics.enableScissor(x, y, x + 24, y + 22);

         try {
            float blocks = Math.max(0.1F, model.getForm().getHitbox().height());
            float scale = Math.min(18.0F, 19.0F / blocks);
            graphics.pose().pushPose();
            graphics.pose().translate(centreX, floor, 0.0);
            Quaternionf rotation = QuaternionUtilsKt.fromEulerXYZDegrees(new Quaternionf(), new Vector3f(5.0F, LeaderboardScreen.FACING, 0.0F));
            CobblemonCompat.drawProfile(
               model,
               graphics.pose(),
               rotation,
               PoseType.PROFILE,
               (PosableState)this.poses.computeIfAbsent(key(slot), k -> new FloatingState()),
               partialTick,
               scale,
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
         } finally {
            graphics.disableScissor();
         }
      }
   }

   private void drawTrainer(GuiGraphics graphics, int x, boolean mine, float partialTick) {
      int px = this.originX + x;
      int py = this.originY + 30;
      graphics.fill(px, py, px + 68, py + 142, mine ? 1429903030 : 1438796619);
      graphics.fill(px, py, px + 68, py + 1, -1426063361);
      graphics.fill(px, py + 142 - 1, px + 68, py + 142, -1426063361);
      graphics.fill(px, py, px + 1, py + 142, -1426063361);
      graphics.fill(px + 68 - 1, py, px + 68, py + 142, -1426063361);
      graphics.fill(px + 1, py + 1, px + 68 - 1, py + 12, mine ? -869302372 : -862176710);
      String who = mine ? this.preview.you() : this.preview.opponent();
      graphics.enableScissor(px + 2, py + 1, px + 68 - 2, py + 12);
      Ui.drawCentered(graphics, this.font, who, px + 34, py + 3, -1);
      graphics.disableScissor();
      graphics.enableScissor(px + 1, py + 12, px + 68 - 1, this.originY + 120 + 18);

      try {
         graphics.blit(PLATFORM, px, this.originY + 120, 68, 18, 0.0F, 0.0F, 113, 30, 113, 30);
         int centreX = px + 34;
         graphics.blit(PLATFORM_SHADOW, centreX - 20, this.originY + 127 - 4, 40, 9, 0.0F, 0.0F, 90, 20, 90, 20);
         PlayerPortrait portrait = this.portraitOf(mine);
         if (portrait != null && portrait.entity() != null) {
            LeaderboardScreen.drawEntity(graphics, centreX, this.originY + 127, 34, mine ? -35.0F : 35.0F, -10.0F, portrait.entity());
         }
      } finally {
         graphics.disableScissor();
      }

      boolean ready = mine ? this.preview.mineReady() : this.preview.theirsReady();
      Component status = ready ? Component.translatable("cobblebattle.preview.ready") : Component.translatable("cobblebattle.preview.choosing");
      Ui.drawCentered(graphics, this.font, status, px + 34, this.originY + 148, ready ? -4590113 : -6303010);
      if (!mine) {
         Ui.drawCentered(
            graphics,
            this.font,
            Component.translatable("cobblebattle.preview.from", new Object[]{this.preview.opponentServer()}),
            px + 34,
            this.originY + 148 + 10,
            -6303010
         );
      }
   }

   private PlayerPortrait portraitOf(boolean mine) {
      Minecraft minecraft = Minecraft.getInstance();
      if (mine) {
         LocalPlayer var4 = minecraft.player;
         return var4 instanceof AbstractClientPlayer ? PlayerPortrait.of(var4) : null;
      } else {
         if (this.theirPortrait == null) {
            this.theirPortrait = PlayerPortrait.lookup(this.preview.opponent(), 0L);
         }

         return this.theirPortrait;
      }
   }

   private boolean locked() {
      return this.preview.mineReady() || this.over();
   }

   private boolean confirmable() {
      return !this.locked() && this.picks.size() == this.preview.pick();
   }

   private void drawConfirm(GuiGraphics graphics, int mouseX, int mouseY) {
      int x = this.originX + 142;
      int y = this.originY + 176;
      boolean ready = this.confirmable();
      boolean hover = ready && this.inConfirm(mouseX, mouseY);
      graphics.fill(x, y, x + 61, y + 15, ready ? (hover ? -1711276033 : 1728053247) : 872415231);
      graphics.fill(x, y, x + 61, y + 1, -1426063361);
      graphics.fill(x, y + 15 - 1, x + 61, y + 15, -1426063361);
      graphics.fill(x, y, x + 1, y + 15, -1426063361);
      graphics.fill(x + 61 - 1, y, x + 61, y + 15, -1426063361);
      Component label;
      if (!this.preview.closed().isEmpty()) {
         label = Component.translatable("cobblebattle.preview.closed");
      } else if (this.preview.mineReady()) {
         label = Component.translatable(this.preview.theirsReady() ? "cobblebattle.preview.starting" : "cobblebattle.preview.waiting");
      } else if (this.picks.size() < this.preview.pick()) {
         label = Component.translatable("cobblebattle.preview.pick_more", new Object[]{this.preview.pick() - this.picks.size()});
      } else {
         label = Component.translatable("cobblebattle.preview.confirm");
      }

      Ui.drawCentered(graphics, this.font, label, x + 30, y + 4, ready ? -1 : -1770753);
   }

   private boolean inConfirm(double mouseX, double mouseY) {
      int x = this.originX + 142;
      int y = this.originY + 176;
      return mouseX >= x && mouseX < x + 61 && mouseY >= y && mouseY < y + 15;
   }

   private void drawTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
      TeamPreviewPayload.Slot slot = this.slotAt(mouseX, mouseY);
      if (slot != null) {
         List<Component> lines = new ArrayList<>(3);
         lines.add(speciesName(slot));
         lines.add(Component.translatable("cobblebattle.preview.level", new Object[]{slot.level()}).copy().append(gender(slot)));
         if (slot.shiny()) {
            lines.add(Component.translatable("cobblebattle.preview.shiny"));
         }

         if (!slot.item().isEmpty()) {
            lines.add(Component.translatable("cobblebattle.preview.item", new Object[]{itemName(slot.item())}));
         }

         graphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
      }
   }

   private TeamPreviewPayload.Slot slotAt(double mouseX, double mouseY) {
      for (int i = 0; i < 6; i++) {
         int py = this.originY + 30 + i * 24;
         if (!(mouseY < py) && !(mouseY >= py + 22)) {
            int mineX = this.originX + 23;
            if (mouseX >= mineX && mouseX < mineX + 72 && i < this.preview.mine().size()) {
               return this.preview.mine().get(i);
            }

            int theirsX = this.originX + 250;
            if (mouseX >= theirsX && mouseX < theirsX + 72 && i < this.preview.theirs().size()) {
               return this.preview.theirs().get(i);
            }
         }
      }

      return null;
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button != 0) {
         return super.mouseClicked(mouseX, mouseY, button);
      } else if (this.confirmable() && this.inConfirm(mouseX, mouseY)) {
         this.send();
         return true;
      } else if (this.locked()) {
         return super.mouseClicked(mouseX, mouseY, button);
      } else {
         for (int i = 0; i < Math.min(6, this.preview.mine().size()); i++) {
            int px = this.originX + 23;
            int py = this.originY + 30 + i * 24;
            if (!(mouseX < px) && !(mouseX >= px + 72) && !(mouseY < py) && !(mouseY >= py + 22)) {
               this.toggle(i);
               return true;
            }
         }

         return super.mouseClicked(mouseX, mouseY, button);
      }
   }

   private void toggle(int slot) {
      if (!this.picks.remove(Integer.valueOf(slot))) {
         if (this.picks.size() < this.preview.pick()) {
            this.picks.add(slot);
         }
      }
   }

   private void send() {
      this.preview = new TeamPreviewPayload(
         this.preview.battleId(),
         this.preview.you(),
         this.preview.opponent(),
         this.preview.opponentServer(),
         this.preview.pick(),
         this.preview.lead(),
         this.preview.deadlineMs(),
         this.preview.mine(),
         this.preview.theirs(),
         true,
         this.preview.theirsReady(),
         this.preview.closed()
      );
      NetworkManager.sendToServer(new TeamPickPayload(this.preview.battleId(), List.copyOf(this.picks)));
   }

   private static String key(TeamPreviewPayload.Slot slot) {
      return slot.species() + "/" + slot.shiny() + "/" + slot.gender();
   }

   private static Component speciesName(TeamPreviewPayload.Slot slot) {
      Species species = PokemonSpecies.getByName(slot.species());
      return species == null ? Component.literal(slot.species()) : species.getTranslatedName();
   }

   private static Component gender(TeamPreviewPayload.Slot slot) {
      if ("M".equalsIgnoreCase(slot.gender())) {
         return Ui.plain(" ♂");
      } else {
         return (Component)("F".equalsIgnoreCase(slot.gender()) ? Ui.plain(" ♀") : Component.empty());
      }
   }

   private static Component itemName(String id) {
      ResourceLocation location = ResourceLocation.tryBuild("cobblemon", id);
      if (location != null && BuiltInRegistries.ITEM.containsKey(location)) {
         Item item = (Item)BuiltInRegistries.ITEM.get(location);
         return new ItemStack(item).getHoverName();
      } else {
         ResourceLocation vanilla = ResourceLocation.tryBuild("minecraft", id);
         return (Component)(vanilla != null && BuiltInRegistries.ITEM.containsKey(vanilla)
            ? new ItemStack((ItemLike)BuiltInRegistries.ITEM.get(vanilla)).getHoverName()
            : Component.literal(id));
      }
   }

   private RenderablePokemon modelOf(TeamPreviewPayload.Slot slot) {
      return this.models.computeIfAbsent(key(slot), k -> {
         Species species = PokemonSpecies.getByName(slot.species());
         if (species == null) {
            return null;
         } else {
            Set<String> aspects = new LinkedHashSet<>();
            if (slot.shiny()) {
               aspects.add("shiny");
            }

            if ("M".equalsIgnoreCase(slot.gender())) {
               aspects.add("male");
            }

            if ("F".equalsIgnoreCase(slot.gender())) {
               aspects.add("female");
            }

            return new RenderablePokemon(species, aspects, ItemStack.EMPTY);
         }
      });
   }

   private static Component large(String text) {
      return Component.literal(text).withStyle(style -> style.withFont(CobblemonResources.INSTANCE.getDEFAULT_LARGE()).withBold(true));
   }
}
