package xiaocaoawa.minecraft.mod.cobblebattle.api;

import net.minecraft.server.level.ServerPlayer;

public record AccountEvent(ServerPlayer player, String accountId, String nickname, long uid, boolean registered) {
}
