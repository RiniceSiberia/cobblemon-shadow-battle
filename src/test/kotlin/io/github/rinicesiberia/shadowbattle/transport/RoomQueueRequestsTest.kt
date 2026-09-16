package io.github.rinicesiberia.shadowbattle.transport

import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RoomQueueRequestsTest {
    @Test
    fun `创建房间保持字段顺序和主机引擎合法性规则`() {
        val request = RoomQueueRequests.create("名称", "密码", "doubles", 50, 4, false, true, false)

        assertEquals(
            "{\"t\":\"room_create\",\"name\":\"名称\",\"password\":\"密码\",\"battleType\":\"doubles\",\"level\":50,\"pick\":4,\"fullHeal\":false,\"engine\":\"host\",\"legality\":false}",
            request.toString()
        )
    }

    @Test
    fun `服务端引擎强制启用合法性检查`() {
        val request = RoomQueueRequests.create("room", "", "singles", -1, 6, true, false, false)

        assertEquals("server", request.get("engine").asString)
        assertTrue(request.get("legality").asBoolean)
    }

    @Test
    fun `加入房间仅在邀请码非空时写入字段`() {
        val direct = RoomQueueRequests.join("r1", "p", "")
        val invited = RoomQueueRequests.join("r2", "", "code")

        assertFalse(direct.has("inviteCode"))
        assertEquals("{\"t\":\"room_join\",\"roomId\":\"r2\",\"password\":\"\",\"inviteCode\":\"code\"}", invited.toString())
    }

    @Test
    fun `房间动作保持ref玩家和邀请码字段顺序`() {
        val uuid = UUID(0L, 3L)

        assertEquals(
            "{\"t\":\"room_lookup\",\"ref\":7,\"player\":{\"uuid\":\"00000000-0000-0000-0000-000000000003\",\"name\":\"玩家\"},\"inviteCode\":\"abc\"}",
            RoomQueueRequests.lookup(7, uuid, "玩家", "abc").toString()
        )
        assertEquals("room_leave", RoomQueueRequests.leave(8, uuid, "玩家").get("t").asString)
        assertEquals("room_start", RoomQueueRequests.start(9, uuid, "玩家").get("t").asString)
    }
}
