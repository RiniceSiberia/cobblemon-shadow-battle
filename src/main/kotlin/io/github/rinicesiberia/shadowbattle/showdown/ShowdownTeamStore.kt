package io.github.rinicesiberia.shadowbattle.showdown

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

/** 保存已通过官方 validator 的队伍。写入采用临时文件替换，避免中途损坏整个目录。 */
class ShowdownTeamStore(private val location: Path = Path.of("config", "cobblebattle-showdown-teams.json")) {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val teams = linkedMapOf<String, MutableList<StoredTeam>>()
    private val ids = AtomicInteger(1)

    init { load() }

    @Synchronized fun list(player: UUID): List<StoredTeam> = teams[player.toString()].orEmpty().toList()
    @Synchronized fun find(player: UUID, key: String): StoredTeam? = list(player).firstOrNull { it.name == key || it.id == key }

    @Synchronized fun upsert(player: UUID, name: String, format: String, packed: String): StoredTeam {
        require(name.isNotBlank()) { "队伍名称不能为空" }
        val current = teams.getOrPut(player.toString()) { mutableListOf() }
        val old = current.indexOfFirst { it.name == name }
        val team = StoredTeam(if (old >= 0) current[old].id else "team-${ids.getAndIncrement()}", name, format, packed, System.currentTimeMillis())
        if (old >= 0) current[old] = team else current += team
        save()
        return team
    }

    @Synchronized fun remove(player: UUID, key: String): Boolean {
        val current = teams[player.toString()] ?: return false
        val removed = current.removeIf { it.name == key || it.id == key }
        if (removed) save()
        return removed
    }

    private fun load() {
        if (!Files.exists(location)) return
        runCatching {
            val type = object : TypeToken<Map<String, List<StoredTeam>>>() {}.type
            gson.fromJson<Map<String, List<StoredTeam>>>(Files.readString(location), type)?.forEach { (key, value) -> teams[key] = value.toMutableList() }
        }
    }

    private fun save() {
        Files.createDirectories(location.parent)
        val temporary = location.resolveSibling(location.fileName.toString() + ".tmp")
        Files.writeString(temporary, gson.toJson(teams))
        Files.move(temporary, location, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE)
    }

    data class StoredTeam(val id: String, val name: String, val format: String, val packed: String, val validatedAt: Long)
}
