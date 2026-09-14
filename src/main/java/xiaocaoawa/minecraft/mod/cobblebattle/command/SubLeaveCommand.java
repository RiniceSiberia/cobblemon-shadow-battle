package xiaocaoawa.minecraft.mod.cobblebattle.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class SubLeaveCommand extends AbstractSubCommand {
   @Override
   public String name() {
      return "leave";
   }

   @Override
   public void build(LiteralArgumentBuilder<CommandSourceStack> node) {
      node.executes(ctx -> this.run((CommandSourceStack)ctx.getSource()));
   }

   private int run(CommandSourceStack source) {
      ServerPlayer participant = requirePlayer(source, "cmd.only_players.leave");
      if (participant == null) {
         return 0;
      } else {
         Component refusal = service().leaveQueue(participant);
         if (refusal != null) {
            source.sendFailure(refusal);
            return 0;
         } else {
            return 1;
         }
      }
   }
}
