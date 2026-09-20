package xiaocaoawa.minecraft.mod.cobblebattle;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.EntityEvent.Add;
import dev.architectury.event.events.common.LifecycleEvent.ServerState;
import dev.architectury.event.events.common.PlayerEvent.PlayerJoin;
import dev.architectury.event.events.common.PlayerEvent.PlayerQuit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xiaocaoawa.minecraft.mod.cobblebattle.battle.CrossServerBattleService;
import xiaocaoawa.minecraft.mod.cobblebattle.battle.MirrorNpc;
import xiaocaoawa.minecraft.mod.cobblebattle.battle.MirrorPokemon;
import xiaocaoawa.minecraft.mod.cobblebattle.battle.RemoteTeamCodec;
import xiaocaoawa.minecraft.mod.cobblebattle.command.MainCommand;
import xiaocaoawa.minecraft.mod.cobblebattle.config.CobbleBattleConfig;
import xiaocaoawa.minecraft.mod.cobblebattle.config.ServerIdentity;
import xiaocaoawa.minecraft.mod.cobblebattle.lang.Msg;
import xiaocaoawa.minecraft.mod.cobblebattle.network.CobbleBattleNetwork;

public final class CobbleBattle {
   public static final String MOD_ID = "cobblebattle";
   private static final Logger MOD_LOGGER = LoggerFactory.getLogger("CobbleBattle");
   private static CobbleBattleConfig activeConfig;
   private static CrossServerBattleService battleService;

   private CobbleBattle() {
   }

   public static void init() {
      activeConfig = CobbleBattleConfig.load();
      Msg.init(activeConfig.language);
      battleService = new CrossServerBattleService(activeConfig);
      CobbleBattleNetwork.init();
      LifecycleEvent.SERVER_STARTED.register((ServerState)startedServer -> {
         RemoteTeamCodec.invalidateSpeciesCache();
         battleService.onServerStarted(startedServer);
      });
      LifecycleEvent.SERVER_STOPPING.register((ServerState)stoppingServer -> battleService.onServerStopping());
      CommandRegistrationEvent.EVENT.register((CommandRegistrationEvent)(commandDispatcher, commandRegistry, environmentSelection) -> MainCommand.register(commandDispatcher));
      PlayerEvent.PLAYER_QUIT.register((PlayerQuit)leavingPlayer -> battleService.onPlayerDisconnect(leavingPlayer));
      PlayerEvent.PLAYER_JOIN.register((PlayerJoin)joiningPlayer -> battleService.onPlayerJoin(joiningPlayer));
      EntityEvent.ADD.register((Add)(addedEntity, entityLevel) -> {
         if (entityLevel.isClientSide()) {
            return EventResult.pass();
         } else if (MirrorNpc.isOrphan(addedEntity)) {
            addedEntity.discard();
            MOD_LOGGER.info("Removed a leftover mirror NPC at {}", addedEntity.blockPosition());
            return EventResult.interruptFalse();
         } else {
            return MirrorPokemon.onEntityAdded(addedEntity) ? EventResult.interruptFalse() : EventResult.pass();
         }
      });
      MOD_LOGGER.info("CobbleBattle initialised (serverId '{}', battle server {}:{})", new Object[]{ServerIdentity.get(), activeConfig.serverHost, activeConfig.serverPort});
   }

   public static CobbleBattleConfig config() {
      return activeConfig;
   }

   public static CrossServerBattleService service() {
      return battleService;
   }
}
