package io.github.rinicesiberia.shadowbattle.transport

import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TeamPreviewMessagesTest {
    @Test
    fun `选择消息保持字段顺序和选择顺序`() {
        assertEquals(
            "{\"t\":\"preview_pick\",\"battleId\":\"b\",\"player\":\"00000000-0000-0000-0000-000000000001\",\"picks\":[2,0,1]}",
            TeamPreviewMessages.pick("b", UUID(0L, 1L), listOf(2, 0, 1)).toString()
        )
    }
}
