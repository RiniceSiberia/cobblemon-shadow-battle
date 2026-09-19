package xiaocaoawa.minecraft.mod.cobblebattle.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ChatScreen extends Screen {
   private final Screen returnScreen;

   public ChatScreen(Screen returnScreen) {
      super(Component.translatable("cobblebattle.chat.title"));
      this.returnScreen = returnScreen;
      ChatInput.start();
   }

   public void onClose() {
      ChatInput.cancel();
      this.minecraft.setScreen(this.returnScreen);
   }

   public boolean isPauseScreen() {
      return false;
   }

   public void renderBackground(GuiGraphics canvas, int pointerX, int pointerY, float frameDelta) {
   }

   public void render(GuiGraphics canvas, int pointerX, int pointerY, float frameDelta) {
      ChatHud.drawChatPanel(canvas, this.width, this.height, ChatInput.draftOrNull());
      super.render(canvas, pointerX, pointerY, frameDelta);
      ChatHud.drawNameCard(canvas, pointerX, pointerY);
   }

   public boolean mouseClicked(double pointerX, double pointerY, int mouseButton) {
      int panelX = (int)pointerX - ChatPanel.originX(this.width);
      int panelY = (int)pointerY - ChatPanel.originY(this.height);
      ChatLog.Channel selectedChannel = ChatPanel.tabAt(panelX, panelY);
      if (selectedChannel != null) {
         ChatState.select(selectedChannel);
         return true;
      } else {
         return super.mouseClicked(pointerX, pointerY, mouseButton);
      }
   }

   public boolean mouseScrolled(double pointerX, double pointerY, double horizontalScroll, double verticalScroll) {
      if (ChatPanel.contains(this.width, this.height, pointerX, pointerY)) {
         ChatHud.scroll(verticalScroll);
         return true;
      } else {
         return super.mouseScrolled(pointerX, pointerY, horizontalScroll, verticalScroll);
      }
   }

   public boolean charTyped(char typedCharacter, int modifierMask) {
      return ChatInput.type(typedCharacter);
   }

   public boolean keyPressed(int pressedKey, int physicalScanCode, int modifierMask) {
      switch (pressedKey) {
         case 257:
         case 335:
            this.submitDraft();
            return true;
         case 258:
            ChatInput.toggleChannel();
            return true;
         case 259:
            ChatInput.backspace();
            return true;
         default:
            return super.keyPressed(pressedKey, physicalScanCode, modifierMask);
      }
   }

   private void submitDraft() {
      ChatInput.send();
      this.onClose();
   }
}
