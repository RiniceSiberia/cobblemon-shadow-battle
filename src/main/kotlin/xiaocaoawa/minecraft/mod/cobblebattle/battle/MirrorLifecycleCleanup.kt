package xiaocaoawa.minecraft.mod.cobblebattle.battle

import com.cobblemon.mod.common.battles.BattleRegistry
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import io.github.rinicesiberia.shadowbattle.battle.MirrorSweepSequence
import io.github.rinicesiberia.shadowbattle.battle.MirrorSweepTarget
import net.minecraft.network.chat.Component
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

/** 负责镜像对战结束后的索引、实体与参与者通知清理。 */
internal class MirrorLifecycleCleanup(
    private val service: CrossServerBattleService,
) {
    fun sweepEntities(mirror: MirrorBattle, delayMs: Long) {
        val bodies = mirror.takeBodies()
        val props = mirror.drainPropEntities()
        MirrorSweepSequence.arrange(bodies, props, delayMs, object : MirrorSweepTarget<MirrorBattle.Body, PokemonEntity> {
            override fun releaseRoster(body: MirrorBattle.Body) {
                body.actor()?.let { MirrorPokemon.release(it.mirrorTeam()) }
            }

            override fun mainThread(): Executor? = service.runningServer()

            override fun discardProp(prop: PokemonEntity) {
                if (!prop.isRemoved) prop.discard()
            }

            override fun discardBody(body: MirrorBattle.Body) {
                val actor = body.actor()
                if (actor != null) {
                    for (battlePokemon in actor.mirrorTeam()) {
                        val entity = battlePokemon.entity
                        if (entity != null && !entity.isRemoved) entity.discard()
                    }
                }
                MirrorNpc.despawn(body.npc())
            }
        }) { delay, work ->
            CompletableFuture.runAsync(work, CompletableFuture.delayedExecutor(delay, TimeUnit.MILLISECONDS))
        }
    }

    fun abort(mirror: MirrorBattle, reason: Component) {
        mirror.markFinished()
        val localBattleId = mirror.localBattleId()
        if (localBattleId != null) {
            CrossServerBattles.forget(localBattleId)
            val server = service.runningServer()
            if (server != null) {
                server.execute {
                    val battle = BattleRegistry.getBattle(localBattleId)
                    if (battle != null && !battle.ended) {
                        for (actor in battle.actors) actor.sendMessage(reason)
                        battle.end()
                        BattleRegistry.closeBattle(battle)
                    }
                }
            }
        }

        sweepEntities(mirror, 0L)
        for (participantId in mirror.localPlayers()) service.tellParticipant(participantId, reason)
    }

    companion object {
        const val RECALL_GRACE_MS: Long = 2_000L
    }
}
