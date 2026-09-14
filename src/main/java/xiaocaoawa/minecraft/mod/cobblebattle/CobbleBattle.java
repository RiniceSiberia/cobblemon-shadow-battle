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
   private static final Logger LOGGER = LoggerFactory.getLogger("CobbleBattle");
   private static CobbleBattleConfig config;
   private static CrossServerBattleService service;

   private CobbleBattle() {
   }

   public static void init() {
      config = CobbleBattleConfig.load();
      Msg.init(config.language);
      service = new CrossServerBattleService(config);
      CobbleBattleNetwork.init();
      LifecycleEvent.SERVER_STARTED.register((ServerState)server -> {
         RemoteTeamCodec.invalidateSpeciesCache();
         service.onServerStarted(server);
      });
      LifecycleEvent.SERVER_STOPPING.register((ServerState)server -> service.onServerStopping());
      CommandRegistrationEvent.EVENT.register((CommandRegistrationEvent)(dispatcher, registry, selection) -> MainCommand.register(dispatcher));
      PlayerEvent.PLAYER_QUIT.register((PlayerQuit)participant -> service.onPlayerDisconnect(participant));
      PlayerEvent.PLAYER_JOIN.register((PlayerJoin)player -> service.onPlayerJoin(player));
      EntityEvent.ADD.register((Add)(entity, level) -> {
         if (level.isClientSide()) {
            return EventResult.pass();
         } else if (MirrorNpc.isOrphan(entity)) {
            entity.discard();
            LOGGER.info("Removed a leftover mirror NPC at {}", entity.blockPosition());
            return EventResult.interruptFalse();
         } else {
            return MirrorPokemon.onEntityAdded(entity) ? EventResult.interruptFalse() : EventResult.pass();
         }
      });
      LOGGER.info("CobbleBattle initialised (serverId '{}', battle server {}:{})", new Object[]{ServerIdentity.get(), config.serverHost, config.serverPort});
   }

   public static CobbleBattleConfig config() {
      return config;
   }

   public static CrossServerBattleService service() {
      return service;
   }
}
