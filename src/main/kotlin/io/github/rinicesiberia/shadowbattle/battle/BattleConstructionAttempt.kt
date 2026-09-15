package io.github.rinicesiberia.shadowbattle.battle

/** 执行一次本地对战启动，并保证构造上下文在退出前结束。 */
object BattleConstructionAttempt {
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
