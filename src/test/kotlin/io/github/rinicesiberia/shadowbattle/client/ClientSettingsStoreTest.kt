package io.github.rinicesiberia.shadowbattle.client

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import java.util.function.Supplier

class ClientSettingsStoreTest {
    @TempDir
    lateinit var temporaryDirectory: Path

    @Test
    fun `缺少文件时保持默认值且只加载一次`() {
        val requests = AtomicInteger()
        val file = temporaryDirectory.resolve("settings.json")
        val store = store({ requests.incrementAndGet(); file })
        assertTrue(store.chatHudEnabled())
        assertTrue(store.chatHudEnabled())
        assertEquals(1, requests.get())
    }

    @Test
    fun `读取已有开关并忽略其他字段`() {
        val file = temporaryDirectory.resolve("settings.json")
        Files.writeString(file, """{"other":1,"chatHud":false}""")
        assertFalse(store({ file }).chatHudEnabled())
    }

    @Test
    fun `损坏文件回退后不在同一实例重复读取`() {
        val file = temporaryDirectory.resolve("settings.json")
        Files.writeString(file, "[")
        val warnings = mutableListOf<String>()
        val store = store({ file }, warnings)
        assertTrue(store.chatHudEnabled())
        Files.writeString(file, """{"chatHud":false}""")
        assertTrue(store.chatHudEnabled())
        assertEquals(1, warnings.size)
        assertTrue(warnings.single().startsWith("Could not read client settings:"))
    }

    @Test
    fun `更新开关创建目录并可由新实例读取`() {
        val file = temporaryDirectory.resolve("nested").resolve("settings.json")
        store({ file }).updateChatHud(false)
        assertEquals("""{"chatHud":false}""", Files.readString(file))
        assertFalse(store({ file }).chatHudEnabled())
    }

    @Test
    fun `保存失败保留内存值并记录诊断`() {
        val blockedParent = temporaryDirectory.resolve("blocked")
        Files.writeString(blockedParent, "file")
        val warnings = mutableListOf<String>()
        val store = store({ blockedParent.resolve("settings.json") }, warnings)
        store.updateChatHud(false)
        assertFalse(store.chatHudEnabled())
        assertEquals(1, warnings.size)
        assertTrue(warnings.single().startsWith("Could not save client settings:"))
    }

    private fun store(location: () -> Path, warnings: MutableList<String> = mutableListOf()): ClientSettingsStore =
        ClientSettingsStore(Supplier(location), Consumer(warnings::add))
}
