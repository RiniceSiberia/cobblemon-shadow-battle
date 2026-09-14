package io.github.rinicesiberia.shadowbattle.configuration

import com.google.gson.GsonBuilder
import org.slf4j.LoggerFactory
import xiaocaoawa.minecraft.mod.cobblebattle.config.CobbleBattleConfig
import java.lang.reflect.Modifier

/** 在完整读取成功后更新现有配置，并区分即时参数与连接参数。 */
object ConfigurationReloading {
    private val logger = LoggerFactory.getLogger("CobbleBattle/Config")
    private val json = GsonBuilder().create()
    private val connectionKeys = setOf("serverHost", "serverPort", "authToken", "connectTimeoutMs")

    @JvmStatic
    fun refresh(target: CobbleBattleConfig): CobbleBattleConfig.Reloaded {
        val previous = json.toJsonTree(target).asJsonObject
        val replacement = ConfigurationRepository.readRequired()
        for (property in CobbleBattleConfig::class.java.fields) {
            if (!Modifier.isStatic(property.modifiers)) {
                try {
                    property.set(target, property.get(replacement))
                } catch (failure: IllegalAccessException) {
                    logger.warn("Could not reload field {}: {}", property.name, failure.message)
                }
            }
        }
        val current = json.toJsonTree(target).asJsonObject
        val immediateChanges = ArrayList<String>()
        val deferredChanges = ArrayList<String>()
        for (key in current.keySet()) {
            if (previous[key] == null || previous[key] != current[key]) {
                (if (key in connectionKeys) deferredChanges else immediateChanges).add(key)
            }
        }
        return CobbleBattleConfig.Reloaded(immediateChanges, deferredChanges)
    }
}
