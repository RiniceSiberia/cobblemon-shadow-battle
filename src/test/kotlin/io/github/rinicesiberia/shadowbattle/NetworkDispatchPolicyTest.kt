package io.github.rinicesiberia.shadowbattle

import io.github.rinicesiberia.shadowbattle.network.ConditionalPayloadSender
import io.github.rinicesiberia.shadowbattle.network.QueuedTypeDispatch
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NetworkDispatchPolicyTest {
    @Test
    fun `不支持消息时不创建也不发送`() {
        var created = 0
        var sent = 0

        val delivered = ConditionalPayloadSender.deliver(
            recipient = "player",
            payloadFactory = { created += 1; "payload" },
            supports = { false },
            send = { _, _ -> sent += 1 },
        )

        assertFalse(delivered)
        assertEquals(0, created)
        assertEquals(0, sent)
    }

    @Test
    fun `支持消息时只创建并发送一次`() {
        val sent = mutableListOf<String>()

        val delivered = ConditionalPayloadSender.deliver(
            recipient = "player",
            payloadFactory = { "payload" },
            supports = { true },
            send = { recipient, payload -> sent += "$recipient:$payload" },
        )

        assertTrue(delivered)
        assertEquals(listOf("player:payload"), sent)
    }

    @Test
    fun `类型匹配的任务先入队后执行`() {
        val queued = mutableListOf<Runnable>()
        val handled = mutableListOf<String>()

        assertFalse(QueuedTypeDispatch.enqueue(7, String::class.java, queued::add, handled::add))
        assertTrue(QueuedTypeDispatch.enqueue("player", String::class.java, queued::add, handled::add))
        assertEquals(emptyList<String>(), handled)
        assertEquals(1, queued.size)

        queued.single().run()
        assertEquals(listOf("player"), handled)
    }
}
