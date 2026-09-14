package io.github.rinicesiberia.shadowbattle.battle

import java.util.concurrent.CopyOnWriteArrayList

/** 保存镜像对战创建的实体引用，并支持结束时一次性转移清理责任。 */
class MirrorEntityRegistry<B : Any, P : Any> {
    private val bodies = CopyOnWriteArrayList<B>()
    private val props = CopyOnWriteArrayList<P>()

    fun addBody(body: B?) {
        if (body != null) bodies.add(body)
    }

    fun bodySnapshot(): List<B> = java.util.List.copyOf(bodies)
    fun takeBodies(): List<B> = takeAll(bodies)

    fun addProp(prop: P?) {
        if (prop != null) props.add(prop)
    }

    fun takeProps(): List<P> = takeAll(props)

    private fun <T : Any> takeAll(source: CopyOnWriteArrayList<T>): List<T> {
        val taken = java.util.List.copyOf(source)
        source.clear()
        return taken
    }
}
