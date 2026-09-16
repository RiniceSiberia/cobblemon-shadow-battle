package io.github.rinicesiberia.shadowbattle.battle

import com.google.gson.JsonObject
import xiaocaoawa.minecraft.mod.cobblebattle.net.BattleServerClient

/** 解码握手响应设置，并决定图鉴缓存的后续动作。 */
object HandshakeResponseDecoding {
    data class Settings(
        val dexReady: Boolean,
        val strictBaseStats: Boolean,
        val strictAbilities: Boolean,
        val strictMoves: Boolean,
        val maxEvPerStat: Int,
        val maxEvTotal: Int,
        val maxIv: Int,
        val chatEnabled: Boolean,
        val emailEnabled: Boolean,
        val instance: String,
        val speciesCount: Int,
        val dexDigest: String
    )

    sealed interface DexAction {
        data object Invalidate : DexAction
        data class AcceptCached(val digest: String) : DexAction
        data object RequestSnapshot : DexAction
    }

    @JvmStatic
    fun decode(document: JsonObject): Settings = Settings(
        BattleServerClient.bool(document, "dexReady", false),
        BattleServerClient.bool(document, "strictBaseStats", true),
        BattleServerClient.bool(document, "strictAbilities", false),
        BattleServerClient.bool(document, "strictMoves", false),
        BattleServerClient.integer(document, "maxEvPerStat", 0),
        BattleServerClient.integer(document, "maxEvTotal", 0),
        BattleServerClient.integer(document, "maxIv", 0),
        BattleServerClient.bool(document, "chatEnabled", true),
        BattleServerClient.bool(document, "emailEnabled", false),
        BattleServerClient.str(document, "instance", "?"),
        BattleServerClient.integer(document, "speciesCount", 0),
        BattleServerClient.str(document, "dexDigest", "")
    )

    @JvmStatic
    fun decideDex(settings: Settings, cachedDigest: String?): DexAction = when {
        !settings.dexReady -> DexAction.Invalidate
        cachedDigest != null && cachedDigest == settings.dexDigest -> DexAction.AcceptCached(cachedDigest)
        else -> DexAction.RequestSnapshot
    }
}
