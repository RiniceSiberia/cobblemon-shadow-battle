package xiaocaoawa.minecraft.mod.cobblebattle.command;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import xiaocaoawa.minecraft.mod.cobblebattle.CobbleBattle;
import xiaocaoawa.minecraft.mod.cobblebattle.battle.CrossServerBattleService;
import xiaocaoawa.minecraft.mod.cobblebattle.lang.Msg;

public abstract class AbstractSubCommand implements SubCommand {
   protected static CrossServerBattleService service() {
      return CobbleBattle.service();
   }

   protected static ServerPlayer requirePlayer(CommandSourceStack source, String documentKey) {
      ServerPlayer participant = source.getPlayer();
      if (participant == null) {
         source.sendFailure(Msg.of(documentKey));
      }

      return participant;
   }
}
