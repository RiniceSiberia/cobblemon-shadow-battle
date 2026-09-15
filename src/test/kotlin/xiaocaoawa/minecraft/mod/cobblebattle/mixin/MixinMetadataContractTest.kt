package xiaocaoawa.minecraft.mod.cobblebattle.mixin

import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.ModifyArg
import org.spongepowered.asm.mixin.injection.ModifyArgs

class MixinMetadataContractTest {
    @Test
    fun `配置保持原包名分侧清单与注入要求`() {
        val resource = requireNotNull(javaClass.classLoader.getResourceAsStream("cobblebattle.mixins.json"))
        val root = resource.bufferedReader().use { JsonParser.parseReader(it).asJsonObject }

        assertEquals(true, root["required"].asBoolean)
        assertEquals(MIXIN_PACKAGE, root["package"].asString)
        assertEquals("JAVA_21", root["compatibilityLevel"].asString)
        assertEquals("0.8", root["minVersion"].asString)
        assertEquals(1, root.getAsJsonObject("injectors")["defaultRequire"].asInt)
        assertEquals(
            listOf("BattleMessagePaneMixin", "PokedexGUIMixin", "CobblemonClientMixin", "ClientPokedexSyncMixin"),
            root.getAsJsonArray("client").map { it.asString },
        )
        assertEquals(
            listOf("BattleActorMixin", "GraalShowdownServiceMixin"),
            root.getAsJsonArray("mixins").map { it.asString },
        )

        for (name in root.getAsJsonArray("client") + root.getAsJsonArray("mixins")) {
            val classResource = "${MIXIN_PACKAGE.replace('.', '/')}/${name.asString}.class"
            assertNotNull(javaClass.classLoader.getResource(classResource), classResource)
        }
    }

    @Test
    fun `Inject处理器保持目标方法名称`() {
        assertEquals(
            mapOf("cobblebattle\$refuseBannedGimmick" to listOf("setActionResponses")),
            injectTargets(BattleActorMixin::class.java),
        )
        assertEquals(
            mapOf("cobblebattle\$makeRoomForChat" to listOf("getAppropriateY")),
            injectTargets(BattleMessagePaneMixin::class.java),
        )
        assertEquals(
            mapOf(
                "cobblebattle\$beforeFullSync" to listOf("runAction"),
                "cobblebattle\$beforeIncrementalSync" to listOf("runIncremental"),
            ),
            injectTargets(ClientPokedexSyncMixin::class.java),
        )
        assertEquals(
            mapOf("cobblebattle\$standInKnowledge" to listOf("getClientPokedexData")),
            injectTargets(CobblemonClientMixin::class.java),
        )
        assertEquals(
            mapOf(
                "cobblebattle\$startBattle" to listOf("startBattle"),
                "cobblebattle\$send" to listOf("send"),
                "cobblebattle\$sendFromShowdown" to listOf("sendFromShowdown"),
            ),
            injectTargets(GraalShowdownServiceMixin::class.java),
        )
        assertEquals(
            mapOf(
                "cobblebattle\$hidePageArrows" to listOf("init", "method_25426"),
                "cobblebattle\$hostBaseStats" to listOf("updateTabInfoElement"),
                "cobblebattle\$drawBack" to listOf("render", "method_25394"),
                "cobblebattle\$clickBack" to listOf("mouseClicked", "method_25402"),
                "cobblebattle\$turnPage" to listOf("updatePokedexRegion"),
            ),
            injectTargets(PokedexGUIMixin::class.java),
        )
    }

    @Test
    fun `图鉴绘制修改保持调用目标描述符`() {
        val methods = PokedexGUIMixin::class.java.declaredMethods.associateBy { it.name }
        val icon = requireNotNull(methods["cobblebattle\$hideBandIcons"]?.getAnnotation(ModifyArg::class.java))
        assertEquals(listOf("render", "method_25394"), icon.method.toList())
        assertEquals("Lcom/cobblemon/mod/common/api/gui/GuiUtilsKt;blitk\$default", icon.at.target)

        val official = requireNotNull(methods["cobblebattle\$bandTextOfficial"]?.getAnnotation(ModifyArgs::class.java))
        val intermediary = requireNotNull(methods["cobblebattle\$bandTextIntermediary"]?.getAnnotation(ModifyArgs::class.java))
        assertEquals(OFFICIAL_TEXT_TARGET, official.at.target)
        assertEquals(INTERMEDIARY_TEXT_TARGET, intermediary.at.target)
        for (annotation in listOf(official, intermediary)) {
            assertEquals(listOf("render", "method_25394"), annotation.method.toList())
            assertEquals(0, annotation.require)
            assertEquals(0, annotation.expect)
        }
    }

    private fun injectTargets(type: Class<*>): Map<String, List<String>> = type.declaredMethods
        .mapNotNull { method -> method.getAnnotation(Inject::class.java)?.let { method.name to it.method.toList() } }
        .toMap()

    private companion object {
        const val MIXIN_PACKAGE = "xiaocaoawa.minecraft.mod.cobblebattle.mixin"
        const val OFFICIAL_TEXT_TARGET =
            "Lcom/cobblemon/mod/common/client/render/RenderHelperKt;drawScaledText\$default" +
                "(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/resources/ResourceLocation;" +
                "Lnet/minecraft/network/chat/MutableComponent;Ljava/lang/Number;Ljava/lang/Number;" +
                "FLjava/lang/Number;IIZZLjava/lang/Integer;Ljava/lang/Integer;ILjava/lang/Object;)V"
        const val INTERMEDIARY_TEXT_TARGET =
            "Lcom/cobblemon/mod/common/client/render/RenderHelperKt;drawScaledText\$default" +
                "(Lnet/minecraft/class_332;Lnet/minecraft/class_2960;Lnet/minecraft/class_5250;" +
                "Ljava/lang/Number;Ljava/lang/Number;FLjava/lang/Number;IIZZLjava/lang/Integer;" +
                "Ljava/lang/Integer;ILjava/lang/Object;)V"
    }
}
