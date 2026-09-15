package io.github.rinicesiberia.shadowbattle.battle

import java.util.function.Consumer

/** 执行一次本地对战启动，并保证构造上下文在退出前结束。 */
object BattleConstructionAttempt {
    /** 回收在构造上下文内执行，即使回收失败也会结束上下文。 */
    @JvmStatic
    fun startWithRecovery(start: Runnable, recover: Consumer<RuntimeException>, finish: Runnable): Boolean = try {
        start.run()
        true
    } catch (failure: RuntimeException) {
        recover.accept(failure)
        false
    } finally {
        finish.run()
    }

    @JvmStatic
    fun captureFailure(start: Runnable, finish: Runnable): RuntimeException? = try {
        start.run()
        null
    } catch (failure: RuntimeException) {
        failure
    } finally {
        finish.run()
    }
}
