package io.github.rinicesiberia.shadowbattle.showdown

import com.mojang.brigadier.arguments.StringArgumentType
import dev.architectury.event.events.common.CommandRegistrationEvent
import dev.architectury.event.events.common.PlayerEvent
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import java.net.URI
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import xiaocaoawa.minecraft.mod.cobblebattle.CobbleBattle

/** 负责命令注册、玩家上线自动登录和下线断开 PS 会话。 */
object ShowdownPlayerSessions {
    private val accounts = ShowdownAccountStore()
    private val sessions = ConcurrentHashMap<UUID, PlayerSession>()
    private val pendingRegistrations = ConcurrentHashMap<UUID, PendingRegistration>()

    @JvmStatic
    fun register() {
        CommandRegistrationEvent.EVENT.register { dispatcher, _, _ ->
            dispatcher.register(
                Commands.literal("pokemonshowdown")
                    .then(Commands.literal("register")
                        .then(passwordArguments(false)))
                    .then(Commands.literal("register-id")
                        .then(Commands.argument("id", StringArgumentType.word()).then(passwordArguments(true))))
                    .then(Commands.literal("login")
                        .then(Commands.argument("username", StringArgumentType.word()).then(passwordArgumentsForLogin())))
                    .then(Commands.literal("captcha")
                        .then(Commands.argument("answer", StringArgumentType.greedyString()).executes { context ->
                            completeRegistration(context.source, StringArgumentType.getString(context, "answer"))
                        }))
                    .then(Commands.literal("logout").executes { context -> logout(context.source) })
                    .then(Commands.literal("status").executes { context -> status(context.source) })
            )
        }
        PlayerEvent.PLAYER_JOIN.register { player -> autoLogin(player) }
        PlayerEvent.PLAYER_QUIT.register { player -> logout(player.uuid) }
    }

    private fun passwordArguments(withId: Boolean) = Commands.argument("password", StringArgumentType.word())
        .then(Commands.argument("confirm", StringArgumentType.word()).executes { context ->
            val player = context.source.getPlayerOrException()
            val username = if (withId) StringArgumentType.getString(context, "id") else player.gameProfile.name
            register(context.source, player, username, StringArgumentType.getString(context, "password"), StringArgumentType.getString(context, "confirm"))
        })

    private fun passwordArgumentsForLogin() = Commands.argument("password", StringArgumentType.word())
        .then(Commands.argument("confirm", StringArgumentType.word()).executes { context ->
            login(context.source, context.source.getPlayerOrException(), StringArgumentType.getString(context, "username"), StringArgumentType.getString(context, "password"), StringArgumentType.getString(context, "confirm"))
        })

    private fun register(source: net.minecraft.commands.CommandSourceStack, player: ServerPlayer, username: String, password: String, confirm: String): Int {
        if (password != confirm) return failure(source, "两次输入的注册密码不一致")
        if (!username.any(Char::isLetter)) return failure(source, "PS 用户名至少需要包含一个英文字母")
        if (username.length > 18) return failure(source, "PS 用户名不能超过 18 个字符")
        val session = replaceSession(player.uuid)
        pendingRegistrations[player.uuid] = PendingRegistration(session, username, password)
        source.sendSuccess({ Component.literal("正在连接 PS 并注册 $username……") }, false)
        session.prepareRegistration(username, password).whenComplete { _, error -> source.server.execute {
            if (error != null) {
                pendingRegistrations.remove(player.uuid)
                source.sendFailure(Component.literal("PS 注册准备失败: ${root(error)}"))
            } else {
                source.sendSystemMessage(Component.literal("PS 注册验证码图片: https://play.pokemonshowdown.com/sprites/gen5ani/pikachu.gif"))
                source.sendSystemMessage(Component.literal("请执行 /pokemonshowdown captcha <答案> 提交验证码。当前图片中的宝可梦名称需要按英文回答。"))
            }
        } }
        return 1
    }

    private fun completeRegistration(source: net.minecraft.commands.CommandSourceStack, answer: String): Int {
        val player = source.getPlayerOrException()
        val pending = pendingRegistrations[player.uuid] ?: return failure(source, "当前没有等待验证码的 PS 注册")
        if (answer.trim().isEmpty()) return failure(source, "验证码答案不能为空")
        pending.session.completeRegistration(answer.trim()).whenComplete { account, error -> source.server.execute {
            pendingRegistrations.remove(player.uuid)
            if (error != null) source.sendFailure(Component.literal("PS 注册失败: ${root(error)}"))
            else {
                accounts.put(player.uuid, ShowdownAccountStore.StoredAccount(account, pending.password))
                source.sendSuccess({ Component.literal("PS 账号注册并登录成功: $account") }, false)
            }
        } }
        return 1
    }

    private fun login(source: net.minecraft.commands.CommandSourceStack, player: ServerPlayer, username: String, password: String, confirm: String): Int {
        if (password != confirm) return failure(source, "两次输入的登录密码不一致")
        val session = replaceSession(player.uuid)
        source.sendSuccess({ Component.literal("正在连接 PS 并登录 $username……") }, false)
        session.login(username, password).whenComplete { account, error -> source.server.execute {
            if (error != null) source.sendFailure(Component.literal("PS 登录失败: ${root(error)}"))
            else {
                accounts.put(player.uuid, ShowdownAccountStore.StoredAccount(account, password))
                source.sendSuccess({ Component.literal("PS 登录成功: $account") }, false)
            }
        } }
        return 1
    }

