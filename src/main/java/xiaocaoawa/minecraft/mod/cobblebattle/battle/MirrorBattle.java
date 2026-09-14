package xiaocaoawa.minecraft.mod.cobblebattle.battle;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.battles.ShowdownInterpreter;
import com.cobblemon.mod.common.entity.npc.NPCEntity;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import io.github.rinicesiberia.shadowbattle.battle.MirrorParticipantState;
import io.github.rinicesiberia.shadowbattle.battle.SequencedOutputBuffer;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xiaocaoawa.minecraft.mod.cobblebattle.api.BattleInfo;

public final class MirrorBattle {
   private static final Logger LOGGER = LoggerFactory.getLogger("CobbleBattle/Mirror");
   private final SequencedOutputBuffer outputBuffer;
   private final MirrorParticipantState participants;
   private final boolean debug;
   private volatile UUID localBattleId;
   private volatile PokemonBattle battle;
   private final List<MirrorBattle.Body> bodies = new CopyOnWriteArrayList<>();
   private final List<PokemonEntity> props = new CopyOnWriteArrayList<>();

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
      this.participants = new MirrorParticipantState(
         remoteBattleId, seat, opponentSeat, localParticipantUuid, secondLocalParticipantUuid, opponentName, opponentServerId, sourceOfTruth, replayView
      );
      this.debug = debug;
   }

   public String remoteBattleId() {
      return this.participants.upstreamBattleId();
   }

   public String seat() {
      return this.participants.primarySeat();
   }

   public String opponentSeat() {
      return this.participants.secondarySeat();
   }

   public boolean isAuthoritative() {
      return this.participants.isAuthoritative();
   }

   public boolean isSpectator() {
      return this.participants.isSpectator();
   }

   public void addWatcher(UUID participantUuid) {
      this.participants.addObserver(participantUuid);
   }

   public boolean removeWatcher(UUID participantUuid) {
      return this.participants.removeObserver(participantUuid);
   }

   public Set<UUID> watchers() {
      return this.participants.observerSnapshot();
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
         return target.equals(this.participants.secondarySeat()) ? new MirrorBattle.Routing(true, false) : new MirrorBattle.Routing(false, true);
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
      this.participants.describePrimary(battleDetails);
   }

   public BattleInfo info() {
      return this.participants.primaryDescription();
   }

   public List<MirrorBattle.Body> takeBodies() {
      List<MirrorBattle.Body> taken = List.copyOf(this.bodies);
      this.bodies.clear();
      return taken;
   }

   public UUID localPlayerUuid() {
      return this.participants.primaryParticipant();
   }

   public boolean bothLocal() {
      return this.participants.hasTwoLocalParticipants();
   }

   public List<UUID> localPlayers() {
      return this.participants.localParticipants();
   }

   public boolean hasLocalPlayer(UUID participantUuid) {
      return this.participants.includes(participantUuid);
   }

   public String seatOf(UUID participantUuid) {
      return this.participants.seatOf(participantUuid);
   }

   public void describeSecond(BattleInfo battleDetails) {
      this.participants.describeSecondary(battleDetails);
   }

   public BattleInfo infoFor(UUID participantUuid) {
      return this.participants.descriptionFor(participantUuid);
   }

   public String opponentName() {
      return this.participants.remoteName();
   }

   public String opponentServerId() {
      return this.participants.remoteServer();
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
         LOGGER.error("Battle {} received output before the local battle existed", this.remoteBattleId());
      } else {
         if (this.debug) {
            LOGGER.info("[{}] << {}", this.remoteBattleId(), content.replace("\n", " \\n "));
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

