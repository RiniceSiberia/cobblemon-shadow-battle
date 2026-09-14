package xiaocaoawa.minecraft.mod.cobblebattle.battle;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class CrossServerBattles {
   private static final Map<UUID, MirrorBattle> BY_LOCAL_ID = new ConcurrentHashMap<>();
   private static final Map<String, UUID> LOCAL_ID_BY_REMOTE_ID = new ConcurrentHashMap<>();
   private static final Map<UUID, Long> WITHOUT_SIMULATOR = new ConcurrentHashMap<>();
   private static final long WITHOUT_SIMULATOR_MEMORY_MS = 120000L;
   private static final ThreadLocal<MirrorBattle> UNDER_CONSTRUCTION = new ThreadLocal<>();
   private static final ThreadLocal<Boolean> INJECTING = ThreadLocal.withInitial(() -> Boolean.FALSE);
   private static volatile Consumer<CrossServerBattles.ChoiceRelay> choiceRelay = relay -> {};
   private static volatile Consumer<CrossServerBattles.ChoiceRelay> outputRelay = relay -> {};

   private CrossServerBattles() {
   }

   public static void injecting(Runnable write) {
      INJECTING.set(Boolean.TRUE);

      try {
         write.run();
      } finally {
         INJECTING.remove();
      }
   }

   public static void setChoiceRelay(Consumer<CrossServerBattles.ChoiceRelay> relay) {
      choiceRelay = relay;
   }

   public static void setOutputRelay(Consumer<CrossServerBattles.ChoiceRelay> relay) {
      outputRelay = relay;
   }

   public static void beginConstruction(MirrorBattle mirror) {
      UNDER_CONSTRUCTION.set(mirror);
   }

   public static void endConstruction() {
      UNDER_CONSTRUCTION.remove();
   }

   public static boolean claimStart(UUID localBattleId) {
      MirrorBattle mirror = UNDER_CONSTRUCTION.get();
      if (mirror == null) {
         return false;
      } else {
         mirror.bindLocalId(localBattleId);
         BY_LOCAL_ID.put(localBattleId, mirror);
         LOCAL_ID_BY_REMOTE_ID.put(mirror.remoteBattleId(), localBattleId);
         return !mirror.isAuthoritative();
      }
   }

   public static boolean isMirror(UUID localBattleId) {
      return BY_LOCAL_ID.containsKey(localBattleId);
   }

   public static MirrorBattle get(UUID localBattleId) {
      return BY_LOCAL_ID.get(localBattleId);
   }

   public static UUID localIdFor(String remoteBattleId) {
      return LOCAL_ID_BY_REMOTE_ID.get(remoteBattleId);
   }

   public static MirrorBattle byRemoteId(String remoteBattleId) {
      UUID localId = LOCAL_ID_BY_REMOTE_ID.get(remoteBattleId);
      return localId == null ? null : BY_LOCAL_ID.get(localId);
   }

   public static boolean relayChoices(UUID localBattleId, String[] messages) {
      MirrorBattle mirror = BY_LOCAL_ID.get(localBattleId);
      if (mirror == null) {
         return hadNoSimulator(localBattleId);
      } else if (!mirror.isAuthoritative()) {
         if (mirror.isFinished()) {
            return true;
         } else {
            for (String line : messages) {
               if (line != null && !line.isBlank()) {
                  choiceRelay.accept(new CrossServerBattles.ChoiceRelay(mirror.remoteBattleId(), line));
               }
            }

            return true;
         }
      } else if (INJECTING.get()) {
         return false;
      } else {
         for (String linex : messages) {
            if (linex != null && !linex.isBlank()) {
               String head = linex.startsWith(">") ? linex.substring(1).trim() : linex.trim();
               if (head.startsWith("forcelose") || head.startsWith("forcetie")) {
                  choiceRelay.accept(new CrossServerBattles.ChoiceRelay(mirror.remoteBattleId(), linex));
               }
            }
         }

         return false;
      }
   }

   public static boolean captureOutput(UUID localBattleId, String chunk) {
      MirrorBattle mirror = BY_LOCAL_ID.get(localBattleId);
      if (mirror != null && mirror.isAuthoritative() && chunk != null && !chunk.isEmpty()) {
         MirrorBattle.Routing routing = mirror.route(chunk);
         if (routing.sendOn()) {
            outputRelay.accept(new CrossServerBattles.ChoiceRelay(mirror.remoteBattleId(), chunk));
         }

         return !routing.interpretOn();
      } else {
         return false;
      }
   }

   public static MirrorBattle forget(UUID localBattleId) {
      MirrorBattle mirror = BY_LOCAL_ID.remove(localBattleId);
      if (mirror != null) {
         LOCAL_ID_BY_REMOTE_ID.remove(mirror.remoteBattleId());
         if (!mirror.isAuthoritative()) {
            long now = System.currentTimeMillis();
            WITHOUT_SIMULATOR.values().removeIf(at -> now - at > 120000L);
            WITHOUT_SIMULATOR.put(localBattleId, now);
         }
      }

      return mirror;
   }

   private static boolean hadNoSimulator(UUID localBattleId) {
      Long endedAt = WITHOUT_SIMULATOR.get(localBattleId);
      if (endedAt == null) {
         return false;
      } else if (System.currentTimeMillis() - endedAt > 120000L) {
         WITHOUT_SIMULATOR.remove(localBattleId);
         return false;
      } else {
         return true;
      }
   }

   public static MirrorBattle byLocalPlayer(UUID playerUuid) {
      for (MirrorBattle mirror : BY_LOCAL_ID.values()) {
         if (mirror.hasLocalPlayer(playerUuid)) {
            return mirror;
         }
      }

      return null;
   }

   public static Collection<MirrorBattle> all() {
      return List.copyOf(BY_LOCAL_ID.values());
   }

   public static void clear() {
      BY_LOCAL_ID.clear();
      LOCAL_ID_BY_REMOTE_ID.clear();
      WITHOUT_SIMULATOR.clear();
   }

   public static int size() {
      return BY_LOCAL_ID.size();
   }

   public record ChoiceRelay(String remoteBattleId, String line) {
   }
}
