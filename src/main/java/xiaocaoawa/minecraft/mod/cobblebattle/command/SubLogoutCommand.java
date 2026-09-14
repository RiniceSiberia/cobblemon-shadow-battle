package xiaocaoawa.minecraft.mod.cobblebattle.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import xiaocaoawa.minecraft.mod.cobblebattle.lang.Msg;

public final class SubLogoutCommand extends AbstractSubCommand {
   @Override
   public String name() {
      return "logout";
   }

   @Override
   public void build(LiteralArgumentBuilder<CommandSourceStack> node) {
      node.executes(ctx -> this.run((CommandSourceStack)ctx.getSource()));
   }

   private int run(CommandSourceStack source) {
      ServerPlayer player = requirePlayer(source, "cmd.only_players.logout");
      if (player == null) {
         return 0;
      } else if (!service().auth().isSignedIn(player.getUUID())) {
         source.sendFailure(Msg.of(ChatFormatting.YELLOW, "auth.not_signed_in"));
         return 0;
      } else {
         service().signOut(player);
         source.sendSuccess(() -> Msg.of(ChatFormatting.GREEN, "auth.logged_out"), false);
         return 1;
      }
   }
}
