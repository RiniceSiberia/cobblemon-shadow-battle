package io.github.rinicesiberia.shadowbattle.battle

import com.cobblemon.mod.common.api.moves.Move
import com.cobblemon.mod.common.api.moves.Moves
import com.cobblemon.mod.common.pokemon.Pokemon
import org.slf4j.Logger

/** 招式装配所需的对象操作，按槽位逐项执行。 */
internal interface MoveAssemblyTarget<M : Any> {
    fun clear()
    fun create(name: String): M?
    fun currentPp(move: M): Int
    fun updatePp(move: M, value: Int)
    fun place(index: Int, move: M)
}

/** 将压缩队伍的招式和 PP 写入 Pokémon。 */
object PackedMoveAssembly {
    @JvmStatic
    fun applyTo(creature: Pokemon, details: PackedTeamDetails, teamSlot: Int, logger: Logger) {
        assemble(details, object : MoveAssemblyTarget<Move> {
            override fun clear() = creature.moveSet.clear()
            override fun create(name: String): Move? {
                val template = Moves.getByName(name)
                if (template == null) {
                    logger.warn("Remote team slot {} has unknown move '{}'", teamSlot, name)
                    return null
                }
                return template.create()
            }
            override fun currentPp(move: Move): Int = move.currentPp
            override fun updatePp(move: Move, value: Int) { move.currentPp = value }
            override fun place(index: Int, move: Move) { creature.moveSet.setMove(index, move) }
        })
    }

    internal fun <M : Any> assemble(details: PackedTeamDetails, target: MoveAssemblyTarget<M>) {
        if (details.moveNames.isEmpty()) return
        target.clear()
        details.moveNames.take(4).forEachIndexed { index, name ->
            if (name.isNotEmpty()) {
                val move = target.create(name)
                if (move != null) {
                    details.movePp.getOrNull(index)?.let { rawPp ->
                        target.updatePp(move, PackedTeamValueParsing.boundedInt(rawPp, target.currentPp(move), 0, 99))
                    }
                    target.place(index, move)
                }
            }
        }
    }
}
