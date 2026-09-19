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
   private static final Logger COMPAT_LOG = LoggerFactory.getLogger("CobbleBattle/Compat");
   private static final MethodHandle DRAW_PROFILE_HANDLE;
   private static final Object NO_PROFILE_TRANSFORM;
   public static final PokedexEntryProgress OWNED = resolveOwnedProgress();

   private CobblemonCompat() {
   }

   public static boolean modernProfile() {
      return NO_PROFILE_TRANSFORM != null;
   }

   public static void drawProfile(
      RenderablePokemon creature,
      PoseStack poseStack,
      Quaternionf modelRotation,
      PoseType requestedPose,
      PosableState poseState,
      float frameDelta,
      float renderScale,
      boolean useBaseScale,
      float red,
      float green,
      float blue,
      float alpha,
      float headYawOffset,
      float headPitchOffset,
      int packedBlockLight
   ) {
      if (DRAW_PROFILE_HANDLE != null) {
         try {
            DRAW_PROFILE_HANDLE.invokeExact(
               (RenderablePokemon)creature,
               (PoseStack)poseStack,
               (Quaternionf)modelRotation,
               (PoseType)requestedPose,
               (PosableState)poseState,
               (float)frameDelta,
               (float)renderScale,
               (boolean)useBaseScale,
               (float)red,
               (float)green,
               (float)blue,
               (float)alpha,
               (float)headYawOffset,
               (float)headPitchOffset,
               (int)packedBlockLight
            );
         } catch (Error | RuntimeException failure) {
            throw failure;
         } catch (Throwable invocationFailure) {
            throw new IllegalStateException(invocationFailure);
         }
      }
   }

   private static PokedexEntryProgress resolveOwnedProgress() {
      for (String candidateName : new String[]{"OWNED", "CAUGHT"}) {
         try {
            return PokedexEntryProgress.valueOf(candidateName);
         } catch (IllegalArgumentException failure) {
         }
      }

      PokedexEntryProgress[] progressLevels = PokedexEntryProgress.values();
      return progressLevels[progressLevels.length - 1];
   }

   static {
      Lookup publicLookup = MethodHandles.publicLookup();
      MethodHandle profileRenderer = null;
      Object noTransformValue = null;

      try {
         Class<?> transformType = Class.forName("com.cobblemon.mod.common.client.gui.ProfileTransformType");
         noTransformValue = Enum.valueOf(transformType.asSubclass(Enum.class), "NONE");
         profileRenderer = publicLookup.findStatic(
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
               transformType,
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
         profileRenderer = MethodHandles.insertArguments(profileRenderer, 7, noTransformValue);
      } catch (ReflectiveOperationException failure) {
         try {
            profileRenderer = publicLookup.findStatic(
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
            profileRenderer = MethodHandles.insertArguments(profileRenderer, 7, false);
            profileRenderer = MethodHandles.dropArguments(profileRenderer, 14, int.class);
         } catch (ReflectiveOperationException signatureFailure) {
            COMPAT_LOG.error(
               "Cobblemon's drawProfilePokemon has a shape this mod does not know; Pokemon will not be drawn on the mod's screens",
               signatureFailure
            );
         }
      }

      DRAW_PROFILE_HANDLE = profileRenderer;
      NO_PROFILE_TRANSFORM = noTransformValue;
   }
}
