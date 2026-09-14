package io.github.rinicesiberia.shadowbattle.network

import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

/** 集中创建模组游戏包类型，保证所有包使用同一命名空间。 */
object PayloadTypeCatalog {
    private const val NAMESPACE = "cobblebattle"

    @JvmStatic
    fun <T : CustomPacketPayload> named(path: String): CustomPacketPayload.Type<T> =
        CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath(NAMESPACE, path))
}
