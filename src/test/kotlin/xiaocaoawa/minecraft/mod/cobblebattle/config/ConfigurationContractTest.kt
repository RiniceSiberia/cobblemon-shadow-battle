package xiaocaoawa.minecraft.mod.cobblebattle.config

import com.google.gson.JsonParser
import io.github.rinicesiberia.shadowbattle.configuration.ConfigurationDocument
import io.github.rinicesiberia.shadowbattle.configuration.ConfigurationRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files

class ConfigurationContractTest {
    @Test
    fun `YAML 支持空文档嵌套列表和标量键`() {
        assertEquals("{}", ConfigurationDocument.decode("").toString())
        val expected = JsonParser.parseString("""{"root":{"enabled":true,"ports":[1,2],"empty":null},"3":"value"}""")
        assertEquals(expected, ConfigurationDocument.decode("root:\n  enabled: true\n  ports: [1, 2]\n  empty: null\n3: value"))
    }

    @Test
    fun `顶层非映射及不安全标签被拒绝`() {
        for (document in listOf("[1, 2]", "hello", "true", "42")) {
            val failure = assertThrows(IllegalArgumentException::class.java) { ConfigurationDocument.decode(document) }
            assertEquals("the top level of cobblebattle.yml must be a mapping", failure.message)
        }
        assertThrows(RuntimeException::class.java) { ConfigurationDocument.decode("!!java.net.URL ['https://example.com']") }
    }

    @Test
    fun `端口边界及最小帧大小保持默认规则`() {
        for (port in listOf(Int.MIN_VALUE, -1, 0, 65536, Int.MAX_VALUE)) {
            val configuration = CobbleBattleConfig()
            configuration.serverPort = port
            configuration.maxFrameBytes = 1
            configuration.validate()
            assertEquals(18470, configuration.serverPort)
            assertEquals(1048576, configuration.maxFrameBytes)
        }
        for (port in listOf(1, 18470, 65535)) {
            val configuration = CobbleBattleConfig()
            configuration.serverPort = port
            configuration.validate()
            assertEquals(port, configuration.serverPort)
        }
    }

    @Test
    fun `模板保留部署参数且其余默认值一致`() {
        val template = requireNotNull(javaClass.getResourceAsStream("/cobblebattle.yml")).bufferedReader().use { it.readText() }
        val defaults = CobbleBattleConfig.GSON.toJsonTree(CobbleBattleConfig()).asJsonObject
        val bundled = ConfigurationDocument.decode(template)
        for (key in defaults.keySet() - setOf("serverHost", "authToken")) assertEquals(defaults[key], bundled[key], key)
        for (key in setOf("serverHost", "authToken")) assertTrue(bundled.has(key))
    }

    @Test
    fun `热加载区分连接字段且失败不改变现有配置`() {
        val location = ConfigurationRepository.location
        Files.createDirectories(location.parent)
        val previous = if (Files.exists(location)) Files.readAllBytes(location) else null
        try {
            val configuration = CobbleBattleConfig()
            Files.writeString(location, "serverHost: battle.local\nserverPort: 18471\nlanguage: en_us\ndebug: true")
            val changes = configuration.reload()
            assertEquals(setOf("serverHost", "serverPort"), changes.needsReconnect().toSet())
            assertEquals(setOf("language", "debug"), changes.live().toSet())
            assertTrue(configuration.reload().nothingChanged())
            val beforeFailure = CobbleBattleConfig.GSON.toJsonTree(configuration)
            Files.writeString(location, "[invalid]")
            assertThrows(IllegalStateException::class.java) { configuration.reload() }
            assertEquals(beforeFailure, CobbleBattleConfig.GSON.toJsonTree(configuration))
            assertEquals(18470, ConfigurationRepository.loadOrDefault().serverPort)
        } finally {
            if (previous == null) Files.deleteIfExists(location) else Files.write(location, previous)
        }
    }
}


