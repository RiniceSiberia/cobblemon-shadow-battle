package xiaocaoawa.minecraft.mod.cobblebattle

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import xiaocaoawa.minecraft.mod.cobblebattle.battle.CrossServerBattles
import xiaocaoawa.minecraft.mod.cobblebattle.battle.MirrorBattle
import java.util.UUID

class BattleProjectionContractTest {
    private val participant = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val secondParticipant = UUID.fromString("00000000-0000-0000-0000-000000000002")
    private val engineId = UUID.fromString("00000000-0000-0000-0000-000000000003")

    @AfterEach
    fun clear() { CrossServerBattles.clear(); CrossServerBattles.endConstruction(); CrossServerBattles.setChoiceRelay {}; CrossServerBattles.setOutputRelay {} }

    @Test
    fun `单端对战只解释本地座位输出双端对战解释所有输出`() {
        val remote = battle(false)
        assertEquals(MirrorBattle.Routing(true, false), remote.route("sideupdate\np2\n|request|{}"))
        assertEquals(MirrorBattle.Routing(false, true), remote.route("sideupdate\np1\n|request|{}"))
        assertEquals(MirrorBattle.Routing(true, true), remote.route("update\n|turn|1"))
        val local = MirrorBattle("remote", "p1", "p2", participant, secondParticipant, "opponent", "server", true, false)
        assertEquals(MirrorBattle.Routing(true, true), local.route("sideupdate\np2\n|request|{}"))
        assertEquals(listOf(participant, secondParticipant), local.localPlayers())
        assertEquals("p2", local.seatOf(secondParticipant))
        assertNull(local.seatOf(null))
        assertFalse(local.hasLocalPlayer(engineId))
    }

    @Test
    fun `普通对战乱序缓存上限六十四观战上限八千一百九十二`() {
        val normal = battle(false)
        repeat(64) { normal.accept(it + 2L, "chunk") }
        assertFalse(normal.isFinished)
        assertTrue(normal.hasStalledChunks())
        normal.accept(66, "overflow")
        assertTrue(normal.isFinished)
        assertFalse(normal.hasStalledChunks())
        val replay = MirrorBattle.spectator("replay", false)
        repeat(8192) { replay.accept(it + 2L, "chunk") }
        assertFalse(replay.isFinished)
        replay.accept(8194, "overflow")
        assertTrue(replay.isFinished)
    }

    @Test
    fun `等待释放的乱序数据按连续序号清空且重复数据被忽略`() {
        val projection = battle(false)
        projection.accept(2, "second")
        projection.accept(1, "first")
        assertTrue(projection.hasStalledChunks())
        projection.release()
        assertFalse(projection.hasStalledChunks())
        projection.accept(1, "duplicate")
        assertFalse(projection.hasStalledChunks())
        projection.accept(4, "waiting")
        projection.markFinished()
        projection.accept(3, "late")
        assertFalse(projection.hasStalledChunks())
    }

    @Test
    fun `镜像注册索引中继清理后抑制延迟选择`() {
        val projection = battle(false)
        val choices = mutableListOf<String>()
        CrossServerBattles.setChoiceRelay { choices += it.line() }
        CrossServerBattles.beginConstruction(projection)
        assertTrue(CrossServerBattles.claimStart(engineId))
        CrossServerBattles.endConstruction()
        assertSame(projection, CrossServerBattles.byRemoteId("remote"))
        assertSame(projection, CrossServerBattles.byLocalPlayer(participant))
        assertTrue(CrossServerBattles.relayChoices(engineId, arrayOf(null, " ", ">p1 move 1")))
        assertEquals(listOf(">p1 move 1"), choices)
        assertSame(projection, CrossServerBattles.forget(engineId))
        assertTrue(CrossServerBattles.relayChoices(engineId, arrayOf("late")))
        assertEquals(0, CrossServerBattles.size())
    }

    @Test
    fun `权威对战只中继强制结束且注入期间不重复中继`() {
        val projection = battle(true)
        val choices = mutableListOf<String>()
        CrossServerBattles.setChoiceRelay { choices += it.line() }
        CrossServerBattles.beginConstruction(projection)
        assertFalse(CrossServerBattles.claimStart(engineId))
        CrossServerBattles.endConstruction()
        assertFalse(CrossServerBattles.relayChoices(engineId, arrayOf(">p1 move 1", ">forcelose p1")))
        assertEquals(listOf(">forcelose p1"), choices)
        CrossServerBattles.injecting { assertFalse(CrossServerBattles.relayChoices(engineId, arrayOf(">forcetie"))) }
        assertEquals(1, choices.size)
    }

    private fun battle(authoritative: Boolean) = MirrorBattle("remote", "p1", "p2", participant, "opponent", "server", authoritative, false)
}
