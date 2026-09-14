package xiaocaoawa.minecraft.mod.cobblebattle

import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import xiaocaoawa.minecraft.mod.cobblebattle.config.CobbleBattleConfig
import xiaocaoawa.minecraft.mod.cobblebattle.net.BattleServerClient
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.ServerSocket
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterInputStream

@Timeout(15)
class TransportContractTest {
    @Test
    fun `按需连接握手后双向压缩并保留 Unicode 内容`() {
        ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress()).use { server ->
            server.soTimeout = 5000
            val settings = CobbleBattleConfig().apply { serverHost = "127.0.0.1"; serverPort = server.localPort }
            val received = LinkedBlockingQueue<JsonObject>()
            val closed = LinkedBlockingQueue<String>()
            val connection = BattleServerClient(settings, received::add, {}, closed::add)
            try {
                assertFalse(connection.send(BattleServerClient.msg("before")))
                assertEquals(1, connection.nextRef())
                assertEquals(2, connection.nextRef())
                connection.start()
                assertFalse(connection.isWanted)
                connection.connect()
                server.accept().use { peer ->
                    peer.soTimeout = 5000
                    val raw = DataOutputStream(peer.getOutputStream())
                    sendFrame(raw, "{\"t\":\"hello_ack\"}")
                    assertEquals("hello_ack", requireNotNull(received.poll(5, TimeUnit.SECONDS))["t"].asString)
                    connection.setHandshaken(true)
                    val zipped = DataOutputStream(DeflaterOutputStream(peer.getOutputStream(), true))
                    sendFrame(zipped, "[1,2]")
                    sendFrame(zipped, "{\"t\":\"chat\",\"text\":\"你好\"}")
                    assertEquals("你好", requireNotNull(received.poll(5, TimeUnit.SECONDS))["text"].asString)
                    assertTrue(received.isEmpty())
                    val response = BattleServerClient.msg("pong").apply { addProperty("text", "回复") }
                    assertTrue(connection.send(response))
                    val incoming = DataInputStream(InflaterInputStream(peer.getInputStream()))
                    val bytes = incoming.readNBytes(incoming.readInt())
                    assertEquals(response.toString(), bytes.toString(Charsets.UTF_8))
                    connection.disconnect("manual")
                    assertEquals("manual", closed.poll(5, TimeUnit.SECONDS))
                    assertFalse(connection.isWanted)
                }
            } finally {
                connection.stop()
            }
        }
    }

    @Test
    fun `负长度和超上限帧触发连接失败回调`() {
        for (length in listOf(-1, 1025)) {
            ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress()).use { server ->
                server.soTimeout = 5000
                val errors = LinkedBlockingQueue<String>()
                val settings = CobbleBattleConfig().apply {
                    serverHost = "127.0.0.1"; serverPort = server.localPort; maxFrameBytes = 1024
                }
                val connection = BattleServerClient(settings, {}, {}, {})
                connection.setOnConnectFailed(errors::add)
                try {
                    connection.start()
                    connection.connect()
                    server.accept().use { peer ->
                        DataOutputStream(peer.getOutputStream()).apply { writeInt(length); flush() }
                        assertEquals("Frame of $length bytes exceeds the configured maximum", errors.poll(5, TimeUnit.SECONDS))
                    }
                } finally {
                    connection.stop()
                }
            }
        }
    }

    private fun sendFrame(target: DataOutputStream, document: String) {
        val encoded = document.toByteArray(Charsets.UTF_8)
        target.writeInt(encoded.size)
        target.write(encoded)
        target.flush()
    }
}
