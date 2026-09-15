package io.github.rinicesiberia.shadowbattle.client

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import xiaocaoawa.minecraft.mod.cobblebattle.account.AuthMode

class AuthenticationFormRulesTest {
    @Test
    fun `初始模式处理验证码请求和禁用邮箱`() {
        assertEquals(AuthMode.REGISTER, AuthenticationFormRules.initialMode(AuthMode.CODE_REQUEST, true))
        assertEquals(AuthMode.LOGIN, AuthenticationFormRules.initialMode(AuthMode.BIND_EMAIL, false))
        assertEquals(AuthMode.REGISTER, AuthenticationFormRules.initialMode(AuthMode.REGISTER, false))
    }

    @Test
    fun `模式循环保持邮箱开关对应顺序`() {
        assertEquals(AuthMode.REGISTER, AuthenticationFormRules.nextMode(AuthMode.LOGIN, false))
        assertEquals(AuthMode.LOGIN, AuthenticationFormRules.nextMode(AuthMode.REGISTER, false))
        assertEquals(AuthMode.REGISTER, AuthenticationFormRules.nextMode(AuthMode.LOGIN, true))
        assertEquals(AuthMode.BIND_EMAIL, AuthenticationFormRules.nextMode(AuthMode.REGISTER, true))
        assertEquals(AuthMode.LOGIN, AuthenticationFormRules.nextMode(AuthMode.BIND_EMAIL, true))
    }

    @Test
    fun `标题提交和切换翻译键保持模式含义`() {
        assertEquals("cobblebattle.auth.title.login", AuthenticationFormRules.titleKey(AuthMode.LOGIN))
        assertEquals("cobblebattle.auth.title.register", AuthenticationFormRules.titleKey(AuthMode.REGISTER))
        assertEquals("cobblebattle.auth.title.bind", AuthenticationFormRules.titleKey(AuthMode.BIND_EMAIL))
        assertEquals("cobblebattle.auth.submit.login", AuthenticationFormRules.submitKey(AuthMode.LOGIN))
        assertEquals("cobblebattle.auth.submit.register", AuthenticationFormRules.submitKey(AuthMode.REGISTER))
        assertEquals("cobblebattle.auth.submit.bind", AuthenticationFormRules.submitKey(AuthMode.BIND_EMAIL))
        assertEquals("cobblebattle.auth.switch.register", AuthenticationFormRules.switchKey(AuthMode.LOGIN, true))
        assertEquals("cobblebattle.auth.switch.bind", AuthenticationFormRules.switchKey(AuthMode.REGISTER, true))
        assertEquals("cobblebattle.auth.switch.login", AuthenticationFormRules.switchKey(AuthMode.BIND_EMAIL, true))
    }

    @Test
    fun `提交按钮按模式要求字段并允许空白密码`() {
        assertTrue(AuthenticationFormRules.canSubmit(AuthMode.LOGIN, true, false, "id", "", "", " "))
        assertFalse(AuthenticationFormRules.canSubmit(AuthMode.LOGIN, true, false, " ", "", "", "pw"))
        assertFalse(AuthenticationFormRules.canSubmit(AuthMode.LOGIN, true, true, "id", "", "", "pw"))
        assertTrue(AuthenticationFormRules.canSubmit(AuthMode.REGISTER, false, false, "id", "", "", "pw"))
        assertFalse(AuthenticationFormRules.canSubmit(AuthMode.REGISTER, true, false, "", "mail", "", "pw"))
        assertTrue(AuthenticationFormRules.canSubmit(AuthMode.REGISTER, true, false, "", "mail", "1", "pw"))
        assertFalse(AuthenticationFormRules.canSubmit(AuthMode.BIND_EMAIL, true, false, "", "mail", "1", "pw"))
        assertTrue(AuthenticationFormRules.canSubmit(AuthMode.BIND_EMAIL, true, false, "id", "mail", "1", "pw"))
    }

    @Test
    fun `验证码按钮和倒计时保持边界`() {
        assertTrue(AuthenticationFormRules.canRequestCode(false, 0, "mail"))
        assertFalse(AuthenticationFormRules.canRequestCode(true, 0, "mail"))
        assertFalse(AuthenticationFormRules.canRequestCode(false, 1, "mail"))
        assertFalse(AuthenticationFormRules.canRequestCode(false, 0, "\u2003"))
        assertEquals(1, AuthenticationFormRules.cooldownSeconds(1))
        assertEquals(1, AuthenticationFormRules.cooldownSeconds(20))
        assertEquals(2, AuthenticationFormRules.cooldownSeconds(21))
    }

    @Test
    fun `验证码请求区分绑定模式并沿用Java裁剪`() {
        val regular = AuthenticationFormRules.codeRequest(AuthMode.REGISTER, " mail ")
        assertEquals(AuthMode.CODE_REQUEST, regular.mode)
        assertEquals("", regular.accountId)
        assertEquals("mail", regular.email)
        val binding = AuthenticationFormRules.codeRequest(AuthMode.BIND_EMAIL, "\u00A0mail\u00A0")
        assertEquals("__bind", binding.accountId)
        assertEquals("\u00A0mail\u00A0", binding.email)
    }

    @Test
    fun `注册确认失败不生成请求且普通提交只裁剪标识字段`() {
        assertNull(AuthenticationFormRules.submission(AuthMode.REGISTER, "id", "mail", "one", "two", "1"))
        val request = AuthenticationFormRules.submission(AuthMode.LOGIN, " id ", " mail ", " pass ", "ignored", " 1 ")
        assertEquals(AuthMode.LOGIN, request?.mode)
        assertEquals("id", request?.accountId)
        assertEquals("mail", request?.email)
        assertEquals(" pass ", request?.password)
        assertEquals("1", request?.code)
    }
}
