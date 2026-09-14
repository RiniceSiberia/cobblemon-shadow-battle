package xiaocaoawa.minecraft.mod.cobblebattle.battle;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.battles.ShowdownInterpreter;
import com.cobblemon.mod.common.entity.npc.NPCEntity;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import io.github.rinicesiberia.shadowbattle.battle.SequencedOutputBuffer;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xiaocaoawa.minecraft.mod.cobblebattle.api.BattleInfo;

public final class MirrorBattle {
   private static final Logger LOGGER = LoggerFactory.getLogger("CobbleBattle/Mirror");
   private final SequencedOutputBuffer outputBuffer;
   private final String remoteBattleId;
   private final String seat;
   private final String opponentSeat;
   private final UUID localParticipantUuid;
   private final UUID secondLocalParticipantUuid;
   private final String opponentName;
   private final String opponentServerId;
   private final boolean debug;
   private final boolean sourceOfTruth;
   private final boolean replayView;
   private final Set<UUID> observers = ConcurrentHashMap.newKeySet();
   private volatile UUID localBattleId;
   private volatile PokemonBattle battle;
   private final List<MirrorBattle.Body> bodies = new CopyOnWriteArrayList<>();
   private final List<PokemonEntity> props = new CopyOnWriteArrayList<>();
   private volatile BattleInfo battleDetails;
   private volatile BattleInfo secondBattleDetails;

   public static MirrorBattle spectator(String remoteBattleId, boolean debug) {
      return new MirrorBattle(remoteBattleId, null, null, null, null, "", "", false, debug, true);
   }

   public MirrorBattle(
      String remoteBattleId,
      String seat,
      String opponentSeat,
      UUID localParticipantUuid,
      String opponentName,
      String opponentServerId,
      boolean sourceOfTruth,
      boolean debug
   ) {
      this(remoteBattleId, seat, opponentSeat, localParticipantUuid, null, opponentName, opponentServerId, sourceOfTruth, debug, false);
   }

   public MirrorBattle(
      String remoteBattleId,
      String seat,
      String opponentSeat,
      UUID localParticipantUuid,
      UUID secondLocalParticipantUuid,
      String opponentName,
      String opponentServerId,
      boolean sourceOfTruth,
      boolean debug
   ) {
      this(remoteBattleId, seat, opponentSeat, localParticipantUuid, secondLocalParticipantUuid, opponentName, opponentServerId, sourceOfTruth, debug, false);
   }

   private MirrorBattle(
      String remoteBattleId,
      String seat,
      String opponentSeat,
      UUID localParticipantUuid,
      UUID secondLocalParticipantUuid,
      String opponentName,
      String opponentServerId,
      boolean sourceOfTruth,
      boolean debug,
      boolean replayView
   ) {
      this.outputBuffer = new SequencedOutputBuffer(remoteBattleId, replayView, this::apply);
      this.replayView = replayView;
      this.remoteBattleId = remoteBattleId;
      this.seat = seat;
      this.opponentSeat = opponentSeat;
      this.localParticipantUuid = localParticipantUuid;
      this.secondLocalParticipantUuid = secondLocalParticipantUuid;
      this.opponentName = opponentName;
      this.opponentServerId = opponentServerId;
      this.sourceOfTruth = sourceOfTruth;
      this.debug = debug;
   }

   public String remoteBattleId() {
      return this.remoteBattleId;
   }

   public String seat() {
      return this.seat;
   }

   public String opponentSeat() {
      return this.opponentSeat;
   }

   public boolean isAuthoritative() {
      return this.sourceOfTruth;
   }

   public boolean isSpectator() {
      return this.replayView;
   }

   public void addWatcher(UUID participantUuid) {
      this.observers.add(participantUuid);
   }

   public boolean removeWatcher(UUID participantUuid) {
      this.observers.remove(participantUuid);
      return this.observers.isEmpty();
   }

   public Set<UUID> watchers() {
      return Set.copyOf(this.observers);
   }

