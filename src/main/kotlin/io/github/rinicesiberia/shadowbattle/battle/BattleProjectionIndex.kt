package io.github.rinicesiberia.shadowbattle.battle

import xiaocaoawa.minecraft.mod.cobblebattle.battle.CrossServerBattles.ChoiceRelay
import xiaocaoawa.minecraft.mod.cobblebattle.battle.MirrorBattle
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.function.LongSupplier

/** 索引本地与远端对战，并隔离注入期间的选择中继。 */
class BattleProjectionIndex(
    private val bindEngine: BiConsumer<MirrorBattle, UUID>,
    private val clock: LongSupplier
) {
    private val projections = ConcurrentHashMap<UUID, MirrorBattle>()
    private val upstreamBindings = ConcurrentHashMap<String, UUID>()
    private val retiredMirrors = ConcurrentHashMap<UUID, Long>()
    private val constructionContext = ThreadLocal<MirrorBattle>()
    private val injectionContext = ThreadLocal.withInitial { false }
    @Volatile private var choicePublisher: Consumer<ChoiceRelay> = Consumer { }
    @Volatile private var outputPublisher: Consumer<ChoiceRelay> = Consumer { }

    fun withInjectedInput(operation: Runnable) {
        injectionContext.set(true)
        try { operation.run() } finally { injectionContext.remove() }
    }
    fun observeChoices(observer: Consumer<ChoiceRelay>) { choicePublisher = observer }
    fun observeOutput(observer: Consumer<ChoiceRelay>) { outputPublisher = observer }
    fun prepareConstruction(projection: MirrorBattle) { constructionContext.set(projection) }
    fun completeConstruction() { constructionContext.remove() }

    fun bindConstructedBattle(engineBattleId: UUID): Boolean {
        val projection = constructionContext.get() ?: return false
        bindEngine.accept(projection, engineBattleId)
        projections[engineBattleId] = projection
        upstreamBindings[projection.remoteBattleId()] = engineBattleId
        return !projection.isAuthoritative
    }
    fun containsEngineBattle(engineBattleId: UUID) = projections.containsKey(engineBattleId)
    fun findByEngine(engineBattleId: UUID): MirrorBattle? = projections[engineBattleId]
    fun resolveEngineId(upstreamBattleId: String): UUID? = upstreamBindings[upstreamBattleId]
    fun findByUpstream(upstreamBattleId: String): MirrorBattle? = upstreamBindings[upstreamBattleId]?.let(projections::get)
    fun findByParticipant(participant: UUID?): MirrorBattle? = projections.values.firstOrNull { it.hasLocalPlayer(participant) }
    fun snapshot(): Collection<MirrorBattle> = java.util.List.copyOf(projections.values)
    fun activeCount() = projections.size
    fun clearSessions() { projections.clear(); upstreamBindings.clear(); retiredMirrors.clear() }

    fun forwardChoices(engineBattleId: UUID, commands: Array<out String?>): Boolean {
        val projection = projections[engineBattleId] ?: return wasRetiredMirror(engineBattleId)
        if (!projection.isAuthoritative) {
            if (!projection.isFinished) {
                for (command in commands) {
                    if (command != null && !isWhitespace(command)) choicePublisher.accept(ChoiceRelay(projection.remoteBattleId(), command))
                }
            }
            return true
        }
        if (injectionContext.get()) return false
        for (command in commands) {
            if (command == null || isWhitespace(command)) continue
            val instruction = (if (command.startsWith(">")) command.substring(1) else command).trim { it <= ' ' }
            if (instruction.startsWith("forcelose") || instruction.startsWith("forcetie")) {
                choicePublisher.accept(ChoiceRelay(projection.remoteBattleId(), command))
            }
        }
        return false
    }

    fun forwardOutput(engineBattleId: UUID, content: String?): Boolean {
        val projection = projections[engineBattleId]
        if (projection == null || !projection.isAuthoritative || content.isNullOrEmpty()) return false
        val destination = projection.route(content)
        if (destination.sendOn()) outputPublisher.accept(ChoiceRelay(projection.remoteBattleId(), content))
        return !destination.interpretOn()
    }

    fun removeBattle(engineBattleId: UUID): MirrorBattle? {
        val projection = projections.remove(engineBattleId) ?: return null
        upstreamBindings.remove(projection.remoteBattleId())
        if (!projection.isAuthoritative) {
            val removedAt = clock.asLong
            retiredMirrors.values.removeIf { removedAt - it > RETIREMENT_WINDOW_MS }
            retiredMirrors[engineBattleId] = removedAt
        }
        return projection
    }

    private fun wasRetiredMirror(engineBattleId: UUID): Boolean {
        val removedAt = retiredMirrors[engineBattleId] ?: return false
        if (clock.asLong - removedAt > RETIREMENT_WINDOW_MS) {
            retiredMirrors.remove(engineBattleId)
            return false
        }
        return true
    }
    private fun isWhitespace(value: String) = value.codePoints().allMatch(Character::isWhitespace)
    private companion object { const val RETIREMENT_WINDOW_MS = 120000L }
}

