package xiaocaoawa.minecraft.mod.cobblebattle.battle

import com.cobblemon.mod.common.battles.BattleRegistry
import io.github.rinicesiberia.shadowbattle.battle.CleanupTiming
import net.minecraft.network.chat.Component
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/** 负责镜像对战结束后的索引、实体与参与者通知清理。 */
internal class MirrorLifecycleCleanup(
    private val service: CrossServerBattleService,
) {
    fun sweepEntities(mirror: MirrorBattle, delayMs: Long) {
        val bodies = mirror.takeBodies()
        val props = mirror.takeProps()
        if (bodies.isEmpty() && props.isEmpty()) return

        for (body in bodies) {
            val actor = body.actor()
            if (actor != null) MirrorPokemon.release(actor.mirrorTeam())
        }

        val server = service.server() ?: return
        val sweep = Runnable {
            server.execute {
                for (entity in props) {
                    if (!entity.isRemoved) entity.discard()
                }

                for (body in bodies) {
                    val actor = body.actor()
                    if (actor != null) {
                        for (battlePokemon in actor.mirrorTeam()) {
                            val entity = battlePokemon.entity
                            if (entity != null && !entity.isRemoved) entity.discard()
                        }
                    }
                    MirrorNpc.despawn(body.npc())
                }
            }
        }
        CleanupTiming.schedule(delayMs, sweep) { delay, work ->
            CompletableFuture.runAsync(work, CompletableFuture.delayedExecutor(delay, TimeUnit.MILLISECONDS))
        }
    }

    fun abort(mirror: MirrorBattle, reason: Component) {
        mirror.markFinished()
        val localBattleId = mirror.localBattleId()
        if (localBattleId != null) {
            CrossServerBattles.forget(localBattleId)
            val server = service.server()
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
