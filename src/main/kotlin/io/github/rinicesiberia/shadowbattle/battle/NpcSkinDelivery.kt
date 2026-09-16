package io.github.rinicesiberia.shadowbattle.battle

import java.util.concurrent.Executor

/** 在查询线程缓存皮肤结果，在服务端线程检查实体并应用新皮肤。 */
class NpcSkinDelivery<S : Any>(private val state: MirrorNpcState<S>, private val lookupExecutor: Executor) {
    fun deliver(
        displayName: String?,
        serverExecutor: Executor,
        lookup: (String) -> S,
        hasTexture: (S) -> Boolean,
        isRemoved: () -> Boolean,
        apply: (S) -> Unit,
    ) {
        val profileName = state.profileName(displayName) ?: return
        val cached = state.cachedSkin(profileName)
        if (cached != null) {
            apply(cached)
            return
        }
        lookupExecutor.execute {
            val resolved = lookup(profileName)
            state.rememberSkin(profileName, resolved)
            if (hasTexture(resolved)) {
                serverExecutor.execute {
                    if (!isRemoved()) apply(resolved)
                }
            }
        }
    }
}
