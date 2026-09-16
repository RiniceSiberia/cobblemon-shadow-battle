package xiaocaoawa.minecraft.mod.cobblebattle.battle

import com.cobblemon.mod.common.battles.pokemon.BattlePokemon
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.pokemon.Pokemon
import io.github.rinicesiberia.shadowbattle.battle.MirrorPropTracker
import net.minecraft.world.entity.Entity
import org.slf4j.LoggerFactory

/** 管理镜像 Pokémon 实体的归属、标签和遗留实体清理。 */
object MirrorPropEntities {
    private val logger = LoggerFactory.getLogger("CobbleBattle/Mirror")
    private val props = MirrorPropTracker<MirrorBattle>()

    @JvmStatic
    fun claim(mirror: MirrorBattle, roster: List<BattlePokemon>) {
        for (member in roster) {
            val creature: Pokemon? = member.effectedPokemon
            if (creature != null) props.claim(creature.uuid, mirror)
        }
    }

    @JvmStatic
    fun release(roster: List<BattlePokemon>) {
        for (member in roster) {
            val creature: Pokemon? = member.effectedPokemon
            if (creature != null) props.release(creature.uuid)
        }
    }

    @JvmStatic
    fun onEntityAdded(entity: Entity?): Boolean {
        if (entity !is PokemonEntity) return false
        val creature: Pokemon? = entity.pokemon
        return props.onAdded(creature?.uuid, { entity.tags.contains(MirrorPokemon.TAG) }, { owner ->
            entity.addTag(MirrorPokemon.TAG)
            owner.attachProp(entity)
        }) {
            logger.info("Removed a leftover mirror Pokemon ({}) at {}", if (creature == null) "?" else creature.species.name, entity.blockPosition())
            entity.discard()
        }
    }
}
