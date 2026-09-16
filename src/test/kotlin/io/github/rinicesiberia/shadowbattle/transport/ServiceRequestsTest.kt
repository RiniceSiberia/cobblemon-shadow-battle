package io.github.rinicesiberia.shadowbattle.transport

import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ServiceRequestsTest {
    private val participant = UUID(0L, 9L)

    @Test
    fun `排行榜查询保持字段顺序`() {
        assertEquals(
            "{\"t\":\"leaderboard_query\",\"ref\":5,\"ranked\":\"ou\",\"player\":{\"uuid\":\"00000000-0000-0000-0000-000000000009\",\"name\":\"P\"}}",
            ServiceRequests.leaderboard(5, "ou", participant, "P").toString()
        )
    }

    @Test
    fun `聊天频道仅接受battle其余回退global`() {
        assertEquals("battle", ServiceRequests.chat(1, "battle", "x", participant, "P").get("channel").asString)
        assertEquals("global", ServiceRequests.chat(2, "BATTLE", "x", participant, "P").get("channel").asString)
        assertEquals("global", ServiceRequests.chat(3, null, "x", participant, "P").get("channel").asString)
        assertEquals(
            "{\"t\":\"chat_send\",\"ref\":4,\"channel\":\"global\",\"text\":\"文本\",\"player\":{\"uuid\":\"00000000-0000-0000-0000-000000000009\",\"name\":\"P\"}}",
            ServiceRequests.chat(4, "global", "文本", participant, "P").toString()
        )
    }
}