   public MirrorBattle.Routing route(String content) {
      if (this.bothLocal()) {
         return new MirrorBattle.Routing(true, true);
      } else {
         int nl = content.indexOf(10);
         String kind = nl == -1 ? content : content.substring(0, nl);
         if (!"sideupdate".equals(kind)) {
            return new MirrorBattle.Routing(true, true);
         } else {
            String rest = content.substring(nl + 1);
            int rnl = rest.indexOf(10);
            String target = (rnl == -1 ? rest : rest.substring(0, rnl)).trim();
            return target.equals(this.opponentSeat) ? new MirrorBattle.Routing(true, false) : new MirrorBattle.Routing(false, true);
         }
      }
   }

   public void attachBody(NPCEntity npc, RemoteBattleActor actor) {
      if (npc != null || actor != null) {
         this.bodies.add(new MirrorBattle.Body(npc, actor));
      }
   }

   public List<MirrorBattle.Body> bodies() {
      return List.copyOf(this.bodies);
   }

   void attachProp(PokemonEntity entity) {
      if (entity != null) {
         this.props.add(entity);
      }
   }

   List<PokemonEntity> takeProps() {
      List<PokemonEntity> taken = List.copyOf(this.props);
      this.props.clear();
      return taken;
   }

   public void describe(BattleInfo battleDetails) {
      this.battleDetails = battleDetails;
   }

   public BattleInfo info() {
      return this.battleDetails;
   }

   public List<MirrorBattle.Body> takeBodies() {
      List<MirrorBattle.Body> taken = List.copyOf(this.bodies);
      this.bodies.clear();
      return taken;
   }

   public UUID localPlayerUuid() {
      return this.localParticipantUuid;
   }

   public boolean bothLocal() {
      return this.secondLocalParticipantUuid != null;
   }

   public List<UUID> localPlayers() {
      if (this.localParticipantUuid == null) {
         return List.of();
      } else {
         return this.secondLocalParticipantUuid == null ? List.of(this.localParticipantUuid) : List.of(this.localParticipantUuid, this.secondLocalParticipantUuid);
      }
   }

   public boolean hasLocalPlayer(UUID participantUuid) {
      return participantUuid != null && (participantUuid.equals(this.localParticipantUuid) || participantUuid.equals(this.secondLocalParticipantUuid));
   }

   public String seatOf(UUID participantUuid) {
      if (participantUuid == null) {
         return null;
      } else if (participantUuid.equals(this.localParticipantUuid)) {
         return this.seat;
      } else {
         return participantUuid.equals(this.secondLocalParticipantUuid) ? this.opponentSeat : null;
      }
   }

   public void describeSecond(BattleInfo battleDetails) {
      this.secondBattleDetails = battleDetails;
   }

   public BattleInfo infoFor(UUID participantUuid) {
      if (participantUuid == null) {
         return null;
      } else if (participantUuid.equals(this.localParticipantUuid)) {
         return this.info();
      } else {
         return participantUuid.equals(this.secondLocalParticipantUuid) ? this.secondBattleDetails : null;
      }
   }

   public String opponentName() {
      return this.opponentName;
   }

   public String opponentServerId() {
      return this.opponentServerId;
   }

   public UUID localBattleId() {
      return this.localBattleId;
   }

   public PokemonBattle battle() {
      return this.battle;
   }

   public boolean isFinished() {
      return this.outputBuffer.isTerminated();
   }

   void bindLocalId(UUID id) {
      this.localBattleId = id;
   }

   public void attach(PokemonBattle battle) {
      this.battle = battle;
   }

   public void release() { this.outputBuffer.enableDelivery(); }

   public void accept(long sequence, String content) { this.outputBuffer.enqueue(sequence, content); }

   private void apply(String content) {
      UUID id = this.localBattleId;
      if (id == null) {
         LOGGER.error("Battle {} received output before the local battle existed", this.remoteBattleId);
      } else {
         if (this.debug) {
            LOGGER.info("[{}] << {}", this.remoteBattleId, content.replace("\n", " \\n "));
         }

         ShowdownInterpreter.INSTANCE.interpretMessage(id, content);
      }
   }

   public void markFinished() { this.outputBuffer.terminate(); }

   public boolean hasStalledChunks() { return this.outputBuffer.hasPendingOutput(); }

   public record Body(NPCEntity npc, RemoteBattleActor actor) {
   }

   public record Routing(boolean sendOn, boolean interpretOn) {
   }
}

