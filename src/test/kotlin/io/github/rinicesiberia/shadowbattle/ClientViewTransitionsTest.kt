package io.github.rinicesiberia.shadowbattle

import io.github.rinicesiberia.shadowbattle.client.ClientViewTransitions
import io.github.rinicesiberia.shadowbattle.client.ViewTransition
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ClientViewTransitionsTest {
    @Test
    fun `房间列表在大厅已打开时原地刷新`() {
        assertEquals(ViewTransition.REFRESH, ClientViewTransitions.roomList(true, true, false, false))
        assertEquals(ViewTransition.REFRESH, ClientViewTransitions.roomList(true, true, true, true))
    }

    @Test
    fun `非刷新列表只在空闲时打开大厅`() {
        assertEquals(ViewTransition.OPEN, ClientViewTransitions.roomList(true, false, false, false))
        assertEquals(ViewTransition.IGNORE, ClientViewTransitions.roomList(true, false, true, false))
        assertEquals(ViewTransition.IGNORE, ClientViewTransitions.roomList(true, false, false, true))
    }

    @Test
    fun `房间状态优先更新同一房间`() {
        assertEquals(ViewTransition.REFRESH, ClientViewTransitions.roomState(true, "room-a", "room-a", true))
        assertEquals(ViewTransition.OPEN, ClientViewTransitions.roomState(true, "room-a", "room-b", false))
        assertEquals(ViewTransition.IGNORE, ClientViewTransitions.roomState(true, "room-a", "room-b", true))
    }

    @Test
    fun `队伍预览关闭消息仍能更新已打开界面`() {
        assertEquals(ViewTransition.REFRESH, ClientViewTransitions.teamPreview(true, "battle-a", "battle-a", "closed"))
        assertEquals(ViewTransition.OPEN, ClientViewTransitions.teamPreview(true, null, "battle-a", ""))
        assertEquals(ViewTransition.IGNORE, ClientViewTransitions.teamPreview(true, null, "battle-a", "closed"))
    }

    @Test
    fun `没有本地玩家时忽略所有界面消息`() {
        assertEquals(ViewTransition.IGNORE, ClientViewTransitions.roomList(false, true, false, false))
        assertEquals(ViewTransition.IGNORE, ClientViewTransitions.roomState(false, "room-a", "room-a", false))
        assertEquals(ViewTransition.IGNORE, ClientViewTransitions.teamPreview(false, "battle-a", "battle-a", ""))
    }
}
