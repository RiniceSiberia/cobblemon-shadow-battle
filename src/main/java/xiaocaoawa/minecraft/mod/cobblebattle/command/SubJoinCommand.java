package xiaocaoawa.minecraft.mod.cobblebattle.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;

public final class SubJoinCommand extends AbstractSubCommand {
   @Override
   public String name() {
      return "join";
   }

   @Override
   public void build(LiteralArgumentBuilder<CommandSourceStack> commandNode) {
      CommandExecutionRuntime.buildJoin(commandNode);
   }
}

