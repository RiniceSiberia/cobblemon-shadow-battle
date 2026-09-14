package xiaocaoawa.minecraft.mod.cobblebattle.api;

import net.minecraft.server.level.ServerPlayer;

public record BattleStartedEvent(ServerPlayer player, BattleInfo battle) {
}
