package io.github.rinicesiberia.shadowbattle.battle

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ConnectionLifecycleRulesTest {
    @Test
    fun `服务端启动按保活或在线玩家决定连接`() {
        assertFalse(ConnectionLifecycleRules.connectOnServerStart(false, 0))
        assertTrue(ConnectionLifecycleRules.connectOnServerStart(true, 0))
        assertTrue(ConnectionLifecycleRules.connectOnServerStart(false, 1))
    }

    @Test
    fun `按需连接要求客户端存在未请求且没有拒绝原因`() {
        assertTrue(ConnectionLifecycleRules.shouldRequestConnection(true, false, null))
        assertFalse(ConnectionLifecycleRules.shouldRequestConnection(false, false, null))
        assertFalse(ConnectionLifecycleRules.shouldRequestConnection(true, true, null))
        assertFalse(ConnectionLifecycleRules.shouldRequestConnection(true, false, "refused"))
    }

    @Test
    fun `空闲延迟至少五秒且仅在无人在线并仍需要连接时释放`() {
        assertEquals(5L, ConnectionLifecycleRules.idleDelaySeconds(-1))
        assertEquals(5L, ConnectionLifecycleRules.idleDelaySeconds(5))
        assertEquals(30L, ConnectionLifecycleRules.idleDelaySeconds(30))
        assertTrue(ConnectionLifecycleRules.shouldReleaseIdleConnection(0, true))
        assertTrue(ConnectionLifecycleRules.shouldReleaseIdleConnection(-1, true))
        assertFalse(ConnectionLifecycleRules.shouldReleaseIdleConnection(1, true))
        assertFalse(ConnectionLifecycleRules.shouldReleaseIdleConnection(0, false))
    }
}
