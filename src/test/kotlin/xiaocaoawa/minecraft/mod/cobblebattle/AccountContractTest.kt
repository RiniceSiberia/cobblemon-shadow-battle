package xiaocaoawa.minecraft.mod.cobblebattle

import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import xiaocaoawa.minecraft.mod.cobblebattle.account.AuthService
import java.util.UUID

class AccountContractTest {
    private val playerId = UUID.fromString("00000000-0000-0000-0000-000000000001")

    @Test
    fun `登录响应建立会话注销本地会话及清空状态`() {
        val accounts = AuthService()
        assertNull(accounts.accountOf(playerId))
        assertNull(accounts.nicknameOf(playerId))
        assertEquals(0L, accounts.uidOf(playerId))
        val reply = JsonObject().apply {
            addProperty("player", playerId.toString())
            addProperty("accountId", "account")
            addProperty("nickname", "玩家")
            addProperty("uid", 123L)
            addProperty("registered", true)
        }
        val session = requireNotNull(accounts.onAccountOk(reply))
        assertTrue(session.registered())
        assertEquals(playerId, session.playerUuid())
        assertEquals("account", accounts.accountOf(playerId))
        assertEquals("玩家", accounts.nicknameOf(playerId))
        assertEquals(123L, accounts.uidOf(playerId))
        assertTrue(accounts.isSignedIn(playerId))
        val snapshot = accounts.signedInPlayers()
        accounts.forget(playerId)
        assertFalse(accounts.isSignedIn(playerId))
        assertEquals(setOf(playerId), snapshot)
        accounts.onAccountOk(reply)
        accounts.clear()
        assertTrue(accounts.signedInPlayers().isEmpty())
    }

    @Test
    fun `无有效玩家的响应不建立会话未知引用不被认领`() {
        val accounts = AuthService()
        for (invalid in listOf("", "not-a-uuid")) {
            val reply = JsonObject().apply { addProperty("player", invalid); addProperty("ref", 99) }
            assertNull(accounts.onAccountOk(reply))
            assertNull(accounts.onAccountError(reply))
            assertNull(accounts.onAccountCodeOk(reply))
            assertFalse(accounts.isAwaiting(reply))
        }
        assertTrue(accounts.signedInPlayers().isEmpty())
    }
}
