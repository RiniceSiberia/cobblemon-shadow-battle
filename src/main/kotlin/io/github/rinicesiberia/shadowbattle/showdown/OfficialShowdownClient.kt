package io.github.rinicesiberia.shadowbattle.showdown

import java.net.URI
import java.net.http.HttpClient
import java.net.http.WebSocket
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/** 官方 PS WebSocket 的传输层。认证和房间状态由上层按 frame 处理。 */
class OfficialShowdownClient @JvmOverloads constructor(
    endpoint: URI,
    private val onFrame: (ShowdownFrame) -> Unit,
    private val onFailure: (Throwable) -> Unit,
    private val httpClient: HttpClient = HttpClient.newHttpClient()
) : AutoCloseable, WebSocket.Listener {
    private val endpoint = endpoint
    private val closed = AtomicBoolean(false)
    private val buffer = StringBuilder()
    @Volatile private var socket: WebSocket? = null
    @Volatile private var heartbeat: ScheduledFuture<*>? = null

    fun connect(): CompletableFuture<Unit> = httpClient.newWebSocketBuilder().buildAsync(endpoint, this).thenApply {
        socket = it
        Unit
    }

    fun send(payload: String): CompletableFuture<Unit> {
        val activeSocket = socket ?: return CompletableFuture.failedFuture(IllegalStateException("官方 PS 尚未连接"))
        return activeSocket.sendText(payload, true).thenApply { Unit }
    }

    override fun onOpen(webSocket: WebSocket) {
        socket = webSocket
        heartbeat?.cancel(false)
        heartbeat = HEARTBEAT.scheduleWithFixedDelay({
            if (!closed.get() && socket === webSocket) {
                webSocket.sendPing(java.nio.ByteBuffer.wrap("cobblebattle".toByteArray()))
                    .exceptionally { error -> onFailure(error); null }
            }
        }, 30, 30, TimeUnit.SECONDS)
        webSocket.request(1)
    }

    override fun onText(webSocket: WebSocket, data: CharSequence, last: Boolean): CompletionStage<*> {
        buffer.append(data)
        if (last) {
            val payload = buffer.toString()
            buffer.setLength(0)
            onFrame(ShowdownFrameParser.parse(payload))
        }
        webSocket.request(1)
        return CompletableFuture.completedFuture(Unit)
    }

    override fun onError(webSocket: WebSocket, error: Throwable) {
        if (!closed.get()) onFailure(error)
    }

    override fun onClose(webSocket: WebSocket, statusCode: Int, reason: String): CompletionStage<*> {
        socket = null
        heartbeat?.cancel(false)
        heartbeat = null
        if (!closed.get()) onFailure(IllegalStateException("官方 PS 连接关闭: $statusCode $reason"))
        return CompletableFuture.completedFuture(Unit)
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) socket?.sendClose(WebSocket.NORMAL_CLOSURE, "shutdown")
        heartbeat?.cancel(false)
        heartbeat = null
        socket = null
    }

    private companion object {
        val HEARTBEAT = Executors.newSingleThreadScheduledExecutor { task ->
            Thread(task, "CobbleBattle-PS-Heartbeat").apply { isDaemon = true }
        }
    }
}
