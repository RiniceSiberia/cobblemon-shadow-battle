package xiaocaoawa.minecraft.mod.cobblebattle.battle;

import com.cobblemon.mod.common.api.npc.NPCClass;
import com.cobblemon.mod.common.api.npc.NPCClasses;
import com.cobblemon.mod.common.entity.npc.NPCEntity;
import com.cobblemon.mod.common.entity.npc.NPCPlayerModelType;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.yggdrasil.ProfileResult;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MirrorNpc {
   private static final Logger LOGGER = LoggerFactory.getLogger("CobbleBattle/NPC");
   private static final double DISTANCE = 4.0;
   private static final Set<UUID> LIVE = ConcurrentHashMap.newKeySet();
   private static final ResourceLocation MIRROR_CLASS = ResourceLocation.fromNamespaceAndPath("cobblebattle", "mirror");
   private static final double STAGE_DISTANCE = 7.0;
   private static final double STAGE_WIDTH = 4.0;
   private static final MirrorNpc.Skin NO_SKIN = new MirrorNpc.Skin(null, null);
   private static final Map<String, MirrorNpc.Skin> SKINS = new ConcurrentHashMap<>();
   private static final ExecutorService SKIN_LOOKUP = Executors.newSingleThreadExecutor(runnable -> {
      Thread thread = new Thread(runnable, "CobbleBattle-Skins");
      thread.setDaemon(true);
      return thread;
   });

   private MirrorNpc() {
   }

   public static boolean isOrphan(Entity entity) {
      if (!(entity instanceof NPCEntity npc)) {
         return false;
      } else {
         NPCClass npcClass = npc.getNpc();
         return npcClass != null && MIRROR_CLASS.equals(npcClass.getResourceIdentifier()) && !LIVE.contains(npc.getUUID());
      }
   }

   private static NPCClass mirrorClass() {
      return NPCClasses.getByIdentifier(MIRROR_CLASS);
   }

   public static NPCEntity spawn(ServerPlayer viewer, String opponentName) {
      float yaw = viewer.getYRot();
      Vec3 forward = Vec3.directionFromRotation(0.0F, yaw).normalize();
      Vec3 spot = viewer.position().add(forward.scale(4.0));
      return spawnAt(viewer, spot, yaw + 180.0F, opponentName);
   }

   public static NPCEntity[] spawnPair(ServerPlayer viewer, String firstName, String secondName) {
      float yaw = viewer.getYRot();
      Vec3 forward = Vec3.directionFromRotation(0.0F, yaw).normalize();
      Vec3 right = Vec3.directionFromRotation(0.0F, yaw + 90.0F).normalize();
      Vec3 centre = viewer.position().add(forward.scale(7.0));
      Vec3 leftSpot = centre.subtract(right.scale(2.0));
      Vec3 rightSpot = centre.add(right.scale(2.0));
      return new NPCEntity[]{spawnAt(viewer, leftSpot, yaw + 90.0F, firstName), spawnAt(viewer, rightSpot, yaw - 90.0F, secondName)};
   }

   private static NPCEntity spawnAt(ServerPlayer viewer, Vec3 spot, float yaw, String opponentName) {
      try {
         ServerLevel level = viewer.serverLevel();
         NPCClass npcClass = mirrorClass();
         if (npcClass == null) {
            LOGGER.warn("NPC class {} is not loaded - the opponent will have no visible body. Is data/cobblebattle/npcs/mirror.json in the jar?", MIRROR_CLASS);
            return null;
         } else {
            NPCEntity npc = new NPCEntity(level);
            npc.setNpc(npcClass);
            npc.initialize(1);
            npc.moveTo(spot.x, spot.y, spot.z, yaw, 0.0F);
            npc.setYHeadRot(yaw);
            npc.setCustomName(Component.literal(opponentName));
            npc.setCustomNameVisible(true);
            npc.setInvulnerable(true);
            npc.setNoAi(true);
            npc.setPersistenceRequired();
            npc.finalizeSpawn(level, level.getCurrentDifficultyAt(npc.blockPosition()), MobSpawnType.EVENT, null);
            LIVE.add(npc.getUUID());
            if (!level.addFreshEntity(npc)) {
               LIVE.remove(npc.getUUID());
               LOGGER.warn("The level refused the mirror NPC for {}", opponentName);
               return null;
            } else {
               applySkin(viewer.getServer(), npc, opponentName);
               return npc;
            }
         }
      } catch (Exception var7) {
         LOGGER.error("Could not spawn the mirror NPC for {}", opponentName, var7);
         return null;
      }
   }

   private static void applySkin(MinecraftServer server, NPCEntity npc, String displayName) {
      if (server != null && displayName != null) {
         int hash = displayName.indexOf(35);
         String name = (hash < 0 ? displayName : displayName.substring(0, hash)).trim();
         if (!name.isEmpty()) {
            MirrorNpc.Skin known = SKINS.get(name);
            if (known != null) {
               known.applyTo(npc);
            } else {
               SKIN_LOOKUP.execute(() -> {
                  MirrorNpc.Skin found = lookUpSkin(server, name);
                  SKINS.put(name, found);
                  if (found.url() != null) {
                     server.execute(() -> {
                        if (!npc.isRemoved()) {
                           found.applyTo(npc);
                        }
                     });
                  }
               });
            }
         }
      }
   }

   private static MirrorNpc.Skin lookUpSkin(MinecraftServer server, String name) {
      try {
         final GameProfile[] found = new GameProfile[1];
         server.getProfileRepository().findProfilesByNames(new String[]{name}, new ProfileLookupCallback() {
            public void onProfileLookupSucceeded(GameProfile profile) {
               found[0] = profile;
            }

            public void onProfileLookupFailed(String profileName, Exception e) {
            }
         });
         if (found[0] == null) {
            return NO_SKIN;
         } else {
            ProfileResult result = server.getSessionService().fetchProfile(found[0].getId(), true);
            if (result == null) {
               return NO_SKIN;
            } else {
               MinecraftProfileTexture skin = server.getSessionService().getTextures(result.profile()).skin();
               if (skin != null && skin.getUrl() != null) {
                  String model = skin.getMetadata("model");
                  return new MirrorNpc.Skin(URI.create(skin.getUrl()), "slim".equalsIgnoreCase(model) ? NPCPlayerModelType.SLIM : NPCPlayerModelType.DEFAULT);
               } else {
                  return NO_SKIN;
               }
            }
         }
      } catch (Exception var6) {
         LOGGER.debug("Could not look up the skin for {}: {}", name, var6.toString());
         return NO_SKIN;
      }
   }

   public static void despawn(NPCEntity npc) {
      if (npc != null) {
         LIVE.remove(npc.getUUID());
         if (!npc.isRemoved()) {
            try {
               npc.discard();
            } catch (RuntimeException var2) {
               LOGGER.warn("Could not remove a mirror NPC: {}", var2.toString());
            }
         }
      }
   }

   private record Skin(URI url, NPCPlayerModelType model) {
      void applyTo(NPCEntity npc) {
         if (this.url != null) {
            try {
               npc.loadTexture(this.url, this.model);
            } catch (RuntimeException var3) {
               MirrorNpc.LOGGER.debug("Could not apply a mirror NPC skin: {}", var3.toString());
            }
         }
      }
   }
}
