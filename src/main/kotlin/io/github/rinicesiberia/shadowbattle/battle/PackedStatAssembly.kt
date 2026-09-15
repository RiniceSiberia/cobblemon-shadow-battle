package io.github.rinicesiberia.shadowbattle.battle

import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.cobblemon.mod.common.pokemon.Pokemon
import org.slf4j.Logger

/** 按压缩队伍协议的六维顺序写入个体值或努力值。 */
object PackedStatAssembly {
    private val statOrder = listOf(Stats.HP, Stats.ATTACK, Stats.DEFENCE, Stats.SPECIAL_ATTACK, Stats.SPECIAL_DEFENCE, Stats.SPEED)

    @JvmStatic
    fun applyTo(creature: Pokemon, rawValues: String, individual: Boolean, teamSlot: Int, logger: Logger) {
        visitValues(rawValues, individual, { count ->
            logger.warn("Remote team slot {} has {} {} values, expected {}", teamSlot, count, if (individual) "IV" else "EV", statOrder.size)
        }) { index, value ->
            if (individual) creature.ivs.set(statOrder[index], value)
            else creature.evs.set(statOrder[index], value)
        }
    }

    internal fun visitValues(rawValues: String, individual: Boolean, invalidCount: (Int) -> Unit, write: (Int, Int) -> Unit) {
        if (rawValues.isEmpty()) return
        val fields = rawValues.split(",", limit = Int.MAX_VALUE)
        if (fields.size != statOrder.size) {
            invalidCount(fields.size)
            return
        }
        fields.forEachIndexed { index, rawValue ->
            write(index, PackedTeamValueParsing.boundedInt(rawValue, 0, 0, if (individual) 31 else 252))
        }
    }
}
