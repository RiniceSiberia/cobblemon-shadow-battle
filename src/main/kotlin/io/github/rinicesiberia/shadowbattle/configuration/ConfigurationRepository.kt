package io.github.rinicesiberia.shadowbattle.configuration

import com.google.gson.GsonBuilder
import org.slf4j.LoggerFactory
import xiaocaoawa.minecraft.mod.cobblebattle.config.CobbleBattleConfig
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/** 负责配置文件的首次落盘、启动回退和严格读取。 */
object ConfigurationRepository {
    @JvmField val location: Path = Path.of("config", "cobblebattle.yml")
    private val logger = LoggerFactory.getLogger("CobbleBattle/Config")
    private val json = GsonBuilder().create()

    @JvmStatic
    fun loadOrDefault(): CobbleBattleConfig {
        try {
            if (!Files.exists(location)) installTemplate()
            if (Files.exists(location)) {
                val configuration = decode(Files.readString(location, Charsets.UTF_8))
                if (configuration != null) return configuration
                logger.warn("config/cobblebattle.yml was empty, falling back to defaults")
            }
        } catch (failure: RuntimeException) {
            logger.error("Could not read config/cobblebattle.yml, using defaults", failure)
        } catch (failure: IOException) {
            logger.error("Could not read config/cobblebattle.yml, using defaults", failure)
        }
        return CobbleBattleConfig().also(ConfigurationValidation::normalize)
    }

    @JvmStatic
    fun readRequired(): CobbleBattleConfig {
        try {
            check(Files.exists(location)) { "$location does not exist" }
            return checkNotNull(decode(Files.readString(location, Charsets.UTF_8))) { "$location is empty" }
        } catch (failure: IOException) {
            throw IllegalStateException("could not read $location: ${failure.message}", failure)
        } catch (failure: RuntimeException) {
            val detail = failure.message ?: failure.toString()
            throw IllegalStateException(detail.substringBefore('\n'), failure)
        }
    }

    private fun decode(source: String): CobbleBattleConfig? =
        json.fromJson(ConfigurationDocument.decode(source), CobbleBattleConfig::class.java)?.also(ConfigurationValidation::normalize)

    private fun installTemplate() {
        val template = ConfigurationRepository::class.java.getResourceAsStream("/cobblebattle.yml")
            ?: ConfigurationRepository::class.java.classLoader.getResourceAsStream("cobblebattle.yml")
        if (template == null) {
            logger.error("Missing bundled resource {} - starting with built-in defaults", "cobblebattle.yml")
            return
        }
        template.use { stream ->
            val source = String(stream.readAllBytes(), Charsets.UTF_8)
            inspectTemplate(source)
            Files.createDirectories(location.parent)
            Files.writeString(location, source, Charsets.UTF_8)
            logger.info("Wrote default config to {}", location.toAbsolutePath())
        }
    }

    private fun inspectTemplate(source: String) {
        val bundled = try {
            ConfigurationDocument.decode(source)
        } catch (failure: RuntimeException) {
            logger.warn("Bundled {} is not readable: {}", "cobblebattle.yml", failure.message)
            return
        }
        val defaults = json.toJsonTree(CobbleBattleConfig()).asJsonObject
        for (key in defaults.keySet()) {
            val expected = defaults[key]
            val actual = bundled[key]
            if (actual == null) logger.warn("Bundled {} is missing '{}'", "cobblebattle.yml", key)
            else if (expected != actual) logger.warn(
                "Bundled {} has {} = {} but the built-in default is {}", "cobblebattle.yml", key, actual, expected
            )
        }
    }
}
