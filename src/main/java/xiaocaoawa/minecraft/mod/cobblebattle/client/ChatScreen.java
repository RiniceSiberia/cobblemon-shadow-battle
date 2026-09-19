package xiaocaoawa.minecraft.mod.cobblebattle.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ChatScreen extends Screen {
   private final Screen parent;

   public ChatScreen(Screen parent) {
      super(Component.translatable("cobblebattle.chat.title"));
      this.parent = parent;
      ChatInput.start();
   }

   public void onClose() {
      ChatInput.cancel();
      this.minecraft.setScreen(this.parent);
   }

   public boolean isPauseScreen() {
      return false;
   }

   public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      ChatHud.drawChatPanel(graphics, this.width, this.height, ChatInput.draftOrNull());
      super.render(graphics, mouseX, mouseY, partialTick);
      ChatHud.drawNameCard(graphics, mouseX, mouseY);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      int relX = (int)mouseX - ChatPanel.originX(this.width);
      int relY = (int)mouseY - ChatPanel.originY(this.height);
      ChatLog.Channel tab = ChatPanel.tabAt(relX, relY);
      if (tab != null) {
         ChatState.select(tab);
         return true;
      } else {
         return super.mouseClicked(mouseX, mouseY, button);
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double amountX, double amountY) {
      if (ChatPanel.contains(this.width, this.height, mouseX, mouseY)) {
         ChatHud.scroll(amountY);
         return true;
      } else {
         return super.mouseScrolled(mouseX, mouseY, amountX, amountY);
      }
   }

   public boolean charTyped(char ch, int modifiers) {
      return ChatInput.type(ch);
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      switch (keyCode) {
         case 257:
         case 335:
            this.send();
            return true;
         case 258:
            ChatInput.toggleChannel();
            return true;
         case 259:
            ChatInput.backspace();
            return true;
         default:
            return super.keyPressed(keyCode, scanCode, modifiers);
      }
   }

   private void send() {
      ChatInput.send();
      this.onClose();
   }
}
