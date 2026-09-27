package io.github.rinicesiberia.shadowbattle.showdown

import com.cobblemon.mod.common.battles.pokemon.BattlePokemon

/**
 * 官方 Showdown 对战开始时的状态规则。
 *
 * 规则只应用于 [BattlePokemon.effectedPokemon]。调用方必须传入
 * `PartyStore.toBattleTeam(true, true, ...)` 创建的战斗副本，不能把玩家 party
 * 中的原始 Pokémon 传进来。
 */
object ShowdownBattleStatePolicy {
    /**
     * 将临时战斗副本恢复到 Showdown 的开局状态。
     *
     * Cobblemon 的 PP Up 会把上限提高到 5 的倍数，官方 Showdown 队伍格式不携带
     * 这个状态，因此清除 PP Up 阶段并恢复招式模板的基础 PP。异常状态及其持续时间
     * 由 `Pokemon.heal()` 清除，当前 HP 也恢复到最大值。
     */
    fun normalize(roster: List<BattlePokemon>) {
        roster.forEach { battlePokemon ->
            battlePokemon.effectedPokemon.heal()
            battlePokemon.moveSet.forEach { move ->
                move.raisedPpStages = 0
                move.currentPp = move.template.maxPp
            }
        }
    }
}
