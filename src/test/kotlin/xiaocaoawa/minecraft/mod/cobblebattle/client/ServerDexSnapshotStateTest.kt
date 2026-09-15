package xiaocaoawa.minecraft.mod.cobblebattle.client

import com.cobblemon.mod.common.api.pokemon.stats.Stats
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import xiaocaoawa.minecraft.mod.cobblebattle.network.ServerDexPayload

class ServerDexSnapshotStateTest {
    @Test
    fun `完整快照建立六项能力值索引`() {
        val state = ServerDexSnapshotState()
        val result = state.accept(payload(false, "digest", "pikachu"))
        val stats = state.statsFor("pikachu", "fallback")

        assertEquals(ServerDexAcceptance.USE_NEW_SNAPSHOT, result)
        assertEquals(35, stats?.get(Stats.HP))
        assertEquals(90, stats?.get(Stats.SPEED))
        assertEquals("digest", state.digest())
    }

    @Test
    fun `未变化消息在空缓存或摘要不匹配时请求完整快照`() {
        val state = ServerDexSnapshotState()
        assertEquals(ServerDexAcceptance.RETRY_FULL_SNAPSHOT, state.accept(payload(true, "digest", "unused")))
        assertEquals("", state.digest())

        state.accept(payload(false, "digest-a", "eevee"))
        assertEquals(ServerDexAcceptance.RETRY_FULL_SNAPSHOT, state.accept(payload(true, "digest-b", "unused")))
        assertEquals("", state.digest())
    }

    @Test
    fun `摘要匹配时复用缓存并按形态优先回退物种`() {
        val state = ServerDexSnapshotState()
        state.accept(payload(false, "digest", "eevee"))
        assertEquals(ServerDexAcceptance.USE_CACHED_SNAPSHOT, state.accept(payload(true, "digest", "unused")))
        assertEquals(35, state.statsFor("missing-form", "eevee")?.get(Stats.HP))
        assertNull(state.statsFor("missing-form", "missing-species"))
        assertTrue(state.contains("eevee"))
        assertFalse(state.isEmpty())
    }

    @Test
    fun `最近排行空值归一为空字符串`() {
        val state = ServerDexSnapshotState()
        state.rememberRanked("ranked-a")
        assertEquals("ranked-a", state.rankedId())
        state.rememberRanked(null)
        assertEquals("", state.rankedId())
    }

    private fun payload(unchanged: Boolean, digest: String, speciesId: String) = ServerDexPayload(
        digest,
        unchanged,
        if (unchanged) emptyList() else listOf(ServerDexPayload.Entry(speciesId, 35, 55, 40, 50, 50, 90)),
    )
}
