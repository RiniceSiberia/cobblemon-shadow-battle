package xiaocaoawa.minecraft.mod.cobblebattle.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;

public final class SubLeaveCommand extends AbstractSubCommand {
   @Override
   public String name() {
      return "leave";
   }

   @Override
   public void build(LiteralArgumentBuilder<CommandSourceStack> commandNode) {
      CommandExecutionRuntime.buildLeave(commandNode);
   }
}

