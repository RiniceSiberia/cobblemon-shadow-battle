package io.github.rinicesiberia.shadowbattle.account

import com.google.gson.JsonObject
import io.github.rinicesiberia.shadowbattle.transport.MessageFields
import xiaocaoawa.minecraft.mod.cobblebattle.account.AuthMode
import java.util.UUID

/** 验证认证输入并按对战服务协议生成请求。 */
object AuthenticationMessages {
    fun validationFailure(operation: AuthMode, emailAddress: String?, secret: String?, emailRequired: Boolean): String? = when {
        operation.isBindEmail -> null
        operation.isCodeRequest -> if (hasNoText(emailAddress)) "auth.need_email" else null
        operation.isRegister && emailRequired && hasNoText(emailAddress) -> "auth.need_email"
        secret.isNullOrEmpty() -> "auth.need_password"
        else -> null
    }

    fun create(
        operation: AuthMode, reference: Int, accountKey: String?, emailAddress: String?, secret: String?,
        confirmationCode: String?, participant: UUID, profileName: String
    ): JsonObject {
        val kind = when {
            operation.isBindEmail -> "account_bind_email"
            operation.isCodeRequest -> "account_code"
            operation.isRegister -> "account_register"
            else -> "account_login"
        }
        return MessageFields.envelope(kind).apply {
            addProperty("ref", reference)
            when {
                operation.isBindEmail -> {
                    addProperty("accountId", accountKey)
                    addProperty("email", emailAddress)
                    addProperty("password", secret)
                    addProperty("verificationCode", confirmationCode)
                }
                operation.isCodeRequest -> {
                    addProperty("email", emailAddress?.trim { it <= ' ' })
                    addProperty("purpose", if (accountKey == "__bind") "bind" else "register")
                }
                else -> {
                    addProperty("accountId", accountKey?.trim { it <= ' ' } ?: "")
                    addProperty("email", emailAddress?.trim { it <= ' ' } ?: "")
                    addProperty("password", secret)
                    addProperty("verificationCode", confirmationCode?.trim { it <= ' ' } ?: "")
                }
            }
            if (!operation.isCodeRequest) {
                add("player", JsonObject().apply {
                    addProperty("uuid", participant.toString())
                    addProperty("name", profileName)
                })
            }
        }
    }

    private fun hasNoText(value: String?): Boolean = value == null || value.codePoints().allMatch(Character::isWhitespace)
}
