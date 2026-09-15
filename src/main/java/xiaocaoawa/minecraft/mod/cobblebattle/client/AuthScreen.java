package xiaocaoawa.minecraft.mod.cobblebattle.client;

import io.github.rinicesiberia.shadowbattle.client.AuthenticationFormRules;
import io.github.rinicesiberia.shadowbattle.client.AuthenticationSubmission;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button.OnPress;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import xiaocaoawa.minecraft.mod.cobblebattle.account.AuthMode;

public final class AuthScreen extends Screen {
   private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath("cobblebattle", "textures/gui/auth_background.png");
   private static final int PANEL_W = 191;
   private static final int PANEL_H = 207;
   private static final int TITLE_X = 39;
   private static final int TITLE_Y = 16;
   private static final int TITLE_W = 44;
   private static final int TITLE_H = 12;
   private static final int AVATAR_SIZE = 48;
   private static final int AVATAR_Y = 37;
   private static final int PARTICIPANT_Y = 88;
   private static final int FIELD_H = 14;
   private static final int FIELD_X = 39;
   private static final int FIELD_W = 112;
   private static final int ROW_1 = 100;
   private static final int ROW_2 = 118;
   private static final int ROW_3 = 136;
   private static final int ROW_4 = 154;
   private static final int ROW_SPACING = 18;
   private static final int BUTTON_H = 16;
   private static final int LINK_GAP = 7;
   private static final int LINK_H = 10;
   private static final int CODE_W = 60;
   private static final int SEND_GAP = 4;
   private static final int SEND_W = 48;
   private static final int TEXT_INSET = 4;
   private static final int FIELD_FILL = -12937546;
   private static final int FIELD_BORDER = -13684945;
   private static final int FIELD_FOCUSED = -14649667;
   private static final int FIELD_TEXT = -1376769;
   private static final int HINT_TEXT = -7350556;
   private static final int TITLE_TEXT = -1376769;
   private static final int ERROR_TEXT = -44445;
   private static final int BUTTON_IDLE = -14649667;
   private static final int BUTTON_HOVER = -12674087;
   private static final int BUTTON_OFF = -12947828;
   private static final int BUTTON_TEXT_OFF = -7362380;
   private static final int PARTICIPANT_TEXT = -4330766;
   private static final int LINK_IDLE = -7350556;
   private static final int LINK_HOVER = -1376769;
   private static final int BACKDROP = -1609559016;
   private static final int CODE_COOLDOWN_TICKS = 1200;
   private AuthMode mode;
   private final String suggestedId;
   private final boolean emailEnabled;
   private EditBox idBox;
   private EditBox emailBox;
   private EditBox codeBox;
   private EditBox passwordBox;
   private EditBox confirmBox;
   private Button submit;
   private Button sendCode;
   private Button switchMode;
   private int idY;
   private int emailY;
   private int codeY;
   private int passwordY;
   private int confirmY;
   private Component error = Component.empty();
   private boolean waiting;
   private boolean requestingCode;
   private int codeCooldown;
   private int originX;
   private int originY;

   public AuthScreen(AuthMode mode, String suggestedId, boolean emailEnabled) {
      super(titleOf(mode));
      this.emailEnabled = emailEnabled;
      this.mode = AuthenticationFormRules.initialMode(mode, emailEnabled);
      this.suggestedId = suggestedId == null ? "" : suggestedId;
   }

   private static Component titleOf(AuthMode mode) {
      return Component.translatable(AuthenticationFormRules.titleKey(mode));
   }

   public Component getTitle() {
      return titleOf(this.mode);
   }

   private boolean needsEmail() {
      return AuthenticationFormRules.requiresEmail(this.mode, this.emailEnabled);
   }

