package xiaocaoawa.minecraft.mod.cobblebattle.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;

public final class SubOpenCommand extends AbstractSubCommand {
   @Override
   public String name() {
      return "open";
   }

   @Override
   public void build(LiteralArgumentBuilder<CommandSourceStack> commandNode) {
      CommandExecutionRuntime.buildOpen(commandNode);
   }
}

