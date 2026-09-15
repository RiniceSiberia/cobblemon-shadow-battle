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
   private static final ResourceLocation BASE = ResourceLocation.fromNamespaceAndPath("cobblebattle", "textures/gui/leaderboard/base.png");
   private static final ResourceLocation SCREEN = ResourceLocation.fromNamespaceAndPath("cobblebattle", "textures/gui/leaderboard/screen_1.png");
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
   private static final int SEAT_W = 138;
   private static final int SEAT_Y = 28;
   private static final int SEAT_H = 128;
   private static final int LEFT_X = 22;
   private static final int RIGHT_X = 186;
   private static final int NAME_H = 12;
   private static final int FLOOR_Y = 138;
   private static final int PLATFORM_H = 36;
   private static final int PLATFORM_Y = 126;
   private static final int PARTICIPANT_SIZE = 34;
   private static final int SHADOW_W = 44;
   private static final int SHADOW_H = 10;
   private static final int UNKNOWN_H = 28;
   private static final int CREATURE_OFFSET = 44;
   private static final int BENCH_X = 22;
   private static final int BENCH_Y = 160;
   private static final int BENCH_W = 302;
   private static final int BENCH_H = 20;
   private static final int START_X = 130;
   private static final int START_Y = 183;
   private static final int START_W = 84;
   private static final int START_H = 12;
   private static final int INVITE_X = 22;
   private static final int INVITE_Y = 185;
   private static final int INVITE_W = 102;
   private static final int PANEL_HOST = 1429903030;
   private static final int PANEL_GUEST = 1438796619;
   private static final int PANEL_EMPTY = 872415231;
   private static final int EDGE = -1426063361;
   private static final int NAME_HOST = -869302372;
   private static final int NAME_GUEST = -862176710;
   private static final int TEXT = -1;
   private static final int TEXT_SOFT = -1770753;
   private static final int START_FILL = 1728053247;
   private static final int START_FILL_HOVER = -1711276033;
   private static final int START_FILL_OFF = 872415231;
   private RoomStatePayload state;
   private final Map<String, PlayerPortrait> portraits = new HashMap<>();
   private final Map<String, RenderablePokemon> leads = new HashMap<>();
   private final Map<String, FloatingState> poses = new HashMap<>();
   private int originX;
   private int originY;
   private final RoomInteractionState interactionState = new RoomInteractionState();

   public RoomScreen(RoomStatePayload state) {
      super(Component.translatable("cobblebattle.room.screen_title"));
      this.state = state;
   }

   public void update(RoomStatePayload state) {
      this.state = state;
   }

   public String roomId() {
      return this.state.roomId();
   }

   protected void init() {
      this.originX = (this.width - 345) / 2;
      this.originY = (this.height - 207) / 2;
   }

   public boolean isPauseScreen() {
      return false;
   }

   public void tick() {
      this.interactionState.tick();

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
         this.drawSeat(graphics, 22, this.state.host(), true, true, partialTick);
         this.drawSeat(graphics, 186, this.state.guest(), this.state.hasGuest(), false, partialTick);
         this.drawVersus(graphics);
         this.drawBench(graphics);
         this.drawInvite(graphics, mouseX, mouseY);
         this.drawStart(graphics, mouseX, mouseY);
         BackButton.draw(graphics, this.font, this.originX, this.originY, mouseX, mouseY);
      } finally {
         graphics.disableScissor();
      }

      graphics.blit(BASE, this.originX, this.originY, 0.0F, 0.0F, 345, 207, 345, 207);
   }

   private void drawHeader(GuiGraphics graphics) {
      Ui.draw(graphics, this.font, this.state.name(), this.originX + 24, this.originY + 15, -1, true);
      Component type = Component.translatable("cobblebattle.room.type." + this.state.battleType());
      Component level = this.state.level() > 0
         ? Component.translatable("cobblebattle.room.level_at", new Object[]{this.state.level()})
         : Component.translatable("cobblebattle.room.level_free");
      List<Component> parts = new ArrayList<>(List.of(type, level));
      if (this.state.pick() > 0 && this.state.pick() < 6) {
         parts.add(Component.translatable("cobblebattle.room.pick_tag", new Object[]{this.state.pick()}));
      }

      if (this.state.hostEngine()) {
         parts.add(Component.translatable("cobblebattle.room.engine_host_tag"));
      }

      if (!this.state.legality()) {
         parts.add(Component.translatable("cobblebattle.room.legality_tag"));
      }

      Component rules = Ui.join(parts.toArray(new Component[0]));
      Ui.draw(graphics, this.font, rules, this.originX + 322 - Ui.width(this.font, rules), this.originY + 14, -1770753, true);
   }

   private void drawSeat(GuiGraphics graphics, int x, RoomStatePayload.Member member, boolean taken, boolean host, float partialTick) {
      int px = this.originX + x;
      int py = this.originY + 28;
      graphics.fill(px, py, px + 138, py + 128, taken ? (host ? 1429903030 : 1438796619) : 872415231);
      graphics.fill(px, py, px + 138, py + 1, -1426063361);
      graphics.fill(px, py + 128 - 1, px + 138, py + 128, -1426063361);
      graphics.fill(px, py, px + 1, py + 128, -1426063361);
      graphics.fill(px + 138 - 1, py, px + 138, py + 128, -1426063361);
      graphics.fill(px + 1, py + 1, px + 138 - 1, py + 12, host ? -869302372 : -862176710);
      Component role = Component.translatable(host ? "cobblebattle.room.host_seat" : "cobblebattle.room.guest_seat");
      if (host) {
         Ui.draw(graphics, this.font, role, px + 5, py + 3, -1770753, false);
         Ui.draw(graphics, this.font, member.name(), px + 5 + Ui.width(this.font, role) + 6, py + 3, -1, true);
      } else {
         Ui.draw(graphics, this.font, role, px + 138 - 5 - Ui.width(this.font, role), py + 3, -1770753, false);
         if (taken) {
            Component name = Component.literal(member.name());
            Ui.draw(graphics, this.font, name, px + 138 - 11 - Ui.width(this.font, role) - Ui.width(this.font, name), py + 3, -1, true);
         }
      }

      if (!taken) {
         Ui.drawCentered(graphics, this.font, Component.translatable("cobblebattle.room.waiting_seat"), px + 69, py + 64 - 4, -1770753);
      } else {
         graphics.enableScissor(px + 1, py + 12, px + 138 - 1, this.originY + 126 + 36);

         try {
            graphics.blit(PLATFORM, px, this.originY + 126, 138, 36, 0.0F, 0.0F, 113, 30, 113, 30);
            int participantX = px + 69 - 22 - 4;
            int creatureX = px + 69 + 22 + 4;
            this.drawShadow(graphics, participantX);
            this.drawShadow(graphics, creatureX);
            PlayerPortrait portrait = this.portraitOf(member, host);
            if (portrait != null && portrait.entity() != null) {
               LeaderboardScreen.drawEntity(graphics, participantX, this.originY + 138, 34, -35.0F, -10.0F, portrait.entity());
            }

            RenderablePokemon lead = this.leadOf(member);
            if (lead != null) {
               FloatingState pose = this.poses.computeIfAbsent(key(member), k -> new FloatingState());
               float blocks = Math.max(0.1F, lead.getForm().getHitbox().height());
               float scale = Math.min(34.0F, 94.0F / blocks);
               graphics.pose().pushPose();
               graphics.pose().translate(creatureX, this.originY + 138, 0.0);
               Quaternionf rotation = QuaternionUtilsKt.fromEulerXYZDegrees(new Quaternionf(), new Vector3f(5.0F, LeaderboardScreen.FACING, 0.0F));
               CobblemonCompat.drawProfile(
                  lead, graphics.pose(), rotation, PoseType.PROFILE, pose, partialTick, scale, true, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F, 0.0F, 13
               );
               graphics.pose().popPose();
            } else {
               UnknownMark.draw(graphics, creatureX, this.originY + 138, 28);
            }
         } finally {
            graphics.disableScissor();
         }

         Ui.drawCentered(
            graphics, this.font, Component.translatable("cobblebattle.room.team", new Object[]{member.teamSize()}), px + 69, py + 128 - 12, -1770753
         );
      }
   }

   private void drawShadow(GuiGraphics graphics, int centreX) {
      graphics.blit(PLATFORM_SHADOW, centreX - 22, this.originY + 138 - 5, 44, 10, 0.0F, 0.0F, 90, 20, 90, 20);
   }

   private void drawVersus(GuiGraphics graphics) {
      Component vs = Component.literal("VS").withStyle(style -> style.withFont(CobblemonResources.INSTANCE.getDEFAULT_LARGE()).withBold(true));
      Ui.drawCentered(graphics, this.font, vs, this.originX + 172, this.originY + 28 + 64 - 8, -1);
   }

   private void drawBench(GuiGraphics graphics) {
      int x = this.originX + 22;
      int y = this.originY + 160;
      graphics.fill(x, y, x + 302, y + 20, 872415231);
      graphics.fill(x, y, x + 302, y + 1, -1426063361);
      graphics.fill(x, y + 20 - 1, x + 302, y + 20, -1426063361);
      List<RoomStatePayload.Member> observers = this.state.watchers();
      Component label = Component.translatable("cobblebattle.room.watchers", new Object[]{observers.size()});
      Ui.draw(graphics, this.font, label, x + 5, y + 6, -1770753, false);
      int at = x + 8 + Ui.width(this.font, label);
      graphics.enableScissor(at, y, x + 302 - 4, y + 20);

      for (RoomStatePayload.Member watcher : observers) {
         String name = watcher.name();
         if (at > x + 302) {
            break;
         }

         Ui.draw(graphics, this.font, name, at, y + 6, -1, false);
         at += Ui.width(this.font, name) + 8;
      }

      if (observers.isEmpty()) {
         Ui.draw(graphics, this.font, Component.translatable("cobblebattle.room.no_watchers"), at, y + 6, -1770753, false);
      }

      graphics.disableScissor();
   }

   private void drawInvite(GuiGraphics graphics, int mouseX, int mouseY) {
      String code = this.state.inviteCode();
      if (!code.isEmpty()) {
         int x = this.originX + 22;
         int y = this.originY + 185;
         boolean hover = this.inInvite(mouseX, mouseY);
         Component label = Component.translatable("cobblebattle.room.invite_show", new Object[]{code});
         Ui.draw(graphics, this.font, label, x, y, hover ? -1 : -1770753, true);
         Component hint = this.interactionState.copied()
            ? Component.translatable("cobblebattle.room.invite_copied")
            : (hover ? Component.translatable("cobblebattle.room.invite_copy") : null);
         if (hint != null) {
            graphics.enableScissor(x, y - 2, x + 102, y + 10);
            Ui.draw(graphics, this.font, hint, x + Ui.width(this.font, label) + 6, y, -1770753, false);
            graphics.disableScissor();
         }
      }
   }

   private boolean inInvite(double mouseX, double mouseY) {
      int x = this.originX + 22;
      int y = this.originY + 185;
      return mouseX >= x && mouseX < x + 102 && mouseY >= y - 2 && mouseY < y + 10;
   }

   private void drawStart(GuiGraphics graphics, int mouseX, int mouseY) {
      int x = this.originX + 130;
      int y = this.originY + 183;
      if (this.state.fighting()) {
         Ui.drawCentered(graphics, this.font, Component.translatable("cobblebattle.room.in_battle"), x + 42, y + 2, -1770753);
      } else {
         boolean host = "host".equals(this.state.youAre());
         boolean ready = this.interactionState.canStart(this.state.youAre(), this.state.hasGuest(), this.state.fighting());
         boolean hover = ready && mouseX >= x && mouseX < x + 84 && mouseY >= y && mouseY < y + 12;
         if (!host) {
            Ui.drawCentered(graphics, this.font, Component.translatable("cobblebattle.room.wait_host"), x + 42, y + 2, -1770753);
         } else {
            graphics.fill(x, y, x + 84, y + 12, ready ? (hover ? -1711276033 : 1728053247) : 872415231);
            graphics.fill(x, y, x + 84, y + 1, -1426063361);
            graphics.fill(x, y + 12 - 1, x + 84, y + 12, -1426063361);
            graphics.fill(x, y, x + 1, y + 12, -1426063361);
            graphics.fill(x + 84 - 1, y, x + 84, y + 12, -1426063361);
            String label = this.interactionState.starting()
               ? "cobblebattle.room.starting"
               : (this.state.hasGuest() ? "cobblebattle.room.start" : "cobblebattle.room.need_opponent");
            Ui.drawCentered(graphics, this.font, Component.translatable(label), x + 42, y + 2, ready ? -1 : -1770753);
         }
      }
   }

   private static String key(RoomStatePayload.Member member) {
      return member.uid() + ":" + member.name();
   }

   private PlayerPortrait portraitOf(RoomStatePayload.Member member, boolean host) {
      boolean mine = host ? "host".equals(this.state.youAre()) : "guest".equals(this.state.youAre());
      Minecraft minecraft = Minecraft.getInstance();
      if (mine) {
         LocalPlayer var6 = minecraft.player;
         if (var6 instanceof AbstractClientPlayer) {
            return PlayerPortrait.of(var6);
         }
      }

      return this.portraits.computeIfAbsent(key(member), k -> PlayerPortrait.lookup(member.name(), member.uid()));
   }

   private RenderablePokemon leadOf(RoomStatePayload.Member member) {
      return member.lead().isEmpty() ? null : this.leads.computeIfAbsent(key(member), k -> {
         Species speciesTemplate = PokemonSpecies.getByName(member.lead());
         return speciesTemplate == null ? null : new RenderablePokemon(speciesTemplate, Set.of(), ItemStack.EMPTY);
      });
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0) {
         if (BackButton.contains(this.originX, this.originY, mouseX, mouseY)) {
            RoomLobbyScreen.send(RoomActionPayload.of("leave"));
            return true;
         }

         if (!this.state.inviteCode().isEmpty() && this.inInvite(mouseX, mouseY)) {
            Minecraft.getInstance().keyboardHandler.setClipboard(this.state.inviteCode());
            this.interactionState.markInvitationCopied();
            return true;
         }

         int x = this.originX + 130;
         int y = this.originY + 183;
         if (this.interactionState.canStart(this.state.youAre(), this.state.hasGuest(), this.state.fighting())
            && mouseX >= x
            && mouseX < x + 84
            && mouseY >= y
            && mouseY < y + 12) {
            this.interactionState.beginStart(this.state.youAre(), this.state.hasGuest(), this.state.fighting());
            RoomLobbyScreen.send(RoomActionPayload.of("start"));
            return true;
         }
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   public void onClose() {
      RoomLobbyScreen.send(RoomActionPayload.of("leave"));
      Minecraft.getInstance().setScreen(null);
   }
}