   protected void init() {
      this.originX = (this.width - 191) / 2;
      this.originY = (this.height - 207) / 2;
      String keptId = this.idBox != null ? this.idBox.getValue() : this.suggestedId;
      String keptEmail = this.emailBox != null ? this.emailBox.getValue() : "";
      this.idBox = null;
      this.emailBox = null;
      this.codeBox = null;
      this.confirmBox = null;
      this.sendCode = null;
      this.idY = -1;
      this.emailY = -1;
      this.codeY = -1;
      this.confirmY = -1;
      int row = 100;
      if (this.mode.isBindEmail()) {
         this.idY = row;
         this.idBox = this.addField(row, 64, false, Component.translatable("cobblebattle.auth.field.legacy_id"));
         this.idBox.setValue(keptId);
         row += 18;
      }

      if (this.needsEmail()) {
         this.emailY = row;
         this.emailBox = this.addField(row, 254, false, Component.translatable("cobblebattle.auth.field.email"));
         this.emailBox.setValue(keptEmail.isEmpty() && !this.mode.isBindEmail() ? keptId : keptEmail);
         row += 18;
         this.codeY = row;
         this.codeBox = this.addField(row, 6, false, 60, Component.translatable("cobblebattle.auth.field.code"));
         this.sendCode = new AuthScreen.PanelButton(
            this.originX + 39 + 60 + 4, this.originY + row, 48, 14, Component.translatable("cobblebattle.auth.send_code"), button -> this.requestCode()
         );
         this.addRenderableWidget(this.sendCode);
         row += 18;
      } else {
         this.idY = row;
         this.idBox = this.addField(row, 16, false, Component.translatable("cobblebattle.auth.field.id"));
         this.idBox.setValue(keptId);
         row += 18;
      }

      this.passwordY = row;
      this.passwordBox = this.addField(row, 64, true, Component.translatable("cobblebattle.auth.field.password"));
      row += 18;
      if (this.mode.isRegister()) {
         this.confirmY = row;
         this.confirmBox = this.addField(row, 64, true, Component.translatable("cobblebattle.auth.field.confirm"));
         row += 18;
      }

      this.submit = new AuthScreen.PanelButton(this.originX + 39, this.originY + row, 112, 16, this.submitLabel(), button -> this.send());
      this.addRenderableWidget(this.submit);
      Component switchLabel = this.switchLabel();
      int switchWidth = Ui.width(this.font, switchLabel) + 8;
      this.switchMode = new AuthScreen.LinkButton(
         this.originX + (191 - switchWidth) / 2, this.originY + row + 16 + 7, switchWidth, 10, switchLabel, button -> this.toggleMode()
      );
      this.addRenderableWidget(this.switchMode);
      EditBox first = this.idBox != null ? this.idBox : this.emailBox;
      this.setInitialFocus(first.getValue().isEmpty() ? first : this.passwordBox);
      this.refreshSubmit();
      this.updateCodeButton();
   }

   private Component submitLabel() {
      return Component.translatable(AuthenticationFormRules.submitKey(this.mode));
   }

   private Component switchLabel() {
      return Component.translatable(AuthenticationFormRules.switchKey(this.mode, this.emailEnabled));
   }

   private void toggleMode() {
      if (!this.waiting) {
         this.mode = AuthenticationFormRules.nextMode(this.mode, this.emailEnabled);
         this.error = Component.empty();
         this.rebuildWidgets();
      }
   }

   private EditBox addField(int y, int maxLength, boolean password, Component label) {
      return this.addField(y, maxLength, password, 112, label);
   }

   private EditBox addField(int y, int maxLength, boolean password, int width, Component label) {
      EditBox box = new EditBox(this.font, this.originX + 39 + 4, this.originY + y + 1, width - 8, 12, label);
      box.setMaxLength(maxLength);
      box.setBordered(false);
      box.setTextColor(-1376769);
      box.setResponder(value -> {
         this.refreshSubmit();
         this.updateCodeButton();
      });
      if (password) {
         box.setFormatter((text, offset) -> Component.literal("*".repeat(text.length())).getVisualOrderText());
      }

      this.addRenderableWidget(box);
      return box;
   }

   private int textY(int fieldY) {
      return this.originY + fieldY + 1 + 2;
   }

   private void refreshSubmit() {
      if (this.submit != null) {
         this.submit.active = AuthenticationFormRules.canSubmit(
            this.mode,
            this.emailEnabled,
            this.waiting,
            this.idBox == null ? "" : this.idBox.getValue(),
            this.emailBox == null ? "" : this.emailBox.getValue(),
            this.codeBox == null ? "" : this.codeBox.getValue(),
            this.passwordBox.getValue()
         );
      }
   }

