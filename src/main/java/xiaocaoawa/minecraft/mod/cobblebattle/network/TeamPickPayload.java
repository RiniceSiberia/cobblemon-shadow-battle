package xiaocaoawa.minecraft.mod.cobblebattle.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import io.github.rinicesiberia.shadowbattle.network.PayloadTypeCatalog;

public record TeamPickPayload(String battleId, List<Integer> picks) implements CustomPacketPayload {
   private static final int MAX_PICKS = 6;
   public static final Type<TeamPickPayload> TYPE = PayloadTypeCatalog.named("team_pick");
   public static final StreamCodec<RegistryFriendlyByteBuf, TeamPickPayload> CODEC = StreamCodec.of((buf, p) -> {
      buf.writeUtf(p.battleId(), 48);
      buf.writeVarInt(p.picks().size());

      for (int pick : p.picks()) {
         buf.writeVarInt(pick);
      }
   }, buf -> {
      String battleId = buf.readUtf(48);
      int count = Math.min(buf.readVarInt(), 6);
      List<Integer> picks = new ArrayList<>(count);

      for (int i = 0; i < count; i++) {
         picks.add(buf.readVarInt());
      }

      return new TeamPickPayload(battleId, List.copyOf(picks));
   });

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
