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
   private static final int CHAT_LINES_PER_SCROLL_STEP = 1;

   private ChatHud() {
   }

   public static void render(GuiGraphics canvas) {
      Minecraft gameClient = Minecraft.getInstance();
      if (ChatPanel.visible() && gameClient.player != null) {
         if (gameClient.screen == null && !gameClient.options.hideGui) {
            drawChatPanel(canvas, canvas.guiWidth(), canvas.guiHeight(), ChatInput.draftOrNull());
         }
      }
   }

   public static void drawOver(Screen activeScreen, GuiGraphics canvas, int pointerX, int pointerY) {
      Minecraft gameClient = Minecraft.getInstance();
      if (ChatPanel.visible() && gameClient.player != null && !gameClient.options.hideGui) {
         if (interactiveOver(activeScreen)) {
            drawChatPanel(canvas, activeScreen.width, activeScreen.height, ChatInput.draftOrNull());
            drawNameCard(canvas, pointerX, pointerY);
         }
      }
   }

   public static void drawNameCard(GuiGraphics canvas, int pointerX, int pointerY) {
      ChatLog.Line hoveredLine = ChatPanel.lineAt(pointerX, pointerY);
      if (hoveredLine != null) {
         List<Component> identityTooltipLines = List.of(
            Component.translatable("cobblebattle.chat.player_info"),
            Component.literal("id:" + hoveredLine.id()).withStyle(ChatFormatting.GRAY),
            Component.literal("uid:" + hoveredLine.uid()).withStyle(ChatFormatting.GRAY)
         );
         canvas.renderTooltip(Minecraft.getInstance().font, identityTooltipLines, Optional.empty(), pointerX, pointerY);
      }
   }

   public static boolean interactiveOver(Screen activeScreen) {
      return activeScreen != null && !(activeScreen instanceof ChatScreen)
         ? activeScreen instanceof net.minecraft.client.gui.screens.ChatScreen || activeScreen instanceof BattleGUI
         : false;
   }

   static void drawChatPanel(GuiGraphics canvas, int viewportWidth, int viewportHeight, String inputDraft) {
      Minecraft gameClient = Minecraft.getInstance();
      int panelLeft = ChatPanel.originX(viewportWidth);
      int panelTop = ChatPanel.originY(viewportHeight);
      ChatPanel.drawBackground(canvas, panelLeft, panelTop);
      ChatPanel.drawTabs(canvas, gameClient.font, panelLeft, panelTop, ChatState.channel() == ChatLog.Channel.BATTLE);
      Component emptyStateMessage = ChatState.channel() == ChatLog.Channel.BATTLE && !ChatState.inBattle()
         ? Component.translatable("cobblebattle.chat.not_in_battle")
         : Component.translatable("cobblebattle.chat.empty");
      ChatPanel.drawMessages(canvas, gameClient.font, panelLeft, panelTop, ChatLog.lines(ChatState.channel()), emptyStateMessage, ChatState.scroll());
      if (inputDraft != null) {
         drawInputDraft(canvas, panelLeft, panelTop, inputDraft);
      } else {
         drawInputHint(canvas, panelLeft, panelTop);
      }
   }

   public static void scroll(double scrollWheelDelta) {
      int targetScrollOffset = ChatState.scroll() + (int)Math.signum(scrollWheelDelta) * CHAT_LINES_PER_SCROLL_STEP;
      int maximumScroll = ChatPanel.maxScroll(Minecraft.getInstance().font, ChatLog.lines(ChatState.channel()));
      ChatState.setScroll(Math.max(0, Math.min(targetScrollOffset, maximumScroll)));
   }

   private static void drawInputHint(GuiGraphics canvas, int panelLeft, int panelTop) {
      Minecraft gameClient = Minecraft.getInstance();
      Component inputHint = Component.translatable("cobblebattle.chat.hint", new Object[]{ChatKeys.OPEN.getTranslatedKeyMessage()});
      int inputLeft = panelLeft + 5;
      int inputTop = panelTop + 65;
      canvas.enableScissor(inputLeft, inputTop, inputLeft + 153, inputTop + 10);
      Ui.draw(canvas, gameClient.font, inputHint, inputLeft + 2, inputTop + 1, -7697782, false);
      canvas.disableScissor();
   }

   private static void drawInputDraft(GuiGraphics canvas, int panelLeft, int panelTop, String inputDraft) {
      Minecraft gameClient = Minecraft.getInstance();
      int inputLeft = panelLeft + 5;
      int inputTop = panelTop + 65;
      int availableTextWidth = 147;
      String visibleDraft = inputDraft;

      while (Ui.width(gameClient.font, visibleDraft) > availableTextWidth && !visibleDraft.isEmpty()) {
         visibleDraft = visibleDraft.substring(Character.charCount(visibleDraft.codePointAt(0)));
      }

      int textTop = inputTop + 1;
      canvas.enableScissor(inputLeft, inputTop, inputLeft + 153, inputTop + 10);
      Ui.draw(canvas, gameClient.font, visibleDraft, inputLeft + 2, textTop, -1644826, false);
      if (System.currentTimeMillis() / 500L % 2L == 0L) {
         int caretLeft = inputLeft + 2 + Ui.width(gameClient.font, visibleDraft);
         canvas.fill(caretLeft, textTop - 1, caretLeft + 1, textTop + 9, -1644826);
      }

      canvas.disableScissor();
   }
}
