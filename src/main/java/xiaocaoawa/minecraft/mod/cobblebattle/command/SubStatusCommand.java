package xiaocaoawa.minecraft.mod.cobblebattle.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;

public final class SubStatusCommand extends AbstractSubCommand {
   @Override
   public String name() {
      return "status";
   }

   @Override
   public void build(LiteralArgumentBuilder<CommandSourceStack> node) {
      CommandExecutionRuntime.buildStatus(node);
   }
}
