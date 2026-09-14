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
   private LivingEntity entity;
   private PlayerSkin skin;

   private PlayerPortrait(PlayerSkin skin) {
      this.skin = skin;
   }

   public LivingEntity entity() {
      return this.entity;
   }

   public PlayerSkin skin() {
      return this.skin;
   }

   public static PlayerPortrait of(AbstractClientPlayer player) {
      PlayerPortrait portrait = new PlayerPortrait(player.getSkin());
      portrait.entity = player;
      return portrait;
   }

   private static String mojangName(String displayName) {
      int hash = displayName.indexOf(35);
      return hash < 0 ? displayName : displayName.substring(0, hash);
   }

   public static PlayerPortrait lookup(String displayName, long uid) {
      Minecraft minecraft = Minecraft.getInstance();
      String name = mojangName(displayName);
      UUID standIn = new UUID(0L, uid);
      final PlayerPortrait portrait = new PlayerPortrait(DefaultPlayerSkin.get(standIn));
      if (minecraft.level != null && !name.isEmpty()) {
         portrait.entity = new RemotePlayer(minecraft.level, new GameProfile(standIn, name)) {
            public PlayerSkin getSkin() {
               return portrait.skin;
            }
         };
         portrait.entity.moveTo(0.0, -1000000.0, 0.0, 0.0F, 0.0F);
         SkullBlockEntity.fetchGameProfile(name)
            .thenCompose(
               profile -> profile.<CompletionStage<PlayerSkin>>map(found -> minecraft.getSkinManager().getOrLoad(found))
                  .orElseGet(() -> CompletableFuture.completedFuture((PlayerSkin)null))
            )
            .thenAccept(fetched -> {
               if (fetched != null) {
                  minecraft.execute(() -> portrait.skin = fetched);
               }
            })
            .exceptionally(e -> null);
         return portrait;
      } else {
         return portrait;
      }
   }
}
