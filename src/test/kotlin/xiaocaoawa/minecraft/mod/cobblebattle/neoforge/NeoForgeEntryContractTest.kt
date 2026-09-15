package xiaocaoawa.minecraft.mod.cobblebattle.neoforge

import net.neoforged.fml.common.Mod
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NeoForgeEntryContractTest {
    @Test
    fun `入口保持模组标识和公开零参数构造`() {
        val entryType = CobbleBattleNeoForge::class.java

        assertEquals("cobblebattle", entryType.getAnnotation(Mod::class.java).value)
        assertEquals(listOf(emptyList<Class<*>>()), entryType.constructors.map { it.parameterTypes.toList() })
    }
}
