package io.github.rinicesiberia.shadowbattle.showdown

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.battles.BattleRegistry
import com.cobblemon.mod.common.pokemon.Pokemon
import com.google.gson.JsonObject
import net.minecraft.server.level.ServerPlayer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/** 将 Cobblemon 自己的扩展 packed team 适配为官方 PS packed team。 */
object CobblemonShowdownTeamExporter {
    private const val CUSTOM_FIELD_COUNT = 17

    @JvmStatic
    fun export(player: ServerPlayer): ExportedShowdownTeam {
        val originalPokemon = Cobblemon.storage.getParty(player).toGappyList().filterNotNull()
        require(originalPokemon.isNotEmpty()) { "当前 party 没有可上传的宝可梦" }
        require(originalPokemon.size <= 6) { "当前 party 超过六只宝可梦" }

        // BattlePokemon.safeCopyOf 会创建新的 UUID 和独立的 Pokemon。所有满血、清状态、
        // PP 标准化都只作用在这个副本，避免把战斗规则写回世界里的原始 party。
        val roster = Cobblemon.storage.getParty(player).toBattleTeam(true, true, null)
        ShowdownBattleStatePolicy.normalize(roster)
        val pokemon: List<Pokemon> = roster.map { it.effectedPokemon }
        require(pokemon.size == originalPokemon.size) { "Cobblemon party 与战斗副本数量不一致" }

        val customPacked = with(BattleRegistry) { roster.packTeam() }
        val officialMembers = customPacked.split(']').filter(String::isNotEmpty).mapIndexed { index, row ->
            officializeRow(row, pokemon.getOrNull(index) ?: error("Cobblemon packed team 与 party 数量不一致"))
        }
        require(officialMembers.size == pokemon.size) { "Cobblemon packed team 与 party 数量不一致" }
        val packed = officialMembers.joinToString("]")
        val identity = originalPokemon.zip(officialMembers).joinToString("\n") { (pk, member) -> "${pk.uuid}|$member" }
        val fingerprint = MessageDigest.getInstance("SHA-256")
            .digest(identity.toByteArray(StandardCharsets.UTF_8)).joinToString("") { "%02x".format(it) }
        val snapshots = originalPokemon.map { it.saveToJSON(player.registryAccess(), JsonObject()) }
        return ExportedShowdownTeam(packed, fingerprint, snapshots)
    }

    private fun officializeRow(row: String, pokemon: Pokemon): String {
        val fields = row.split('|')
        require(fields.size >= CUSTOM_FIELD_COUNT) { "Cobblemon packed team 字段数量不足" }
        val species = pokemon.showdownId()
        val nickname = sanitizeName(pokemon.nickname?.string.orEmpty().ifBlank { species })
        val speciesField = if (ShowdownIdentifiers.speciesId(nickname) == ShowdownIdentifiers.speciesId(species)) "" else sanitizeName(species)
        return listOf(
            nickname,
            speciesField,
            fields[6],
            fields[7],
            fields[8],
            fields[10],
            fields[11],
            fields[12],
            fields[13],
            fields[14],
            fields[15],
            fields[16],
        ).joinToString("|")
    }

    private fun sanitizeName(value: String): String = value.replace("|", "").replace("]", "").replace("\n", "").replace("\r", "")
}

data class ExportedShowdownTeam(val packed: String, val fingerprint: String, val pokemonSnapshots: List<JsonObject>)
