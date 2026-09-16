package io.github.rinicesiberia.shadowbattle.battle

import com.cobblemon.mod.common.api.npc.NPCClasses
import com.cobblemon.mod.common.entity.npc.NPCEntity
import com.cobblemon.mod.common.entity.npc.NPCPlayerModelType
import com.mojang.authlib.GameProfile
import com.mojang.authlib.ProfileLookupCallback
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.MobSpawnType
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.concurrent.Executors

/** 负责镜像 NPC 的世界实体、身份登记和玩家皮肤。 */
object MirrorNpcRuntime {
    private val logger = LoggerFactory.getLogger("CobbleBattle/NPC")
    private val mirrorClassId = ResourceLocation.fromNamespaceAndPath("cobblebattle", "mirror")
    private val state = MirrorNpcState<NpcAppearance>()
    private val noSkin = NpcAppearance(null, NPCPlayerModelType.DEFAULT)
    private val skinWorker = Executors.newSingleThreadExecutor { task ->
        Thread(task, "CobbleBattle-Skins").apply { isDaemon = true }
    }
    private val skins = NpcSkinDelivery(state, skinWorker)

    @JvmStatic
    fun isOrphan(entity: Entity?): Boolean {
        if (entity !is NPCEntity) return false
        val npcClass = entity.npc ?: return false
        return mirrorClassId == npcClass.resourceIdentifier && !state.isLive(entity.uuid)
    }

    @JvmStatic
    fun spawn(viewer: ServerPlayer, opponentName: String?): NPCEntity? =
        spawnAt(viewer, MirrorStageLayout.opponent(viewer.position(), viewer.yRot), opponentName)

    @JvmStatic
    fun spawnPair(viewer: ServerPlayer, firstName: String?, secondName: String?): Array<NPCEntity?> {
        val positions = MirrorStageLayout.spectatorPair(viewer.position(), viewer.yRot)
        return arrayOf(spawnAt(viewer, positions[0], firstName), spawnAt(viewer, positions[1], secondName))
    }

    private fun spawnAt(viewer: ServerPlayer, placement: MirrorStagePosition, opponentName: String?): NPCEntity? {
        try {
            val level = viewer.serverLevel()
            val npcClass = NPCClasses.getByIdentifier(mirrorClassId)
            if (npcClass == null) {
                logger.warn("NPC class {} is not loaded - the opponent will have no visible body. Is data/cobblebattle/npcs/mirror.json in the jar?", mirrorClassId)
                return null
            }
            val npc = NPCEntity(level)
            npc.npc = npcClass
            npc.initialize(1)
            val spot = placement.location
            npc.moveTo(spot.x, spot.y, spot.z, placement.yaw, 0.0f)
            npc.setYHeadRot(placement.yaw)
            npc.customName = Component.literal(opponentName ?: throw NullPointerException("opponentName"))
            npc.isCustomNameVisible = true
            npc.isInvulnerable = true
            npc.isNoAi = true
            npc.setPersistenceRequired()
            npc.finalizeSpawn(level, level.getCurrentDifficultyAt(npc.blockPosition()), MobSpawnType.EVENT, null)
            state.markLive(npc.uuid)
            if (!level.addFreshEntity(npc)) {
                state.markGone(npc.uuid)
                logger.warn("The level refused the mirror NPC for {}", opponentName)
                return null
            }
            applySkin(viewer.server, npc, opponentName)
            return npc
        } catch (failure: Exception) {
            logger.error("Could not spawn the mirror NPC for {}", opponentName, failure)
            return null
        }
    }

    private fun applySkin(server: MinecraftServer?, npc: NPCEntity, displayName: String?) {
        if (server == null || displayName == null) return
        skins.deliver(displayName, server, { lookupSkin(server, it) }, { it.url != null }, { npc.isRemoved }) { it.applyTo(npc) }
    }

    private fun lookupSkin(server: MinecraftServer, profileName: String): NpcAppearance {
        return try {
            var found: GameProfile? = null
            server.profileRepository.findProfilesByNames(arrayOf(profileName), object : ProfileLookupCallback {
                override fun onProfileLookupSucceeded(profile: GameProfile) { found = profile }
                override fun onProfileLookupFailed(profileName: String, failure: Exception) { }
            })
            val profile = found ?: return noSkin
            val result = server.sessionService.fetchProfile(profile.id, true) ?: return noSkin
            val texture = server.sessionService.getTextures(result.profile()).skin() ?: return noSkin
            val url = texture.url ?: return noSkin
            val model = if ("slim".equals(texture.getMetadata("model"), ignoreCase = true)) NPCPlayerModelType.SLIM else NPCPlayerModelType.DEFAULT
            NpcAppearance(URI.create(url), model)
        } catch (failure: Exception) {
            logger.debug("Could not look up the skin for {}: {}", profileName, failure.toString())
            noSkin
        }
    }

    @JvmStatic
    fun despawn(npc: NPCEntity?) {
        if (npc == null) return
        state.markGone(npc.uuid)
        if (!npc.isRemoved) {
            try { npc.discard() } catch (failure: RuntimeException) {
                logger.warn("Could not remove a mirror NPC: {}", failure.toString())
            }
        }
    }

    private data class NpcAppearance(val url: URI?, val model: NPCPlayerModelType) {
        fun applyTo(npc: NPCEntity) {
            if (url != null) {
                try { npc.loadTexture(url, model) } catch (failure: RuntimeException) {
                    logger.debug("Could not apply a mirror NPC skin: {}", failure.toString())
                }
            }
        }
    }
}
