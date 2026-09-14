package xiaocaoawa.minecraft.mod.cobblebattle.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import xiaocaoawa.minecraft.mod.cobblebattle.config.CobbleBattleConfig;
import xiaocaoawa.minecraft.mod.cobblebattle.lang.Msg;

public final class SubReloadCommand extends AbstractSubCommand {
   @Override
   public String name() {
      return "reload";
   }

   @Override
   public int permissionLevel() {
      return 2;
   }

   @Override
   public void build(LiteralArgumentBuilder<CommandSourceStack> node) {
      node.executes(ctx -> this.run((CommandSourceStack)ctx.getSource()));
   }

   private int run(CommandSourceStack source) {
      CobbleBattleConfig config = service().config();

      CobbleBattleConfig.Reloaded result;
      try {
         result = config.reload();
      } catch (RuntimeException failure) {
         source.sendFailure(Msg.of(ChatFormatting.RED, "cmd.reload.failed", failure.getMessage()));
         return 0;
      }

      Msg.init(config.language);
      source.sendSuccess(() -> Msg.of(ChatFormatting.AQUA, "cmd.reload.done"), true);
      if (result.nothingChanged()) {
         source.sendSuccess(() -> Msg.of(ChatFormatting.GRAY, "cmd.reload.unchanged"), false);
         return 1;
      } else {
         if (!result.live().isEmpty()) {
            source.sendSuccess(() -> Msg.of(ChatFormatting.GREEN, "cmd.reload.live", String.join(", ", result.live())), false);
         }

         if (!result.needsReconnect().isEmpty()) {
            source.sendSuccess(() -> Msg.of(ChatFormatting.YELLOW, "cmd.reload.reconnecting", String.join(", ", result.needsReconnect())), true);
            service().reconnect("config reloaded: " + String.join(", ", result.needsReconnect()));
         } else if (service().connectionRefusal() != null) {
            source.sendSuccess(() -> Msg.of(ChatFormatting.YELLOW, "cmd.reload.retrying", service().connectionRefusal()), true);
            service().reconnect("retrying after the battle server refused the handshake");
         }

         return 1;
      }
   }
}
