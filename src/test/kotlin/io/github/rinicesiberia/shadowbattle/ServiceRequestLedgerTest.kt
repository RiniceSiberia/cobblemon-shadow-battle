package io.github.rinicesiberia.shadowbattle

import io.github.rinicesiberia.shadowbattle.battle.ServiceRequestLedger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.UUID

class ServiceRequestLedgerTest {
    private val participant = UUID.fromString("00000000-0000-0000-0000-000000000001")

    @Test
    fun `请求通道使用相同引用时仍可独立领取`() {
        val ledger = ServiceRequestLedger()
        ledger.bindMenu(7, participant)
        ledger.bindLeaderboard(7, participant)
        ledger.bindChat(7, participant)

        assertEquals(participant, ledger.claimMenu(7))
        assertEquals(participant, ledger.claimLeaderboard(7))
        assertEquals(participant, ledger.claimChat(7))
        assertNull(ledger.claimMenu(7))
        assertNull(ledger.claimLeaderboard(7))
        assertNull(ledger.claimChat(7))
    }

    @Test
    fun `排行榜和聊天只保留最近六十四个引用`() {
        val ledger = ServiceRequestLedger()
        for (reference in 1..65) {
            ledger.bindLeaderboard(reference, participant)
            ledger.bindChat(reference, participant)
        }

        assertNull(ledger.claimLeaderboard(1))
        assertNull(ledger.claimChat(1))
        assertEquals(participant, ledger.claimLeaderboard(2))
        assertEquals(participant, ledger.claimChat(2))
    }

    @Test
    fun `发送失败可撤回菜单和排行榜引用`() {
        val ledger = ServiceRequestLedger()
        ledger.bindMenu(3, participant)
        ledger.bindLeaderboard(4, participant)
        ledger.removeMenu(3)
        ledger.removeLeaderboard(4)

        assertNull(ledger.claimMenu(3))
        assertNull(ledger.claimLeaderboard(4))
    }
}
