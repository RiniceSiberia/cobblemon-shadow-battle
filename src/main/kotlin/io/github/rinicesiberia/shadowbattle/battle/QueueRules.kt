package io.github.rinicesiberia.shadowbattle.battle

import java.util.Locale

/** 排队与房间队伍人数的协议规则。 */
object QueueRules {
    @JvmStatic
    fun requiredSlots(battleType: String?): Int = when (battleType.orEmpty().lowercase(Locale.ROOT)) {
        "doubles", "double", "double_battle" -> 2
        "triples", "triple", "triple_battle" -> 3
        else -> 1
    }
}
