package io.github.rinicesiberia.shadowbattle.battle

import com.google.gson.JsonObject
import xiaocaoawa.minecraft.mod.cobblebattle.api.BattleOutcome
import xiaocaoawa.minecraft.mod.cobblebattle.api.ScoreChange
import java.util.UUID

/** 解析远端结束帧中的胜负和参与者分数。 */
object BattleResultProjection {
    data class ParticipantScore(
        val participant: UUID,
        val before: Long,
        val after: Long,
        val won: Boolean
    )

    @JvmStatic
    fun outcome(reason: String, winnerSeat: String, participantSeat: String?): BattleOutcome = when {
        reason == "win" && winnerSeat.isNotEmpty() ->
            if (winnerSeat == participantSeat) BattleOutcome.WIN else BattleOutcome.LOSS
        reason == "tie" -> BattleOutcome.TIE
        else -> BattleOutcome.ABORTED
    }

    @JvmStatic
    fun scoreFor(document: JsonObject, participant: UUID): ScoreChange? {
        val scores = document["scores"]?.takeIf { it.isJsonArray }?.asJsonArray ?: return null
        for (element in scores) {
            if (!element.isJsonObject) continue
            val score = element.asJsonObject
            if (participant == uuidOrNull(text(score, "player", ""))) {
                return ScoreChange(longNumber(score, "before", 0L), longNumber(score, "after", 0L))
            }
        }
        return null
    }

    @JvmStatic
    fun participantScores(document: JsonObject): List<ParticipantScore> {
        val scores = document["scores"]?.takeIf { it.isJsonArray }?.asJsonArray ?: return emptyList()
        return buildList {
            for (element in scores) {
                if (!element.isJsonObject) continue
                val score = element.asJsonObject
                val participant = uuidOrNull(text(score, "player", "")) ?: continue
                add(
                    ParticipantScore(
                        participant,
                        longNumber(score, "before", 0L),
                        longNumber(score, "after", 0L),
                        flag(score, "won", false)
                    )
                )
            }
        }
    }

    private fun uuidOrNull(raw: String): UUID? = try {
        raw.takeIf(String::isNotBlank)?.let(UUID::fromString)
    } catch (failure: IllegalArgumentException) {
        null
    }

    private fun text(document: JsonObject, key: String, defaultValue: String): String =
        document[key]?.takeUnless { it.isJsonNull }?.asString ?: defaultValue

    private fun longNumber(document: JsonObject, key: String, defaultValue: Long): Long =
        document[key]?.takeUnless { it.isJsonNull }?.asLong ?: defaultValue

    private fun flag(document: JsonObject, key: String, defaultValue: Boolean): Boolean =
        document[key]?.takeUnless { it.isJsonNull }?.asBoolean ?: defaultValue
}
