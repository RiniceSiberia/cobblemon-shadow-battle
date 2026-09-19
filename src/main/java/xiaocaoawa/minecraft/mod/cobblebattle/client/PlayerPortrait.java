package xiaocaoawa.minecraft.mod.cobblebattle.client;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.SkullBlockEntity;

public final class PlayerPortrait {
   private LivingEntity portraitEntity;
   private PlayerSkin playerSkin;

   private PlayerPortrait(PlayerSkin initialSkin) {
      this.playerSkin = initialSkin;
   }

   public LivingEntity entity() {
      return this.portraitEntity;
   }

   public PlayerSkin skin() {
      return this.playerSkin;
   }

   public static PlayerPortrait of(AbstractClientPlayer clientPlayer) {
      PlayerPortrait portrait = new PlayerPortrait(clientPlayer.getSkin());
      portrait.portraitEntity = clientPlayer;
      return portrait;
   }

   private static String extractMojangName(String decoratedName) {
      int separatorIndex = decoratedName.indexOf(35);
      return separatorIndex < 0 ? decoratedName : decoratedName.substring(0, separatorIndex);
   }

   public static PlayerPortrait lookup(String accountDisplayName, long accountNumber) {
      Minecraft clientInstance = Minecraft.getInstance();
      String mojangName = extractMojangName(accountDisplayName);
      UUID syntheticUuid = new UUID(0L, accountNumber);
      final PlayerPortrait portraitRecord = new PlayerPortrait(DefaultPlayerSkin.get(syntheticUuid));
      if (clientInstance.level != null && !mojangName.isEmpty()) {
         portraitRecord.portraitEntity = new RemotePlayer(clientInstance.level, new GameProfile(syntheticUuid, mojangName)) {
            public PlayerSkin getSkin() {
               return portraitRecord.playerSkin;
            }
         };
         portraitRecord.portraitEntity.moveTo(0.0, -1000000.0, 0.0, 0.0F, 0.0F);
         SkullBlockEntity.fetchGameProfile(mojangName)
            .thenCompose(
               profileResult -> profileResult.<CompletionStage<PlayerSkin>>map(foundProfile -> clientInstance.getSkinManager().getOrLoad(foundProfile))
                  .orElseGet(() -> CompletableFuture.completedFuture((PlayerSkin)null))
            )
            .thenAccept(fetchedSkin -> {
               if (fetchedSkin != null) {
                  clientInstance.execute(() -> portraitRecord.playerSkin = fetchedSkin);
               }
            })
            .exceptionally(failure -> null);
         return portraitRecord;
      } else {
         return portraitRecord;
      }
   }
}
