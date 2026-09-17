package xiaocaoawa.minecraft.mod.cobblebattle.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class ChatRoomScreen extends Screen {
   private static final ResourceLocation FRAME_TEXTURE = ResourceLocation.fromNamespaceAndPath("cobblebattle", "textures/gui/leaderboard/base.png");
   private static final ResourceLocation CHAT_SCREEN_TEXTURE = ResourceLocation.fromNamespaceAndPath("cobblebattle", "textures/gui/leaderboard/screen_1.png");
   private static final int PANEL_WIDTH = 345;
   private static final int PANEL_HEIGHT = 207;
   private static final int CONTENT_LEFT_OFFSET = 18;
   private static final int CONTENT_TOP_OFFSET = 13;
   private static final int CONTENT_WIDTH = 309;
   private static final int CONTENT_HEIGHT = 183;
   private static final int TITLE_LEFT_OFFSET = 24;
   private static final int TITLE_TOP_OFFSET = 15;
   private static final int TABS_LEFT_OFFSET = 110;
   private static final int TAB_WIDTH = 40;
   private static final int TAB_HEIGHT = 10;
   private static final int TABS_TOP_OFFSET = 14;
   private static final int MESSAGE_LIST_LEFT_OFFSET = 22;
   private static final int MESSAGE_LIST_TOP_OFFSET = 27;
   private static final int MESSAGE_LIST_WIDTH = 301;
   private static final int MESSAGE_LIST_HEIGHT = 150;
   private static final int INPUT_TOP_OFFSET = 180;
   private static final int INPUT_HEIGHT = 12;
   private static final int INPUT_WIDTH = 250;
   private static final int CONTENT_PADDING = 4;
   private static final int FACE_SIZE = 8;
   private static final int MAX_BUBBLE_WIDTH = 180;
   private static final int MESSAGE_LINE_HEIGHT = 10;
   private static final int SENDER_NAME_HEIGHT = 10;
   private static final int MESSAGE_GAP = 4;
   private static final int TITLE_COLOR = -1;
   private static final int ACTIVE_TAB_COLOR = -1426063361;
   private static final int INACTIVE_TAB_COLOR = 1157627903;
   private static final int SENDER_NAME_COLOR = -1;
   private static final int SELECTED_TAB_TEXT_COLOR = -15451066;
   private static final int REMOTE_BUBBLE_COLOR = -12937546;
   private static final int LOCAL_BUBBLE_COLOR = -13664356;
   private static final int MESSAGE_TEXT_COLOR = -1;
   private static final int INPUT_FILL_COLOR = -721409;
   private static final int INPUT_BORDER_COLOR = -8460315;
   private static final int INPUT_TEXT_COLOR = -15451066;
   private EditBox messageInput;
   private int panelLeft;
   private int panelTop;
   private int scrollOffsetPixels;

   public ChatRoomScreen() {
      super(Component.translatable("cobblebattle.chatroom.title"));
   }

   protected void init() {
      this.panelLeft = (this.width - 345) / 2;
      this.panelTop = (this.height - 207) / 2;
      String preservedDraft = this.messageInput == null ? "" : this.messageInput.getValue();
      this.messageInput = new EditBox(this.font, this.panelLeft + 22 + 4, this.panelTop + 180 + 2, 242, 10, Component.empty());
      this.messageInput.setBordered(false);
      this.messageInput.setTextColor(-15451066);
      this.messageInput.setMaxLength(200);
      this.messageInput.setValue(preservedDraft);
      this.addWidget(this.messageInput);
      this.setInitialFocus(this.messageInput);
   }

   public boolean isPauseScreen() {
      return false;
   }

   public void render(GuiGraphics canvas, int pointerX, int pointerY, float frameDelta) {
      this.renderBackground(canvas, pointerX, pointerY, frameDelta);
      canvas.blit(CHAT_SCREEN_TEXTURE, this.panelLeft, this.panelTop, 0.0F, 0.0F, 345, 207, 345, 207);
      Backdrop.draw(canvas, this.panelLeft, this.panelTop);
      int clipLeft = this.panelLeft + 18;
      int clipTop = this.panelTop + 13;
      canvas.enableScissor(clipLeft, clipTop, clipLeft + 309, clipTop + 183);

      try {
         this.renderHeader(canvas, pointerX, pointerY);
         this.renderMessages(canvas);
         this.renderInputBackground(canvas);
         BackButton.draw(canvas, this.font, this.panelLeft, this.panelTop, pointerX, pointerY);
      } finally {
         canvas.disableScissor();
      }

      this.messageInput.render(canvas, pointerX, pointerY, frameDelta);
      canvas.blit(FRAME_TEXTURE, this.panelLeft, this.panelTop, 0.0F, 0.0F, 345, 207, 345, 207);
   }

   private void renderHeader(GuiGraphics canvas, int pointerX, int pointerY) {
      Ui.draw(canvas, this.font, this.getTitle(), this.panelLeft + 24, this.panelTop + 15, -1, true);
      this.renderChannelTab(canvas, 0, Component.translatable("cobblebattle.chat.tab.global"), ChatState.channel() == ChatLog.Channel.GLOBAL, pointerX, pointerY);
      this.renderChannelTab(canvas, 1, Component.translatable("cobblebattle.chat.tab.battle"), ChatState.channel() == ChatLog.Channel.BATTLE, pointerX, pointerY);
   }

   private void renderChannelTab(GuiGraphics canvas, int channelIndex, Component tabLabel, boolean selected, int pointerX, int pointerY) {
      int elementLeft = this.panelLeft + 110 + channelIndex * 44;
      int messageTop = this.panelTop + 14;
      boolean pointerOver = pointerX >= elementLeft && pointerX < elementLeft + 40 && pointerY >= messageTop && pointerY < messageTop + 10;
      canvas.fill(elementLeft, messageTop, elementLeft + 40, messageTop + 10, !selected && !pointerOver ? 1157627903 : -1426063361);
      Ui.drawCentered(canvas, this.font, tabLabel, elementLeft + 20, messageTop + 1, selected ? -15451066 : -1);
   }

   private void renderMessages(GuiGraphics canvas) {
      int clipLeft = this.panelLeft + 22;
      int clipTop = this.panelTop + 27;
      int clipRight = clipLeft + 301;
      int clipBottom = clipTop + 150;
      canvas.enableScissor(clipLeft, clipTop, clipRight, clipBottom);

      try {
         List<ChatLog.Line> chatLines = ChatLog.lines(ChatState.channel());
         if (chatLines.isEmpty() || !ChatState.enabled()) {
            Component emptyStateMessage = !ChatState.enabled()
               ? Component.translatable("cobblebattle.chat.disabled")
               : (
                  ChatState.channel() == ChatLog.Channel.BATTLE && !ChatState.inBattle()
                     ? Component.translatable("cobblebattle.chat.not_in_battle")
                     : Component.translatable("cobblebattle.chat.empty")
               );
            Ui.drawCentered(canvas, this.font, emptyStateMessage, clipLeft + 150, clipTop + 75 - 4, -1);
            return;
         }

         List<ChatRoomScreen.MessageBubble> messageBubbles = new ArrayList<>(chatLines.size());
         int contentHeight = 0;

         for (ChatLog.Line chatLine : chatLines) {
            ChatRoomScreen.MessageBubble laidOutBubble = this.layoutMessageBubble(chatLine);
            messageBubbles.add(laidOutBubble);
            contentHeight += laidOutBubble.height() + 4;
         }

         int maxScrollOffsetPixels = Math.max(0, contentHeight - 150);
         if (this.scrollOffsetPixels > maxScrollOffsetPixels) {
            this.scrollOffsetPixels = maxScrollOffsetPixels;
         }

         int messageTop = clipBottom - 4 + this.scrollOffsetPixels - contentHeight;

         for (ChatRoomScreen.MessageBubble visibleBubble : messageBubbles) {
            if (messageTop + visibleBubble.height() >= clipTop && messageTop <= clipBottom) {
               this.renderMessageBubble(canvas, visibleBubble, clipLeft, clipRight, messageTop);
            }

            messageTop += visibleBubble.height() + 4;
         }
      } finally {
         canvas.disableScissor();
      }
   }

   private ChatRoomScreen.MessageBubble layoutMessageBubble(ChatLog.Line chatLine) {
      boolean sentByViewer = ChatState.uid() != 0L && chatLine.uid() == ChatState.uid();
      List<String> wrappedRows = ChatPanel.wrap(this.font, chatLine.text(), 172);
      int bubbleWidth = 0;

      for (String wrappedLine : wrappedRows) {
         bubbleWidth = Math.max(bubbleWidth, Ui.width(this.font, wrappedLine));
      }

      bubbleWidth += 8;
      int bubbleHeight = 10 + wrappedRows.size() * 10 + 8 - 2;
      return new ChatRoomScreen.MessageBubble(chatLine, sentByViewer, wrappedRows, bubbleWidth, bubbleHeight);
   }

   private void renderMessageBubble(GuiGraphics canvas, ChatRoomScreen.MessageBubble laidOutBubble, int clipLeft, int clipRight, int messageTop) {
      ChatLog.Line chatLine = laidOutBubble.line();
      String senderName = chatLine.name();
      int senderNameWidth = Ui.width(this.font, senderName);
      int bubbleBodyTop = messageTop + 10;
      if (laidOutBubble.mine()) {
         int localFaceLeft = clipRight - 4 - 8;
         ChatPanel.drawFace(canvas, chatLine.sender(), localFaceLeft, messageTop);
         Ui.draw(canvas, this.font, senderName, localFaceLeft - 3 - senderNameWidth, messageTop, -1, true);
         int localBubbleLeft = localFaceLeft - 3 - laidOutBubble.width();
         canvas.fill(localBubbleLeft, bubbleBodyTop, localBubbleLeft + laidOutBubble.width(), bubbleBodyTop + laidOutBubble.height() - 10, -13664356);
         this.renderWrappedRows(canvas, laidOutBubble, localBubbleLeft + 4, bubbleBodyTop + 4 - 1);
      } else {
         int remoteFaceLeft = clipLeft + 4;
         ChatPanel.drawFace(canvas, chatLine.sender(), remoteFaceLeft, messageTop);
         Ui.draw(canvas, this.font, senderName, remoteFaceLeft + 8 + 3, messageTop, -1, true);
         int remoteBubbleLeft = remoteFaceLeft + 8 + 3;
         canvas.fill(remoteBubbleLeft, bubbleBodyTop, remoteBubbleLeft + laidOutBubble.width(), bubbleBodyTop + laidOutBubble.height() - 10, -12937546);
         this.renderWrappedRows(canvas, laidOutBubble, remoteBubbleLeft + 4, bubbleBodyTop + 4 - 1);
      }
   }

   private void renderWrappedRows(GuiGraphics canvas, ChatRoomScreen.MessageBubble laidOutBubble, int elementLeft, int messageTop) {
      for (String wrappedLine : laidOutBubble.rows()) {
         Ui.draw(canvas, this.font, wrappedLine, elementLeft, messageTop, -1, false);
         messageTop += 10;
      }
   }

   private void renderInputBackground(GuiGraphics canvas) {
      int elementLeft = this.panelLeft + 22;
      int messageTop = this.panelTop + 180;
      canvas.fill(elementLeft - 1, messageTop - 1, elementLeft + 250 + 1, messageTop + 12 + 1, -8460315);
      canvas.fill(elementLeft, messageTop, elementLeft + 250, messageTop + 12, -721409);
   }

   private void submitMessage() {
      String messageText = this.messageInput.getValue().trim();
      if (!messageText.isEmpty() && ChatState.enabled()) {
         ChatScreenHandler.send(ChatState.channel(), messageText);
         this.messageInput.setValue("");
         this.scrollOffsetPixels = 0;
      }
   }

   public boolean mouseClicked(double pointerX, double pointerY, int mouseButton) {
      if (mouseButton == 0) {
         if (BackButton.contains(this.panelLeft, this.panelTop, pointerX, pointerY)) {
            ServerDex.requestMain();
            return true;
         }

         for (int channelIndex = 0; channelIndex < 2; channelIndex++) {
            int elementLeft = this.panelLeft + 110 + channelIndex * 44;
            int messageTop = this.panelTop + 14;
            if (pointerX >= elementLeft && pointerX < elementLeft + 40 && pointerY >= messageTop && pointerY < messageTop + 10) {
               ChatState.select(channelIndex == 0 ? ChatLog.Channel.GLOBAL : ChatLog.Channel.BATTLE);
               this.scrollOffsetPixels = 0;
               return true;
            }
         }
      }

      return super.mouseClicked(pointerX, pointerY, mouseButton);
   }

   public boolean mouseScrolled(double pointerX, double pointerY, double horizontalScroll, double verticalScroll) {
      this.scrollOffsetPixels = Math.max(0, this.scrollOffsetPixels + (int)Math.signum(verticalScroll) * 10 * 2);
      return true;
   }

   public boolean keyPressed(int pressedKeyCode, int physicalScanCode, int modifierMask) {
      if (pressedKeyCode != 257 && pressedKeyCode != 335) {
         return super.keyPressed(pressedKeyCode, physicalScanCode, modifierMask);
      } else {
         this.submitMessage();
         return true;
      }
   }

   public void onClose() {
      Minecraft.getInstance().setScreen(null);
   }

   private record MessageBubble(ChatLog.Line line, boolean mine, List<String> rows, int width, int height) {
   }
}
