package xiaocaoawa.minecraft.mod.cobblebattle.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import xiaocaoawa.minecraft.mod.cobblebattle.lang.Msg;

public final class SubCheckCommand extends AbstractSubCommand {
   @Override
   public String name() {
      return "check";
   }

   @Override
   public void build(LiteralArgumentBuilder<CommandSourceStack> node) {
      node.executes(ctx -> this.run((CommandSourceStack)ctx.getSource()));
   }

   private int run(CommandSourceStack source) {
      ServerPlayer player = requirePlayer(source, "cmd.only_players.check");
      if (player == null) {
         return 0;
      } else if (!service().dex().isReady()) {
         source.sendFailure(Msg.of(ChatFormatting.RED, "queue.dex_not_ready"));
         return 0;
      } else {
         Component result = service().describePartyCompatibility(player);
         source.sendSuccess(() -> result, false);
         return 1;
      }
   }
}
