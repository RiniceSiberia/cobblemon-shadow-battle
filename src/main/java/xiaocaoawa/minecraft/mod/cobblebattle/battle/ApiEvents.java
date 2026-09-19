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
   private static final Logger API_LOGGER = LoggerFactory.getLogger("CobbleBattle/API");

   private ApiEvents() {
   }

   public static void battleStarted(ServerPlayer participant, BattleInfo battleInfo) {
      dispatchEvent(
         "BATTLE_STARTED",
         () -> ((CobbleBattleApi.BattleStarted)CobbleBattleApi.BATTLE_STARTED.invoker()).onBattleStarted(new BattleStartedEvent(participant, battleInfo))
      );
   }

   public static void battleEnded(ServerPlayer participant, BattleInfo battleInfo, BattleOutcome battleOutcome, String endReason, ScoreChange scoreChange) {
      dispatchEvent(
         "BATTLE_ENDED",
         () -> ((CobbleBattleApi.BattleEnded)CobbleBattleApi.BATTLE_ENDED.invoker())
            .onBattleEnded(new BattleEndedEvent(participant, battleInfo, battleOutcome, endReason, scoreChange))
      );
   }

   public static void queueJoined(ServerPlayer participant, String rankedFormatId, String rankedFormatName, int waitingPlayers) {
      dispatchEvent(
         "QUEUE_JOINED",
         () -> ((CobbleBattleApi.QueueJoined)CobbleBattleApi.QUEUE_JOINED.invoker()).onQueueJoined(new QueueEvent(participant, rankedFormatId, rankedFormatName, waitingPlayers, ""))
      );
   }

   public static void queueLeft(ServerPlayer participant, String leaveReason) {
      dispatchEvent("QUEUE_LEFT", () -> ((CobbleBattleApi.QueueLeft)CobbleBattleApi.QUEUE_LEFT.invoker()).onQueueLeft(new QueueEvent(participant, "", "", 0, leaveReason)));
   }

   public static void signedIn(ServerPlayer participant, String externalAccountId, String displayLabel, long accountNumber, boolean isRegistered) {
      dispatchEvent(
         "SIGNED_IN",
         () -> ((CobbleBattleApi.SignedIn)CobbleBattleApi.SIGNED_IN.invoker()).onSignedIn(new AccountEvent(participant, externalAccountId, displayLabel, accountNumber, isRegistered))
      );
   }

   public static void signedOut(ServerPlayer participant, String externalAccountId, String displayLabel, long accountNumber) {
      dispatchEvent(
         "SIGNED_OUT",
         () -> ((CobbleBattleApi.SignedOut)CobbleBattleApi.SIGNED_OUT.invoker()).onSignedOut(new AccountEvent(participant, externalAccountId, displayLabel, accountNumber, false))
      );
   }

   public static void spectateStarted(ServerPlayer participant, String battleIdentifier) {
      dispatchEvent(
         "SPECTATE_STARTED",
         () -> ((CobbleBattleApi.SpectateStarted)CobbleBattleApi.SPECTATE_STARTED.invoker()).onSpectateStarted(new SpectateEvent(participant, battleIdentifier))
      );
   }

   public static void spectateEnded(ServerPlayer participant, String battleIdentifier) {
      dispatchEvent(
         "SPECTATE_ENDED", () -> ((CobbleBattleApi.SpectateEnded)CobbleBattleApi.SPECTATE_ENDED.invoker()).onSpectateEnded(new SpectateEvent(participant, battleIdentifier))
      );
   }

   private static void dispatchEvent(String eventName, Runnable listenerInvocation) {
      try {
         listenerInvocation.run();
      } catch (Throwable failure) {
         API_LOGGER.error("A listener on CobbleBattleApi.{} threw. The battle is unaffected, but the listeners registered after it were not called.", eventName, failure);
      }
   }
}
