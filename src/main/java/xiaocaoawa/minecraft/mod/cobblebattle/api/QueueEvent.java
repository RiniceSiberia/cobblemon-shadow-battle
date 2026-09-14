package xiaocaoawa.minecraft.mod.cobblebattle.api;

import net.minecraft.server.level.ServerPlayer;

public record QueueEvent(ServerPlayer player, String rankedId, String rankedName, int waiting, String reason) {
}
