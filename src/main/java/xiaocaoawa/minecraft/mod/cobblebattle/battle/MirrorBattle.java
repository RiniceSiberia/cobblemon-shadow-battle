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
   private final UUID localPlayerUuid;
   private final UUID secondLocalPlayerUuid;
   private final String opponentName;
   private final String opponentServerId;
   private final boolean debug;
   private final boolean authoritative;
   private final boolean spectator;
   private final Set<UUID> watchers = ConcurrentHashMap.newKeySet();
   private volatile UUID localBattleId;
   private volatile PokemonBattle battle;
   private final List<MirrorBattle.Body> bodies = new CopyOnWriteArrayList<>();
   private final List<PokemonEntity> props = new CopyOnWriteArrayList<>();
   private volatile BattleInfo info;
   private volatile BattleInfo secondInfo;

   public static MirrorBattle spectator(String remoteBattleId, boolean debug) {
      return new MirrorBattle(remoteBattleId, null, null, null, null, "", "", false, debug, true);
   }

   public MirrorBattle(
      String remoteBattleId,
      String seat,
      String opponentSeat,
      UUID localPlayerUuid,
      String opponentName,
      String opponentServerId,
      boolean authoritative,
      boolean debug
   ) {
      this(remoteBattleId, seat, opponentSeat, localPlayerUuid, null, opponentName, opponentServerId, authoritative, debug, false);
   }

   public MirrorBattle(
      String remoteBattleId,
      String seat,
      String opponentSeat,
      UUID localPlayerUuid,
      UUID secondLocalPlayerUuid,
      String opponentName,
      String opponentServerId,
      boolean authoritative,
      boolean debug
   ) {
      this(remoteBattleId, seat, opponentSeat, localPlayerUuid, secondLocalPlayerUuid, opponentName, opponentServerId, authoritative, debug, false);
   }

   private MirrorBattle(
      String remoteBattleId,
      String seat,
      String opponentSeat,
      UUID localPlayerUuid,
      UUID secondLocalPlayerUuid,
      String opponentName,
      String opponentServerId,
      boolean authoritative,
      boolean debug,
      boolean spectator
   ) {
      this.outputBuffer = new SequencedOutputBuffer(remoteBattleId, spectator, this::apply);
      this.spectator = spectator;
      this.remoteBattleId = remoteBattleId;
      this.seat = seat;
      this.opponentSeat = opponentSeat;
      this.localPlayerUuid = localPlayerUuid;
      this.secondLocalPlayerUuid = secondLocalPlayerUuid;
      this.opponentName = opponentName;
      this.opponentServerId = opponentServerId;
      this.authoritative = authoritative;
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
      return this.authoritative;
   }

   public boolean isSpectator() {
      return this.spectator;
   }

   public void addWatcher(UUID playerUuid) {
      this.watchers.add(playerUuid);
   }

   public boolean removeWatcher(UUID playerUuid) {
      this.watchers.remove(playerUuid);
      return this.watchers.isEmpty();
   }

   public Set<UUID> watchers() {
      return Set.copyOf(this.watchers);
   }

   public MirrorBattle.Routing route(String chunk) {
      if (this.bothLocal()) {
         return new MirrorBattle.Routing(true, true);
      } else {
         int nl = chunk.indexOf(10);
         String kind = nl == -1 ? chunk : chunk.substring(0, nl);
         if (!"sideupdate".equals(kind)) {
            return new MirrorBattle.Routing(true, true);
         } else {
            String rest = chunk.substring(nl + 1);
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

   public void describe(BattleInfo info) {
      this.info = info;
   }

   public BattleInfo info() {
      return this.info;
   }

   public List<MirrorBattle.Body> takeBodies() {
      List<MirrorBattle.Body> taken = List.copyOf(this.bodies);
      this.bodies.clear();
      return taken;
   }

   public UUID localPlayerUuid() {
      return this.localPlayerUuid;
   }

   public boolean bothLocal() {
      return this.secondLocalPlayerUuid != null;
   }

   public List<UUID> localPlayers() {
      if (this.localPlayerUuid == null) {
         return List.of();
      } else {
         return this.secondLocalPlayerUuid == null ? List.of(this.localPlayerUuid) : List.of(this.localPlayerUuid, this.secondLocalPlayerUuid);
      }
   }

   public boolean hasLocalPlayer(UUID playerUuid) {
      return playerUuid != null && (playerUuid.equals(this.localPlayerUuid) || playerUuid.equals(this.secondLocalPlayerUuid));
   }

   public String seatOf(UUID playerUuid) {
      if (playerUuid == null) {
         return null;
      } else if (playerUuid.equals(this.localPlayerUuid)) {
         return this.seat;
      } else {
         return playerUuid.equals(this.secondLocalPlayerUuid) ? this.opponentSeat : null;
      }
   }

   public void describeSecond(BattleInfo info) {
      this.secondInfo = info;
   }

   public BattleInfo infoFor(UUID playerUuid) {
      if (playerUuid == null) {
         return null;
      } else if (playerUuid.equals(this.localPlayerUuid)) {
         return this.info();
      } else {
         return playerUuid.equals(this.secondLocalPlayerUuid) ? this.secondInfo : null;
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

   public void accept(long seq, String chunk) { this.outputBuffer.enqueue(seq, chunk); }

   private void apply(String chunk) {
      UUID id = this.localBattleId;
      if (id == null) {
         LOGGER.error("Battle {} received output before the local battle existed", this.remoteBattleId);
      } else {
         if (this.debug) {
            LOGGER.info("[{}] << {}", this.remoteBattleId, chunk.replace("\n", " \\n "));
         }

         ShowdownInterpreter.INSTANCE.interpretMessage(id, chunk);
      }
   }

   public void markFinished() { this.outputBuffer.terminate(); }

   public boolean hasStalledChunks() { return this.outputBuffer.hasPendingOutput(); }

   public record Body(NPCEntity npc, RemoteBattleActor actor) {
   }

   public record Routing(boolean sendOn, boolean interpretOn) {
   }
}

