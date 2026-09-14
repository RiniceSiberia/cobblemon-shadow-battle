package xiaocaoawa.minecraft.mod.cobblebattle.client;

import com.cobblemon.mod.common.client.gui.battle.BattleGUI;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ChatHud {
   private static final int ROWS_PER_NOTCH = 1;

   private ChatHud() {
   }

   public static void render(GuiGraphics graphics) {
      Minecraft minecraft = Minecraft.getInstance();
      if (ChatPanel.visible() && minecraft.player != null) {
         if (minecraft.screen == null && !minecraft.options.hideGui) {
            draw(graphics, graphics.guiWidth(), graphics.guiHeight(), ChatInput.draftOrNull());
         }
      }
   }

   public static void drawOver(Screen screen, GuiGraphics graphics, int mouseX, int mouseY) {
      Minecraft minecraft = Minecraft.getInstance();
      if (ChatPanel.visible() && minecraft.player != null && !minecraft.options.hideGui) {
         if (interactiveOver(screen)) {
            draw(graphics, screen.width, screen.height, ChatInput.draftOrNull());
            drawNameCard(graphics, mouseX, mouseY);
         }
      }
   }

   public static void drawNameCard(GuiGraphics graphics, int mouseX, int mouseY) {
      ChatLog.Line line = ChatPanel.lineAt(mouseX, mouseY);
      if (line != null) {
         List<Component> card = List.of(
            Component.translatable("cobblebattle.chat.player_info"),
            Component.literal("id:" + line.id()).withStyle(ChatFormatting.GRAY),
            Component.literal("uid:" + line.uid()).withStyle(ChatFormatting.GRAY)
         );
         graphics.renderTooltip(Minecraft.getInstance().font, card, Optional.empty(), mouseX, mouseY);
      }
   }

   public static boolean interactiveOver(Screen screen) {
      return screen != null && !(screen instanceof ChatScreen)
         ? screen instanceof net.minecraft.client.gui.screens.ChatScreen || screen instanceof BattleGUI
         : false;
   }

   static void draw(GuiGraphics graphics, int screenWidth, int screenHeight, String draft) {
      Minecraft minecraft = Minecraft.getInstance();
      int originX = ChatPanel.originX(screenWidth);
      int originY = ChatPanel.originY(screenHeight);
      ChatPanel.drawBackground(graphics, originX, originY);
      ChatPanel.drawTabs(graphics, minecraft.font, originX, originY, ChatState.channel() == ChatLog.Channel.BATTLE);
      Component empty = ChatState.channel() == ChatLog.Channel.BATTLE && !ChatState.inBattle()
         ? Component.translatable("cobblebattle.chat.not_in_battle")
         : Component.translatable("cobblebattle.chat.empty");
      ChatPanel.drawMessages(graphics, minecraft.font, originX, originY, ChatLog.lines(ChatState.channel()), empty, ChatState.scroll());
      if (draft != null) {
         drawDraft(graphics, originX, originY, draft);
      } else {
         drawHint(graphics, originX, originY);
      }
   }

   public static void scroll(double notches) {
      int connectionRequested = ChatState.scroll() + (int)Math.signum(notches) * 1;
      int max = ChatPanel.maxScroll(Minecraft.getInstance().font, ChatLog.lines(ChatState.channel()));
      ChatState.setScroll(Math.max(0, Math.min(connectionRequested, max)));
   }

   private static void drawHint(GuiGraphics graphics, int originX, int originY) {
      Minecraft minecraft = Minecraft.getInstance();
      Component hint = Component.translatable("cobblebattle.chat.hint", new Object[]{ChatKeys.OPEN.getTranslatedKeyMessage()});
      int left = originX + 5;
      int top = originY + 65;
      graphics.enableScissor(left, top, left + 153, top + 10);
      Ui.draw(graphics, minecraft.font, hint, left + 2, top + 1, -7697782, false);
      graphics.disableScissor();
   }

   private static void drawDraft(GuiGraphics graphics, int originX, int originY, String draft) {
      Minecraft minecraft = Minecraft.getInstance();
      int left = originX + 5;
      int top = originY + 65;
      int usable = 147;
      String shown = draft;

      while (Ui.width(minecraft.font, shown) > usable && !shown.isEmpty()) {
         shown = shown.substring(Character.charCount(shown.codePointAt(0)));
      }

      int textY = top + 1;
      graphics.enableScissor(left, top, left + 153, top + 10);
      Ui.draw(graphics, minecraft.font, shown, left + 2, textY, -1644826, false);
      if (System.currentTimeMillis() / 500L % 2L == 0L) {
         int caretX = left + 2 + Ui.width(minecraft.font, shown);
         graphics.fill(caretX, textY - 1, caretX + 1, textY + 9, -1644826);
      }

      graphics.disableScissor();
   }
}
