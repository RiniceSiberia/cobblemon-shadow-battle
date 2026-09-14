package xiaocaoawa.minecraft.mod.cobblebattle.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import xiaocaoawa.minecraft.mod.cobblebattle.battle.CrossServerBattleService;
import xiaocaoawa.minecraft.mod.cobblebattle.lang.Msg;

public final class SubOpenCommand extends AbstractSubCommand {
   private static final SuggestionProvider<CommandSourceStack> RANKED = (ctx, builder) -> SharedSuggestionProvider.suggest(
      service().ranked().stream().map(CrossServerBattleService.Ranked::id).toList(), builder
   );

   @Override
   public String name() {
      return "open";
   }

   @Override
   public void build(LiteralArgumentBuilder<CommandSourceStack> node) {
      ((LiteralArgumentBuilder)node.executes(ctx -> this.run((CommandSourceStack)ctx.getSource(), null)))
         .then(
            Commands.argument("ranked", StringArgumentType.word())
               .suggests(RANKED)
               .executes(ctx -> this.run((CommandSourceStack)ctx.getSource(), StringArgumentType.getString(ctx, "ranked")))
         );
   }

   private int run(CommandSourceStack source, String ranked) {
      ServerPlayer player = requirePlayer(source, "cmd.only_players.open");
      if (player == null) {
         return 0;
      } else {
         Component refusal = !service().auth().isSignedIn(player.getUUID())
            ? service().openAuthScreen(player)
            : (ranked == null ? service().openMainMenu(player) : openLeaderboard(player, ranked));
         if (refusal != null) {
            source.sendFailure(refusal);
            return 0;
         } else {
            return 1;
         }
      }
   }

   private static Component openLeaderboard(ServerPlayer player, String ranked) {
      String chosen = ranked != null ? ranked : service().config().defaultRanked;
      return (Component)(!service().ranked().isEmpty() && !service().hasRanked(chosen)
         ? Msg.of(
            ChatFormatting.RED,
            "cmd.join.unknown_ranked",
            chosen,
            String.join(", ", service().ranked().stream().map(CrossServerBattleService.Ranked::id).toList())
         )
         : service().requestLeaderboard(player, chosen));
   }
}
