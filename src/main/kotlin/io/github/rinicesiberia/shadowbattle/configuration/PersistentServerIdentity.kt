package io.github.rinicesiberia.shadowbattle.configuration

import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import java.util.function.Supplier

/** 缓存服务器身份，并在文件尚未提供有效身份时生成和保存。 */
class PersistentServerIdentity(
    private val location: Path,
    private val identifiers: Supplier<UUID> = Supplier(UUID::randomUUID)
) {
    @Volatile private var cachedIdentity: String? = null
    private val logger = LoggerFactory.getLogger("CobbleBattle/Config")

    fun resolve(): String {
        cachedIdentity?.let { return it }
        return synchronized(this) {
            cachedIdentity ?: readOrCreate().also { cachedIdentity = it }
        }
    }

    private fun readOrCreate(): String {
        try {
            if (Files.exists(location)) {
                val stored = Files.readString(location, Charsets.UTF_8).trim { it <= ' ' }
                if (stored.isNotEmpty()) return stored
                logger.warn("{} was empty, generating a new server id", location)
            }
        } catch (failure: IOException) {
            logger.warn("Could not read {}: {}", location, failure.message)
        }
        val created = "mc-" + java.lang.Long.toHexString(identifiers.get().mostSignificantBits)
        try {
            Files.createDirectories(location.parent)
            Files.writeString(location, created, Charsets.UTF_8)
            logger.info("Generated server id {} and saved it to {}", created, location)
        } catch (failure: IOException) {
            logger.warn("Generated server id {} but could not save it: {}", created, failure.message)
            logger.warn("A new one will be generated next start, which the battle server sees as a new client.")
        }
        return created
    }

    companion object {
        @JvmField val server = PersistentServerIdentity(Path.of("config", "cobblebattle-id.txt"))
    }
}
