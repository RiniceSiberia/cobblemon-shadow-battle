package io.github.rinicesiberia.shadowbattle.client

import xiaocaoawa.minecraft.mod.cobblebattle.account.AuthMode

/** 认证界面的模式、字段可用性与提交参数规则。 */
object AuthenticationFormRules {
    @JvmStatic
    fun initialMode(requested: AuthMode, emailEnabled: Boolean): AuthMode {
        val connectedMode = if (requested == AuthMode.CODE_REQUEST) AuthMode.REGISTER else requested
        return if (!emailEnabled && connectedMode.isBindEmail) AuthMode.LOGIN else connectedMode
    }

    @JvmStatic
    fun titleKey(mode: AuthMode): String = when {
        mode.isBindEmail -> "cobblebattle.auth.title.bind"
        mode.isRegister -> "cobblebattle.auth.title.register"
        else -> "cobblebattle.auth.title.login"
    }

    @JvmStatic
    fun submitKey(mode: AuthMode): String = when {
        mode.isBindEmail -> "cobblebattle.auth.submit.bind"
        mode.isRegister -> "cobblebattle.auth.submit.register"
        else -> "cobblebattle.auth.submit.login"
    }

    @JvmStatic
    fun switchKey(mode: AuthMode, emailEnabled: Boolean): String = when {
        !emailEnabled && mode.isRegister -> "cobblebattle.auth.switch.login"
        !emailEnabled -> "cobblebattle.auth.switch.register"
        mode.isBindEmail -> "cobblebattle.auth.switch.login"
        mode.isRegister -> "cobblebattle.auth.switch.bind"
        else -> "cobblebattle.auth.switch.register"
    }

    @JvmStatic
    fun requiresEmail(mode: AuthMode, emailEnabled: Boolean): Boolean =
        emailEnabled && (mode.isRegister || mode.isBindEmail)

    @JvmStatic
    fun nextMode(mode: AuthMode, emailEnabled: Boolean): AuthMode = when {
        !emailEnabled && mode.isRegister -> AuthMode.LOGIN
        !emailEnabled -> AuthMode.REGISTER
        mode == AuthMode.LOGIN -> AuthMode.REGISTER
        mode.isRegister -> AuthMode.BIND_EMAIL
        else -> AuthMode.LOGIN
    }

    @JvmStatic
    fun canSubmit(
        mode: AuthMode,
        emailEnabled: Boolean,
        waiting: Boolean,
        accountId: String,
        email: String,
        code: String,
        password: String,
    ): Boolean {
        val needsEmail = requiresEmail(mode, emailEnabled)
        val primaryPresent = if (needsEmail) !isJavaBlank(email) else !isJavaBlank(accountId)
        val identityPresent = primaryPresent && (!mode.isBindEmail || !isJavaBlank(accountId))
        val codePresent = !needsEmail || !isJavaBlank(code)
        return !waiting && identityPresent && codePresent && password.isNotEmpty()
    }

    @JvmStatic
    fun canRequestCode(waiting: Boolean, cooldownTicks: Int, email: String): Boolean =
        !waiting && cooldownTicks <= 0 && !isJavaBlank(email)

    @JvmStatic
    fun cooldownSeconds(cooldownTicks: Int): Int = (cooldownTicks + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND

    @JvmStatic
    fun codeRequest(mode: AuthMode, email: String): AuthenticationSubmission = AuthenticationSubmission(
        AuthMode.CODE_REQUEST,
        if (mode.isBindEmail) BIND_ACCOUNT_MARKER else "",
        trimJava(email),
        "",
        "",
    )

    @JvmStatic
    fun submission(
        mode: AuthMode,
        accountId: String,
        email: String,
        password: String,
        confirmation: String,
        code: String,
    ): AuthenticationSubmission? {
        if (mode.isRegister && password != confirmation) return null
        return AuthenticationSubmission(mode, trimJava(accountId), trimJava(email), password, trimJava(code))
    }

    private fun isJavaBlank(value: String): Boolean = value.codePoints().allMatch(Character::isWhitespace)

    private fun trimJava(value: String): String = value.trim { it.code <= SPACE_CODE_POINT }

    private const val TICKS_PER_SECOND = 20
    private const val SPACE_CODE_POINT = 0x20
    private const val BIND_ACCOUNT_MARKER = "__bind"
}

/** 已规范化且可直接发送的认证参数。 */
data class AuthenticationSubmission(
    val mode: AuthMode,
    val accountId: String,
    val email: String,
    val password: String,
    val code: String,
)
