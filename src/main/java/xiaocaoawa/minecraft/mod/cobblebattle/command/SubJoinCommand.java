package xiaocaoawa.minecraft.mod.cobblebattle.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import xiaocaoawa.minecraft.mod.cobblebattle.battle.CrossServerBattleService;
import xiaocaoawa.minecraft.mod.cobblebattle.lang.Msg;

public final class SubJoinCommand extends AbstractSubCommand {
   private static final SuggestionProvider<CommandSourceStack> RANKED = (ctx, builder) -> SharedSuggestionProvider.suggest(
      service().ranked().stream().map(CrossServerBattleService.Ranked::id).toList(), builder
   );

   @Override
   public String name() {
      return "join";
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
      ServerPlayer participant = requirePlayer(source, "cmd.only_players.join");
      if (participant == null) {
         return 0;
      } else {
         String chosen = ranked != null ? ranked : service().config().defaultRanked;
         if (!service().ranked().isEmpty() && !service().hasRanked(chosen)) {
            source.sendFailure(Msg.of(ChatFormatting.RED, "cmd.join.unknown_ranked", chosen, offered()));
            return 0;
         } else {
            Component refusal = service().queue(participant, chosen);
            if (refusal != null) {
               source.sendFailure(refusal);
               return 0;
            } else {
               return 1;
            }
         }
      }
   }

   private static String offered() {
      List<String> ids = service().ranked().stream().map(CrossServerBattleService.Ranked::id).toList();
      return String.join(", ", ids);
   }
}
