package io.github.rinicesiberia.shadowbattle

import com.google.gson.Gson
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.net.URLClassLoader
import java.nio.file.Path
import java.util.jar.JarFile

class ArtifactContractTest {
    @Test
    fun `发布包包含隔离 YAML 和模组资源且未嵌入游戏依赖`() {
        val artifact = Path.of(requireNotNull(System.getProperty("battle.artifact")))
        JarFile(artifact.toFile()).use { archive ->
            val entries = archive.entries().asSequence().map { it.name }.toSet()
            assertTrue("io/github/rinicesiberia/shadowbattle/internal/yaml/Yaml.class" in entries)
            assertTrue("META-INF/neoforge.mods.toml" in entries)
            assertTrue("cobblebattle.mixins.json" in entries)
            assertTrue(entries.none { it.startsWith("org/yaml/snakeyaml/") })
            assertTrue(entries.none { it.startsWith("net/minecraft/") || it.startsWith("com/cobblemon/") })
        }
        val urls = arrayOf(
            artifact.toUri().toURL(), Gson::class.java.protectionDomain.codeSource.location,
            Unit::class.java.protectionDomain.codeSource.location
        )
        URLClassLoader(urls, ClassLoader.getPlatformClassLoader()).use { isolated ->
            val parser = isolated.loadClass("io.github.rinicesiberia.shadowbattle.configuration.ConfigurationDocument")
            val decoded = parser.getMethod("decode", String::class.java).invoke(null, "enabled: true\nport: 18470")
            assertEquals("{\"enabled\":true,\"port\":18470}", decoded.toString())
        }
    }
}
