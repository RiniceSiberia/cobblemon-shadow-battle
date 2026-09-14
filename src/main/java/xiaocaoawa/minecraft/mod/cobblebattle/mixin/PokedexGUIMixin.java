package xiaocaoawa.minecraft.mod.cobblebattle.mixin;

import com.cobblemon.mod.common.api.pokedex.entry.PokedexEntry;
import com.cobblemon.mod.common.api.pokedex.entry.PokedexForm;
import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.client.CobblemonResources;
import com.cobblemon.mod.common.client.gui.pokedex.PokedexGUI;
import com.cobblemon.mod.common.client.gui.pokedex.ScaledButton;
import com.cobblemon.mod.common.client.gui.pokedex.widgets.StatsWidget;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import xiaocaoawa.minecraft.mod.cobblebattle.client.BackButton;
import xiaocaoawa.minecraft.mod.cobblebattle.client.ServerDex;

@Mixin(
   value = {PokedexGUI.class},
   remap = false
)
public abstract class PokedexGUIMixin {
   @Shadow
   private PokedexEntry selectedEntry;
   @Shadow
   private PokedexForm selectedForm;
   @Shadow
   private ScaledButton regionSelectWidgetUp;
   @Shadow
   private ScaledButton regionSelectWidgetDown;
   private static final ResourceLocation cobblebattle$BLANK = ResourceLocation.fromNamespaceAndPath("cobblebattle", "textures/gui/blank.png");

   @Shadow
   public abstract GuiEventListener getTabInfoElement();

   @Inject(
      method = {"init", "method_25426"},
      at = {@At("TAIL")}
   )
   private void cobblebattle$hidePageArrows(CallbackInfo ci) {
      if (ServerDex.active()) {
         if (this.regionSelectWidgetUp != null) {
            this.regionSelectWidgetUp.setVisible(false);
         }

         if (this.regionSelectWidgetDown != null) {
            this.regionSelectWidgetDown.setVisible(false);
         }
      }
   }

   @Inject(
      method = {"updateTabInfoElement"},
      at = {@At("TAIL")}
   )
   private void cobblebattle$hostBaseStats(CallbackInfo ci) {
      if (ServerDex.active()) {
         if (this.getTabInfoElement() instanceof StatsWidget stats) {
            Map<Stat, Integer> host = ServerDex.statsFor(this.selectedEntry, this.selectedForm);
            if (host != null) {
               stats.setBaseStats(host);
            }
         }
      }
   }

   @ModifyArg(
      method = {"render", "method_25394"},
      at = @At(
         value = "INVOKE",
         target = "Lcom/cobblemon/mod/common/api/gui/GuiUtilsKt;blitk$default"
      ),
      index = 1
   )
   private ResourceLocation cobblebattle$hideBandIcons(ResourceLocation texture) {
      if (ServerDex.active() && texture != null) {
         String path = texture.getPath();
         return !path.endsWith("globe_icon.png") && !path.endsWith("caught_seen_icon.png") ? texture : cobblebattle$BLANK;
      } else {
         return texture;
      }
   }

   @ModifyArgs(
      method = {"render", "method_25394"},
      at = @At(
         value = "INVOKE",
         target = "Lcom/cobblemon/mod/common/client/render/RenderHelperKt;drawScaledText$default(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/network/chat/MutableComponent;Ljava/lang/Number;Ljava/lang/Number;FLjava/lang/Number;IIZZLjava/lang/Integer;Ljava/lang/Integer;ILjava/lang/Object;)V"
      ),
      require = 0,
      expect = 0
   )
   private void cobblebattle$bandTextOfficial(Args args) {
      this.cobblebattle$bandText(args);
   }

   @ModifyArgs(
      method = {"render", "method_25394"},
      at = @At(
         value = "INVOKE",
         target = "Lcom/cobblemon/mod/common/client/render/RenderHelperKt;drawScaledText$default(Lnet/minecraft/class_332;Lnet/minecraft/class_2960;Lnet/minecraft/class_5250;Ljava/lang/Number;Ljava/lang/Number;FLjava/lang/Number;IIZZLjava/lang/Integer;Ljava/lang/Integer;ILjava/lang/Object;)V"
      ),
      require = 0,
      expect = 0
   )
   private void cobblebattle$bandTextIntermediary(Args args) {
      this.cobblebattle$bandText(args);
   }

   private void cobblebattle$bandText(Args args) {
      if (ServerDex.active()) {
         MutableComponent text = (MutableComponent)args.get(2);
         if (text.getContents() instanceof TranslatableContents key) {
            if (key.getKey().startsWith("cobblemon.ui.pokedex.region.")) {
               PokedexGUI self = (PokedexGUI)(Object)this;
               int originX = (self.width - 345) / 2;
               Font font = Minecraft.getInstance().font;
               int width = font.width(text.copy().withStyle(style -> style.withFont(CobblemonResources.INSTANCE.getDEFAULT_LARGE())));
               args.set(3, originX + 322 - width);
            }
         } else {
            args.set(2, Component.empty());
         }
      }
   }

   @Inject(
      method = {"render", "method_25394"},
      at = {@At("TAIL")}
   )
   private void cobblebattle$drawBack(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      if (ServerDex.active()) {
         PokedexGUI self = (PokedexGUI)(Object)this;
         int x = (self.width - 345) / 2;
         int y = (self.height - 207) / 2;
         BackButton.draw(graphics, Minecraft.getInstance().font, x, y, mouseX, mouseY);
      }
   }

   @Inject(
      method = {"mouseClicked", "method_25402"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void cobblebattle$clickBack(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
      if (ServerDex.active() && button == 0) {
         PokedexGUI self = (PokedexGUI)(Object)this;
         int x = (self.width - 345) / 2;
         int y = (self.height - 207) / 2;
         if (BackButton.contains(x, y, mouseX, mouseY)) {
            ServerDex.requestMain();
            cir.setReturnValue(true);
         }
      }
   }

   @Inject(
      method = {"updatePokedexRegion"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void cobblebattle$turnPage(boolean nextIndex, CallbackInfo ci) {
      if (ServerDex.active()) {
         ci.cancel();
      }
   }
}
