package io.github.rinicesiberia.shadowbattle.battle

/** 根据回收宽限时间选择立即执行或提交延迟任务。 */
internal object CleanupTiming {
    fun schedule(
        delayMs: Long,
        work: Runnable,
        defer: (Long, Runnable) -> Unit,
    ) {
        if (delayMs <= 0L) {
            work.run()
        } else {
            defer(delayMs, work)
        }
    }
}
