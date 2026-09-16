package io.github.rinicesiberia.shadowbattle.battle

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import xiaocaoawa.minecraft.mod.cobblebattle.battle.CrossServerBattleService
import xiaocaoawa.minecraft.mod.cobblebattle.net.BattleServerClient

/** 解码远端提供的排位赛配置，保持服务端数组顺序和默认值。 */
object RankedCompetitionDecoding {
    @JvmStatic
    fun decode(document: JsonObject): LinkedHashMap<String, CrossServerBattleService.Ranked> {
        val competitions = LinkedHashMap<String, CrossServerBattleService.Ranked>()
        if (!document.has("ranked") || !document.get("ranked").isJsonArray) return competitions
        for (element: JsonElement in document.getAsJsonArray("ranked")) {
            if (!element.isJsonObject) continue
            val offered = element.asJsonObject
            val id = BattleServerClient.str(offered, "id", "")
            if (id.isEmpty()) continue
            val rules = ArrayList<String>()
            if (offered.has("ruleSet") && offered.get("ruleSet").isJsonArray) {
                for (rule: JsonElement in offered.getAsJsonArray("ruleSet")) {
                    if (rule.isJsonPrimitive) rules += rule.asString
                }
            }
            competitions[id] = CrossServerBattleService.Ranked(
                id,
                BattleServerClient.str(offered, "name", id),
                BattleServerClient.str(offered, "battleType", "singles"),
                BattleServerClient.integer(offered, "slotsPerActor", 1),
                BattleServerClient.integer(offered, "adjustLevel", -1),
                BattleServerClient.bool(offered, "fullHeal", false),
                BattleServerClient.str(offered, "winScore", "1"),
                BattleServerClient.str(offered, "failScore", "1"),
                rules.toList()
            )
        }
        return competitions
    }
}
