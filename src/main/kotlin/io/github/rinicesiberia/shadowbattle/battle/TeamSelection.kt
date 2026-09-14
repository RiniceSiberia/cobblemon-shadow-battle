package io.github.rinicesiberia.shadowbattle.battle

import com.google.gson.JsonObject
import org.slf4j.LoggerFactory

/** 按远端预选顺序生成参战队伍，非法选择统一回退到完整队伍。 */
object TeamSelection {
    private val logger = LoggerFactory.getLogger("CobbleBattle")

    @JvmStatic
    fun <T> choose(roster: List<T>, document: JsonObject, seat: String): List<T> {
        val picks = document.getAsJsonObject("picks")
        val selected = picks?.get(seat)
        if (selected == null || !selected.isJsonArray) return roster

        val indexes = selected.asJsonArray
        if (indexes.isEmpty) return roster

        val chosen = ArrayList<T>(indexes.size())
        val visited = LinkedHashSet<Int>()
        for (element in indexes) {
            val index = try {
                element.asInt
            } catch (failure: RuntimeException) {
                logger.warn("A pick for seat {} was not a slot number; using the whole team", seat)
                return roster
            }
            if (index !in roster.indices || !visited.add(index)) {
                logger.warn("Pick {} for seat {} is not a slot in a team of {}; using the whole team", index, seat, roster.size)
                return roster
            }
            chosen.add(roster[index])
        }
        return chosen
    }
}
