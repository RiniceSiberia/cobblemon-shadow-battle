package io.github.rinicesiberia.shadowbattle.transport

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.util.UUID
import xiaocaoawa.minecraft.mod.cobblebattle.net.BattleServerClient

/** 构造队伍预览选择消息。 */
object TeamPreviewMessages {
    @JvmStatic
    fun pick(battleId: String, participant: UUID, picks: List<Int>): JsonObject =
        BattleServerClient.msg("preview_pick").apply {
            addProperty("battleId", battleId)
            addProperty("player", participant.toString())
            add("picks", JsonArray().apply { picks.forEach(::add) })
        }
}
