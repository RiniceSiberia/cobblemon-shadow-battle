package xiaocaoawa.minecraft.mod.cobblebattle.battle;

import io.github.rinicesiberia.shadowbattle.battle.BattleProjectionIndex;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Consumer;

public final class CrossServerBattles {
    private static final BattleProjectionIndex BATTLE_INDEX = new BattleProjectionIndex(MirrorBattle::bindLocalBattleId, System::currentTimeMillis);
    private CrossServerBattles() {}
    public static void injecting(Runnable inputWriter) { BATTLE_INDEX.withInjectedInput(inputWriter); }
    public static void setChoiceRelay(Consumer<ChoiceRelay> choiceObserver) { BATTLE_INDEX.observeChoices(choiceObserver); }
    public static void setOutputRelay(Consumer<ChoiceRelay> outputObserver) { BATTLE_INDEX.observeOutput(outputObserver); }
    public static void beginConstruction(MirrorBattle mirrorBattle) { BATTLE_INDEX.prepareConstruction(mirrorBattle); }
    public static void endConstruction() { BATTLE_INDEX.completeConstruction(); }
    public static boolean claimStart(UUID localBattleId) { return BATTLE_INDEX.bindConstructedBattle(localBattleId); }
    public static boolean isMirror(UUID localBattleId) { return BATTLE_INDEX.containsEngineBattle(localBattleId); }
    public static MirrorBattle get(UUID localBattleId) { return BATTLE_INDEX.findByEngine(localBattleId); }
    public static UUID localIdFor(String remoteBattleId) { return BATTLE_INDEX.resolveEngineId(remoteBattleId); }
    public static MirrorBattle byRemoteId(String remoteBattleId) { return BATTLE_INDEX.findByUpstream(remoteBattleId); }
    public static boolean relayChoices(UUID localBattleId, String[] messages) { return BATTLE_INDEX.forwardChoices(localBattleId, messages); }
    public static boolean captureOutput(UUID localBattleId, String content) { return BATTLE_INDEX.forwardOutput(localBattleId, content); }
    public static MirrorBattle forget(UUID localBattleId) { return BATTLE_INDEX.removeBattle(localBattleId); }
    public static MirrorBattle byLocalPlayer(UUID participantUuid) { return BATTLE_INDEX.findByParticipant(participantUuid); }
    public static Collection<MirrorBattle> all() { return BATTLE_INDEX.snapshot(); }
    public static void clear() { BATTLE_INDEX.clearSessions(); }
    public static int size() { return BATTLE_INDEX.activeCount(); }
    public record ChoiceRelay(String remoteBattleId, String line) {}
}
