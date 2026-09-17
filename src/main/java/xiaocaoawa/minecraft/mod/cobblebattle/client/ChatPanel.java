package xiaocaoawa.minecraft.mod.cobblebattle.client;

import com.cobblemon.mod.common.client.CobblemonClient;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class ChatPanel {
   public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("cobblebattle", "textures/gui/battle_chat.png");
   public static final int WIDTH = 169;
   public static final int HEIGHT = 79;
   public static final int TAB_Y = 0;
   public static final int TAB_H = 7;
   public static final int TAB_W = 21;
   public static final int TAB_GLOBAL_X = 0;
   public static final int TAB_BATTLE_X = 21;
   public static final int LIST_X = 5;
   public static final int LIST_Y = 14;
   public static final int LIST_W = 153;
   public static final int LIST_H = 44;
   public static final int INPUT_X = 5;
   public static final int INPUT_Y = 65;
   public static final int INPUT_W = 153;
   public static final int INPUT_H = 10;
   public static final int PAD = 2;
   public static final int FACE = 8;
   public static final int ROW_H = 10;
   public static final int VISIBLE_ROWS = 4;
   public static final int TEXT_W = 139;
   public static final int TEXT_COLOUR = -1644826;
   public static final int NAME_COLOUR = -8399617;
   public static final int UID_COLOUR = -6645094;
   public static final int EMPTY_COLOUR = -7697782;
   public static final int TAB_ON_COLOUR = -1;
   public static final int TAB_OFF_COLOUR = -4605511;
   public static final int DRAFT_COLOUR = -1644826;
   public static final int CARET_COLOUR = -1644826;
   public static final int BATTLE_SLOT_RIGHT_INSET = 181;
   public static final int BATTLE_SLOT_BOTTOM_INSET = 30;
   public static final int BATTLE_SLOT_GAP = 2;
   private static final List<ChatPanel.SenderHitArea> SENDER_HIT_AREAS = new ArrayList<>();
   private static int messageClipLeft;
   private static int messageClipTop;

   private ChatPanel() {
   }

   public static int battleLogShift() {
      return 81;
   }

   public static boolean battleLogShowing() {
      return CobblemonClient.INSTANCE.getBattle() != null;
   }

   public static int originX(int viewportWidth) {
      return viewportWidth - 181;
   }

   public static int originY(int viewportHeight) {
      return viewportHeight - 30 - 79;
   }

   public static boolean visible() {
      return ChatState.signedIn() && ChatState.enabled() && ClientSettings.chatHud() ? !battleLogShowing() || ChatState.inBattle() : false;
   }

   public static boolean contains(int viewportWidth, int viewportHeight, double pointerX, double pointerY) {
      int panelLeft = originX(viewportWidth);
      int panelTop = originY(viewportHeight);
      return pointerX >= panelLeft && pointerX < panelLeft + 169 && pointerY >= panelTop && pointerY < panelTop + 79;
   }

   public static boolean inInputBox(int panelRelativeX, int panelRelativeY) {
      return panelRelativeX >= 5 && panelRelativeX < 158 && panelRelativeY >= 65 && panelRelativeY < 75;
   }

   public static void drawBackground(GuiGraphics canvas, int panelLeft, int panelTop) {
      canvas.blit(TEXTURE, panelLeft, panelTop, 0.0F, 0.0F, 169, 79, 169, 79);
   }

   public static void drawTabs(GuiGraphics canvas, Font textRenderer, int panelLeft, int panelTop, boolean battleChannelSelected) {
      Component globalLabel = Component.translatable("cobblebattle.chat.tab.global");
      Component battleLabel = Component.translatable("cobblebattle.chat.tab.battle");
      float textScale = computeTabTextScale(textRenderer, globalLabel, battleLabel);
      renderTabLabel(canvas, textRenderer, panelLeft, panelTop, globalLabel, !battleChannelSelected, textScale);
      renderTabLabel(canvas, textRenderer, panelLeft + 21, panelTop, battleLabel, battleChannelSelected, textScale);
   }

   private static void renderTabLabel(GuiGraphics canvas, Font textRenderer, int tabLeft, int tabTop, Component caption, boolean active, float textScale) {
      float scaledTextHeight = 8.0F * textScale;
      canvas.pose().pushPose();
      canvas.pose().translate(tabLeft + 10.5F, tabTop + (7.0F - scaledTextHeight) / 2.0F, 0.0F);
      canvas.pose().scale(textScale, textScale, 1.0F);
      Ui.drawCentered(canvas, textRenderer, caption, 0, 0, active ? -1 : -4605511);
      canvas.pose().popPose();
   }

   private static float computeTabTextScale(Font textRenderer, Component... captions) {
      int widestCaptionWidth = 1;

      for (Component caption : captions) {
         widestCaptionWidth = Math.max(widestCaptionWidth, Ui.width(textRenderer, caption));
      }

      float widthScale = 17.0F / widestCaptionWidth;
      float heightScale = 0.625F;
      return Math.min(1.0F, Math.min(widthScale, heightScale));
   }

   private static List<ChatPanel.VisualMessageRow> buildVisualRows(Font textRenderer, List<ChatLog.Line> chatLines) {
      List<ChatPanel.VisualMessageRow> visualRows = new ArrayList<>();

      for (ChatLog.Line chatLine : chatLines) {
         List<String> wrappedLines = wrap(textRenderer, senderPrefix(chatLine) + chatLine.text(), 139);

         for (int wrappedIndex = 0; wrappedIndex < wrappedLines.size(); wrappedIndex++) {
            visualRows.add(new ChatPanel.VisualMessageRow(chatLine, wrappedLines.get(wrappedIndex), wrappedIndex == 0));
         }
      }

      return visualRows;
   }

   private static String senderPrefix(ChatLog.Line chatLine) {
      return chatLine.name() + ": ";
   }

   public static ChatLog.Line lineAt(double pointerX, double pointerY) {
      if (!(pointerX < messageClipLeft) && !(pointerX >= messageClipLeft + 153) && !(pointerY < messageClipTop) && !(pointerY >= messageClipTop + 44)) {
         for (ChatPanel.SenderHitArea hitArea : SENDER_HIT_AREAS) {
            if (pointerX >= hitArea.left() && pointerX < hitArea.left() + hitArea.width() && pointerY >= hitArea.top() && pointerY < hitArea.top() + hitArea.height()) {
               return hitArea.chatLine();
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public static int maxScroll(Font textRenderer, List<ChatLog.Line> chatLines) {
      return Math.max(0, buildVisualRows(textRenderer, chatLines).size() - 4);
   }

   public static void drawMessages(GuiGraphics canvas, Font textRenderer, int panelLeft, int panelTop, List<ChatLog.Line> chatLines, Component emptyStateText, int rowScrollOffset) {
      int messageLeft = panelLeft + 5;
      int messageTop = panelTop + 14;
      SENDER_HIT_AREAS.clear();
      messageClipLeft = messageLeft;
      messageClipTop = messageTop;
      canvas.enableScissor(messageLeft, messageTop, messageLeft + 153, messageTop + 44);

      try {
         if (!chatLines.isEmpty()) {
            List<ChatPanel.VisualMessageRow> visualRows = buildVisualRows(textRenderer, chatLines);
            int boundedScrollOffset = Math.max(0, Math.min(rowScrollOffset, Math.max(0, visualRows.size() - 4)));
            int visibleEndIndex = visualRows.size() - boundedScrollOffset;
            int visibleStartIndex = Math.max(0, visibleEndIndex - 4);
            int rowTop = messageTop + 44 - 2 - (visibleEndIndex - visibleStartIndex) * 10;

            for (int rowIndex = visibleStartIndex; rowIndex < visibleEndIndex; rowIndex++) {
               renderMessageRow(canvas, textRenderer, messageLeft, rowTop, visualRows.get(rowIndex));
               rowTop += 10;
            }

            if (boundedScrollOffset > 0) {
               renderScrollMarker(canvas, messageLeft, messageTop);
            }

            return;
         }

         Ui.drawCentered(canvas, textRenderer, emptyStateText, messageLeft + 76, messageTop + (44 - 9) / 2, -7697782);
      } finally {
         canvas.disableScissor();
      }
   }

   private static void renderScrollMarker(GuiGraphics canvas, int messageLeft, int messageTop) {
      int markerLeft = messageLeft + 153 - 2 - 2;
      int markerTop = messageTop + 44 - 2 - 3;
      canvas.fill(markerLeft, markerTop, markerLeft + 2, markerTop + 2, -8399617);
   }

   private static void renderMessageRow(GuiGraphics canvas, Font textRenderer, int messageLeft, int rowTop, ChatPanel.VisualMessageRow visualRow) {
      int textLeft = messageLeft + 2 + 8 + 2;
      int textTop = rowTop + 1;
      if (!visualRow.firstForMessage()) {
         Ui.draw(canvas, textRenderer, visualRow.renderedText(), textLeft, textTop, -1644826, false);
      } else {
         drawFace(canvas, visualRow.chatLine().sender(), messageLeft + 2, rowTop);
         String expectedPrefix = senderPrefix(visualRow.chatLine());
         if (!visualRow.renderedText().startsWith(expectedPrefix)) {
            Ui.draw(canvas, textRenderer, visualRow.renderedText(), textLeft, textTop, -1644826, false);
         } else {
            String senderName = visualRow.chatLine().name();
            int senderNameWidth = Ui.width(textRenderer, senderName);
            SENDER_HIT_AREAS.add(new ChatPanel.SenderHitArea(textLeft, textTop, senderNameWidth, 9, visualRow.chatLine()));
            Ui.draw(canvas, textRenderer, senderName, textLeft, textTop, -8399617, false);
            Ui.draw(canvas, textRenderer, ": ", textLeft + senderNameWidth, textTop, -6645094, false);
            Ui.draw(canvas, textRenderer, visualRow.renderedText().substring(expectedPrefix.length()), textLeft + senderNameWidth + Ui.width(textRenderer, ": "), textTop, -1644826, false);
         }
      }
   }

   public static void drawFace(GuiGraphics canvas, UUID senderId, int faceLeft, int faceTop) {
      Minecraft client = Minecraft.getInstance();
      ResourceLocation skinTexture = null;
      LocalPlayer localPlayer = client.player;
      if (localPlayer instanceof AbstractClientPlayer && localPlayer.getUUID().equals(senderId)) {
         skinTexture = localPlayer.getSkin().texture();
      }

      if (skinTexture == null) {
         skinTexture = DefaultPlayerSkin.get(senderId).texture();
      }

      canvas.blit(skinTexture, faceLeft, faceTop, 8, 8, 8.0F, 8.0F, 8, 8, 64, 64);
      canvas.blit(skinTexture, faceLeft, faceTop, 8, 8, 40.0F, 8.0F, 8, 8, 64, 64);
   }

   public static List<String> wrap(Font textRenderer, String content, int maximumWidth) {
      List<String> wrappedLines = new ArrayList<>();
      StringBuilder currentLine = new StringBuilder();
      int currentLineWidth = 0;
      int textOffset = 0;

      while (textOffset < content.length()) {
         int codePoint = content.codePointAt(textOffset);
         String codePointText = new String(Character.toChars(codePoint));
         textOffset += Character.charCount(codePoint);
         int glyphWidth = Ui.width(textRenderer, codePointText);
         if (currentLineWidth + glyphWidth > maximumWidth && currentLine.length() > 0) {
            wrappedLines.add(currentLine.toString());
            currentLine.setLength(0);
            currentLineWidth = 0;
         }

         currentLine.append(codePointText);
         currentLineWidth += glyphWidth;
      }

      if (currentLine.length() > 0) {
         wrappedLines.add(currentLine.toString());
      }

      if (wrappedLines.isEmpty()) {
         wrappedLines.add("");
      }

      return wrappedLines;
   }

   public static ChatLog.Channel tabAt(int panelRelativeX, int panelRelativeY) {
      if (panelRelativeY < 0 || panelRelativeY >= 7) {
         return null;
      } else if (panelRelativeX >= 0 && panelRelativeX < 21) {
         return ChatLog.Channel.GLOBAL;
      } else {
         return panelRelativeX >= 21 && panelRelativeX < 42 ? ChatLog.Channel.BATTLE : null;
      }
   }

   private record SenderHitArea(int left, int top, int width, int height, ChatLog.Line chatLine) {
   }

   private record VisualMessageRow(ChatLog.Line chatLine, String renderedText, boolean firstForMessage) {
   }
}
