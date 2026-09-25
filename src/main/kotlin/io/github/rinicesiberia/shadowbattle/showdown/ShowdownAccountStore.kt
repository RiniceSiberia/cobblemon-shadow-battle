package io.github.rinicesiberia.shadowbattle.showdown

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

/** 保存 Minecraft 玩家对应的 PS 凭据；密码只用于自动登录，不会写入日志。 */
class ShowdownAccountStore(private val location: Path = Path.of("config", "cobblebattle-showdown-accounts.json")) {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val accounts = linkedMapOf<String, StoredAccount>()

    init { load() }

    @Synchronized fun get(player: UUID): StoredAccount? = accounts[player.toString()]

    @Synchronized fun put(player: UUID, account: StoredAccount) {
        require(account.username.isNotBlank()) { "PS 用户名不能为空" }
        require(account.password.isNotEmpty()) { "PS 密码不能为空" }
        accounts[player.toString()] = account
        save()
    }

    @Synchronized fun remove(player: UUID): Boolean {
        val removed = accounts.remove(player.toString()) != null
        if (removed) save()
        return removed
    }

    private fun load() {
        if (!Files.exists(location)) return
        runCatching {
            val type = object : TypeToken<Map<String, StoredAccount>>() {}.type
            accounts.putAll(gson.fromJson<Map<String, StoredAccount>>(Files.readString(location), type) ?: emptyMap())
        }
    }

    private fun save() {
        runCatching {
            Files.createDirectories(location.parent)
            Files.writeString(location, gson.toJson(accounts))
        }.getOrElse { throw IllegalStateException("无法保存 PS 账号文件 $location", it) }
    }

    data class StoredAccount(val username: String, val password: String)
}
