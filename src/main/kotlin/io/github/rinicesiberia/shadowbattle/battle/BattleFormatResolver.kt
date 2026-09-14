package io.github.rinicesiberia.shadowbattle.battle

import com.cobblemon.mod.common.battles.BattleFormat
import com.google.gson.JsonObject
import org.slf4j.LoggerFactory

/** 将远端规则描述转换为 Cobblemon 对战格式。 */
object BattleFormatResolver {
    private val logger = LoggerFactory.getLogger("CobbleBattle")

    @JvmStatic
    fun resolve(document: JsonObject): BattleFormat {
        val described = document.getAsJsonObject("formatJson")
        val identifier = if (described == null) {
            text(document, "format", "singles")
        } else {
            described.getAsJsonObject("battleType")?.let { text(it, "name", "singles") } ?: "singles"
        }
        val base = BattleFormat.Companion.fromFormatIdentifier(identifier)
        if (described == null) {
            logger.warn("The battle server sent no rulebook for this battle; falling back to plain {}", identifier)
            return base
        }

        val rules = LinkedHashSet<String>()
        val ruleSet = described["ruleSet"]
        if (ruleSet != null && ruleSet.isJsonArray) {
            for (rule in ruleSet.asJsonArray) {
                if (rule.isJsonPrimitive) rules.add(rule.asString)
            }
        }
        if (rules.isEmpty()) rules.addAll(base.ruleSet)
        val adjustLevel = number(described, "adjustLevel", base.adjustLevel)
        return base.copy(base.mod, base.battleType, rules, base.gen, adjustLevel)
    }

    private fun text(document: JsonObject, key: String, defaultValue: String): String =
        document[key]?.takeUnless { it.isJsonNull }?.asString ?: defaultValue

    private fun number(document: JsonObject, key: String, defaultValue: Int): Int =
        document[key]?.takeUnless { it.isJsonNull }?.asInt ?: defaultValue
}
