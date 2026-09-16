package io.github.rinicesiberia.shadowbattle.battle

/** 集中连接启动、按需连接和空闲释放的纯判定。 */
object ConnectionLifecycleRules {
    @JvmStatic
    fun connectOnServerStart(keepConnectedWhenEmpty: Boolean, onlinePlayers: Int): Boolean =
        keepConnectedWhenEmpty || onlinePlayers > 0

    @JvmStatic
    fun shouldRequestConnection(clientPresent: Boolean, connectionWanted: Boolean, refusal: String?): Boolean =
        clientPresent && !connectionWanted && refusal == null

    @JvmStatic
    fun idleDelaySeconds(configuredSeconds: Long): Long = configuredSeconds.coerceAtLeast(5L)

    @JvmStatic
    fun shouldReleaseIdleConnection(onlinePlayers: Int, connectionWanted: Boolean): Boolean =
        onlinePlayers <= 0 && connectionWanted
}
