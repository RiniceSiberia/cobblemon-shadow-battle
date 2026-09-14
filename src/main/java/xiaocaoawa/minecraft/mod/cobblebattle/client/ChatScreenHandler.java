package xiaocaoawa.minecraft.mod.cobblebattle.client;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientRawInputEvent;
import dev.architectury.event.events.client.ClientScreenInputEvent;
import dev.architectury.event.events.client.ClientGuiEvent.RenderHud;
import dev.architectury.event.events.client.ClientGuiEvent.ScreenRenderPost;
import dev.architectury.event.events.client.ClientRawInputEvent.KeyPressed;
import dev.architectury.event.events.client.ClientScreenInputEvent.KeyTyped;
import dev.architectury.event.events.client.ClientScreenInputEvent.MouseClicked;
import dev.architectury.event.events.client.ClientScreenInputEvent.MouseScrolled;
import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.NetworkManager.Side;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.Minecraft;
import xiaocaoawa.minecraft.mod.cobblebattle.network.ChatLinePayload;
import xiaocaoawa.minecraft.mod.cobblebattle.network.ChatStatePayload;
import xiaocaoawa.minecraft.mod.cobblebattle.network.SendChatPayload;

public final class ChatScreenHandler {
   private ChatScreenHandler() {
   }

   public static void init() {
      KeyMappingRegistry.register(ChatKeys.OPEN);
      NetworkManager.registerReceiver(
         Side.S2C,
         ChatLinePayload.TYPE,
         ChatLinePayload.CODEC,
         (body, context) -> context.queue(
            () -> ChatLog.add(
               ChatLog.Channel.of(body.channel()), new ChatLog.Line(body.uid(), body.id(), body.name(), body.sender(), body.text())
            )
         )
      );
      NetworkManager.registerReceiver(Side.S2C, ChatStatePayload.TYPE, ChatStatePayload.CODEC, (payload, context) -> context.queue(() -> {
         ChatState.setEnabled(payload.enabled());
         ChatState.accept(payload.signedIn(), payload.inBattle(), payload.uid(), payload.name());
      }));
      ClientGuiEvent.RENDER_HUD.register((RenderHud)(graphics, delta) -> ChatHud.render(graphics));
      ClientGuiEvent.RENDER_POST.register((ScreenRenderPost)(screen, graphics, mouseX, mouseY, delta) -> ChatHud.drawOver(screen, graphics, mouseX, mouseY));
      ClientRawInputEvent.KEY_PRESSED.register((KeyPressed)(minecraft, keyCode, scanCode, action, modifiers) -> {
         if (action != 1 || minecraft.screen != null || !ready(minecraft)) {
            return EventResult.pass();
         } else if (!ChatKeys.OPEN.isUnbound() && ChatKeys.OPEN.matches(keyCode, scanCode)) {
            minecraft.setScreen(new ChatScreen(null));
            return EventResult.interruptTrue();
         } else {
            return EventResult.pass();
         }
      });
      ClientScreenInputEvent.MOUSE_CLICKED_PRE.register((MouseClicked)(minecraft, screen, mouseX, mouseY, button) -> {
         if (!ready(minecraft) || !ChatHud.interactiveOver(screen)) {
            return EventResult.pass();
         } else if (!ChatPanel.contains(screen.width, screen.height, mouseX, mouseY)) {
            ChatInput.cancel();
            return EventResult.pass();
         } else {
            int relX = (int)mouseX - ChatPanel.originX(screen.width);
            int relY = (int)mouseY - ChatPanel.originY(screen.height);
            ChatLog.Channel tab = ChatPanel.tabAt(relX, relY);
            if (tab != null) {
               ChatState.select(tab);
            } else if (ChatPanel.inInputBox(relX, relY)) {
               ChatInput.start();
            }

            return EventResult.interruptTrue();
         }
      });
      ClientScreenInputEvent.KEY_PRESSED_PRE
         .register((dev.architectury.event.events.client.ClientScreenInputEvent.KeyPressed)(minecraft, screen, keyCode, scanCode, modifiers) -> {
            if (!ready(minecraft) || !ChatHud.interactiveOver(screen)) {
               return EventResult.pass();
            } else if (ChatInput.typing()) {
               switch (keyCode) {
                  case 256:
                     ChatInput.cancel();
                     break;
                  case 257:
                  case 335:
                     ChatInput.send();
                     break;
                  case 258:
                     ChatInput.toggleChannel();
                     break;
                  case 259:
                     ChatInput.backspace();
               }

               return EventResult.interruptTrue();
            } else if (!ChatKeys.OPEN.isUnbound() && ChatKeys.OPEN.matches(keyCode, scanCode)) {
               ChatInput.start();
               return EventResult.interruptTrue();
            } else {
               return EventResult.pass();
            }
         });
      ClientScreenInputEvent.MOUSE_SCROLLED_PRE.register((MouseScrolled)(minecraft, screen, mouseX, mouseY, amountX, amountY) -> {
         if (!ready(minecraft) || !ChatHud.interactiveOver(screen)) {
            return EventResult.pass();
         } else if (!ChatPanel.contains(screen.width, screen.height, mouseX, mouseY)) {
            return EventResult.pass();
         } else {
            ChatHud.scroll(amountY);
            return EventResult.interruptTrue();
         }
      });
      ClientScreenInputEvent.CHAR_TYPED_PRE.register((KeyTyped)(minecraft, screen, ch, modifiers) -> {
         if (ready(minecraft) && ChatHud.interactiveOver(screen) && ChatInput.typing()) {
            return ChatInput.type(ch) ? EventResult.interruptTrue() : EventResult.pass();
         } else {
            return EventResult.pass();
         }
      });
   }

   private static boolean ready(Minecraft minecraft) {
      return minecraft.player != null && ChatPanel.visible();
   }

   static void send(ChatLog.Channel selectedConversation, String text) {
      NetworkManager.sendToServer(new SendChatPayload(selectedConversation.id, text));
   }

   public static void onDisconnect() {
      ChatState.reset();
      ChatInput.reset();
   }
}
