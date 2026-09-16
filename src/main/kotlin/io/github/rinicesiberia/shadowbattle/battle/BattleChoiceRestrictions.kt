package io.github.rinicesiberia.shadowbattle.battle

import java.util.Locale

/** 将对战机制标识投影为规则条款及拒绝提示。 */
object BattleChoiceRestrictions {
    private val clauses = mapOf(
        "mega" to "Mega Clause",
        "zmove" to "Z-Move Clause",
        "max" to "Dynamax Clause",
        "terastal" to "Terastal Clause",
        "ultra" to "Ultra Burst Clause",
    )

    private val messages = mapOf(
        "mega" to "battle.banned.mega",
        "zmove" to "battle.banned.zmove",
        "max" to "battle.banned.dynamax",
        "terastal" to "battle.banned.terastal",
        "ultra" to "battle.banned.ultra",
    )

    @JvmStatic
    fun refusalKey(gimmick: String?, activeRules: Set<String>?): String? {
        if (gimmick == null || activeRules.isNullOrEmpty()) return null
        val normalized = gimmick.lowercase(Locale.ROOT)
        val clause = clauses[normalized] ?: return null
        if (!activeRules.contains(clause)) return null
        return messages[normalized] ?: "battle.banned.gimmick"
    }
}