   private void updateCodeButton() {
      if (this.sendCode != null) {
         this.sendCode.active = AuthenticationFormRules.canRequestCode(this.waiting, this.codeCooldown, this.emailBox.getValue());
         this.sendCode
            .setMessage(
               this.codeCooldown > 0
                  ? Component.literal(AuthenticationFormRules.cooldownSeconds(this.codeCooldown) + "s")
                  : Component.translatable("cobblebattle.auth.send_code")
            );
      }
   }

   private void requestCode() {
      if (!this.waiting && this.emailBox != null && !this.emailBox.getValue().isBlank()) {
         this.error = Component.empty();
         this.waiting = true;
         this.requestingCode = true;
         this.codeCooldown = 1200;
         this.refreshSubmit();
         this.updateCodeButton();
         AuthenticationSubmission request = AuthenticationFormRules.codeRequest(this.mode, this.emailBox.getValue());
         AuthScreenHandler.submit(request.getMode(), request.getAccountId(), request.getEmail(), request.getPassword(), request.getCode());
      }
   }

   public void tick() {
      super.tick();
      if (this.codeCooldown > 0) {
         this.codeCooldown--;
         this.updateCodeButton();
      }
   }

   private void send() {
      if (!this.waiting) {
         String password = this.passwordBox.getValue();
         AuthenticationSubmission request = AuthenticationFormRules.submission(
            this.mode,
            this.idBox == null ? "" : this.idBox.getValue(),
            this.emailBox == null ? "" : this.emailBox.getValue(),
            password,
            this.confirmBox == null ? "" : this.confirmBox.getValue(),
            this.codeBox == null ? "" : this.codeBox.getValue()
         );
         if (request == null) {
            this.error = Component.translatable("cobblebattle.auth.error.mismatch");
         } else {
            this.error = Component.empty();
            this.waiting = true;
            this.requestingCode = false;
            this.refreshSubmit();
            this.updateCodeButton();
            AuthScreenHandler.submit(request.getMode(), request.getAccountId(), request.getEmail(), request.getPassword(), request.getCode());
         }
      }
   }

   void onResult(boolean ok, String document) {
      this.waiting = false;
      if (this.requestingCode) {
         this.requestingCode = false;
         if (!ok) {
            this.codeCooldown = 0;
         }

         this.error = Component.literal(document);
         if (ok && this.codeBox != null) {
            this.setFocused(this.codeBox);
         }

         this.refreshSubmit();
         this.updateCodeButton();
      } else if (ok) {
         this.onClose();
      } else {
         this.error = Component.literal(document);
         this.passwordBox.setValue("");
         if (this.confirmBox != null) {
            this.confirmBox.setValue("");
         }

         this.setFocused(this.passwordBox);
         this.refreshSubmit();
         this.updateCodeButton();
      }
   }

