package xiaocaoawa.minecraft.mod.cobblebattle.client;

import com.cobblemon.mod.common.api.pokedex.PokedexEntryProgress;
import com.cobblemon.mod.common.client.gui.PokemonGuiUtilsKt;
import com.cobblemon.mod.common.client.render.models.blockbench.PosableState;
import com.cobblemon.mod.common.entity.PoseType;
import com.cobblemon.mod.common.pokemon.RenderablePokemon;
import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MethodHandles.Lookup;
import org.joml.Quaternionf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CobblemonCompat {
   private static final Logger LOGGER = LoggerFactory.getLogger("CobbleBattle/Compat");
   private static final MethodHandle DRAW_PROFILE;
   private static final Object TRANSFORM_NONE;
   public static final PokedexEntryProgress OWNED = highestProgress();

   private CobblemonCompat() {
   }

   public static boolean modernProfile() {
      return TRANSFORM_NONE != null;
   }

   public static void drawProfile(
      RenderablePokemon creature,
      PoseStack pose,
      Quaternionf rotation,
      PoseType poseType,
      PosableState state,
      float partialTicks,
      float scale,
      boolean applyBaseScale,
      float r,
      float g,
      float b,
      float a,
      float headYaw,
      float headPitch,
      int blockLight
   ) {
      if (DRAW_PROFILE != null) {
         try {
            DRAW_PROFILE.invokeExact(
               (RenderablePokemon)creature,
               (PoseStack)pose,
               (Quaternionf)rotation,
               (PoseType)poseType,
               (PosableState)state,
               (float)partialTicks,
               (float)scale,
               (boolean)applyBaseScale,
               (float)r,
               (float)g,
               (float)b,
               (float)a,
               (float)headYaw,
               (float)headPitch,
               (int)blockLight
            );
         } catch (Error | RuntimeException failure) {
            throw failure;
         } catch (Throwable var17) {
            throw new IllegalStateException(var17);
         }
      }
   }

   private static PokedexEntryProgress highestProgress() {
      for (String name : new String[]{"OWNED", "CAUGHT"}) {
         try {
            return PokedexEntryProgress.valueOf(name);
         } catch (IllegalArgumentException failure) {
         }
      }

      PokedexEntryProgress[] all = PokedexEntryProgress.values();
      return all[all.length - 1];
   }

   static {
      Lookup lookup = MethodHandles.publicLookup();
      MethodHandle draw = null;
      Object none = null;

      try {
         Class<?> transform = Class.forName("com.cobblemon.mod.common.client.gui.ProfileTransformType");
         none = Enum.valueOf(transform.asSubclass(Enum.class), "NONE");
         draw = lookup.findStatic(
            PokemonGuiUtilsKt.class,
            "drawProfilePokemon",
            MethodType.methodType(
               void.class,
               RenderablePokemon.class,
               PoseStack.class,
               Quaternionf.class,
               PoseType.class,
               PosableState.class,
               float.class,
               float.class,
               transform,
               boolean.class,
               float.class,
               float.class,
               float.class,
               float.class,
               float.class,
               float.class,
               int.class
            )
         );
         draw = MethodHandles.insertArguments(draw, 7, none);
      } catch (ReflectiveOperationException failure) {
         try {
            draw = lookup.findStatic(
               PokemonGuiUtilsKt.class,
               "drawProfilePokemon",
               MethodType.methodType(
                  void.class,
                  RenderablePokemon.class,
                  PoseStack.class,
                  Quaternionf.class,
                  PoseType.class,
                  PosableState.class,
                  float.class,
                  float.class,
                  boolean.class,
                  boolean.class,
                  float.class,
                  float.class,
                  float.class,
                  float.class,
                  float.class,
                  float.class
               )
            );
            draw = MethodHandles.insertArguments(draw, 7, false);
            draw = MethodHandles.dropArguments(draw, 14, int.class);
         } catch (ReflectiveOperationException var5) {
            LOGGER.error("Cobblemon's drawProfilePokemon has a shape this mod does not know; Pokemon will not be drawn on the mod's screens", var5);
         }
      }

      DRAW_PROFILE = draw;
      TRANSFORM_NONE = none;
   }
}
