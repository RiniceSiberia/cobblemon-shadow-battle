package xiaocaoawa.minecraft.mod.cobblebattle.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import io.github.rinicesiberia.shadowbattle.network.PayloadTypeCatalog;

public record TeamPreviewPayload(
   String battleId,
   String you,
   String opponent,
   String opponentServer,
   int pick,
   int lead,
   long deadlineMs,
   List<TeamPreviewPayload.Slot> mine,
   List<TeamPreviewPayload.Slot> theirs,
   boolean mineReady,
   boolean theirsReady,
   String closed
) implements CustomPacketPayload {
   private static final int MAX_SLOTS = 6;
   public static final Type<TeamPreviewPayload> TYPE = PayloadTypeCatalog.named("team_preview");
   public static final StreamCodec<RegistryFriendlyByteBuf, TeamPreviewPayload> CODEC = StreamCodec.of(
      (buffer, preview) -> {
         buffer.writeUtf(preview.battleId(), 48);
         buffer.writeUtf(preview.you(), 64);
         buffer.writeUtf(preview.opponent(), 64);
         buffer.writeUtf(preview.opponentServer(), 64);
         buffer.writeVarInt(preview.pick());
         buffer.writeVarInt(preview.lead());
         buffer.writeLong(preview.deadlineMs());
         writeSlots(buffer, preview.mine());
         writeSlots(buffer, preview.theirs());
         buffer.writeBoolean(preview.mineReady());
         buffer.writeBoolean(preview.theirsReady());
         buffer.writeUtf(preview.closed(), 32);
      },
      buffer -> new TeamPreviewPayload(
         buffer.readUtf(48), buffer.readUtf(64), buffer.readUtf(64), buffer.readUtf(64),
         buffer.readVarInt(), buffer.readVarInt(), buffer.readLong(), readSlots(buffer), readSlots(buffer),
         buffer.readBoolean(), buffer.readBoolean(), buffer.readUtf(32)
      )
   );

   private static void writeSlots(RegistryFriendlyByteBuf buffer, List<TeamPreviewPayload.Slot> slots) {
      buffer.writeVarInt(slots.size());

      for (TeamPreviewPayload.Slot slotEntry : slots) {
         TeamPreviewPayload.Slot.CODEC.encode(buffer, slotEntry);
      }
   }

   private static List<TeamPreviewPayload.Slot> readSlots(RegistryFriendlyByteBuf buffer) {
      int slotCount = Math.min(buffer.readVarInt(), 6);
      List<TeamPreviewPayload.Slot> decodedSlots = new ArrayList<>(slotCount);

      for (int slotIndex = 0; slotIndex < slotCount; slotIndex++) {
         decodedSlots.add((TeamPreviewPayload.Slot)TeamPreviewPayload.Slot.CODEC.decode(buffer));
      }

      return decodedSlots;
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public record Slot(String species, int level, String gender, boolean shiny, String item) {
      public static final StreamCodec<RegistryFriendlyByteBuf, TeamPreviewPayload.Slot> CODEC = StreamCodec.of((buffer, slotEntry) -> {
         buffer.writeUtf(slotEntry.species(), 64);
         buffer.writeVarInt(slotEntry.level());
         buffer.writeUtf(slotEntry.gender(), 2);
         buffer.writeBoolean(slotEntry.shiny());
         buffer.writeUtf(slotEntry.item(), 96);
      }, buffer -> new TeamPreviewPayload.Slot(buffer.readUtf(64), buffer.readVarInt(), buffer.readUtf(2), buffer.readBoolean(), buffer.readUtf(96)));
   }
}
