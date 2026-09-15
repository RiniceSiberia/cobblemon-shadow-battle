package xiaocaoawa.minecraft.mod.cobblebattle.neoforge

import net.neoforged.api.distmarker.Dist
import net.neoforged.fml.common.Mod
import net.neoforged.fml.loading.FMLEnvironment
import xiaocaoawa.minecraft.mod.cobblebattle.CobbleBattle
import xiaocaoawa.minecraft.mod.cobblebattle.client.CobbleBattleClient

/** NeoForge 模组入口。 */
@Mod(CobbleBattle.MOD_ID)
class CobbleBattleNeoForge {
    init {
        CobbleBattle.init()
        if (FMLEnvironment.dist == Dist.CLIENT) CobbleBattleClient.init()
    }
}
