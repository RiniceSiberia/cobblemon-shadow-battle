package xiaocaoawa.minecraft.mod.cobblebattle.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import io.github.rinicesiberia.shadowbattle.command.CommandDecisions
import io.github.rinicesiberia.shadowbattle.command.ReloadFollowUp
import net.minecraft.ChatFormatting
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import xiaocaoawa.minecraft.mod.cobblebattle.CobbleBattle
import xiaocaoawa.minecraft.mod.cobblebattle.battle.CrossServerBattleService
import xiaocaoawa.minecraft.mod.cobblebattle.lang.Msg

/** 构建命令树并执行各子命令的服务调用。 */
internal object CommandExecutionRuntime {
    @JvmStatic
    fun register(dispatcher: CommandDispatcher<CommandSourceStack>, subcommands: List<SubCommand>) {
        val root = Commands.literal("cbattle")
        for (subcommand in subcommands) {
            val node = Commands.literal(subcommand.name())
            val permissionLevel = subcommand.permissionLevel()
            if (permissionLevel > 0) node.requires { source -> source.hasPermission(permissionLevel) }
            subcommand.build(node)
            root.then(node)
        }
        dispatcher.register(root)
    }

    @JvmStatic
    fun buildOpen(node: LiteralArgumentBuilder<CommandSourceStack>) {
        node.executes { context -> executeOpen(context.source, null) }
        node.then(
            Commands.argument<String>("ranked", StringArgumentType.word())
                .suggests { _, builder -> rankedSuggestions(builder) }
                .executes { context -> executeOpen(context.source, StringArgumentType.getString(context, "ranked")) },
        )
    }

    @JvmStatic
    fun buildLogout(node: LiteralArgumentBuilder<CommandSourceStack>) {
        node.executes { context -> executeLogout(context.source) }
    }

    @JvmStatic
    fun buildJoin(node: LiteralArgumentBuilder<CommandSourceStack>) {
        node.executes { context -> executeJoin(context.source, null) }
        node.then(
            Commands.argument<String>("ranked", StringArgumentType.word())
                .suggests { _, builder -> rankedSuggestions(builder) }
                .executes { context -> executeJoin(context.source, StringArgumentType.getString(context, "ranked")) },
        )
    }

    @JvmStatic
    fun buildLeave(node: LiteralArgumentBuilder<CommandSourceStack>) {
        node.executes { context -> executeLeave(context.source) }
    }

    @JvmStatic
    fun buildCheck(node: LiteralArgumentBuilder<CommandSourceStack>) {
        node.executes { context -> executeCheck(context.source) }
    }

    @JvmStatic
    fun buildStatus(node: LiteralArgumentBuilder<CommandSourceStack>) {
        node.executes { context -> executeStatus(context.source) }
    }

    @JvmStatic
    fun buildReload(node: LiteralArgumentBuilder<CommandSourceStack>) {
        node.executes { context -> executeReload(context.source) }
    }

    private fun executeOpen(source: CommandSourceStack, requestedRankedId: String?): Int {
        val participant = requirePlayer(source, "cmd.only_players.open") ?: return 0
        val refusal = if (!service().auth().isSignedIn(participant.uuid)) {
            service().openAuthScreen(participant)
        } else if (requestedRankedId == null) {
            service().openMainMenu(participant)
        } else {
            openLeaderboard(participant, requestedRankedId)
        }
        return reportFailure(source, refusal)
    }

    private fun executeLogout(source: CommandSourceStack): Int {
        val participant = requirePlayer(source, "cmd.only_players.logout") ?: return 0
        if (!service().auth().isSignedIn(participant.uuid)) {
            source.sendFailure(Msg.of(ChatFormatting.YELLOW, "auth.not_signed_in"))
            return 0
        }
        service().signOut(participant)
        source.sendSuccess({ Msg.of(ChatFormatting.GREEN, "auth.logged_out") }, false)
        return 1
    }

    private fun executeJoin(source: CommandSourceStack, requestedRankedId: String?): Int {
        val participant = requirePlayer(source, "cmd.only_players.join") ?: return 0
        val selection = rankedSelection(requestedRankedId)
        if (!selection.accepted) {
            source.sendFailure(
                Msg.of(ChatFormatting.RED, "cmd.join.unknown_ranked", selection.chosenId, selection.offeredIds),
            )
            return 0
        }
        return reportFailure(source, service().queue(participant, selection.chosenId))
    }

    private fun executeLeave(source: CommandSourceStack): Int {
        val participant = requirePlayer(source, "cmd.only_players.leave") ?: return 0
        return reportFailure(source, service().leaveQueue(participant))
    }

    private fun executeCheck(source: CommandSourceStack): Int {
        val participant = requirePlayer(source, "cmd.only_players.check") ?: return 0
        if (!service().dex().isReady) {
            source.sendFailure(Msg.of(ChatFormatting.RED, "queue.dex_not_ready"))
            return 0
        }
        val result = service().describePartyCompatibility(participant)
        source.sendSuccess({ result }, false)
        return 1
    }

