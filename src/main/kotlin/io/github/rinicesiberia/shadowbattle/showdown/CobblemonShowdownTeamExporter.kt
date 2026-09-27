package io.github.rinicesiberia.shadowbattle.showdown

import com.cobblemon.mod.common.api.pokemon.stats.Stat
import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.pokemon.PokemonStats
import com.google.gson.JsonObject
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.level.ServerPlayer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/** 将当前 party 转换为官方 packed team，并生成完整快照与严格的一致性指纹。 */
object CobblemonShowdownTeamExporter {
    @JvmStatic
    fun export(player: ServerPlayer): ExportedShowdownTeam {
        val pokemon = Cobblemon.storage.getParty(player).toGappyList().filterNotNull()
        require(pokemon.isNotEmpty()) { "当前 party 没有可上传的宝可梦" }
        require(pokemon.size <= 6) { "当前 party 超过六只宝可梦" }
        val members = pokemon.map(::exportSet)
        val packed = PackedTeamCodec.pack(members.map(ExportedPokemon::set))
        val snapshots = pokemon.map { it.saveToJSON(player.registryAccess(), JsonObject()) }
        val identity = members.joinToString("\n") { "${it.uuid}|${PackedTeamCodec.pack(listOf(it.set))}" }
        return ExportedShowdownTeam(packed, sha256(identity), snapshots)
    }

    private fun exportSet(pokemon: Pokemon): ExportedPokemon {
        val naturalIvs = pokemon.ivs.toValues { stats, stat -> stats.getOrDefault(stat) }
        val effectiveIvs = pokemon.ivs.toValues(::effectiveIv)
        val hiddenPower = if (naturalIvs != effectiveIvs) hiddenPowerType(naturalIvs) else ""
        val heldItem = pokemon.heldItem()
        return ExportedPokemon(
            uuid = pokemon.uuid.toString(),
            set = ShowdownSet(
                nickname = pokemon.nickname?.string.orEmpty(),
                species = pokemon.form.showdownId(),
                item = if (heldItem.isEmpty) "" else BuiltInRegistries.ITEM.getKey(heldItem.item).path,
                ability = pokemon.ability.name,
                moves = pokemon.moveSet.map { it.template.name },
                nature = pokemon.effectiveNature.name.path,
                evs = pokemon.evs.toValues { stats, stat -> stats.getOrDefault(stat) }.packed(),
                gender = pokemon.gender.showdownName,
                ivs = effectiveIvs.packed(),
                shiny = pokemon.shiny,
                happiness = pokemon.friendship.toString(),
                pokeball = pokemon.caughtBall.name.path,
                hpType = hiddenPower,
                level = pokemon.level.toString(),
                gigantamax = if (pokemon.gmaxFactor) "G" else "",
                dynamaxLevel = pokemon.dmaxLevel.toString(),
                teraType = pokemon.teraType.showdownId(),
            ),
        )
    }

    private fun PokemonStats.toValues(read: (PokemonStats, Stat) -> Int) = StatValues(
        read(this, Stats.HP), read(this, Stats.ATTACK), read(this, Stats.DEFENCE),
        read(this, Stats.SPECIAL_ATTACK), read(this, Stats.SPECIAL_DEFENCE), read(this, Stats.SPEED),
    )

    private fun effectiveIv(stats: PokemonStats, stat: Stat): Int {
        val method = stats.javaClass.methods.firstOrNull { it.name == "getEffectiveBattleIV" && it.parameterCount == 1 }
        return (runCatching { method?.invoke(stats, stat) as? Number }.getOrNull()?.toInt() ?: stats.getOrDefault(stat)).coerceIn(0, 31)
    }

    private fun hiddenPowerType(ivs: StatValues): String {
        val bits = (ivs.hp and 1) + 2 * (ivs.atk and 1) + 4 * (ivs.def and 1) +
            8 * (ivs.spe and 1) + 16 * (ivs.spa and 1) + 32 * (ivs.spd and 1)
        return HIDDEN_POWER_TYPES[bits * 15 / 63]
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8)).joinToString("") { "%02x".format(it) }

    private val HIDDEN_POWER_TYPES = listOf(
        "Fighting", "Flying", "Poison", "Ground", "Rock", "Bug", "Ghost", "Steel",
        "Fire", "Water", "Grass", "Electric", "Psychic", "Ice", "Dragon", "Dark",
    )
}

data class ExportedShowdownTeam(val packed: String, val fingerprint: String, val pokemonSnapshots: List<JsonObject>)
private data class ExportedPokemon(val uuid: String, val set: ShowdownSet)
private data class StatValues(val hp: Int, val atk: Int, val def: Int, val spa: Int, val spd: Int, val spe: Int) {
    fun packed(): String = listOf(hp, atk, def, spa, spd, spe).joinToString(",")
}
