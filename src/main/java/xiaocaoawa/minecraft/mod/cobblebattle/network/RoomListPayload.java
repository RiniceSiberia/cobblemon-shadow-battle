package xiaocaoawa.minecraft.mod.cobblebattle.network;

import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import io.github.rinicesiberia.shadowbattle.network.PayloadTypeCatalog;

public record RoomListPayload(List<RoomListPayload.Room> rooms, boolean refresh) implements CustomPacketPayload {
   public static final Type<RoomListPayload> TYPE = PayloadTypeCatalog.named("room_list");
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
         (buffer, room) -> {
            buffer.writeUtf(room.id(), 16); buffer.writeUtf(room.name(), 64); buffer.writeUtf(room.host(), 64); buffer.writeVarLong(room.hostUid());
            buffer.writeUtf(room.battleType(), 32); buffer.writeVarInt(room.level()); buffer.writeVarInt(room.pick()); buffer.writeBoolean(room.fullHeal());
            buffer.writeBoolean(room.locked()); buffer.writeUtf(room.lead(), 64); buffer.writeBoolean(room.hasGuest()); buffer.writeVarInt(room.watchers());
            buffer.writeBoolean(room.mine()); buffer.writeBoolean(room.hostEngine()); buffer.writeBoolean(room.fighting()); buffer.writeBoolean(room.legality());
         },
         buffer -> new RoomListPayload.Room(
            buffer.readUtf(16), buffer.readUtf(64), buffer.readUtf(64), buffer.readVarLong(), buffer.readUtf(32), buffer.readVarInt(), buffer.readVarInt(),
            buffer.readBoolean(), buffer.readBoolean(), buffer.readUtf(64), buffer.readBoolean(), buffer.readVarInt(), buffer.readBoolean(),
            buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean()
         )
      );
   }
}
