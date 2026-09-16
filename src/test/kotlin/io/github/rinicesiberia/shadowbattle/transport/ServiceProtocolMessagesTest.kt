package io.github.rinicesiberia.shadowbattle.transport

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class ServiceProtocolMessagesTest {
    @Test
    fun `握手保持协议版本和字段顺序`() {
        assertEquals(
            "{\"t\":\"hello\",\"ref\":1,\"protocol\":10,\"token\":\"secret\",\"serverId\":\"server\",\"modVersion\":\"1.0\",\"cobblemonVersion\":\"1.6\"}",
            ServiceProtocolMessages.hello(1, "secret", "server", "1.6").toString()
        )
    }

    @Test
    fun `图鉴和房间目录仅在缓存值存在时携带缓存字段`() {
        assertFalse(ServiceProtocolMessages.dexQuery(2, null).has("have"))
        assertEquals("digest", ServiceProtocolMessages.dexQuery(2, "digest").get("have").asString)
        assertFalse(ServiceProtocolMessages.roomList(3, null).has("hash"))
        assertEquals("hash", ServiceProtocolMessages.roomList(3, "hash").get("hash").asString)
    }

    @Test
    fun `对战转发和观察者消息保持字段名`() {
        assertEquals("{\"t\":\"battle_output\",\"battleId\":\"b\",\"data\":\"line\"}", ServiceProtocolMessages.battleOutput("b", "line").toString())
        assertEquals("{\"t\":\"choice\",\"battleId\":\"b\",\"line\":\"move 1\"}", ServiceProtocolMessages.choice("b", "move 1").toString())
        assertEquals("{\"t\":\"chat_watch\",\"watchers\":4}", ServiceProtocolMessages.chatWatch(4).toString())
    }
}
