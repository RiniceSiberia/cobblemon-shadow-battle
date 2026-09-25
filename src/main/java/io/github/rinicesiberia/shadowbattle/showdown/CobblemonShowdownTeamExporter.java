package io.github.rinicesiberia.shadowbattle.showdown;

import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.util.PlayerExtensionsKt;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;

/** 将 Cobblemon 当前 party 投影为官方 packed team。可选字段缺失时留空，由 PS validator 给出规则错误。 */
public final class CobblemonShowdownTeamExporter {
   private CobblemonShowdownTeamExporter() {}

   public static String export(ServerPlayer player) {
      List<BattlePokemon> roster = PlayerExtensionsKt.party(player).toBattleTeam(false, false, null);
      List<ShowdownSet> sets = new ArrayList<>();
      for (BattlePokemon battlePokemon : roster) {
         Pokemon pokemon = battlePokemon.getEffectedPokemon();
         String species = name(property(pokemon, "species"));
         String nickname = text(property(pokemon, "nickname"));
         sets.add(new ShowdownSet(nickname, species, name(property(pokemon, "heldItem")), name(property(pokemon, "ability")), List.<String>of(), name(property(pokemon, "nature")), "", "", "", booleanValue(property(pokemon, "shiny")), "", "", "", "100", "", "", ""));
      }
      if (sets.isEmpty()) throw new IllegalArgumentException("当前 party 没有可上传的宝可梦");
      return PackedTeamCodec.INSTANCE.pack(sets);
   }

   private static Object property(Object target, String name) {
      try {
         String getter = "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
         for (Method method : target.getClass().getMethods()) {
            if (method.getParameterCount() == 0 && (method.getName().equalsIgnoreCase(getter) || method.getName().equalsIgnoreCase(name))) return method.invoke(target);
         }
      } catch (ReflectiveOperationException ignored) { }
      return null;
   }

   private static String name(Object value) {
      if (value == null) return "";
      if (value instanceof String text) return text;
      String nested = text(property(value, "name"));
      return nested.isEmpty() ? value.toString() : nested;
   }
   private static String text(Object value) { return value == null ? "" : value.toString().trim(); }
   private static boolean booleanValue(Object value) { return value instanceof Boolean flag && flag; }
}
