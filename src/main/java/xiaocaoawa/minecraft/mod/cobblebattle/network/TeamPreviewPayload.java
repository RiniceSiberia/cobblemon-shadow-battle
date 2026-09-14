package xiaocaoawa.minecraft.mod.cobblebattle.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;

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
   public static final Type<TeamPreviewPayload> TYPE = new Type(ResourceLocation.fromNamespaceAndPath("cobblebattle", "team_preview"));
   public static final StreamCodec<RegistryFriendlyByteBuf, TeamPreviewPayload> CODEC = StreamCodec.of(
      (buf, p) -> {
         buf.writeUtf(p.battleId(), 48);
         buf.writeUtf(p.you(), 64);
         buf.writeUtf(p.opponent(), 64);
         buf.writeUtf(p.opponentServer(), 64);
         buf.writeVarInt(p.pick());
         buf.writeVarInt(p.lead());
         buf.writeLong(p.deadlineMs());
         writeSlots(buf, p.mine());
         writeSlots(buf, p.theirs());
         buf.writeBoolean(p.mineReady());
         buf.writeBoolean(p.theirsReady());
         buf.writeUtf(p.closed(), 32);
      },
      buf -> new TeamPreviewPayload(
         buf.readUtf(48),
         buf.readUtf(64),
         buf.readUtf(64),
         buf.readUtf(64),
         buf.readVarInt(),
         buf.readVarInt(),
         buf.readLong(),
         readSlots(buf),
         readSlots(buf),
         buf.readBoolean(),
         buf.readBoolean(),
         buf.readUtf(32)
      )
   );

   private static void writeSlots(RegistryFriendlyByteBuf buf, List<TeamPreviewPayload.Slot> slots) {
      buf.writeVarInt(slots.size());

      for (TeamPreviewPayload.Slot slot : slots) {
         TeamPreviewPayload.Slot.CODEC.encode(buf, slot);
      }
   }

   private static List<TeamPreviewPayload.Slot> readSlots(RegistryFriendlyByteBuf buf) {
      int count = Math.min(buf.readVarInt(), 6);
      List<TeamPreviewPayload.Slot> outputStream = new ArrayList<>(count);

      for (int i = 0; i < count; i++) {
         outputStream.add((TeamPreviewPayload.Slot)TeamPreviewPayload.Slot.CODEC.decode(buf));
      }

      return outputStream;
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public record Slot(String species, int level, String gender, boolean shiny, String item) {
      public static final StreamCodec<RegistryFriendlyByteBuf, TeamPreviewPayload.Slot> CODEC = StreamCodec.of((buf, s) -> {
         buf.writeUtf(s.species(), 64);
         buf.writeVarInt(s.level());
         buf.writeUtf(s.gender(), 2);
         buf.writeBoolean(s.shiny());
         buf.writeUtf(s.item(), 96);
      }, buf -> new TeamPreviewPayload.Slot(buf.readUtf(64), buf.readVarInt(), buf.readUtf(2), buf.readBoolean(), buf.readUtf(96)));
   }
}
