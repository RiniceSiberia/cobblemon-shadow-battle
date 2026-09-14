package xiaocaoawa.minecraft.mod.cobblebattle.network;

import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;

public record RoomListPayload(List<RoomListPayload.Room> rooms, boolean refresh) implements CustomPacketPayload {
   public static final Type<RoomListPayload> TYPE = new Type(ResourceLocation.fromNamespaceAndPath("cobblebattle", "room_list"));
   public static final StreamCodec<RegistryFriendlyByteBuf, RoomListPayload> CODEC = StreamCodec.composite(
      RoomListPayload.Room.CODEC.apply(ByteBufCodecs.list(1024)), RoomListPayload::rooms, ByteBufCodecs.BOOL, RoomListPayload::refresh, RoomListPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public record Room(
      String id,
      String name,
      String host,
      long hostUid,
      String battleType,
      int level,
      int pick,
      boolean fullHeal,
      boolean locked,
      String lead,
      boolean hasGuest,
      int watchers,
      boolean mine,
      boolean hostEngine,
      boolean fighting,
      boolean legality
   ) {
      public static final StreamCodec<RegistryFriendlyByteBuf, RoomListPayload.Room> CODEC = StreamCodec.of(
         (buf, r) -> {
            buf.writeUtf(r.id(), 16);
            buf.writeUtf(r.name(), 64);
            buf.writeUtf(r.host(), 64);
            buf.writeVarLong(r.hostUid());
            buf.writeUtf(r.battleType(), 32);
            buf.writeVarInt(r.level());
            buf.writeVarInt(r.pick());
            buf.writeBoolean(r.fullHeal());
            buf.writeBoolean(r.locked());
            buf.writeUtf(r.lead(), 64);
            buf.writeBoolean(r.hasGuest());
            buf.writeVarInt(r.watchers());
            buf.writeBoolean(r.mine());
            buf.writeBoolean(r.hostEngine());
            buf.writeBoolean(r.fighting());
            buf.writeBoolean(r.legality());
         },
         buf -> new RoomListPayload.Room(
            buf.readUtf(16),
            buf.readUtf(64),
            buf.readUtf(64),
            buf.readVarLong(),
            buf.readUtf(32),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readBoolean(),
            buf.readBoolean(),
            buf.readUtf(64),
            buf.readBoolean(),
            buf.readVarInt(),
            buf.readBoolean(),
            buf.readBoolean(),
            buf.readBoolean(),
            buf.readBoolean()
         )
      );
   }
}
