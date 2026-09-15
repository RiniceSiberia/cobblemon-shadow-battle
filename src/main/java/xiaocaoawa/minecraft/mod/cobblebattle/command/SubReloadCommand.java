package xiaocaoawa.minecraft.mod.cobblebattle.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;

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
      CommandExecutionRuntime.buildReload(node);
   }
}
