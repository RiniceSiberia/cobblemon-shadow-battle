package io.github.rinicesiberia.shadowbattle.transport

import com.google.gson.JsonObject

/** 创建对战建场确认和中止消息，保留线协议中的原始文本与空值。 */
object BattleControlMessages {
    @JvmStatic
    fun acknowledgement(battleId: String?): JsonObject = MessageFields.envelope("battle_ack").apply {
        addProperty("battleId", battleId)
    }

    @JvmStatic
    fun abort(battleId: String?, reason: String?): JsonObject = MessageFields.envelope("battle_abort").apply {
        addProperty("battleId", battleId)
        addProperty("reason", reason)
    }
}
