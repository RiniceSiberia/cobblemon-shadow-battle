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
   private static final ResourceLocation AUTH_PANEL_TEXTURE = ResourceLocation.fromNamespaceAndPath("cobblebattle", "textures/gui/auth_background.png");
   private static final int PANEL_WIDTH = 191;
   private static final int PANEL_HEIGHT = 207;
   private static final int TITLE_LEFT_OFFSET = 39;
   private static final int TITLE_TOP_OFFSET = 16;
   private static final int TITLE_WIDTH = 44;
   private static final int TITLE_HEIGHT = 12;
   private static final int AVATAR_EDGE_LENGTH = 48;
   private static final int AVATAR_TOP_OFFSET = 37;
   private static final int PLAYER_NAME_TOP_OFFSET = 88;
   private static final int INPUT_HEIGHT = 14;
   private static final int INPUT_LEFT_OFFSET = 39;
   private static final int INPUT_WIDTH = 112;
   private static final int FIRST_INPUT_ROW = 100;
   private static final int SECOND_INPUT_ROW = 118;
   private static final int THIRD_INPUT_ROW = 136;
   private static final int FOURTH_INPUT_ROW = 154;
   private static final int INPUT_ROW_SPACING = 18;
   private static final int ACTION_BUTTON_HEIGHT = 16;
   private static final int MODE_LINK_GAP = 7;
   private static final int MODE_LINK_HEIGHT = 10;
   private static final int CODE_INPUT_WIDTH = 60;
   private static final int CODE_BUTTON_GAP = 4;
   private static final int CODE_BUTTON_WIDTH = 48;
   private static final int INPUT_TEXT_INSET = 4;
   private static final int INPUT_FILL_COLOR = -12937546;
   private static final int INPUT_BORDER_COLOR = -13684945;
   private static final int FOCUSED_INPUT_BORDER_COLOR = -14649667;
   private static final int INPUT_TEXT_COLOR = -1376769;
   private static final int INPUT_HINT_COLOR = -7350556;
   private static final int TITLE_TEXT_COLOR = -1376769;
   private static final int ERROR_TEXT_COLOR = -44445;
   private static final int BUTTON_IDLE_COLOR = -14649667;
   private static final int BUTTON_HOVER_COLOR = -12674087;
   private static final int BUTTON_DISABLED_COLOR = -12947828;
   private static final int BUTTON_DISABLED_TEXT_COLOR = -7362380;
   private static final int PLAYER_NAME_COLOR = -4330766;
   private static final int LINK_IDLE_COLOR = -7350556;
   private static final int LINK_HOVER_COLOR = -1376769;
   private static final int SCREEN_BACKDROP_COLOR = -1609559016;
   private static final int CODE_REQUEST_COOLDOWN_TICKS = 1200;
   private AuthMode authenticationMode;
   private final String suggestedAccountId;
   private final boolean emailAuthenticationAvailable;
   private EditBox accountIdInput;
   private EditBox emailInput;
   private EditBox verificationCodeInput;
   private EditBox passwordInput;
   private EditBox passwordConfirmationInput;
   private Button submitButton;
   private Button verificationCodeButton;
   private Button modeSwitchButton;
   private int accountIdRow;
   private int emailRow;
   private int verificationCodeRow;
   private int passwordRow;
   private int passwordConfirmationRow;
   private Component statusMessage = Component.empty();
   private boolean requestInProgress;
   private boolean verificationCodeRequestInProgress;
   private int codeRequestCooldownTicks;
   private int panelLeft;
   private int panelTop;

   public AuthScreen(AuthMode authenticationMode, String suggestedAccountId, boolean emailAuthenticationAvailable) {
      super(authenticationTitle(authenticationMode));
      this.emailAuthenticationAvailable = emailAuthenticationAvailable;
      this.authenticationMode = AuthenticationFormRules.initialMode(authenticationMode, emailAuthenticationAvailable);
      this.suggestedAccountId = suggestedAccountId == null ? "" : suggestedAccountId;
   }

   private static Component authenticationTitle(AuthMode authenticationMode) {
      return Component.translatable(AuthenticationFormRules.titleKey(authenticationMode));
   }

   public Component getTitle() {
      return authenticationTitle(this.authenticationMode);
   }

   private boolean requiresEmail() {
      return AuthenticationFormRules.requiresEmail(this.authenticationMode, this.emailAuthenticationAvailable);
   }

   protected void init() {
      this.panelLeft = (this.width - 191) / 2;
      this.panelTop = (this.height - 207) / 2;
      String preservedAccountId = this.accountIdInput != null ? this.accountIdInput.getValue() : this.suggestedAccountId;
      String preservedEmail = this.emailInput != null ? this.emailInput.getValue() : "";
      this.accountIdInput = null;
      this.emailInput = null;
      this.verificationCodeInput = null;
      this.passwordConfirmationInput = null;
      this.verificationCodeButton = null;
      this.accountIdRow = -1;
      this.emailRow = -1;
      this.verificationCodeRow = -1;
      this.passwordConfirmationRow = -1;
      int nextInputRow = 100;
      if (this.authenticationMode.isBindEmail()) {
         this.accountIdRow = nextInputRow;
         this.accountIdInput = this.createStandardInputField(nextInputRow, 64, false, Component.translatable("cobblebattle.auth.field.legacy_id"));
         this.accountIdInput.setValue(preservedAccountId);
         nextInputRow += 18;
      }

      if (this.requiresEmail()) {
         this.emailRow = nextInputRow;
         this.emailInput = this.createStandardInputField(nextInputRow, 254, false, Component.translatable("cobblebattle.auth.field.email"));
         this.emailInput.setValue(preservedEmail.isEmpty() && !this.authenticationMode.isBindEmail() ? preservedAccountId : preservedEmail);
         nextInputRow += 18;
         this.verificationCodeRow = nextInputRow;
         this.verificationCodeInput = this.createSizedInputField(nextInputRow, 6, false, 60, Component.translatable("cobblebattle.auth.field.code"));
         this.verificationCodeButton = new AuthScreen.ActionPanelButton(
            this.panelLeft + 39 + 60 + 4, this.panelTop + nextInputRow, 48, 14, Component.translatable("cobblebattle.auth.send_code"), pressedButton -> this.requestVerificationCode()
         );
         this.addRenderableWidget(this.verificationCodeButton);
         nextInputRow += 18;
      } else {
         this.accountIdRow = nextInputRow;
         this.accountIdInput = this.createStandardInputField(nextInputRow, 16, false, Component.translatable("cobblebattle.auth.field.id"));
         this.accountIdInput.setValue(preservedAccountId);
         nextInputRow += 18;
      }

      this.passwordRow = nextInputRow;
      this.passwordInput = this.createStandardInputField(nextInputRow, 64, true, Component.translatable("cobblebattle.auth.field.password"));
      nextInputRow += 18;
      if (this.authenticationMode.isRegister()) {
         this.passwordConfirmationRow = nextInputRow;
         this.passwordConfirmationInput = this.createStandardInputField(nextInputRow, 64, true, Component.translatable("cobblebattle.auth.field.confirm"));
         nextInputRow += 18;
      }

      this.submitButton = new AuthScreen.ActionPanelButton(this.panelLeft + 39, this.panelTop + nextInputRow, 112, 16, this.submitButtonLabel(), pressedButton -> this.submitCredentials());
      this.addRenderableWidget(this.submitButton);
      Component modeSwitchLabel = this.modeSwitchLabel();
      int modeSwitchWidth = Ui.width(this.font, modeSwitchLabel) + 8;
      this.modeSwitchButton = new AuthScreen.ModeLinkButton(
         this.panelLeft + (191 - modeSwitchWidth) / 2, this.panelTop + nextInputRow + 16 + 7, modeSwitchWidth, 10, modeSwitchLabel, pressedButton -> this.advanceAuthenticationMode()
      );
      this.addRenderableWidget(this.modeSwitchButton);
      EditBox initialFocusField = this.accountIdInput != null ? this.accountIdInput : this.emailInput;
      this.setInitialFocus(initialFocusField.getValue().isEmpty() ? initialFocusField : this.passwordInput);
      this.refreshSubmitAvailability();
      this.refreshCodeRequestButton();
   }

   private Component submitButtonLabel() {
      return Component.translatable(AuthenticationFormRules.submitKey(this.authenticationMode));
   }

   private Component modeSwitchLabel() {
      return Component.translatable(AuthenticationFormRules.switchKey(this.authenticationMode, this.emailAuthenticationAvailable));
   }

   private void advanceAuthenticationMode() {
      if (!this.requestInProgress) {
         this.authenticationMode = AuthenticationFormRules.nextMode(this.authenticationMode, this.emailAuthenticationAvailable);
         this.statusMessage = Component.empty();
         this.rebuildWidgets();
      }
   }

   private EditBox createStandardInputField(int verticalPosition, int characterLimit, boolean maskContent, Component accessibleLabel) {
      return this.createSizedInputField(verticalPosition, characterLimit, maskContent, 112, accessibleLabel);
   }

   private EditBox createSizedInputField(int verticalPosition, int characterLimit, boolean maskContent, int elementWidth, Component accessibleLabel) {
      EditBox inputField = new EditBox(this.font, this.panelLeft + 39 + 4, this.panelTop + verticalPosition + 1, elementWidth - 8, 12, accessibleLabel);
      inputField.setMaxLength(characterLimit);
      inputField.setBordered(false);
      inputField.setTextColor(-1376769);
      inputField.setResponder(currentValue -> {
         this.refreshSubmitAvailability();
         this.refreshCodeRequestButton();
      });
      if (maskContent) {
         inputField.setFormatter((displayText, textOffset) -> Component.literal("*".repeat(displayText.length())).getVisualOrderText());
      }

      this.addRenderableWidget(inputField);
      return inputField;
   }

   private int inputTextTop(int inputRow) {
      return this.panelTop + inputRow + 1 + 2;
   }

   private void refreshSubmitAvailability() {
      if (this.submitButton != null) {
         this.submitButton.active = AuthenticationFormRules.canSubmit(
            this.authenticationMode,
            this.emailAuthenticationAvailable,
            this.requestInProgress,
            this.accountIdInput == null ? "" : this.accountIdInput.getValue(),
            this.emailInput == null ? "" : this.emailInput.getValue(),
            this.verificationCodeInput == null ? "" : this.verificationCodeInput.getValue(),
            this.passwordInput.getValue()
         );
      }
   }

   private void refreshCodeRequestButton() {
      if (this.verificationCodeButton != null) {
         this.verificationCodeButton.active = AuthenticationFormRules.canRequestCode(this.requestInProgress, this.codeRequestCooldownTicks, this.emailInput.getValue());
         this.verificationCodeButton
            .setMessage(
               this.codeRequestCooldownTicks > 0
                  ? Component.literal(AuthenticationFormRules.cooldownSeconds(this.codeRequestCooldownTicks) + "s")
                  : Component.translatable("cobblebattle.auth.send_code")
            );
      }
   }

   private void requestVerificationCode() {
      if (!this.requestInProgress && this.emailInput != null && !this.emailInput.getValue().isBlank()) {
         this.statusMessage = Component.empty();
         this.requestInProgress = true;
         this.verificationCodeRequestInProgress = true;
         this.codeRequestCooldownTicks = 1200;
         this.refreshSubmitAvailability();
         this.refreshCodeRequestButton();
         AuthenticationSubmission authenticationSubmission = AuthenticationFormRules.codeRequest(this.authenticationMode, this.emailInput.getValue());
         AuthScreenHandler.submit(authenticationSubmission.getMode(), authenticationSubmission.getAccountId(), authenticationSubmission.getEmail(), authenticationSubmission.getPassword(), authenticationSubmission.getCode());
      }
   }

   public void tick() {
      super.tick();
      if (this.codeRequestCooldownTicks > 0) {
         this.codeRequestCooldownTicks--;
         this.refreshCodeRequestButton();
      }
   }

   private void submitCredentials() {
      if (!this.requestInProgress) {
         String maskContent = this.passwordInput.getValue();
         AuthenticationSubmission authenticationSubmission = AuthenticationFormRules.submission(
            this.authenticationMode,
            this.accountIdInput == null ? "" : this.accountIdInput.getValue(),
            this.emailInput == null ? "" : this.emailInput.getValue(),
            maskContent,
            this.passwordConfirmationInput == null ? "" : this.passwordConfirmationInput.getValue(),
            this.verificationCodeInput == null ? "" : this.verificationCodeInput.getValue()
         );
         if (authenticationSubmission == null) {
            this.statusMessage = Component.translatable("cobblebattle.auth.error.mismatch");
         } else {
            this.statusMessage = Component.empty();
            this.requestInProgress = true;
            this.verificationCodeRequestInProgress = false;
            this.refreshSubmitAvailability();
            this.refreshCodeRequestButton();
            AuthScreenHandler.submit(authenticationSubmission.getMode(), authenticationSubmission.getAccountId(), authenticationSubmission.getEmail(), authenticationSubmission.getPassword(), authenticationSubmission.getCode());
         }
      }
   }

   void handleAuthenticationResult(boolean successful, String resultMessage) {
      this.requestInProgress = false;
      if (this.verificationCodeRequestInProgress) {
         this.verificationCodeRequestInProgress = false;
         if (!successful) {
            this.codeRequestCooldownTicks = 0;
         }

         this.statusMessage = Component.literal(resultMessage);
         if (successful && this.verificationCodeInput != null) {
            this.setFocused(this.verificationCodeInput);
         }

         this.refreshSubmitAvailability();
         this.refreshCodeRequestButton();
      } else if (successful) {
         this.onClose();
      } else {
         this.statusMessage = Component.literal(resultMessage);
         this.passwordInput.setValue("");
         if (this.passwordConfirmationInput != null) {
            this.passwordConfirmationInput.setValue("");
         }

         this.setFocused(this.passwordInput);
         this.refreshSubmitAvailability();
         this.refreshCodeRequestButton();
      }
   }

   public void renderBackground(GuiGraphics canvas, int pointerX, int pointerY, float frameDelta) {
   }

   public void render(GuiGraphics canvas, int pointerX, int pointerY, float frameDelta) {
      canvas.fill(0, 0, this.width, this.height, -1609559016);
      canvas.blit(AUTH_PANEL_TEXTURE, this.panelLeft, this.panelTop, 0.0F, 0.0F, 191, 207, 191, 207);
      this.renderTitle(canvas);
      this.renderPlayerAvatar(canvas);
      this.renderPlayerName(canvas);
      this.renderInputBackground(canvas, this.accountIdRow, this.accountIdInput, 112);
      this.renderInputBackground(canvas, this.emailRow, this.emailInput, 112);
      this.renderInputBackground(canvas, this.verificationCodeRow, this.verificationCodeInput, 60);
      this.renderInputBackground(canvas, this.passwordRow, this.passwordInput, 112);
      this.renderInputBackground(canvas, this.passwordConfirmationRow, this.passwordConfirmationInput, 112);
      super.render(canvas, pointerX, pointerY, frameDelta);
      this.renderInputHints(canvas);
      this.renderStatusMessage(canvas);
   }

   private void renderTitle(GuiGraphics canvas) {
      int titleCenterX = this.panelLeft + 39 + 22;
      int verticalPosition = this.panelTop + 16 + (12 - 9) / 2;
      Ui.drawCentered(canvas, this.font, this.getTitle(), titleCenterX, verticalPosition, -1376769);
   }

   private void renderPlayerAvatar(GuiGraphics canvas) {
      LocalPlayer localPlayer = this.minecraft.player;
      if (localPlayer instanceof AbstractClientPlayer) {
         ResourceLocation skinTexture = localPlayer.getSkin().texture();
         int horizontalPosition = this.panelLeft + 71;
         int verticalPosition = this.panelTop + 37;
         canvas.blit(skinTexture, horizontalPosition, verticalPosition, 48, 48, 8.0F, 8.0F, 8, 8, 64, 64);
         canvas.blit(skinTexture, horizontalPosition, verticalPosition, 48, 48, 40.0F, 8.0F, 8, 8, 64, 64);
      }
   }

   private void renderPlayerName(GuiGraphics canvas) {
      if (this.minecraft != null && this.minecraft.player != null) {
         Ui.drawCentered(canvas, this.font, this.minecraft.player.getGameProfile().getName(), this.panelLeft + 95, this.panelTop + 88, -4330766);
      }
   }

   private void renderInputBackground(GuiGraphics canvas, int verticalPosition, EditBox inputField, int elementWidth) {
      if (inputField != null) {
         int leftEdge = this.panelLeft + 39;
         int topEdge = this.panelTop + verticalPosition;
         canvas.fill(leftEdge, topEdge, leftEdge + elementWidth, topEdge + 14, inputField.isFocused() ? -14649667 : -13684945);
         canvas.fill(leftEdge + 1, topEdge + 1, leftEdge + elementWidth - 1, topEdge + 14 - 1, -12937546);
      }
   }

   private void renderInputHints(GuiGraphics canvas) {
      this.renderInputHint(canvas, this.accountIdInput, this.accountIdRow, this.authenticationMode.isBindEmail() ? "cobblebattle.auth.hint.legacy_id" : "cobblebattle.auth.hint.id");
      this.renderInputHint(canvas, this.emailInput, this.emailRow, "cobblebattle.auth.hint.email");
      this.renderInputHint(canvas, this.verificationCodeInput, this.verificationCodeRow, "cobblebattle.auth.hint.code");
      this.renderInputHint(canvas, this.passwordInput, this.passwordRow, "cobblebattle.auth.hint.password");
      this.renderInputHint(canvas, this.passwordConfirmationInput, this.passwordConfirmationRow, "cobblebattle.auth.hint.confirm");
   }

   private void renderInputHint(GuiGraphics canvas, EditBox inputField, int verticalPosition, String translationKey) {
      if (inputField != null && inputField.getValue().isEmpty() && !inputField.isFocused()) {
         Ui.draw(canvas, this.font, Component.translatable(translationKey), this.panelLeft + 39 + 4, this.inputTextTop(verticalPosition), -7350556, false);
      }
   }

   private void renderStatusMessage(GuiGraphics canvas) {
      Component displayText = (Component)(this.requestInProgress ? Component.translatable("cobblebattle.auth.waiting") : this.statusMessage);
      if (!displayText.getString().isEmpty()) {
         Ui.drawCentered(canvas, this.font, displayText, this.width / 2, this.panelTop + 207 + 6, this.requestInProgress ? -7350556 : -44445);
      }
   }

   public boolean keyPressed(int pressedKeyCode, int physicalScanCode, int modifierMask) {
      if (pressedKeyCode != 257 && pressedKeyCode != 335) {
         return super.keyPressed(pressedKeyCode, physicalScanCode, modifierMask);
      } else {
         EditBox nextInput = this.nextInputAfterFocus();
         if (nextInput != null) {
            this.setFocused(nextInput);
         } else if (this.submitButton.active) {
            this.submitCredentials();
         }

         return true;
      }
   }

   private EditBox nextInputAfterFocus() {
      EditBox[] focusOrder = new EditBox[]{this.accountIdInput, this.emailInput, this.verificationCodeInput, this.passwordInput, this.passwordConfirmationInput};
      boolean passedFocusedInput = false;

      for (EditBox inputField : focusOrder) {
         if (inputField != null) {
            if (passedFocusedInput) {
               return inputField;
            }

            passedFocusedInput = inputField.isFocused();
         }
      }

      return null;
   }

   public boolean shouldCloseOnEsc() {
      return !this.requestInProgress;
   }

   public boolean isPauseScreen() {
      return false;
   }

   private static final class ModeLinkButton extends Button {
      private ModeLinkButton(int horizontalPosition, int verticalPosition, int elementWidth, int elementHeight, Component accessibleLabel, OnPress pressAction) {
         super(horizontalPosition, verticalPosition, elementWidth, elementHeight, accessibleLabel, pressAction, DEFAULT_NARRATION);
      }

      protected void renderWidget(GuiGraphics canvas, int pointerX, int pointerY, float frameDelta) {
         Ui.drawCentered(
            canvas,
            Minecraft.getInstance().font,
            this.getMessage(),
            this.getX() + this.width / 2,
            this.getY() + (this.height - 8) / 2,
            this.isHovered() ? -1376769 : -7350556
         );
      }

      public void updateWidgetNarration(NarrationElementOutput narrationOutput) {
         this.defaultButtonNarrationText(narrationOutput);
      }
   }

   private static final class ActionPanelButton extends Button {
      private ActionPanelButton(int horizontalPosition, int verticalPosition, int elementWidth, int elementHeight, Component accessibleLabel, OnPress pressAction) {
         super(horizontalPosition, verticalPosition, elementWidth, elementHeight, accessibleLabel, pressAction, DEFAULT_NARRATION);
      }

      protected void renderWidget(GuiGraphics canvas, int pointerX, int pointerY, float frameDelta) {
         int leftEdge = this.getX();
         int topEdge = this.getY();
         int rightEdge = leftEdge + this.width;
         int bottomEdge = topEdge + this.height;
         int interiorColor = !this.active ? -12947828 : (this.isHovered() ? -12674087 : -14649667);
         canvas.fill(leftEdge, topEdge, rightEdge, bottomEdge, -13684945);
         canvas.fill(leftEdge + 1, topEdge + 1, rightEdge - 1, bottomEdge - 1, interiorColor);
         Ui.drawCentered(
            canvas, Minecraft.getInstance().font, this.getMessage(), leftEdge + this.width / 2, topEdge + (this.height - 8) / 2, this.active ? -1376769 : -7362380
         );
      }

      public void updateWidgetNarration(NarrationElementOutput narrationOutput) {
         this.defaultButtonNarrationText(narrationOutput);
      }
   }
}
