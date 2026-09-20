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
   public static final StreamCodec<RegistryFriendlyByteBuf, TeamPickPayload> CODEC = StreamCodec.of((buffer, payload) -> {
      buffer.writeUtf(payload.battleId(), 48);
      buffer.writeVarInt(payload.picks().size());

      for (int selectedSlot : payload.picks()) {
         buffer.writeVarInt(selectedSlot);
      }
   }, buffer -> {
      String battleIdentifier = buffer.readUtf(48);
      int selectionCount = Math.min(buffer.readVarInt(), 6);
      List<Integer> selectedSlots = new ArrayList<>(selectionCount);

      for (int selectionIndex = 0; selectionIndex < selectionCount; selectionIndex++) {
         selectedSlots.add(buffer.readVarInt());
      }

      return new TeamPickPayload(battleIdentifier, List.copyOf(selectedSlots));
   });

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
