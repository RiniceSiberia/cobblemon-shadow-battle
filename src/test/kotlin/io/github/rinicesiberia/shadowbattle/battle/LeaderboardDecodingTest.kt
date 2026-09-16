package io.github.rinicesiberia.shadowbattle.battle

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import xiaocaoawa.minecraft.mod.cobblebattle.network.LeaderboardPayload

class LeaderboardDecodingTest {
    @Test
    fun `排行榜过滤非对象并保留条目默认值`() {
        val document = JsonObject().apply {
            addProperty("ranked", "ou")
            addProperty("players", 7)
            add("top", JsonArray().apply {
                add("ignored")
                add(JsonObject().apply { addProperty("name", "Alice") })
            })
        }

        val board = LeaderboardDecoding.decode(document)

        assertEquals("ou", board.rankedId())
        assertEquals(7, board.players())
        assertEquals(1, board.top().size)
        assertEquals(0, board.top().single().rank())
        assertEquals("Alice", board.top().single().name())
        assertSame(LeaderboardPayload.Entry.NONE, board.you())
    }

    @Test
    fun `完整条目字段按协议读取`() {
        val entry = JsonObject().apply {
            addProperty("rank", 2)
            addProperty("uid", 42L)
            addProperty("name", "Bob")
            addProperty("score", 1510L)
            addProperty("wins", 10)
            addProperty("losses", 3)
            addProperty("streak", 4)
            addProperty("favourite", "Pikachu")
        }
        val board = LeaderboardDecoding.decode(JsonObject().apply { add("top", JsonArray().apply { add(entry) }); add("you", entry) })

        val decoded = board.top().single()
        assertEquals(2, decoded.rank())
        assertEquals(42L, decoded.uid())
        assertEquals("Bob", decoded.name())
        assertEquals(1510L, decoded.score())
        assertEquals(10, decoded.wins())
        assertEquals(3, decoded.losses())
        assertEquals(4, decoded.streak())
        assertEquals("Pikachu", decoded.favourite())
        assertEquals(decoded, board.you())
    }
}
