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
   private static final List<ChatPanel.NameBox> NAME_BOXES = new ArrayList<>();
   private static int listLeft;
   private static int listTop;

   private ChatPanel() {
   }

   public static int battleLogShift() {
      return 81;
   }

   public static boolean battleLogShowing() {
      return CobblemonClient.INSTANCE.getBattle() != null;
   }

   public static int originX(int screenWidth) {
      return screenWidth - 181;
   }

   public static int originY(int screenHeight) {
      return screenHeight - 30 - 79;
   }

   public static boolean visible() {
      return ChatState.signedIn() && ChatState.enabled() && ClientSettings.chatHud() ? !battleLogShowing() || ChatState.inBattle() : false;
   }

   public static boolean contains(int screenWidth, int screenHeight, double x, double y) {
      int left = originX(screenWidth);
      int top = originY(screenHeight);
      return x >= left && x < left + 169 && y >= top && y < top + 79;
   }

   public static boolean inInputBox(int relX, int relY) {
      return relX >= 5 && relX < 158 && relY >= 65 && relY < 75;
   }

   public static void drawBackground(GuiGraphics graphics, int originX, int originY) {
      graphics.blit(TEXTURE, originX, originY, 0.0F, 0.0F, 169, 79, 169, 79);
   }

   public static void drawTabs(GuiGraphics graphics, Font font, int originX, int originY, boolean battleSelected) {
      Component global = Component.translatable("cobblebattle.chat.tab.global");
      Component battle = Component.translatable("cobblebattle.chat.tab.battle");
      float scale = tabScale(font, global, battle);
      drawTab(graphics, font, originX + 0, originY + 0, global, !battleSelected, scale);
      drawTab(graphics, font, originX + 21, originY + 0, battle, battleSelected, scale);
   }

   private static void drawTab(GuiGraphics graphics, Font font, int x, int y, Component label, boolean selected, float scale) {
      float textHeight = 8.0F * scale;
      graphics.pose().pushPose();
      graphics.pose().translate(x + 10.5F, y + (7.0F - textHeight) / 2.0F, 0.0F);
      graphics.pose().scale(scale, scale, 1.0F);
      Ui.drawCentered(graphics, font, label, 0, 0, selected ? -1 : -4605511);
      graphics.pose().popPose();
   }

   private static float tabScale(Font font, Component... labels) {
      int widest = 1;

      for (Component label : labels) {
         widest = Math.max(widest, Ui.width(font, label));
      }

      float byWidth = 17.0F / widest;
      float byHeight = 0.625F;
      return Math.min(1.0F, Math.min(byWidth, byHeight));
   }

   private static List<ChatPanel.Row> rows(Font font, List<ChatLog.Line> lines) {
      List<ChatPanel.Row> outputStream = new ArrayList<>();

      for (ChatLog.Line line : lines) {
         List<String> wrapped = wrap(font, prefixOf(line) + line.text(), 139);

         for (int i = 0; i < wrapped.size(); i++) {
            outputStream.add(new ChatPanel.Row(line, wrapped.get(i), i == 0));
         }
      }

      return outputStream;
   }

   private static String prefixOf(ChatLog.Line line) {
      return line.name() + ": ";
   }

   public static ChatLog.Line lineAt(double mouseX, double mouseY) {
      if (!(mouseX < listLeft) && !(mouseX >= listLeft + 153) && !(mouseY < listTop) && !(mouseY >= listTop + 44)) {
         for (ChatPanel.NameBox box : NAME_BOXES) {
            if (mouseX >= box.x() && mouseX < box.x() + box.w() && mouseY >= box.y() && mouseY < box.y() + box.h()) {
               return box.line();
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public static int maxScroll(Font font, List<ChatLog.Line> lines) {
      return Math.max(0, rows(font, lines).size() - 4);
   }

   public static void drawMessages(GuiGraphics graphics, Font font, int originX, int originY, List<ChatLog.Line> lines, Component empty, int scrollOffsets) {
      int left = originX + 5;
      int top = originY + 14;
      NAME_BOXES.clear();
      listLeft = left;
      listTop = top;
      graphics.enableScissor(left, top, left + 153, top + 44);

      try {
         if (!lines.isEmpty()) {
            List<ChatPanel.Row> all = rows(font, lines);
            int clamped = Math.max(0, Math.min(scrollOffsets, Math.max(0, all.size() - 4)));
            int end = all.size() - clamped;
            int start = Math.max(0, end - 4);
            int y = top + 44 - 2 - (end - start) * 10;

            for (int i = start; i < end; i++) {
               drawRow(graphics, font, left, y, all.get(i));
               y += 10;
            }

            if (clamped > 0) {
               drawScrollOffsetsHint(graphics, left, top);
            }

            return;
         }

         Ui.drawCentered(graphics, font, empty, left + 76, top + (44 - 9) / 2, -7697782);
      } finally {
         graphics.disableScissor();
      }
   }

   private static void drawScrollOffsetsHint(GuiGraphics graphics, int left, int top) {
      int x = left + 153 - 2 - 2;
      int y = top + 44 - 2 - 3;
      graphics.fill(x, y, x + 2, y + 2, -8399617);
   }

   private static void drawRow(GuiGraphics graphics, Font font, int left, int y, ChatPanel.Row row) {
      int textX = left + 2 + 8 + 2;
      int textY = y + 1;
      if (!row.first()) {
         Ui.draw(graphics, font, row.text(), textX, textY, -1644826, false);
      } else {
         drawFace(graphics, row.line().sender(), left + 2, y);
         String prefix = prefixOf(row.line());
         if (!row.text().startsWith(prefix)) {
            Ui.draw(graphics, font, row.text(), textX, textY, -1644826, false);
         } else {
            String who = row.line().name();
            int nameWidth = Ui.width(font, who);
            NAME_BOXES.add(new ChatPanel.NameBox(textX, textY, nameWidth, 9, row.line()));
            Ui.draw(graphics, font, who, textX, textY, -8399617, false);
            Ui.draw(graphics, font, ": ", textX + nameWidth, textY, -6645094, false);
            Ui.draw(graphics, font, row.text().substring(prefix.length()), textX + nameWidth + Ui.width(font, ": "), textY, -1644826, false);
         }
      }
   }

   public static void drawFace(GuiGraphics graphics, UUID sender, int x, int y) {
      Minecraft minecraft = Minecraft.getInstance();
      ResourceLocation skin = null;
      LocalPlayer var7 = minecraft.player;
      if (var7 instanceof AbstractClientPlayer && var7.getUUID().equals(sender)) {
         skin = var7.getSkin().texture();
      }

      if (skin == null) {
         skin = DefaultPlayerSkin.get(sender).texture();
      }

      graphics.blit(skin, x, y, 8, 8, 8.0F, 8.0F, 8, 8, 64, 64);
      graphics.blit(skin, x, y, 8, 8, 40.0F, 8.0F, 8, 8, 64, 64);
   }

   public static List<String> wrap(Font font, String text, int width) {
      List<String> outputStream = new ArrayList<>();
      StringBuilder row = new StringBuilder();
      int rowWidth = 0;
      int i = 0;

      while (i < text.length()) {
         int cp = text.codePointAt(i);
         String ch = new String(Character.toChars(cp));
         i += Character.charCount(cp);
         int chWidth = Ui.width(font, ch);
         if (rowWidth + chWidth > width && row.length() > 0) {
            outputStream.add(row.toString());
            row.setLength(0);
            rowWidth = 0;
         }

         row.append(ch);
         rowWidth += chWidth;
      }

      if (row.length() > 0) {
         outputStream.add(row.toString());
      }

      if (outputStream.isEmpty()) {
         outputStream.add("");
      }

      return outputStream;
   }

   public static ChatLog.Channel tabAt(int relX, int relY) {
      if (relY < 0 || relY >= 7) {
         return null;
      } else if (relX >= 0 && relX < 21) {
         return ChatLog.Channel.GLOBAL;
      } else {
         return relX >= 21 && relX < 42 ? ChatLog.Channel.BATTLE : null;
      }
   }

   private record NameBox(int x, int y, int w, int h, ChatLog.Line line) {
   }

   private record Row(ChatLog.Line line, String text, boolean first) {
   }
}
