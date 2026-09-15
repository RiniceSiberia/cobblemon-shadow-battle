package xiaocaoawa.minecraft.mod.cobblebattle.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;

public final class SubLogoutCommand extends AbstractSubCommand {
   @Override
   public String name() {
      return "logout";
   }

   @Override
   public void build(LiteralArgumentBuilder<CommandSourceStack> node) {
      CommandExecutionRuntime.buildLogout(node);
   }
}
