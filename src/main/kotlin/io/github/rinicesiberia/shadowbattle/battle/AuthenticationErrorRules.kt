package io.github.rinicesiberia.shadowbattle.battle

/** 将账户服务错误码映射为认证界面翻译键。 */
object AuthenticationErrorRules {
    @JvmStatic
    fun translationKey(code: String?): String? = when (code.orEmpty()) {
        "BAD_CREDENTIALS" -> "auth.err.bad_credentials"
        "BAD_EMAIL" -> "auth.err.bad_email"
        "BAD_CODE" -> "auth.err.bad_code"
        "EMAIL_EXISTS" -> "auth.err.email_taken"
        "CODE_TOO_SOON" -> "auth.err.code_too_soon"
        "SMTP_FAILED" -> "auth.err.smtp_failed"
        "BAD_ACCOUNT_ID" -> "auth.err.bad_id"
        "ACCOUNT_EXISTS" -> "auth.err.taken"
        "BAD_PASSWORD" -> "auth.err.bad_password"
        "ACCOUNT_IN_USE" -> "auth.err.in_use"
        "NOT_ACCOUNT_OWNER" -> "auth.err.not_owner"
        "TOO_MANY_ATTEMPTS" -> "auth.err.too_many"
        else -> null
    }
}
