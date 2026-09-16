package io.github.rinicesiberia.shadowbattle.battle

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import xiaocaoawa.minecraft.mod.cobblebattle.net.BattleServerClient
import xiaocaoawa.minecraft.mod.cobblebattle.network.LeaderboardPayload

/** 将排行榜响应转换为网络层使用的排行榜数据。 */
object LeaderboardDecoding {
    @JvmStatic
    fun decode(document: JsonObject): LeaderboardPayload {
        val source = if (document.has("top") && document.get("top").isJsonArray) {
            document.getAsJsonArray("top")
        } else {
            JsonArray()
        }
        val top = ArrayList<LeaderboardPayload.Entry>()
        for (element: JsonElement in source) {
            if (element.isJsonObject) top += entryOf(element.asJsonObject)
        }
        val you = if (document.has("you") && document.get("you").isJsonObject) {
            entryOf(document.getAsJsonObject("you"))
        } else {
            LeaderboardPayload.Entry.NONE
        }
        return LeaderboardPayload(
            BattleServerClient.str(document, "ranked", ""),
            BattleServerClient.str(document, "name", ""),
            BattleServerClient.integer(document, "players", 0),
            top.toList(),
            you
        )
    }

    private fun entryOf(document: JsonObject): LeaderboardPayload.Entry = LeaderboardPayload.Entry(
        BattleServerClient.integer(document, "rank", 0),
        if (document.has("uid")) document.get("uid").asLong else 0L,
        BattleServerClient.str(document, "name", "?"),
        if (document.has("score")) document.get("score").asLong else 0L,
        BattleServerClient.integer(document, "wins", 0),
        BattleServerClient.integer(document, "losses", 0),
        BattleServerClient.integer(document, "streak", 0),
        BattleServerClient.str(document, "favourite", "")
    )
}