    private fun autoLogin(player: ServerPlayer) {
        val account = accounts.get(player.uuid) ?: return
        val session = replaceSession(player.uuid)
        session.login(account.username, account.password).whenComplete { _, error ->
            if (error != null) player.server.execute { player.sendSystemMessage(Component.literal("PS 自动登录失败: ${root(error)}")) }
        }
    }

    private fun logout(source: net.minecraft.commands.CommandSourceStack): Int {
        val player = source.getPlayerOrException()
        logout(player.uuid)
        source.sendSuccess({ Component.literal("已断开 PS 会话") }, false)
        return 1
    }

    private fun logout(player: UUID) { pendingRegistrations.remove(player); sessions.remove(player)?.close() }

    private fun status(source: net.minecraft.commands.CommandSourceStack): Int {
        val player = source.getPlayerOrException()
        val session = sessions[player.uuid]
        source.sendSuccess({ Component.literal(session?.status ?: "没有 PS 会话") }, false)
        return 1
    }

    private fun replaceSession(player: UUID): PlayerSession {
        sessions.remove(player)?.close()
        return PlayerSession(URI.create(CobbleBattle.config().showdownWebSocket)).also { sessions[player] = it }
    }

    private fun failure(source: net.minecraft.commands.CommandSourceStack, message: String): Int {
        source.sendFailure(Component.literal(message))
        return 0
    }

    private fun root(error: Throwable): String {
        var current = error
        while (true) current = current.cause ?: break
        return current.message ?: current.javaClass.simpleName
    }

    private class PlayerSession(private val endpoint: URI) : AutoCloseable {
        @Volatile var status: String = "未连接"
            private set
        private val http = ShowdownLoginHttp()
        private var client: OfficialShowdownClient? = null
        private var challstr: String? = null
        private var pendingUsername: String? = null
        private var pendingPassword: String? = null
        private var pendingResult: CompletableFuture<String>? = null

        fun login(username: String, password: String): CompletableFuture<String> = authenticate(username, password)

        fun prepareRegistration(username: String, password: String): CompletableFuture<Unit> {
            pendingUsername = username
            pendingPassword = password
            return connectAndAwaitChallenge().thenApply { Unit }
        }

        fun completeRegistration(captcha: String): CompletableFuture<String> {
            val username = pendingUsername ?: return CompletableFuture.failedFuture(IllegalStateException("PS 注册会话已过期"))
            val password = pendingPassword ?: return CompletableFuture.failedFuture(IllegalStateException("PS 注册密码已清理"))
            val challenge = challstr ?: return CompletableFuture.failedFuture(IllegalStateException("PS 尚未发送 challstr"))
            val result = CompletableFuture<String>()
            pendingResult = result
            runCatching {
                val response = http.register(username, password.toCharArray(), challenge, captcha)
                response.error?.let { error -> error(error) }
                val assertion = response.assertion ?: error("PS 未返回注册 assertion")
                if (assertion == ";" || assertion.startsWith(";;")) error(assertion.removePrefix(";;").ifBlank { "PS 拒绝了注册" })
                client?.send("|/trn $username,0,$assertion")
                status = "等待 PS 确认注册"
            }.onFailure { error -> status = "失败"; result.completeExceptionally(error) }
            result.whenComplete { _, _ -> pendingPassword = null }
            return result
        }

        private fun authenticate(username: String, password: String): CompletableFuture<String> {
            val result = CompletableFuture<String>()
            pendingUsername = username
            pendingResult = result
            status = "连接中"
            connectAndAwaitChallenge().thenAccept { challenge ->
                runCatching {
                    val response = http.login(username, password.toCharArray(), challenge)
                    response.error?.let { error -> error(error) }
                    val assertion = response.assertion ?: error("PS 未返回登录 assertion")
                    if (assertion == ";" || assertion.startsWith(";;")) error(assertion.removePrefix(";;").ifBlank { "PS 拒绝了登录" })
                    client?.send("|/trn $username,0,$assertion")
                    status = "等待 PS 确认登录"
                }.onFailure { error -> status = "失败"; result.completeExceptionally(error) }
            }.exceptionally { error -> result.completeExceptionally(error); null }
            return result
        }

        private fun connectAndAwaitChallenge(): CompletableFuture<String> {
            challstr?.let { return CompletableFuture.completedFuture(it) }
            val result = CompletableFuture<String>()
            lateinit var active: OfficialShowdownClient
            active = OfficialShowdownClient(endpoint, { frame ->
                frame.lines.firstOrNull { it.startsWith("|updateuser|") }?.let { line ->
                    val fields = line.split('|')
                    val named = fields.getOrNull(2) == "1"
                    val actualName = fields.getOrNull(1).orEmpty()
                    if (named && pendingUsername != null && ShowdownIdentifiers.same(actualName, pendingUsername.orEmpty())) {
                        status = "已登录 $actualName"
                        pendingResult?.complete(actualName)
                        pendingResult = null
                    }
                }
                frame.lines.firstOrNull { it.startsWith("|challstr|") }?.let { line ->
                    challstr = line.substringAfter("|challstr|")
                    result.complete(challstr)
                }
            }, { error -> status = "连接失败"; result.completeExceptionally(error); pendingResult?.completeExceptionally(error) })
            client = active
            active.connect().exceptionally { result.completeExceptionally(it); null }
            result.whenComplete { _, error -> if (error != null) active.close() }
            return result
        }

        override fun close() {
            status = "已断开"
            client?.send("|/logout")
            client?.close()
            client = null
            pendingPassword = null
        }
    }

    private data class PendingRegistration(val session: PlayerSession, val username: String, val password: String)
}
