package io.github.rinicesiberia.shadowbattle.battle

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ServiceErrorRulesTest {
    @Test
    fun `认证错误码保持原翻译键`() {
        val expected = linkedMapOf(
            "BAD_CREDENTIALS" to "auth.err.bad_credentials",
            "BAD_EMAIL" to "auth.err.bad_email",
            "BAD_CODE" to "auth.err.bad_code",
            "EMAIL_EXISTS" to "auth.err.email_taken",
            "CODE_TOO_SOON" to "auth.err.code_too_soon",
            "SMTP_FAILED" to "auth.err.smtp_failed",
            "BAD_ACCOUNT_ID" to "auth.err.bad_id",
            "ACCOUNT_EXISTS" to "auth.err.taken",
            "BAD_PASSWORD" to "auth.err.bad_password",
            "ACCOUNT_IN_USE" to "auth.err.in_use",
            "NOT_ACCOUNT_OWNER" to "auth.err.not_owner",
            "TOO_MANY_ATTEMPTS" to "auth.err.too_many"
        )
        expected.forEach { (code, key) -> assertEquals(key, AuthenticationErrorRules.translationKey(code), code) }
        assertNull(AuthenticationErrorRules.translationKey("NOT_LOGGED_IN"))
        assertNull(AuthenticationErrorRules.translationKey(null))
    }

    @Test
    fun `聊天错误码保持原翻译键且不接收认证错误`() {
        assertEquals("chat.err.too_fast", ChatErrorRules.translationKey("CHAT_TOO_FAST"))
        assertEquals("chat.err.not_in_battle", ChatErrorRules.translationKey("NOT_IN_BATTLE"))
        assertEquals("chat.err.disabled", ChatErrorRules.translationKey("CHAT_DISABLED"))
        assertEquals("queue.not_signed_in", ChatErrorRules.translationKey("NOT_LOGGED_IN"))
        assertNull(ChatErrorRules.translationKey("BAD_CREDENTIALS"))
        assertNull(ChatErrorRules.translationKey(null))
    }
}
