package xiaocaoawa.minecraft.mod.cobblebattle.api;

import net.minecraft.server.level.ServerPlayer;

public record SpectateEvent(ServerPlayer player, String battleId) {
}
