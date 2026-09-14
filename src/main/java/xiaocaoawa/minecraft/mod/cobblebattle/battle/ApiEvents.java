package xiaocaoawa.minecraft.mod.cobblebattle.battle;

import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xiaocaoawa.minecraft.mod.cobblebattle.api.AccountEvent;
import xiaocaoawa.minecraft.mod.cobblebattle.api.BattleEndedEvent;
import xiaocaoawa.minecraft.mod.cobblebattle.api.BattleInfo;
import xiaocaoawa.minecraft.mod.cobblebattle.api.BattleOutcome;
import xiaocaoawa.minecraft.mod.cobblebattle.api.BattleStartedEvent;
import xiaocaoawa.minecraft.mod.cobblebattle.api.CobbleBattleApi;
import xiaocaoawa.minecraft.mod.cobblebattle.api.QueueEvent;
import xiaocaoawa.minecraft.mod.cobblebattle.api.ScoreChange;
import xiaocaoawa.minecraft.mod.cobblebattle.api.SpectateEvent;

public final class ApiEvents {
   private static final Logger LOGGER = LoggerFactory.getLogger("CobbleBattle/API");

   private ApiEvents() {
   }

   public static void battleStarted(ServerPlayer player, BattleInfo battle) {
      fire(
         "BATTLE_STARTED",
         () -> ((CobbleBattleApi.BattleStarted)CobbleBattleApi.BATTLE_STARTED.invoker()).onBattleStarted(new BattleStartedEvent(player, battle))
      );
   }

   public static void battleEnded(ServerPlayer player, BattleInfo battle, BattleOutcome outcome, String reason, ScoreChange score) {
      fire(
         "BATTLE_ENDED",
         () -> ((CobbleBattleApi.BattleEnded)CobbleBattleApi.BATTLE_ENDED.invoker())
            .onBattleEnded(new BattleEndedEvent(player, battle, outcome, reason, score))
      );
   }

   public static void queueJoined(ServerPlayer player, String rankedId, String rankedName, int waiting) {
      fire(
         "QUEUE_JOINED",
         () -> ((CobbleBattleApi.QueueJoined)CobbleBattleApi.QUEUE_JOINED.invoker()).onQueueJoined(new QueueEvent(player, rankedId, rankedName, waiting, ""))
      );
   }

   public static void queueLeft(ServerPlayer player, String reason) {
      fire("QUEUE_LEFT", () -> ((CobbleBattleApi.QueueLeft)CobbleBattleApi.QUEUE_LEFT.invoker()).onQueueLeft(new QueueEvent(player, "", "", 0, reason)));
   }

   public static void signedIn(ServerPlayer player, String accountId, String nickname, long uid, boolean registered) {
      fire(
         "SIGNED_IN",
         () -> ((CobbleBattleApi.SignedIn)CobbleBattleApi.SIGNED_IN.invoker()).onSignedIn(new AccountEvent(player, accountId, nickname, uid, registered))
      );
   }

   public static void signedOut(ServerPlayer player, String accountId, String nickname, long uid) {
      fire(
         "SIGNED_OUT",
         () -> ((CobbleBattleApi.SignedOut)CobbleBattleApi.SIGNED_OUT.invoker()).onSignedOut(new AccountEvent(player, accountId, nickname, uid, false))
      );
   }

   public static void spectateStarted(ServerPlayer player, String battleId) {
      fire(
         "SPECTATE_STARTED",
         () -> ((CobbleBattleApi.SpectateStarted)CobbleBattleApi.SPECTATE_STARTED.invoker()).onSpectateStarted(new SpectateEvent(player, battleId))
      );
   }

   public static void spectateEnded(ServerPlayer player, String battleId) {
      fire(
         "SPECTATE_ENDED", () -> ((CobbleBattleApi.SpectateEnded)CobbleBattleApi.SPECTATE_ENDED.invoker()).onSpectateEnded(new SpectateEvent(player, battleId))
      );
   }

   private static void fire(String event, Runnable invoke) {
      try {
         invoke.run();
      } catch (Throwable var3) {
         LOGGER.error("A listener on CobbleBattleApi.{} threw. The battle is unaffected, but the listeners registered after it were not called.", event, var3);
      }
   }
}
