package xiaocaoawa.minecraft.mod.cobblebattle.battle;

import io.github.rinicesiberia.shadowbattle.battle.BattleProjectionIndex;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Consumer;

public final class CrossServerBattles {
    private static final BattleProjectionIndex INDEX = new BattleProjectionIndex(MirrorBattle::bindLocalId, System::currentTimeMillis);
    private CrossServerBattles() {}
    public static void injecting(Runnable write) { INDEX.withInjectedInput(write); }
    public static void setChoiceRelay(Consumer<ChoiceRelay> relay) { INDEX.observeChoices(relay); }
    public static void setOutputRelay(Consumer<ChoiceRelay> relay) { INDEX.observeOutput(relay); }
    public static void beginConstruction(MirrorBattle mirror) { INDEX.prepareConstruction(mirror); }
    public static void endConstruction() { INDEX.completeConstruction(); }
    public static boolean claimStart(UUID localBattleId) { return INDEX.bindConstructedBattle(localBattleId); }
    public static boolean isMirror(UUID localBattleId) { return INDEX.containsEngineBattle(localBattleId); }
    public static MirrorBattle get(UUID localBattleId) { return INDEX.findByEngine(localBattleId); }
    public static UUID localIdFor(String remoteBattleId) { return INDEX.resolveEngineId(remoteBattleId); }
    public static MirrorBattle byRemoteId(String remoteBattleId) { return INDEX.findByUpstream(remoteBattleId); }
    public static boolean relayChoices(UUID localBattleId, String[] messages) { return INDEX.forwardChoices(localBattleId, messages); }
    public static boolean captureOutput(UUID localBattleId, String content) { return INDEX.forwardOutput(localBattleId, content); }
    public static MirrorBattle forget(UUID localBattleId) { return INDEX.removeBattle(localBattleId); }
    public static MirrorBattle byLocalPlayer(UUID participantUuid) { return INDEX.findByParticipant(participantUuid); }
    public static Collection<MirrorBattle> all() { return INDEX.snapshot(); }
    public static void clear() { INDEX.clearSessions(); }
    public static int size() { return INDEX.activeCount(); }
    public record ChoiceRelay(String remoteBattleId, String line) {}
}
