package xiaocaoawa.minecraft.mod.cobblebattle.command;

import com.mojang.brigadier.CommandDispatcher;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;

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
      CommandExecutionRuntime.register(dispatcher, SUBCOMMANDS);
   }
}
