package io.github.rinicesiberia.shadowbattle.battle

import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HandshakeResponseDecodingTest {
    @Test
    fun `缺少字段时保持握手默认设置`() {
        val settings = HandshakeResponseDecoding.decode(JsonObject())

        assertFalse(settings.dexReady)
        assertTrue(settings.strictBaseStats)
        assertFalse(settings.strictAbilities)
        assertFalse(settings.strictMoves)
        assertEquals(0, settings.maxEvPerStat)
        assertEquals(0, settings.maxEvTotal)
        assertEquals(0, settings.maxIv)
        assertTrue(settings.chatEnabled)
        assertFalse(settings.emailEnabled)
        assertEquals("?", settings.instance)
        assertEquals(0, settings.speciesCount)
        assertEquals("", settings.dexDigest)
    }

    @Test
    fun `图鉴未就绪优先失效缓存`() {
        val settings = HandshakeResponseDecoding.decode(JsonObject().apply { addProperty("dexDigest", "same") })
        assertSame(HandshakeResponseDecoding.DexAction.Invalidate, HandshakeResponseDecoding.decideDex(settings, "same"))
    }

    @Test
    fun `图鉴摘要相同复用缓存其余请求快照`() {
        val settings = HandshakeResponseDecoding.decode(JsonObject().apply {
            addProperty("dexReady", true)
            addProperty("dexDigest", "same")
        })
        assertEquals(HandshakeResponseDecoding.DexAction.AcceptCached("same"), HandshakeResponseDecoding.decideDex(settings, "same"))
        assertSame(HandshakeResponseDecoding.DexAction.RequestSnapshot, HandshakeResponseDecoding.decideDex(settings, null))
        assertSame(HandshakeResponseDecoding.DexAction.RequestSnapshot, HandshakeResponseDecoding.decideDex(settings, "other"))
    }
}
