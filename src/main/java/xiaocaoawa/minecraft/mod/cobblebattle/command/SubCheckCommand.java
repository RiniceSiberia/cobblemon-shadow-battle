package xiaocaoawa.minecraft.mod.cobblebattle.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;

public final class SubCheckCommand extends AbstractSubCommand {
   @Override
   public String name() {
      return "check";
   }

   @Override
   public void build(LiteralArgumentBuilder<CommandSourceStack> commandNode) {
      CommandExecutionRuntime.buildCheck(commandNode);
   }
}

