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

   public static Component of(Component content) {
      return (Component)(!Style.DEFAULT_FONT.equals(content.getStyle().getFont()) ? content : content.copy().withStyle(fontStyle -> fontStyle.withFont(FONT)));
   }

   public static Component of(String content) {
      return Component.literal(content).withStyle(fontStyle -> fontStyle.withFont(FONT));
   }

   public static Component plain(String content) {
      return Component.literal(content).withStyle(fontStyle -> fontStyle.withFont(Style.DEFAULT_FONT));
   }

   public static Component join(Component... segments) {
      MutableComponent outputStream = Component.empty();

      for (int segmentIndex = 0; segmentIndex < segments.length; segmentIndex++) {
         if (segmentIndex > 0) {
            outputStream.append(plain(" · "));
         }

         outputStream.append(segments[segmentIndex]);
      }

      return outputStream;
   }

   public static int draw(GuiGraphics canvas, Font textRenderer, String content, int horizontalPosition, int verticalPosition, int color) {
      return canvas.drawString(textRenderer, of(content), horizontalPosition, verticalPosition, color);
   }

   public static int draw(GuiGraphics canvas, Font textRenderer, String content, int horizontalPosition, int verticalPosition, int color, boolean drawShadow) {
      return canvas.drawString(textRenderer, of(content), horizontalPosition, verticalPosition, color, drawShadow);
   }

   public static int draw(GuiGraphics canvas, Font textRenderer, Component content, int horizontalPosition, int verticalPosition, int color) {
      return canvas.drawString(textRenderer, of(content), horizontalPosition, verticalPosition, color);
   }

   public static int draw(GuiGraphics canvas, Font textRenderer, Component content, int horizontalPosition, int verticalPosition, int color, boolean drawShadow) {
      return canvas.drawString(textRenderer, of(content), horizontalPosition, verticalPosition, color, drawShadow);
   }

   public static void drawCentered(GuiGraphics canvas, Font textRenderer, String content, int horizontalPosition, int verticalPosition, int color) {
      canvas.drawCenteredString(textRenderer, of(content), horizontalPosition, verticalPosition, color);
   }

   public static void drawCentered(GuiGraphics canvas, Font textRenderer, Component content, int horizontalPosition, int verticalPosition, int color) {
      canvas.drawCenteredString(textRenderer, of(content), horizontalPosition, verticalPosition, color);
   }

   public static void drawCenteredPlain(GuiGraphics canvas, Font textRenderer, String content, int horizontalPosition, int verticalPosition, int color) {
      canvas.drawCenteredString(textRenderer, content, horizontalPosition, verticalPosition, color);
   }

   public static int width(Font textRenderer, String content) {
      return textRenderer.width(of(content));
   }

   public static int width(Font textRenderer, Component content) {
      return textRenderer.width(of(content));
   }

   public static int width(Font textRenderer, FormattedText content) {
      return content instanceof Component styledComponent ? width(textRenderer, styledComponent) : textRenderer.width(content);
   }
}
