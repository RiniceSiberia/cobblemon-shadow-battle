package io.github.rinicesiberia.shadowbattle.account

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import xiaocaoawa.minecraft.mod.cobblebattle.account.AuthMode
import xiaocaoawa.minecraft.mod.cobblebattle.account.AuthService
import xiaocaoawa.minecraft.mod.cobblebattle.lang.Msg
import xiaocaoawa.minecraft.mod.cobblebattle.net.BattleServerClient
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** 跟踪玩家账户会话和等待对战服务确认的认证请求。 */
class AccountSessions {
    private val authenticatedPlayers = ConcurrentHashMap<UUID, VerifiedAccount>()
    private val pendingRequests = ConcurrentHashMap<Int, PendingAuthentication>()

    fun accountKey(participant: UUID): String? = authenticatedPlayers[participant]?.accountKey
    fun displayLabel(participant: UUID): String? = authenticatedPlayers[participant]?.displayLabel
    fun numericIdentity(participant: UUID): Long = authenticatedPlayers[participant]?.numericIdentity ?: 0L
    fun containsParticipant(participant: UUID): Boolean = authenticatedPlayers.containsKey(participant)
    fun participantSnapshot(): Set<UUID> = java.util.Set.copyOf(authenticatedPlayers.keys)
    fun discardParticipant(participant: UUID) { authenticatedPlayers.remove(participant) }
    fun resetSessions() { authenticatedPlayers.clear(); pendingRequests.clear() }

    fun submitCredentials(
        connection: BattleServerClient?, participant: ServerPlayer, operation: AuthMode,
        accountKey: String?, emailAddress: String?, secret: String?, confirmationCode: String?, emailRequired: Boolean
    ): Component? {
        if (connection == null || !connection.isHandshaken) return failure("auth.not_connected")
        AuthenticationMessages.validationFailure(operation, emailAddress, secret, emailRequired)?.let { return failure(it) }
        val requestId = connection.nextRef()
        val envelope = AuthenticationMessages.create(
            operation, requestId, accountKey, emailAddress, secret, confirmationCode,
            participant.uuid, if (operation.isCodeRequest) "" else participant.gameProfile.name
        )
        pendingRequests[requestId] = PendingAuthentication(participant.uuid, operation)
        if (connection.send(envelope)) return null
        pendingRequests.remove(requestId)
        return failure("auth.send_failed")
    }

    fun signOutParticipant(connection: BattleServerClient?, participant: ServerPlayer) {
        authenticatedPlayers.remove(participant.uuid)
        if (connection != null && connection.isHandshaken) {
            val request = BattleServerClient.msg("account_logout")
            request.add("player", JsonObject().apply { addProperty("uuid", participant.uuid.toString()) })
            connection.send(request)
        }
    }

    fun acceptConfirmation(document: JsonObject): UUID? = consumePending(document["ref"])?.participant
    fun rejectAuthentication(document: JsonObject): UUID? = consumePending(document["ref"])?.participant
    fun hasPendingReference(document: JsonObject): Boolean =
        document["ref"]?.takeUnless { it.isJsonNull }?.let { pendingRequests.containsKey(it.asInt) } ?: false

    fun acceptAuthentication(document: JsonObject): AuthService.Outcome? {
        val parsedIdentity = decodeParticipant(BattleServerClient.str(document, "player", ""))
        val accountKey = BattleServerClient.str(document, "accountId", "")
        val displayLabel = BattleServerClient.str(document, "nickname", "")
        val newlyRegistered = BattleServerClient.bool(document, "registered", false)
        val awaiting = consumePending(document["ref"])
        val participant = parsedIdentity ?: awaiting?.participant ?: return null
        val numericIdentity = document["uid"].asLong
        authenticatedPlayers[participant] = VerifiedAccount(accountKey, displayLabel, numericIdentity)
        return AuthService.Outcome(participant, accountKey, displayLabel, numericIdentity, newlyRegistered)
    }

    private fun consumePending(reference: JsonElement?): PendingAuthentication? =
        reference?.takeUnless { it.isJsonNull }?.let { pendingRequests.remove(it.asInt) }

    private fun decodeParticipant(encoded: String?): UUID? = try {
        if (hasNoText(encoded)) null else UUID.fromString(encoded)
    } catch (_: IllegalArgumentException) { null }

    private fun hasNoText(value: String?): Boolean = value == null || value.codePoints().allMatch(Character::isWhitespace)

    private fun failure(messageKey: String): Component = Msg.of(ChatFormatting.RED, messageKey)
    private data class PendingAuthentication(val participant: UUID, val operation: AuthMode)
    private data class VerifiedAccount(val accountKey: String, val displayLabel: String, val numericIdentity: Long)
}