   public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      graphics.fill(0, 0, this.width, this.height, -1609559016);
      graphics.blit(BACKGROUND, this.originX, this.originY, 0.0F, 0.0F, 191, 207, 191, 207);
      this.drawTitle(graphics);
      this.drawAvatar(graphics);
      this.drawParticipantName(graphics);
      this.drawField(graphics, this.idY, this.idBox, 112);
      this.drawField(graphics, this.emailY, this.emailBox, 112);
      this.drawField(graphics, this.codeY, this.codeBox, 60);
      this.drawField(graphics, this.passwordY, this.passwordBox, 112);
      this.drawField(graphics, this.confirmY, this.confirmBox, 112);
      super.render(graphics, mouseX, mouseY, partialTick);
      this.drawPlaceholders(graphics);
      this.drawStatus(graphics);
   }

   private void drawTitle(GuiGraphics graphics) {
      int centreX = this.originX + 39 + 22;
      int y = this.originY + 16 + (12 - 9) / 2;
      Ui.drawCentered(graphics, this.font, this.getTitle(), centreX, y, -1376769);
   }

   private void drawAvatar(GuiGraphics graphics) {
      LocalPlayer skin = this.minecraft.player;
      if (skin instanceof AbstractClientPlayer) {
         ResourceLocation var6 = skin.getSkin().texture();
         int x = this.originX + 71;
         int y = this.originY + 37;
         graphics.blit(var6, x, y, 48, 48, 8.0F, 8.0F, 8, 8, 64, 64);
         graphics.blit(var6, x, y, 48, 48, 40.0F, 8.0F, 8, 8, 64, 64);
      }
   }

   private void drawParticipantName(GuiGraphics graphics) {
      if (this.minecraft != null && this.minecraft.player != null) {
         Ui.drawCentered(graphics, this.font, this.minecraft.player.getGameProfile().getName(), this.originX + 95, this.originY + 88, -4330766);
      }
   }

   private void drawField(GuiGraphics graphics, int y, EditBox box, int width) {
      if (box != null) {
         int left = this.originX + 39;
         int top = this.originY + y;
         graphics.fill(left, top, left + width, top + 14, box.isFocused() ? -14649667 : -13684945);
         graphics.fill(left + 1, top + 1, left + width - 1, top + 14 - 1, -12937546);
      }
   }

   private void drawPlaceholders(GuiGraphics graphics) {
      this.hint(graphics, this.idBox, this.idY, this.mode.isBindEmail() ? "cobblebattle.auth.hint.legacy_id" : "cobblebattle.auth.hint.id");
      this.hint(graphics, this.emailBox, this.emailY, "cobblebattle.auth.hint.email");
      this.hint(graphics, this.codeBox, this.codeY, "cobblebattle.auth.hint.code");
      this.hint(graphics, this.passwordBox, this.passwordY, "cobblebattle.auth.hint.password");
      this.hint(graphics, this.confirmBox, this.confirmY, "cobblebattle.auth.hint.confirm");
   }

   private void hint(GuiGraphics graphics, EditBox box, int y, String key) {
      if (box != null && box.getValue().isEmpty() && !box.isFocused()) {
         Ui.draw(graphics, this.font, Component.translatable(key), this.originX + 39 + 4, this.textY(y), -7350556, false);
      }
   }

   private void drawStatus(GuiGraphics graphics) {
      Component text = (Component)(this.waiting ? Component.translatable("cobblebattle.auth.waiting") : this.error);
      if (!text.getString().isEmpty()) {
         Ui.drawCentered(graphics, this.font, text, this.width / 2, this.originY + 207 + 6, this.waiting ? -7350556 : -44445);
      }
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode != 257 && keyCode != 335) {
         return super.keyPressed(keyCode, scanCode, modifiers);
      } else {
         EditBox next = this.nextAfterFocused();
         if (next != null) {
            this.setFocused(next);
         } else if (this.submit.active) {
            this.send();
         }

         return true;
      }
   }

   private EditBox nextAfterFocused() {
      EditBox[] order = new EditBox[]{this.idBox, this.emailBox, this.codeBox, this.passwordBox, this.confirmBox};
      boolean seen = false;

      for (EditBox box : order) {
         if (box != null) {
            if (seen) {
               return box;
            }

            seen = box.isFocused();
         }
      }

      return null;
   }

   public boolean shouldCloseOnEsc() {
      return !this.waiting;
   }

   public boolean isPauseScreen() {
      return false;
   }

   private static final class LinkButton extends Button {
      private LinkButton(int x, int y, int width, int height, Component label, OnPress onPress) {
         super(x, y, width, height, label, onPress, DEFAULT_NARRATION);
      }

      protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
         Ui.drawCentered(
            graphics,
            Minecraft.getInstance().font,
            this.getMessage(),
            this.getX() + this.width / 2,
            this.getY() + (this.height - 8) / 2,
            this.isHovered() ? -1376769 : -7350556
         );
      }

      public void updateWidgetNarration(NarrationElementOutput output) {
         this.defaultButtonNarrationText(output);
      }
   }

   private static final class PanelButton extends Button {
      private PanelButton(int x, int y, int width, int height, Component label, OnPress onPress) {
         super(x, y, width, height, label, onPress, DEFAULT_NARRATION);
      }

      protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
         int left = this.getX();
         int top = this.getY();
         int right = left + this.width;
         int bottom = top + this.height;
         int fill = !this.active ? -12947828 : (this.isHovered() ? -12674087 : -14649667);
         graphics.fill(left, top, right, bottom, -13684945);
         graphics.fill(left + 1, top + 1, right - 1, bottom - 1, fill);
         Ui.drawCentered(
            graphics, Minecraft.getInstance().font, this.getMessage(), left + this.width / 2, top + (this.height - 8) / 2, this.active ? -1376769 : -7362380
         );
      }

      public void updateWidgetNarration(NarrationElementOutput output) {
         this.defaultButtonNarrationText(output);
      }
   }
}
