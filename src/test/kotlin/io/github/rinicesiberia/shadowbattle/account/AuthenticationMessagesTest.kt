package io.github.rinicesiberia.shadowbattle.account

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import xiaocaoawa.minecraft.mod.cobblebattle.account.AuthMode
import java.util.UUID

class AuthenticationMessagesTest {
    private val participant = UUID.fromString("00000000-0000-0000-0000-000000000001")

    @Test
    fun `登录注册规范化标识与验证码但不修改密码`() {
        for (operation in listOf(AuthMode.LOGIN, AuthMode.REGISTER)) {
            val request = AuthenticationMessages.create(operation, 7, " account ", " a@b.c ", " secret ", " 123 ", participant, "玩家")
            assertEquals(if (operation == AuthMode.LOGIN) "account_login" else "account_register", request["t"].asString)
            assertEquals(7, request["ref"].asInt)
            assertEquals("account", request["accountId"].asString)
            assertEquals("a@b.c", request["email"].asString)
            assertEquals(" secret ", request["password"].asString)
            assertEquals("123", request["verificationCode"].asString)
            assertEquals(participant.toString(), request.getAsJsonObject("player")["uuid"].asString)
        }
    }

    @Test
    fun `绑定邮箱保留原始输入验证码请求不携带玩家信息`() {
        val binding = AuthenticationMessages.create(AuthMode.BIND_EMAIL, 1, null, " mail ", null, " code ", participant, "玩家")
        assertTrue(binding["accountId"].isJsonNull)
        assertEquals(" mail ", binding["email"].asString)
        assertEquals(" code ", binding["verificationCode"].asString)
        for (account in listOf("__bind", "normal")) {
            val request = AuthenticationMessages.create(AuthMode.CODE_REQUEST, 2, account, " mail ", null, null, participant, "玩家")
            assertEquals(if (account == "__bind") "bind" else "register", request["purpose"].asString)
            assertEquals("mail", request["email"].asString)
            assertFalse(request.has("player"))
            assertFalse(request.has("password"))
        }
    }

    @Test
    fun `输入校验遵守模式优先级和 Java 空白定义`() {
        assertNull(AuthenticationMessages.validationFailure(AuthMode.BIND_EMAIL, null, null, true))
        assertEquals("auth.need_email", AuthenticationMessages.validationFailure(AuthMode.CODE_REQUEST, "\t", "", false))
        assertEquals("auth.need_email", AuthenticationMessages.validationFailure(AuthMode.REGISTER, null, null, true))
        assertEquals("auth.need_password", AuthenticationMessages.validationFailure(AuthMode.REGISTER, null, null, false))
        assertNull(AuthenticationMessages.validationFailure(AuthMode.LOGIN, null, " ", true))
        assertNull(AuthenticationMessages.validationFailure(AuthMode.CODE_REQUEST, "\u00a0", null, true))
    }
}
