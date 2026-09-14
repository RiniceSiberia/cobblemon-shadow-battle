package xiaocaoawa.minecraft.mod.cobblebattle.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public final class MainCommand {
   private static final List<SubCommand> SUBCOMMANDS = List.of(
      new SubOpenCommand(),
      new SubLogoutCommand(),
      new SubJoinCommand(),
      new SubLeaveCommand(),
      new SubCheckCommand(),
      new SubStatusCommand(),
      new SubReloadCommand()
   );

   private MainCommand() {
   }

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("cbattle");

      for (SubCommand sub : SUBCOMMANDS) {
         LiteralArgumentBuilder<CommandSourceStack> node = Commands.literal(sub.name());
         int level = sub.permissionLevel();
         if (level > 0) {
            node.requires(source -> source.hasPermission(level));
         }

         sub.build(node);
         root.then(node);
      }

      dispatcher.register(root);
   }
}
