package io.github.rinicesiberia.shadowbattle.battle

import org.slf4j.LoggerFactory
import java.util.function.Consumer

/** 在战斗准备完成后按序投递输出，超过乱序缓存容量时终止接收。 */
class SequencedOutputBuffer(
    private val upstreamBattleId: String,
    private val replay: Boolean,
    private val delivery: Consumer<String>
) {
    private val logger = LoggerFactory.getLogger("CobbleBattle/Mirror")
    private val monitor = Any()
    private val waitingChunks = HashMap<Long, String?>()
    private var expectedSequence = 1L
    private var deliveryEnabled = false
    @Volatile private var terminated = false

    fun isTerminated() = terminated
    fun enableDelivery() = synchronized(monitor) { deliveryEnabled = true; deliverAvailable() }

    fun enqueue(sequence: Long, content: String?) = synchronized(monitor) {
        if (terminated) return@synchronized
        if (sequence < expectedSequence) {
            logger.debug("Battle {} ignoring duplicate chunk seq {}", upstreamBattleId, sequence)
            return@synchronized
        }
        waitingChunks[sequence] = content
        if (waitingChunks.size > if (replay) 8192 else 64) {
            logger.error(
                "Battle {} has {} out-of-order chunks waiting on seq {} - the stream is broken",
                upstreamBattleId, waitingChunks.size, expectedSequence
            )
            terminated = true
            waitingChunks.clear()
        } else deliverAvailable()
    }

    fun terminate() = synchronized(monitor) { terminated = true; waitingChunks.clear() }
    fun hasPendingOutput(): Boolean = synchronized(monitor) { waitingChunks.isNotEmpty() }

    private fun deliverAvailable() {
        if (!deliveryEnabled) return
        while (true) {
            val content = waitingChunks.remove(expectedSequence) ?: break
            expectedSequence++
            delivery.accept(content)
        }
    }
}
