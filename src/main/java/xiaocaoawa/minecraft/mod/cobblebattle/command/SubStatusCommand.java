package xiaocaoawa.minecraft.mod.cobblebattle.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import xiaocaoawa.minecraft.mod.cobblebattle.battle.CrossServerBattleService;
import xiaocaoawa.minecraft.mod.cobblebattle.dex.RemoteDex;
import xiaocaoawa.minecraft.mod.cobblebattle.lang.Msg;

public final class SubStatusCommand extends AbstractSubCommand {
   @Override
   public String name() {
      return "status";
   }

   @Override
   public void build(LiteralArgumentBuilder<CommandSourceStack> node) {
      node.executes(ctx -> this.run((CommandSourceStack)ctx.getSource()));
   }

   private int run(CommandSourceStack source) {
      CrossServerBattleService svc = service();
      RemoteDex dex = svc.dex();
      source.sendSuccess(() -> Msg.of(ChatFormatting.AQUA, "cmd.status.title"), false);
      boolean connected = svc.isConnected();
      String refused = svc.connectionRefusal();
      String state = connected
         ? Msg.raw("cmd.status.connected")
         : (refused != null ? Msg.raw("cmd.status.refused", refused) : Msg.raw("cmd.status.disconnected"));
      source.sendSuccess(
         () -> Msg.of(
            connected ? ChatFormatting.GREEN : ChatFormatting.RED, "cmd.status.battle_server", svc.config().serverHost, svc.config().serverPort, state
         ),
         false
      );
      ServerPlayer player = source.getPlayer();
      if (player != null) {
         String account = svc.auth().accountOf(player.getUUID());
         String nickname = svc.auth().nicknameOf(player.getUUID());
         long uid = svc.auth().uidOf(player.getUUID());
         source.sendSuccess(
            () -> account == null
               ? Msg.of(ChatFormatting.YELLOW, "cmd.status.account_none")
               : Msg.of(ChatFormatting.GREEN, "cmd.status.account", account, nickname, uid),
            false
         );
      }

      String dexText = dex.isReady()
         ? Msg.raw("cmd.status.dex_ready", dex.size(), dex.digest() == null ? "?" : dex.digest().substring(0, 12))
         : Msg.raw("cmd.status.dex_not_synced");
      source.sendSuccess(() -> Msg.of(dex.isReady() ? ChatFormatting.GREEN : ChatFormatting.YELLOW, "cmd.status.dex", dexText), false);
      return 1;
   }
}
