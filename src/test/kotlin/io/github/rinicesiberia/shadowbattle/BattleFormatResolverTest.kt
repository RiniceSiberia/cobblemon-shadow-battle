package io.github.rinicesiberia.shadowbattle

import com.cobblemon.mod.common.battles.BattleFormat
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.github.rinicesiberia.shadowbattle.battle.BattleFormatResolver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BattleFormatResolverTest {
    @Test
    fun `缺少规则描述时使用远端格式标识的默认规则`() {
        val document = JsonObject().apply { addProperty("format", "doubles") }
        assertEquals(BattleFormat.Companion.fromFormatIdentifier("doubles"), BattleFormatResolver.resolve(document))
    }

    @Test
    fun `规则描述覆盖规则集和等级调整`() {
        val described = JsonObject().apply {
            add("battleType", JsonObject().apply { addProperty("name", "singles") })
            add("ruleSet", JsonArray().apply { add("rule-a"); add("rule-b"); add("rule-a") })
            addProperty("adjustLevel", 37)
        }
        val resolved = BattleFormatResolver.resolve(JsonObject().apply { add("formatJson", described) })
        assertEquals(linkedSetOf("rule-a", "rule-b"), resolved.ruleSet)
        assertEquals(37, resolved.adjustLevel)
    }

    @Test
    fun `空规则描述沿用基础格式规则`() {
        val described = JsonObject().apply {
            add("battleType", JsonObject().apply { addProperty("name", "singles") })
            add("ruleSet", JsonArray())
        }
        val resolved = BattleFormatResolver.resolve(JsonObject().apply { add("formatJson", described) })
        assertEquals(BattleFormat.Companion.fromFormatIdentifier("singles").ruleSet, resolved.ruleSet)
    }
}
