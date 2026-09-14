package xiaocaoawa.minecraft.mod.cobblebattle.battle;

import com.cobblemon.mod.common.api.battles.model.actor.ActorType;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.net.NetworkPacket;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import xiaocaoawa.minecraft.mod.cobblebattle.lang.Msg;

public class RemoteBattleActor extends BattleActor {
   private final String displayName;
   private final String originServerId;
   private final String seat;

   public RemoteBattleActor(UUID playerUuid, String displayName, String originServerId, String seat, List<BattlePokemon> pokemon) {
      super(playerUuid, new ArrayList<>(pokemon));
      this.displayName = displayName;
      this.originServerId = originServerId;
      this.seat = seat;
   }

   public String getOriginServerId() {
      return this.originServerId;
   }

   public String getSeat() {
      return this.seat;
   }

   public ActorType getType() {
      return ActorType.NPC;
   }

   public MutableComponent getName() {
      return Component.literal(this.displayName);
   }

   public MutableComponent nameOwned(String name) {
      return Msg.of("actor.possessive", this.displayName, name);
   }

   public void sendUpdate(NetworkPacket<?> packet) {
   }

   public void sendMessage(Component component) {
   }

   public void awardExperience(BattlePokemon battlePokemon, int experience) {
   }

   public void win(List<? extends BattleActor> otherWinners, List<? extends BattleActor> losers) {
   }

   public void lose(List<? extends BattleActor> winners, List<? extends BattleActor> otherLosers) {
   }

   public List<BattlePokemon> mirrorTeam() {
      return Collections.unmodifiableList(this.getPokemonList());
   }
}
