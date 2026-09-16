package io.github.rinicesiberia.shadowbattle.battle

import java.util.concurrent.Executor

/** 结束回收所需的归属操作、主线程和实体销毁动作。 */
internal interface MirrorSweepTarget<B, P> {
    fun releaseRoster(body: B)
    fun mainThread(): Executor?
    fun discardProp(prop: P)
    fun discardBody(body: B)
}

/** 立即释放归属，随后按宽限时间在主线程回收实体。 */
internal object MirrorSweepSequence {
    fun <B, P> arrange(bodies: List<B>, props: List<P>, delayMs: Long, target: MirrorSweepTarget<B, P>, defer: (Long, Runnable) -> Unit) {
        if (bodies.isEmpty() && props.isEmpty()) return
        bodies.forEach(target::releaseRoster)
        val mainThread = target.mainThread() ?: return
        val sweep = Runnable {
            mainThread.execute {
                props.forEach(target::discardProp)
                bodies.forEach(target::discardBody)
            }
        }
        CleanupTiming.schedule(delayMs, sweep, defer)
    }
}