    private fun executeStatus(source: CommandSourceStack): Int {
        val battleService = service()
        val dex = battleService.dex()
        source.sendSuccess({ Msg.of(ChatFormatting.AQUA, "cmd.status.title") }, false)
        val connected = battleService.isConnected
        val refusal = battleService.connectionRefusal()
        val connectionState = when {
            connected -> Msg.raw("cmd.status.connected")
            refusal != null -> Msg.raw("cmd.status.refused", refusal)
            else -> Msg.raw("cmd.status.disconnected")
        }
        source.sendSuccess(
            {
                Msg.of(
                    if (connected) ChatFormatting.GREEN else ChatFormatting.RED,
                    "cmd.status.battle_server",
                    battleService.config().serverHost,
                    battleService.config().serverPort,
                    connectionState,
                )
            },
            false,
        )
        source.player?.let { participant ->
            val accountId = battleService.auth().accountOf(participant.uuid)
            val displayName = battleService.auth().nicknameOf(participant.uuid)
            val accountNumber = battleService.auth().uidOf(participant.uuid)
            source.sendSuccess(
                {
                    if (accountId == null) {
                        Msg.of(ChatFormatting.YELLOW, "cmd.status.account_none")
                    } else {
                        Msg.of(ChatFormatting.GREEN, "cmd.status.account", accountId, displayName, accountNumber)
                    }
                },
                false,
            )
        }
        val dexText = if (dex.isReady) {
            Msg.raw("cmd.status.dex_ready", dex.size(), dex.digest()?.substring(0, 12) ?: "?")
        } else {
            Msg.raw("cmd.status.dex_not_synced")
        }
        source.sendSuccess(
            { Msg.of(if (dex.isReady) ChatFormatting.GREEN else ChatFormatting.YELLOW, "cmd.status.dex", dexText) },
            false,
        )
        return 1
    }

    private fun executeReload(source: CommandSourceStack): Int {
        val configuration = service().config()
        val result = try {
            configuration.reload()
        } catch (failure: RuntimeException) {
            source.sendFailure(Msg.of(ChatFormatting.RED, "cmd.reload.failed", failure.message))
            return 0
        }
        Msg.init(configuration.language)
        source.sendSuccess({ Msg.of(ChatFormatting.AQUA, "cmd.reload.done") }, true)
        if (result.nothingChanged()) {
            source.sendSuccess({ Msg.of(ChatFormatting.GRAY, "cmd.reload.unchanged") }, false)
            return 1
        }
        if (result.live().isNotEmpty()) {
            source.sendSuccess(
                { Msg.of(ChatFormatting.GREEN, "cmd.reload.live", result.live().joinToString(", ")) },
                false,
            )
        }
        when (CommandDecisions.reloadFollowUp(result.needsReconnect(), service().connectionRefusal())) {
            ReloadFollowUp.RECONNECT -> {
                val changedFields = result.needsReconnect().joinToString(", ")
                source.sendSuccess(
                    { Msg.of(ChatFormatting.YELLOW, "cmd.reload.reconnecting", changedFields) },
                    true,
                )
                service().reconnect("config reloaded: $changedFields")
            }
            ReloadFollowUp.RETRY -> {
                val refusal = service().connectionRefusal()
                source.sendSuccess(
                    { Msg.of(ChatFormatting.YELLOW, "cmd.reload.retrying", refusal) },
                    true,
                )
                service().reconnect("retrying after the battle server refused the handshake")
            }
            ReloadFollowUp.NONE -> Unit
        }
        return 1
    }

    private fun openLeaderboard(participant: ServerPlayer, requestedRankedId: String?): Component? {
        val selection = rankedSelection(requestedRankedId)
        return if (!selection.accepted) {
            Msg.of(ChatFormatting.RED, "cmd.join.unknown_ranked", selection.chosenId, selection.offeredIds)
        } else {
            service().requestLeaderboard(participant, selection.chosenId)
        }
    }

    private fun rankedSelection(requestedRankedId: String?) = CommandDecisions.rankedSelection(
        requestedId = requestedRankedId,
        defaultId = service().config().defaultRanked,
        availableIds = service().ranked().map(CrossServerBattleService.Ranked::id),
    )

    private fun rankedSuggestions(builder: com.mojang.brigadier.suggestion.SuggestionsBuilder) =
        SharedSuggestionProvider.suggest(service().ranked().map(CrossServerBattleService.Ranked::id), builder)

    private fun requirePlayer(source: CommandSourceStack, messageKey: String): ServerPlayer? {
        val participant = source.player
        if (participant == null) source.sendFailure(Msg.of(messageKey))
        return participant
    }

    private fun reportFailure(source: CommandSourceStack, failure: Component?): Int {
        if (failure == null) return 1
        source.sendFailure(failure)
        return 0
    }

    private fun service(): CrossServerBattleService = CobbleBattle.service()
}
