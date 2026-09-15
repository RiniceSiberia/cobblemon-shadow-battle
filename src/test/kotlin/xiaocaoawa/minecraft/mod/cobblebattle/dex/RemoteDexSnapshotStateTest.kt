package xiaocaoawa.minecraft.mod.cobblebattle.dex

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class RemoteDexSnapshotStateTest {
    @Test
    fun `采用快照后保留物种数值和摘要`() {
        val state = RemoteDexSnapshotState()
        state.adopt(snapshot("1234567890abcdef", "pikachu", 35))
        state.markAccepted()

        assertTrue(state.isReady())
        assertEquals("1234567890abcdef", state.digest())
        assertEquals("1234567890ab", state.shortDigest())
        assertEquals(1, state.size())
        assertEquals(35, state.get("pikachu")?.baseStats()?.get("hp"))
        assertThrows(UnsupportedOperationException::class.java) {
            state.get("pikachu")?.baseStats()?.put("hp", 1)
        }
    }

    @Test
    fun `未变化确认只在已有缓存时就绪`() {
        val state = RemoteDexSnapshotState()
        assertFalse(state.confirmUnchanged())
        state.adopt(snapshot("digest", "eevee", 55))
        assertTrue(state.confirmUnchanged())
    }

    @Test
    fun `暂停保留缓存而失效清空缓存`() {
        val state = RemoteDexSnapshotState()
        state.adopt(snapshot("digest", "eevee", 55))
        state.markAccepted()
        state.suspend()
        assertFalse(state.isReady())
        assertEquals("digest", state.cachedDigest())

        state.invalidate()
        assertFalse(state.isReady())
        assertNull(state.cachedDigest())
        assertEquals(0, state.size())
    }

    @Test
    fun `磁盘文档按固定六项能力值输出`() {
        val state = RemoteDexSnapshotState()
        state.adopt(snapshot("digest", "eevee", 55))
        val encoded = state.serializableDocument()
        val species = encoded?.getAsJsonArray("species")?.single()?.asJsonObject

        assertEquals("digest", encoded?.get("digest")?.asString)
        assertEquals("eevee", species?.get("id")?.asString)
        assertEquals(setOf("hp", "atk", "def", "spa", "spd", "spe"), species?.getAsJsonObject("baseStats")?.keySet())
    }

    @Test
    fun `远端图鉴快照写入磁盘后可恢复并确认`() {
        val cacheFile = Path.of("config", "cobblebattle-dex.json")
        Files.deleteIfExists(cacheFile)
        try {
            RemoteDex().accept(snapshot("persisted-digest", "eevee", 55))
            assertTrue(Files.isRegularFile(cacheFile))

            val restored = RemoteDex()
            restored.loadFromDisk()
            assertFalse(restored.isReady())
            assertEquals("persisted-digest", restored.cachedDigest())
            assertEquals(1, restored.size())

            restored.accept(JsonObject().apply { addProperty("unchanged", true) })
            assertTrue(restored.isReady())
        } finally {
            Files.deleteIfExists(cacheFile)
        }
    }

    private fun snapshot(digest: String, speciesId: String, hp: Int): JsonObject {
        val stats = JsonObject().apply {
            addProperty("hp", hp)
            addProperty("atk", 1)
            addProperty("def", 2)
            addProperty("spa", 3)
            addProperty("spd", 4)
            addProperty("spe", 5)
        }
        return JsonObject().apply {
            addProperty("digest", digest)
            add("species", JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("id", speciesId)
                    add("baseStats", stats)
                })
            })
        }
    }
}
