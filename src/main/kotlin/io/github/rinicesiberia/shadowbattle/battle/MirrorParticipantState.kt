package io.github.rinicesiberia.shadowbattle.battle

import xiaocaoawa.minecraft.mod.cobblebattle.api.BattleInfo
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** 保存镜像对战的参与者、座位、观战者及公开业务描述。 */
class MirrorParticipantState(
    private val upstreamBattleId: String,
    private val primarySeat: String?,
    private val secondarySeat: String?,
    private val primaryParticipant: UUID?,
    private val secondaryParticipant: UUID?,
    private val remoteName: String,
    private val remoteServer: String,
    private val authoritative: Boolean,
    private val spectator: Boolean
) {
    private val observers = ConcurrentHashMap.newKeySet<UUID>()
    @Volatile private var primaryDescription: BattleInfo? = null
    @Volatile private var secondaryDescription: BattleInfo? = null

    fun upstreamBattleId(): String = upstreamBattleId
    fun primarySeat(): String? = primarySeat
    fun secondarySeat(): String? = secondarySeat
    fun isAuthoritative(): Boolean = authoritative
    fun isSpectator(): Boolean = spectator
    fun remoteName(): String = remoteName
    fun remoteServer(): String = remoteServer
    fun primaryParticipant(): UUID? = primaryParticipant

    fun addObserver(participant: UUID) { observers.add(participant) }
    fun removeObserver(participant: UUID): Boolean { observers.remove(participant); return observers.isEmpty() }
    fun observerSnapshot(): Set<UUID> = java.util.Set.copyOf(observers)

    fun hasTwoLocalParticipants(): Boolean = secondaryParticipant != null

    fun localParticipants(): List<UUID> = when {
        primaryParticipant == null -> java.util.List.of()
        secondaryParticipant == null -> java.util.List.of(primaryParticipant)
        else -> java.util.List.of(primaryParticipant, secondaryParticipant)
    }

    fun includes(participant: UUID?): Boolean = participant != null &&
        (participant == primaryParticipant || participant == secondaryParticipant)

    fun seatOf(participant: UUID?): String? = when (participant) {
        null -> null
        primaryParticipant -> primarySeat
        secondaryParticipant -> secondarySeat
        else -> null
    }

    fun describePrimary(description: BattleInfo) { primaryDescription = description }
    fun describeSecondary(description: BattleInfo) { secondaryDescription = description }
    fun primaryDescription(): BattleInfo? = primaryDescription
    fun descriptionFor(participant: UUID?): BattleInfo? = when (participant) {
        null -> null
        primaryParticipant -> primaryDescription
        secondaryParticipant -> secondaryDescription
        else -> null
    }
}
