package io.github.rinicesiberia.shadowbattle.network

/** 校验接收对象类型后，把处理动作交给调用方提供的任务队列。 */
internal object QueuedTypeDispatch {
    fun <T : Any> enqueue(
        subject: Any?,
        expectedType: Class<T>,
        queue: (Runnable) -> Unit,
        action: (T) -> Unit,
    ): Boolean {
        if (!expectedType.isInstance(subject)) return false
        val accepted = expectedType.cast(subject)
        queue(Runnable { action(accepted) })
        return true
    }
}
