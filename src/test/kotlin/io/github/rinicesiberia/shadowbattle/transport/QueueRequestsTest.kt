package io.github.rinicesiberia.shadowbattle.transport

import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class QueueRequestsTest {
    @Test
    fun `加入队列保持类型后写入排行字段`() {
        assertEquals("{\"t\":\"queue_join\",\"ranked\":\"ou\"}", QueueRequests.join("ou").toString())
    }

    @Test
    fun `主动离队写ref而断线离队省略ref`() {
        val uuid = UUID(0L, 4L)
        val active = QueueRequests.leave(11, uuid, "P")
        val disconnected = QueueRequests.leave(null, uuid, "P")

        assertEquals("{\"t\":\"queue_leave\",\"ref\":11,\"player\":{\"uuid\":\"00000000-0000-0000-0000-000000000004\",\"name\":\"P\"}}", active.toString())
        assertFalse(disconnected.has("ref"))
        assertEquals(active.get("player"), disconnected.get("player"))
    }
}
