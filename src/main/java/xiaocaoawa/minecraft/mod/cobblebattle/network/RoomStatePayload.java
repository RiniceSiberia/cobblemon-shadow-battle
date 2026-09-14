package xiaocaoawa.minecraft.mod.cobblebattle.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;

public record RoomStatePayload(
   String roomId,
   String inviteCode,
   String name,
   boolean locked,
   String battleType,
   int level,
   int pick,
   boolean fullHeal,
   boolean hostEngine,
   boolean legality,
   boolean fighting,
   RoomStatePayload.Member host,
   boolean hasGuest,
   RoomStatePayload.Member guest,
   List<RoomStatePayload.Member> watchers,
   String youAre
) implements CustomPacketPayload {
   public static final String HOST = "host";
   public static final String GUEST = "guest";
   public static final String WATCHER = "watcher";
   public static final Type<RoomStatePayload> TYPE = new Type(ResourceLocation.fromNamespaceAndPath("cobblebattle", "room_state"));
   public static final StreamCodec<RegistryFriendlyByteBuf, RoomStatePayload> CODEC = StreamCodec.of(
      (buf, s) -> {
         buf.writeUtf(s.roomId(), 16);
         buf.writeUtf(s.inviteCode(), 32);
         buf.writeUtf(s.name(), 64);
         buf.writeBoolean(s.locked());
         buf.writeUtf(s.battleType(), 32);
         buf.writeVarInt(s.level());
         buf.writeVarInt(s.pick());
         buf.writeBoolean(s.fullHeal());
         buf.writeBoolean(s.hostEngine());
         buf.writeBoolean(s.legality());
         buf.writeBoolean(s.fighting());
         RoomStatePayload.Member.CODEC.encode(buf, s.host());
         buf.writeBoolean(s.hasGuest());
         RoomStatePayload.Member.CODEC.encode(buf, s.guest());
         buf.writeVarInt(s.watchers().size());

         for (RoomStatePayload.Member watcher : s.watchers()) {
            RoomStatePayload.Member.CODEC.encode(buf, watcher);
         }

         buf.writeUtf(s.youAre(), 16);
      },
      buf -> {
         String roomId = buf.readUtf(16);
         String inviteCode = buf.readUtf(32);
         String name = buf.readUtf(64);
         boolean locked = buf.readBoolean();
         String battleType = buf.readUtf(32);
         int level = buf.readVarInt();
         int pick = buf.readVarInt();
         boolean fullHeal = buf.readBoolean();
         boolean hostEngine = buf.readBoolean();
         boolean legality = buf.readBoolean();
         boolean fighting = buf.readBoolean();
         RoomStatePayload.Member host = (RoomStatePayload.Member)RoomStatePayload.Member.CODEC.decode(buf);
         boolean hasGuest = buf.readBoolean();
         RoomStatePayload.Member guest = (RoomStatePayload.Member)RoomStatePayload.Member.CODEC.decode(buf);
         int count = Math.min(buf.readVarInt(), 256);
         ArrayList<RoomStatePayload.Member> observers = new ArrayList<>(count);

         for (int i = 0; i < count; i++) {
            observers.add((RoomStatePayload.Member)RoomStatePayload.Member.CODEC.decode(buf));
         }

         return new RoomStatePayload(
            roomId,
            inviteCode,
            name,
            locked,
            battleType,
            level,
            pick,
            fullHeal,
            hostEngine,
            legality,
            fighting,
            host,
            hasGuest,
            guest,
            List.copyOf(observers),
            buf.readUtf(16)
         );
      }
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public record Member(String name, long uid, String lead, int teamSize) {
      public static final RoomStatePayload.Member NOBODY = new RoomStatePayload.Member("", 0L, "", 0);
      public static final StreamCodec<RegistryFriendlyByteBuf, RoomStatePayload.Member> CODEC = StreamCodec.composite(
         ByteBufCodecs.stringUtf8(64),
         RoomStatePayload.Member::name,
         ByteBufCodecs.VAR_LONG,
         RoomStatePayload.Member::uid,
         ByteBufCodecs.stringUtf8(64),
         RoomStatePayload.Member::lead,
         ByteBufCodecs.VAR_INT,
         RoomStatePayload.Member::teamSize,
         RoomStatePayload.Member::new
      );
   }
}
