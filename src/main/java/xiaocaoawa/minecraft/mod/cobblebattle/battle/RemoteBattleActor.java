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
   private final String actorDisplayName;
   private final String originServerIdentifier;
   private final String battleSeat;

   public RemoteBattleActor(UUID participantUuid, String actorDisplayName, String originServerIdentifier, String battleSeat, List<BattlePokemon> remoteTeam) {
      super(participantUuid, new ArrayList<>(remoteTeam));
      this.actorDisplayName = actorDisplayName;
      this.originServerIdentifier = originServerIdentifier;
      this.battleSeat = battleSeat;
   }

   public String getOriginServerId() {
      return this.originServerIdentifier;
   }

   public String getSeat() {
      return this.battleSeat;
   }

   public ActorType getType() {
      return ActorType.NPC;
   }

   public MutableComponent getName() {
      return Component.literal(this.actorDisplayName);
   }

   public MutableComponent nameOwned(String ownedName) {
      return Msg.of("actor.possessive", this.actorDisplayName, ownedName);
   }

   public void sendUpdate(NetworkPacket<?> updatePacket) {
   }

   public void sendMessage(Component messageComponent) {
   }

   public void awardExperience(BattlePokemon battlePokemon, int experienceAmount) {
   }

   public void win(List<? extends BattleActor> coWinners, List<? extends BattleActor> losingActors) {
   }

   public void lose(List<? extends BattleActor> winningActors, List<? extends BattleActor> coLosers) {
   }

   public List<BattlePokemon> mirrorTeam() {
      return Collections.unmodifiableList(this.getPokemonList());
   }
}
