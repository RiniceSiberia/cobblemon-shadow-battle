package io.github.rinicesiberia.shadowbattle.battle

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RankedCompetitionDecodingTest {
    @Test
    fun `忽略无效条目并保留字段默认值`() {
        val document = JsonObject().apply {
            add("ranked", JsonArray().apply {
                add("ignored")
                add(JsonObject())
                add(JsonObject().apply { addProperty("id", "ou") })
            })
        }

        val competition = RankedCompetitionDecoding.decode(document).getValue("ou")
        assertEquals("ou", competition.name())
        assertEquals("singles", competition.battleType())
        assertEquals(1, competition.slots())
        assertEquals(-1, competition.adjustLevel())
        assertEquals(false, competition.fullHeal())
        assertEquals("1", competition.winScore())
        assertEquals("1", competition.failScore())
        assertTrue(competition.rules().isEmpty())
    }

    @Test
    fun `规则仅接受原始值且重复标识以后项覆盖`() {
        val first = JsonObject().apply { addProperty("id", "ou"); addProperty("name", "first") }
        val second = JsonObject().apply {
            addProperty("id", "ou")
            addProperty("name", "second")
            addProperty("battleType", "doubles")
            add("ruleSet", JsonArray().apply {
                add("Species Clause")
                add(3)
                add(JsonObject())
            })
        }
        val decoded = RankedCompetitionDecoding.decode(JsonObject().apply {
            add("ranked", JsonArray().apply { add(first); add(second) })
        })

        assertEquals(listOf("ou"), decoded.keys.toList())
        assertEquals("second", decoded.getValue("ou").name())
        assertEquals("doubles", decoded.getValue("ou").battleType())
        assertEquals(listOf("Species Clause", "3"), decoded.getValue("ou").rules())
    }

    @Test
    fun `缺少排位数组返回空映射`() {
        assertTrue(RankedCompetitionDecoding.decode(JsonObject()).isEmpty())
    }
}
