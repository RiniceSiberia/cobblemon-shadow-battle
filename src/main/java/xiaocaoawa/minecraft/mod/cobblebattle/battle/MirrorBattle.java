package xiaocaoawa.minecraft.mod.cobblebattle.battle;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.battles.ShowdownInterpreter;
import com.cobblemon.mod.common.entity.npc.NPCEntity;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import io.github.rinicesiberia.shadowbattle.battle.MirrorEntityRegistry;
import io.github.rinicesiberia.shadowbattle.battle.MirrorParticipantState;
import io.github.rinicesiberia.shadowbattle.battle.SequencedOutputBuffer;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xiaocaoawa.minecraft.mod.cobblebattle.api.BattleInfo;

public final class MirrorBattle {
   private static final Logger MIRROR_LOG = LoggerFactory.getLogger("CobbleBattle/Mirror");
   private final SequencedOutputBuffer sequencedOutput;
   private final MirrorParticipantState participants;
   private final boolean protocolTraceEnabled;
   private volatile UUID registeredBattleId;
   private volatile PokemonBattle attachedBattle;
   private final MirrorEntityRegistry<MirrorBattle.Body, PokemonEntity> entities = new MirrorEntityRegistry<>();

   public static MirrorBattle spectator(String upstreamBattleId, boolean protocolTraceEnabled) {
      return new MirrorBattle(upstreamBattleId, null, null, null, null, "", "", false, protocolTraceEnabled, true);
   }

   public MirrorBattle(
      String upstreamBattleId,
      String primarySeat,
      String secondarySeat,
      UUID localParticipantUuid,
      String remoteParticipantName,
      String remoteServerId,
      boolean sourceOfTruth,
      boolean protocolTraceEnabled
   ) {
      this(
         upstreamBattleId,
         primarySeat,
         secondarySeat,
         localParticipantUuid,
         null,
         remoteParticipantName,
         remoteServerId,
         sourceOfTruth,
         protocolTraceEnabled,
         false
      );
   }

   public MirrorBattle(
      String upstreamBattleId,
      String primarySeat,
      String secondarySeat,
      UUID localParticipantUuid,
      UUID secondLocalParticipantUuid,
      String remoteParticipantName,
      String remoteServerId,
      boolean sourceOfTruth,
      boolean protocolTraceEnabled
   ) {
      this(
         upstreamBattleId,
         primarySeat,
         secondarySeat,
         localParticipantUuid,
         secondLocalParticipantUuid,
         remoteParticipantName,
         remoteServerId,
         sourceOfTruth,
         protocolTraceEnabled,
         false
      );
   }

   private MirrorBattle(
      String upstreamBattleId,
      String primarySeat,
      String secondarySeat,
      UUID localParticipantUuid,
      UUID secondLocalParticipantUuid,
      String remoteParticipantName,
      String remoteServerId,
      boolean sourceOfTruth,
      boolean protocolTraceEnabled,
      boolean replayView
   ) {
      this.sequencedOutput = new SequencedOutputBuffer(upstreamBattleId, replayView, this::deliverBufferedOutput);
      this.participants = new MirrorParticipantState(
         upstreamBattleId,
         primarySeat,
         secondarySeat,
         localParticipantUuid,
         secondLocalParticipantUuid,
         remoteParticipantName,
         remoteServerId,
         sourceOfTruth,
         replayView
      );
      this.protocolTraceEnabled = protocolTraceEnabled;
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
         int headerTerminator = content.indexOf(10);
         String messageType = headerTerminator == -1 ? content : content.substring(0, headerTerminator);
         if (!"sideupdate".equals(messageType)) {
            return new MirrorBattle.Routing(true, true);
         } else {
            String messageBody = content.substring(headerTerminator + 1);
            int seatTerminator = messageBody.indexOf(10);
            String targetSeat = (seatTerminator == -1 ? messageBody : messageBody.substring(0, seatTerminator)).trim();
            return targetSeat.equals(this.participants.secondarySeat())
               ? new MirrorBattle.Routing(true, false)
               : new MirrorBattle.Routing(false, true);
         }
      }
   }

   public void attachBody(NPCEntity mirrorNpc, RemoteBattleActor remoteActor) {
      if (mirrorNpc != null || remoteActor != null) {
         this.entities.addBody(new MirrorBattle.Body(mirrorNpc, remoteActor));
      }
   }

   public List<MirrorBattle.Body> bodies() {
      return this.entities.bodySnapshot();
   }

   void registerPropEntity(PokemonEntity propEntity) {
      this.entities.addProp(propEntity);
   }

   List<PokemonEntity> drainPropEntities() {
      return this.entities.takeProps();
   }

   public void describe(BattleInfo battleDetails) {
      this.participants.describePrimary(battleDetails);
   }

   public BattleInfo info() {
      return this.participants.primaryDescription();
   }

   public List<MirrorBattle.Body> takeBodies() {
      return this.entities.takeBodies();
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
      return this.registeredBattleId;
   }

   public PokemonBattle battle() {
      return this.attachedBattle;
   }

   public boolean isFinished() {
      return this.sequencedOutput.isTerminated();
   }

   void bindLocalBattleId(UUID boundBattleId) {
      this.registeredBattleId = boundBattleId;
   }

   public void attach(PokemonBattle attachedBattle) {
      this.attachedBattle = attachedBattle;
   }

   public void release() { this.sequencedOutput.enableDelivery(); }

   public void accept(long sequence, String content) { this.sequencedOutput.enqueue(sequence, content); }

   private void deliverBufferedOutput(String content) {
      UUID boundBattleId = this.registeredBattleId;
      if (boundBattleId == null) {
         MIRROR_LOG.error("Battle {} received output before the local battle existed", this.remoteBattleId());
      } else {
         if (this.protocolTraceEnabled) {
            MIRROR_LOG.info("[{}] << {}", this.remoteBattleId(), content.replace("\n", " \\n "));
         }

         ShowdownInterpreter.INSTANCE.interpretMessage(boundBattleId, content);
      }
   }

   public void markFinished() { this.sequencedOutput.terminate(); }

   public boolean hasStalledChunks() { return this.sequencedOutput.hasPendingOutput(); }

   public record Body(NPCEntity npc, RemoteBattleActor actor) {
   }

   public record Routing(boolean sendOn, boolean interpretOn) {
   }
}

