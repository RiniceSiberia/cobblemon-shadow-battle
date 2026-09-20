package xiaocaoawa.minecraft.mod.cobblebattle.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;

public interface SubCommand {
   String name();

   default int permissionLevel() {
      return 0;
   }

   void build(LiteralArgumentBuilder<CommandSourceStack> commandNode);
}

