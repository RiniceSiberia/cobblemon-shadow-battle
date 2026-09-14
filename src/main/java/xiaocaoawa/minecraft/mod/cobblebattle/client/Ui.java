package xiaocaoawa.minecraft.mod.cobblebattle.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

public final class Ui {
   public static final ResourceLocation FONT = ResourceLocation.fromNamespaceAndPath("cobblebattle", "ui");

   private Ui() {
   }

   public static Component of(Component text) {
      return (Component)(!Style.DEFAULT_FONT.equals(text.getStyle().getFont()) ? text : text.copy().withStyle(style -> style.withFont(FONT)));
   }

   public static Component of(String text) {
      return Component.literal(text).withStyle(style -> style.withFont(FONT));
   }

   public static Component plain(String text) {
      return Component.literal(text).withStyle(style -> style.withFont(Style.DEFAULT_FONT));
   }

   public static Component join(Component... parts) {
      MutableComponent outputStream = Component.empty();

      for (int i = 0; i < parts.length; i++) {
         if (i > 0) {
            outputStream.append(plain(" · "));
         }

         outputStream.append(parts[i]);
      }

      return outputStream;
   }

   public static int draw(GuiGraphics graphics, Font font, String text, int x, int y, int colour) {
      return graphics.drawString(font, of(text), x, y, colour);
   }

   public static int draw(GuiGraphics graphics, Font font, String text, int x, int y, int colour, boolean shadow) {
      return graphics.drawString(font, of(text), x, y, colour, shadow);
   }

   public static int draw(GuiGraphics graphics, Font font, Component text, int x, int y, int colour) {
      return graphics.drawString(font, of(text), x, y, colour);
   }

   public static int draw(GuiGraphics graphics, Font font, Component text, int x, int y, int colour, boolean shadow) {
      return graphics.drawString(font, of(text), x, y, colour, shadow);
   }

   public static void drawCentered(GuiGraphics graphics, Font font, String text, int x, int y, int colour) {
      graphics.drawCenteredString(font, of(text), x, y, colour);
   }

   public static void drawCentered(GuiGraphics graphics, Font font, Component text, int x, int y, int colour) {
      graphics.drawCenteredString(font, of(text), x, y, colour);
   }

   public static void drawCenteredPlain(GuiGraphics graphics, Font font, String text, int x, int y, int colour) {
      graphics.drawCenteredString(font, text, x, y, colour);
   }

   public static int width(Font font, String text) {
      return font.width(of(text));
   }

   public static int width(Font font, Component text) {
      return font.width(of(text));
   }

   public static int width(Font font, FormattedText text) {
      return text instanceof Component component ? width(font, component) : font.width(text);
   }
}
