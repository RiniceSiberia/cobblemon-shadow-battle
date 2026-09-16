package io.github.rinicesiberia.shadowbattle.battle

import com.google.gson.JsonObject
import xiaocaoawa.minecraft.mod.cobblebattle.net.BattleServerClient
import xiaocaoawa.minecraft.mod.cobblebattle.network.TeamPreviewPayload

/** 解码队伍预览席位阵容并按对象顺序查找对手席位。 */
object TeamPreviewRosterDecoding {
    @JvmStatic
    fun roster(rosters: JsonObject?, seat: String?): List<TeamPreviewPayload.Slot> {
        if (rosters == null || seat == null) return emptyList()
        val element = rosters.get(seat) ?: return emptyList()
        if (!element.isJsonArray) return emptyList()
        val slots = ArrayList<TeamPreviewPayload.Slot>()
        for (entry in element.asJsonArray) {
            if (!entry.isJsonObject) continue
            val slot = entry.asJsonObject
            slots += TeamPreviewPayload.Slot(
                BattleServerClient.str(slot, "species", ""),
                BattleServerClient.integer(slot, "level", 1),
                BattleServerClient.str(slot, "gender", ""),
                BattleServerClient.bool(slot, "shiny", false),
                BattleServerClient.str(slot, "item", "")
            )
        }
        return slots
    }

    @JvmStatic
    fun opponentSeat(rosters: JsonObject?, seat: String): String? =
        rosters?.keySet()?.firstOrNull { it != seat }
}
