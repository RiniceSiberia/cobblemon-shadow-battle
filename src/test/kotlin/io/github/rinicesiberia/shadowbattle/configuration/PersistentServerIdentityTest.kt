package io.github.rinicesiberia.shadowbattle.configuration

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Supplier

class PersistentServerIdentityTest {
    @TempDir lateinit var directory: Path

    @Test
    fun `已有身份按 Java trim 规则读取并缓存`() {
        val location = directory.resolve("identity")
        Files.writeString(location, "\tcustom-identity\n")
        val repository = PersistentServerIdentity(location, Supplier { error("不应生成新身份") })
        assertEquals("custom-identity", repository.resolve())
        Files.delete(location)
        assertEquals("custom-identity", repository.resolve())
    }

    @Test
    fun `空文件生成固定输入对应的身份且并发只生成一次`() {
        val location = directory.resolve("identity")
        Files.writeString(location, "  ")
        val calls = AtomicInteger()
        val repository = PersistentServerIdentity(location, Supplier {
            calls.incrementAndGet()
            UUID.fromString("12345678-1234-5678-0000-000000000000")
        })
        val workers = Executors.newFixedThreadPool(4)
        try {
            val values = (1..16).map { workers.submit<String> { repository.resolve() } }.map { it.get(5, TimeUnit.SECONDS) }
            assertEquals(setOf("mc-1234567812345678"), values.toSet())
            assertEquals(1, calls.get())
            assertEquals(values.first(), Files.readString(location))
        } finally {
            workers.shutdownNow()
        }
    }

    @Test
    fun `写入失败仍返回生成的身份`() {
        val parentFile = directory.resolve("not-a-directory")
        Files.writeString(parentFile, "occupied")
        val repository = PersistentServerIdentity(parentFile.resolve("identity"), Supplier { UUID(1, 2) })
        assertEquals("mc-1", repository.resolve())
        assertEquals("occupied", Files.readString(parentFile))
    }
}
