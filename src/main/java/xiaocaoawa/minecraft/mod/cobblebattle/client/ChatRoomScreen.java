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
   private static final ResourceLocation BASE = ResourceLocation.fromNamespaceAndPath("cobblebattle", "textures/gui/leaderboard/base.png");
   private static final ResourceLocation SCREEN = ResourceLocation.fromNamespaceAndPath("cobblebattle", "textures/gui/leaderboard/screen_1.png");
   private static final int WIDTH = 345;
   private static final int HEIGHT = 207;
   private static final int HOLE_X = 18;
   private static final int HOLE_Y = 13;
   private static final int HOLE_W = 309;
   private static final int HOLE_H = 183;
   private static final int TITLE_X = 24;
   private static final int TITLE_Y = 15;
   private static final int TAB_X = 110;
   private static final int TAB_W = 40;
   private static final int TAB_H = 10;
   private static final int TAB_Y = 14;
   private static final int LIST_X = 22;
   private static final int LIST_Y = 27;
   private static final int LIST_W = 301;
   private static final int LIST_H = 150;
   private static final int INPUT_Y = 180;
   private static final int INPUT_H = 12;
   private static final int INPUT_W = 250;
   private static final int PAD = 4;
   private static final int FACE = 8;
   private static final int BUBBLE_MAX_W = 180;
   private static final int LINE_H = 10;
   private static final int NAME_H = 10;
   private static final int GAP = 4;
   private static final int TITLE_COLOUR = -1;
   private static final int TAB_ON = -1426063361;
   private static final int TAB_OFF = 1157627903;
   private static final int NAME_COLOUR = -1;
   private static final int NAME_SHADE = -15451066;
   private static final int BUBBLE = -12937546;
   private static final int BUBBLE_MINE = -13664356;
   private static final int BUBBLE_TEXT = -1;
   private static final int INPUT_FILL = -721409;
   private static final int INPUT_EDGE = -8460315;
   private static final int INPUT_TEXT = -15451066;
   private EditBox input;
   private int originX;
   private int originY;
   private int scrollOffsets;

   public ChatRoomScreen() {
      super(Component.translatable("cobblebattle.chatroom.title"));
   }

   protected void init() {
      this.originX = (this.width - 345) / 2;
      this.originY = (this.height - 207) / 2;
      String was = this.input == null ? "" : this.input.getValue();
      this.input = new EditBox(this.font, this.originX + 22 + 4, this.originY + 180 + 2, 242, 10, Component.empty());
      this.input.setBordered(false);
      this.input.setTextColor(-15451066);
      this.input.setMaxLength(200);
      this.input.setValue(was);
      this.addWidget(this.input);
      this.setInitialFocus(this.input);
   }

   public boolean isPauseScreen() {
      return false;
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(graphics, mouseX, mouseY, partialTick);
      graphics.blit(SCREEN, this.originX, this.originY, 0.0F, 0.0F, 345, 207, 345, 207);
      Backdrop.draw(graphics, this.originX, this.originY);
      int left = this.originX + 18;
      int top = this.originY + 13;
      graphics.enableScissor(left, top, left + 309, top + 183);

      try {
         this.drawHeader(graphics, mouseX, mouseY);
         this.drawMessages(graphics);
         this.drawInput(graphics);
         BackButton.draw(graphics, this.font, this.originX, this.originY, mouseX, mouseY);
      } finally {
         graphics.disableScissor();
      }

      this.input.render(graphics, mouseX, mouseY, partialTick);
      graphics.blit(BASE, this.originX, this.originY, 0.0F, 0.0F, 345, 207, 345, 207);
   }

   private void drawHeader(GuiGraphics graphics, int mouseX, int mouseY) {
      Ui.draw(graphics, this.font, this.getTitle(), this.originX + 24, this.originY + 15, -1, true);
      this.drawTab(graphics, 0, Component.translatable("cobblebattle.chat.tab.global"), ChatState.channel() == ChatLog.Channel.GLOBAL, mouseX, mouseY);
      this.drawTab(graphics, 1, Component.translatable("cobblebattle.chat.tab.battle"), ChatState.channel() == ChatLog.Channel.BATTLE, mouseX, mouseY);
   }

   private void drawTab(GuiGraphics graphics, int index, Component label, boolean on, int mouseX, int mouseY) {
      int x = this.originX + 110 + index * 44;
      int y = this.originY + 14;
      boolean hover = mouseX >= x && mouseX < x + 40 && mouseY >= y && mouseY < y + 10;
      graphics.fill(x, y, x + 40, y + 10, !on && !hover ? 1157627903 : -1426063361);
      Ui.drawCentered(graphics, this.font, label, x + 20, y + 1, on ? -15451066 : -1);
   }

   private void drawMessages(GuiGraphics graphics) {
      int left = this.originX + 22;
      int top = this.originY + 27;
      int right = left + 301;
      int bottom = top + 150;
      graphics.enableScissor(left, top, right, bottom);

      try {
         List<ChatLog.Line> lines = ChatLog.lines(ChatState.channel());
         if (lines.isEmpty() || !ChatState.enabled()) {
            Component empty = !ChatState.enabled()
               ? Component.translatable("cobblebattle.chat.disabled")
               : (
                  ChatState.channel() == ChatLog.Channel.BATTLE && !ChatState.inBattle()
                     ? Component.translatable("cobblebattle.chat.not_in_battle")
                     : Component.translatable("cobblebattle.chat.empty")
               );
            Ui.drawCentered(graphics, this.font, empty, left + 150, top + 75 - 4, -1);
            return;
         }

         List<ChatRoomScreen.Bubble> bubbles = new ArrayList<>(lines.size());
         int total = 0;

         for (ChatLog.Line line : lines) {
            ChatRoomScreen.Bubble bubble = this.layout(line);
            bubbles.add(bubble);
            total += bubble.height() + 4;
         }

         int maxScrollOffsets = Math.max(0, total - 150);
         if (this.scrollOffsets > maxScrollOffsets) {
            this.scrollOffsets = maxScrollOffsets;
         }

         int y = bottom - 4 + this.scrollOffsets - total;

         for (ChatRoomScreen.Bubble bubble : bubbles) {
            if (y + bubble.height() >= top && y <= bottom) {
               this.drawBubble(graphics, bubble, left, right, y);
            }

            y += bubble.height() + 4;
         }
      } finally {
         graphics.disableScissor();
      }
   }

   private ChatRoomScreen.Bubble layout(ChatLog.Line line) {
      boolean mine = ChatState.uid() != 0L && line.uid() == ChatState.uid();
      List<String> rows = ChatPanel.wrap(this.font, line.text(), 172);
      int width = 0;

      for (String row : rows) {
         width = Math.max(width, Ui.width(this.font, row));
      }

      width += 8;
      int height = 10 + rows.size() * 10 + 8 - 2;
      return new ChatRoomScreen.Bubble(line, mine, rows, width, height);
   }

   private void drawBubble(GuiGraphics graphics, ChatRoomScreen.Bubble bubble, int left, int right, int y) {
      ChatLog.Line line = bubble.line();
      String name = line.name();
      int nameWidth = Ui.width(this.font, name);
      int bubbleTop = y + 10;
      if (bubble.mine()) {
         int faceX = right - 4 - 8;
         ChatPanel.drawFace(graphics, line.sender(), faceX, y);
         Ui.draw(graphics, this.font, name, faceX - 3 - nameWidth, y, -1, true);
         int bx = faceX - 3 - bubble.width();
         graphics.fill(bx, bubbleTop, bx + bubble.width(), bubbleTop + bubble.height() - 10, -13664356);
         this.drawRows(graphics, bubble, bx + 4, bubbleTop + 4 - 1);
      } else {
         int faceX = left + 4;
         ChatPanel.drawFace(graphics, line.sender(), faceX, y);
         Ui.draw(graphics, this.font, name, faceX + 8 + 3, y, -1, true);
         int bx = faceX + 8 + 3;
         graphics.fill(bx, bubbleTop, bx + bubble.width(), bubbleTop + bubble.height() - 10, -12937546);
         this.drawRows(graphics, bubble, bx + 4, bubbleTop + 4 - 1);
      }
   }

   private void drawRows(GuiGraphics graphics, ChatRoomScreen.Bubble bubble, int x, int y) {
      for (String row : bubble.rows()) {
         Ui.draw(graphics, this.font, row, x, y, -1, false);
         y += 10;
      }
   }

   private void drawInput(GuiGraphics graphics) {
      int x = this.originX + 22;
      int y = this.originY + 180;
      graphics.fill(x - 1, y - 1, x + 250 + 1, y + 12 + 1, -8460315);
      graphics.fill(x, y, x + 250, y + 12, -721409);
   }

   private void send() {
      String text = this.input.getValue().trim();
      if (!text.isEmpty() && ChatState.enabled()) {
         ChatScreenHandler.send(ChatState.channel(), text);
         this.input.setValue("");
         this.scrollOffsets = 0;
      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0) {
         if (BackButton.contains(this.originX, this.originY, mouseX, mouseY)) {
            ServerDex.requestMain();
            return true;
         }

         for (int i = 0; i < 2; i++) {
            int x = this.originX + 110 + i * 44;
            int y = this.originY + 14;
            if (mouseX >= x && mouseX < x + 40 && mouseY >= y && mouseY < y + 10) {
               ChatState.select(i == 0 ? ChatLog.Channel.GLOBAL : ChatLog.Channel.BATTLE);
               this.scrollOffsets = 0;
               return true;
            }
         }
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double amountX, double amountY) {
      this.scrollOffsets = Math.max(0, this.scrollOffsets + (int)Math.signum(amountY) * 10 * 2);
      return true;
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode != 257 && keyCode != 335) {
         return super.keyPressed(keyCode, scanCode, modifiers);
      } else {
         this.send();
         return true;
      }
   }

   public void onClose() {
      Minecraft.getInstance().setScreen(null);
   }

   private record Bubble(ChatLog.Line line, boolean mine, List<String> rows, int width, int height) {
   }
}
