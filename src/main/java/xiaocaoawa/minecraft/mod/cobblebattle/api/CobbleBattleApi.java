package xiaocaoawa.minecraft.mod.cobblebattle.api;

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;

public final class CobbleBattleApi {
   public static final Event<CobbleBattleApi.BattleStarted> BATTLE_STARTED = EventFactory.createLoop(CobbleBattleApi.BattleStarted.class);
   public static final Event<CobbleBattleApi.BattleEnded> BATTLE_ENDED = EventFactory.createLoop(CobbleBattleApi.BattleEnded.class);
   public static final Event<CobbleBattleApi.QueueJoined> QUEUE_JOINED = EventFactory.createLoop(CobbleBattleApi.QueueJoined.class);
   public static final Event<CobbleBattleApi.QueueLeft> QUEUE_LEFT = EventFactory.createLoop(CobbleBattleApi.QueueLeft.class);
   public static final Event<CobbleBattleApi.SignedIn> SIGNED_IN = EventFactory.createLoop(CobbleBattleApi.SignedIn.class);
   public static final Event<CobbleBattleApi.SignedOut> SIGNED_OUT = EventFactory.createLoop(CobbleBattleApi.SignedOut.class);
   public static final Event<CobbleBattleApi.SpectateStarted> SPECTATE_STARTED = EventFactory.createLoop(CobbleBattleApi.SpectateStarted.class);
   public static final Event<CobbleBattleApi.SpectateEnded> SPECTATE_ENDED = EventFactory.createLoop(CobbleBattleApi.SpectateEnded.class);

   private CobbleBattleApi() {
   }

   @FunctionalInterface
   public interface BattleEnded {
      void onBattleEnded(BattleEndedEvent var1);
   }

   @FunctionalInterface
   public interface BattleStarted {
      void onBattleStarted(BattleStartedEvent var1);
   }

   @FunctionalInterface
   public interface QueueJoined {
      void onQueueJoined(QueueEvent var1);
   }

   @FunctionalInterface
   public interface QueueLeft {
      void onQueueLeft(QueueEvent var1);
   }

   @FunctionalInterface
   public interface SignedIn {
      void onSignedIn(AccountEvent var1);
   }

   @FunctionalInterface
   public interface SignedOut {
      void onSignedOut(AccountEvent var1);
   }

   @FunctionalInterface
   public interface SpectateEnded {
      void onSpectateEnded(SpectateEvent var1);
   }

   @FunctionalInterface
   public interface SpectateStarted {
      void onSpectateStarted(SpectateEvent var1);
   }
}
