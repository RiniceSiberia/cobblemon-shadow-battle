package io.github.rinicesiberia.shadowbattle.battle

import com.cobblemon.mod.common.api.Priority
import com.cobblemon.mod.common.api.abilities.Abilities
import com.cobblemon.mod.common.api.pokemon.Natures
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies
import com.cobblemon.mod.common.api.types.tera.TeraType
import com.cobblemon.mod.common.api.types.tera.TeraTypes
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon
import com.cobblemon.mod.common.pokemon.FormData
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.pokemon.Species
import com.cobblemon.mod.common.pokemon.properties.BattleCloneProperty
import com.cobblemon.mod.common.pokemon.properties.UncatchableProperty
import org.slf4j.LoggerFactory
import java.util.Locale

/** 从远端压缩队伍创建对战 Pokémon，并维护物种索引缓存。 */
object RemoteRosterAssembly {
    private val logger = LoggerFactory.getLogger("CobbleBattle/Team")
    @Volatile private var speciesIndex: Map<String, SpeciesLookupEntry<Species, FormData>>? = null

    @JvmStatic
    fun invalidateSpeciesCache() { speciesIndex = null }

    @JvmStatic
    fun warmSpeciesCache() { lookupSpecies() }

    private fun lookupSpecies(): Map<String, SpeciesLookupEntry<Species, FormData>> {
        speciesIndex?.let { return it }
        val built = SpeciesLookupIndex.build(PokemonSpecies.species, Species::showdownId, Species::standardForm, Species::forms, FormData::showdownId)
        speciesIndex = built
        logger.debug("Indexed {} showdown species ids", built.size)
        return built
    }

    @JvmStatic
    fun decode(packed: String): List<BattlePokemon> = PackedRosterDecoding.assemble(packed, ::decodeCreature) { creature ->
        try {
            BattleCloneProperty.isBattleClone().apply(creature)
            UncatchableProperty.uncatchable().apply(creature)
        } catch (failure: RuntimeException) {
            logger.error("Could not mark a rebuilt opposing Pokemon as a battle prop - it may be left in the world when the battle ends", failure)
        }
        BattlePokemon(creature, creature) { entity -> entity.recallWithAnimation() }
    }

    private fun decodeCreature(content: String, teamSlot: Int): Pokemon? {
        val fields = content.split("|", limit = Int.MAX_VALUE).toTypedArray()
        if (fields.size < 17) {
            logger.error("Remote team slot {} has {} fields, expected at least 17 - skipping", teamSlot, fields.size)
            return null
        }
        val template = lookupSpecies()[fields[0]]
        if (template == null) {
            logger.error("Remote team slot {} references unknown species '{}' - this server's data is out of step with the battle host", teamSlot, fields[0])
            return null
        }
        val creature = Pokemon()
        creature.species = template.species
        creature.form = template.form
        val remoteId = PackedTeamValueParsing.uuidOrNull(fields[2])
        if (remoteId != null) creature.uuid = remoteId
        else logger.warn("Remote team carried a malformed uuid '{}'", fields[2])
        creature.level = PackedTeamValueParsing.boundedInt(fields[15], 1, 1, 100)
        creature.gender = PackedTeamValueParsing.gender(fields[12])
        creature.shiny = fields[14] == "S"
        val details = PackedTeamDetailsParser.parse(fields)
        optional(teamSlot, "nature") {
            details.natureName?.let(Natures::getNature)?.let { creature.nature = it }
        }
        PackedStatAssembly.applyTo(creature, fields[13], true, teamSlot, logger)
        PackedStatAssembly.applyTo(creature, fields[11], false, teamSlot, logger)
        optional(teamSlot, "ability") {
            details.abilityName?.let(Abilities::get)?.let { creature.updateAbility(it.create(false, Priority.LOWEST)) }
        }
        PackedMoveAssembly.applyTo(creature, details, teamSlot, logger)
        details.friendship?.let { rawFriendship ->
            optional(teamSlot, "friendship") {
                creature.setFriendship(PackedTeamValueParsing.boundedInt(rawFriendship, creature.friendship, 0, 255), true)
            }
        }
        details.teraName?.let { rawTera ->
            optional(teamSlot, "tera type") { findTera(rawTera)?.let { creature.teraType = it } }
        }
        val currentHealth = PackedTeamValueParsing.boundedInt(fields[3], creature.maxHealth, 0, Int.MAX_VALUE)
        creature.currentHealth = minOf(currentHealth, creature.maxHealth)
        return creature
    }

    private fun optional(teamSlot: Int, attribute: String, apply: () -> Unit) {
        PackedRosterDecoding.applyOptional(apply) { failure ->
            logger.warn("Remote team slot {}: could not apply {} ({}) - carrying on without it", teamSlot, attribute, failure.toString())
        }
    }

    private fun findTera(rawTera: String): TeraType? {
        val value = rawTera.trim { it.code <= 0x20 }
        TeraTypes.getByName(value)?.let { return it }
        return try {
            TeraTypes.get(value.lowercase(Locale.ROOT))
        } catch (_: Exception) {
            logger.warn("Remote team carried an unusable tera type '{}'", rawTera)
            null
        }
    }
}

/** 压缩队伍条目的过滤、有效槽位编号和可选属性失败隔离。 */
internal object PackedRosterDecoding {
    fun <C : Any, B> assemble(packed: String, decode: (String, Int) -> C?, wrap: (C) -> B): List<B> {
        val roster = ArrayList<B>()
        for (content in packed.split("]")) {
            if (!content.codePoints().allMatch(Character::isWhitespace)) {
                val creature = decode(content, roster.size)
                if (creature != null) roster.add(wrap(creature))
            }
        }
        return roster
    }

    fun applyOptional(apply: () -> Unit, report: (Exception) -> Unit) {
        try { apply() } catch (failure: Exception) { report(failure) }
    }
}
