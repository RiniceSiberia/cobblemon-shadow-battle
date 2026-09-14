package io.github.rinicesiberia.shadowbattle.transport

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.slf4j.LoggerFactory
import xiaocaoawa.minecraft.mod.cobblebattle.config.CobbleBattleConfig
import java.io.*
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterInputStream

/** 管理按需建立的对战服务连接及握手后的压缩消息流。 */
class RemoteBattleConnection(
    private val settings: CobbleBattleConfig,
    private val receiver: Consumer<JsonObject>,
    private val connectionOpened: Runnable,
    private val connectionClosed: Consumer<String>
) {
    private val logger = LoggerFactory.getLogger("CobbleBattle/Net")
    private val json = Gson()
    private val active = AtomicBoolean(false)
    private val resetBackoff = AtomicBoolean(false)
    private val referenceSequence = AtomicInteger(0)
    private val demandMonitor = java.lang.Object()
    private val outputMonitor = Any()
    @Volatile private var connectionRequested = false
    @Volatile private var refusal: String? = null
    @Volatile private var requestedCloseReason: String? = null
    @Volatile private var failureObserver: Consumer<String?> = Consumer { }
    @Volatile private var connectionWorker: Thread? = null
    @Volatile private var inputWorker: Thread? = null
    @Volatile private var activeSocket: Socket? = null
    @Volatile private var outputStream: OutputStream? = null
    @Volatile private var compressor: Deflater? = null
    @Volatile private var greetingAccepted = false

    fun isSocketOpen(): Boolean = activeSocket?.let { it.isConnected && !it.isClosed } ?: false
    fun isSessionReady(): Boolean = greetingAccepted && isSocketOpen()
    fun markSessionReady(accepted: Boolean) { greetingAccepted = accepted }
    fun allocateReference(): Int = referenceSequence.incrementAndGet()
    fun hasConnectionDemand(): Boolean = connectionRequested
    fun refusalCause(): String? = refusal
    fun observeConnectionFailure(observer: Consumer<String?>) { failureObserver = observer }

    fun launch() {
        if (active.compareAndSet(false, true)) {
            Thread(::maintainConnection, "CobbleBattle-Net").apply { isDaemon = true; start() }
        }
    }

    fun shutdown() {
        active.set(false)
        connectionRequested = false
        releaseSocket("shutdown")
        inputWorker?.interrupt()
        signalDemand()
    }

    fun requestConnection() {
        refusal = null
        if (!connectionRequested) {
            connectionRequested = true
            signalDemand()
        }
    }

    fun closeByRequest(reason: String) {
        connectionRequested = false
        requestedCloseReason = reason
        releaseSocket(reason)
    }

    fun pauseAfterRefusal(reason: String) {
        refusal = reason
        connectionRequested = false
        requestedCloseReason = reason
        releaseSocket(reason)
    }

    fun restartConnection(reason: String) {
        if (active.get()) {
            logger.info("Dropping the battle server connection to reconnect: {}", reason)
            refusal = null
            connectionRequested = true
            resetBackoff.set(true)
            requestedCloseReason = reason
            releaseSocket(reason)
            signalDemand()
        }
    }

    private fun signalDemand() {
        synchronized(demandMonitor) { demandMonitor.notifyAll() }
        connectionWorker?.interrupt()
    }

    private fun maintainConnection() {
        connectionWorker = Thread.currentThread()
        var attempts = 0
        while (active.get()) {
            if (!connectionRequested) {
                attempts = 0
                synchronized(demandMonitor) {
                    while (active.get() && !connectionRequested) {
                        try { demandMonitor.wait() }
                        catch (_: InterruptedException) { Thread.interrupted() }
                    }
                }
                continue
            }
            if (resetBackoff.compareAndSet(true, false)) attempts = 0
            Thread.interrupted()
            try {
                logger.info("Connecting to battle server {}:{} (attempt {})", settings.serverHost, settings.serverPort, ++attempts)
                val candidate = Socket()
                candidate.tcpNoDelay = true
                candidate.connect(InetSocketAddress(settings.serverHost, settings.serverPort), settings.connectTimeoutMs)
                if (!connectionRequested) {
                    candidate.close()
                    continue
                }
                activeSocket = candidate
                outputStream = BufferedOutputStream(candidate.getOutputStream(), STREAM_BUFFER_SIZE)
                compressor = null
                greetingAccepted = false
                attempts = 0
                logger.info("Connected to battle server")
                connectionOpened.run()
                inputWorker = Thread.currentThread()
                receiveFrames(BufferedInputStream(candidate.getInputStream(), STREAM_BUFFER_SIZE))
            } catch (failure: IOException) {
                if (active.get() && connectionRequested) {
                    logger.warn("Battle server connection problem: {}", failure.message)
                    if (!greetingAccepted) failureObserver.accept(failure.message)
                }
            } finally {
                val hadSession = greetingAccepted
                greetingAccepted = false
                val reason = requestedCloseReason
                requestedCloseReason = null
                releaseSocket("reader ended")
                if (hadSession) connectionClosed.accept(reason ?: "connection lost")
            }
            if (!active.get()) break
            if (connectionRequested) {
                val delay = minOf(settings.reconnectMaxDelayMs.toLong(), settings.reconnectBaseDelayMs.toLong() * maxOf(1, attempts))
                try { Thread.sleep(delay) }
                catch (_: InterruptedException) {
                    if (!active.get()) {
                        Thread.currentThread().interrupt()
                        break
                    }
                    Thread.interrupted()
                }
            }
        }
        logger.info("Battle server client stopped")
    }

    private fun receiveFrames(rawInput: BufferedInputStream) {
        var input = DataInputStream(rawInput)
        while (active.get()) {
            val length = input.readInt()
            if (length < 0 || length > settings.maxFrameBytes) {
                throw IOException("Frame of $length bytes exceeds the configured maximum")
            }
            val bytes = ByteArray(length)
            input.readFully(bytes)
            val document = try {
                val parsed = JsonParser.parseString(String(bytes, Charsets.UTF_8))
                if (!parsed.isJsonObject) {
                    logger.warn("Ignoring a non-object frame from the battle server")
                    continue
                }
                parsed.asJsonObject
            } catch (failure: RuntimeException) {
                throw IOException("Malformed JSON frame: ${failure.message}", failure)
            }
            if (MessageFields.text(document, "t", "") == "hello_ack") {
                enableCompression()
                input = DataInputStream(InflaterInputStream(rawInput))
            }
            try { receiver.accept(document) }
            catch (failure: RuntimeException) {
                logger.error("Handler for '{}' threw", if (document.has("t")) document["t"].asString else "?", failure)
            }
        }
    }

    private fun enableCompression() {
        synchronized(outputMonitor) {
            val currentOutput = outputStream
            val currentSocket = activeSocket
            if (currentOutput != null && currentSocket != null && compressor == null) {
                try {
                    currentOutput.flush()
                    val streamCompressor = Deflater(Deflater.DEFAULT_COMPRESSION)
                    outputStream = BufferedOutputStream(
                        DeflaterOutputStream(currentSocket.getOutputStream(), streamCompressor, STREAM_BUFFER_SIZE, true), STREAM_BUFFER_SIZE
                    )
                    compressor = streamCompressor
                    logger.info("The battle server link is deflated")
                } catch (failure: IOException) {
                    logger.warn("Could not switch the link to deflate: {}", failure.message)
                    releaseSocket("compression failed")
                }
            }
        }
    }

    fun sendMessage(document: JsonObject): Boolean = greetingAccepted && transmit(document)
    fun sendGreeting(document: JsonObject): Boolean = transmit(document)

    private fun transmit(document: JsonObject): Boolean {
        if (!isSocketOpen()) return false
        val body = json.toJson(document).toByteArray(Charsets.UTF_8)
        val frame = ByteArray(4 + body.size)
        frame[0] = (body.size ushr 24).toByte()
        frame[1] = (body.size ushr 16).toByte()
        frame[2] = (body.size ushr 8).toByte()
        frame[3] = body.size.toByte()
        body.copyInto(frame, 4)
        synchronized(outputMonitor) {
            val destination = outputStream ?: return false
            return try {
                destination.write(frame)
                destination.flush()
                true
            } catch (failure: IOException) {
                logger.warn("Failed to write to the battle server: {}", failure.message)
                releaseSocket("write failed")
                false
            }
        }
    }

    private fun releaseSocket(reason: String) {
        val closingSocket = activeSocket
        activeSocket = null
        synchronized(outputMonitor) {
            outputStream = null
            compressor?.let { activeCompressor -> compressor = null; activeCompressor.end() }
        }
        if (closingSocket != null) {
            try { closingSocket.close() } catch (_: IOException) { }
            logger.debug("Socket closed ({})", reason)
        }
    }

    private companion object { const val STREAM_BUFFER_SIZE = 16384 }
}

