package io.github.rinicesiberia.shadowbattle.configuration

import org.slf4j.LoggerFactory
import xiaocaoawa.minecraft.mod.cobblebattle.config.CobbleBattleConfig

/** 规范化配置中的端口和帧大小边界。 */
object ConfigurationValidation {
    private val logger = LoggerFactory.getLogger("CobbleBattle/Config")

    fun normalize(configuration: CobbleBattleConfig) {
        if (configuration.serverPort <= 0 || configuration.serverPort > 65_535) {
            logger.warn("cobblebattle.yml: serverPort {} is out of range, using 18470", configuration.serverPort)
            configuration.serverPort = 18_470
        }
        if (configuration.maxFrameBytes < 1_048_576) {
            configuration.maxFrameBytes = 1_048_576
        }
    }
}
