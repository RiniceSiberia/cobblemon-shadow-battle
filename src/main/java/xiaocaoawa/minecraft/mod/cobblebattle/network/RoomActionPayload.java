package xiaocaoawa.minecraft.mod.cobblebattle.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import io.github.rinicesiberia.shadowbattle.network.PayloadTypeCatalog;

public record RoomActionPayload(
   String action,
   String roomId,
   String name,
   String password,
   String battleType,
   int level,
   int pick,
   boolean fullHeal,
   boolean hostEngine,
   boolean legality,
   String inviteCode
) implements CustomPacketPayload {
   public static final String LIST = "list";
   public static final String CREATE = "create";
   public static final String JOIN = "join";
   public static final String JOIN_CODE = "join_code";
   public static final String LEAVE = "leave";
   public static final String START = "start";
   public static final Type<RoomActionPayload> TYPE = PayloadTypeCatalog.named("room_action");
   public static final StreamCodec<RegistryFriendlyByteBuf, RoomActionPayload> CODEC = StreamCodec.of(
      (buf, a) -> {
         buf.writeUtf(a.action(), 16);
         buf.writeUtf(a.roomId(), 16);
         buf.writeUtf(a.name(), 64);
         buf.writeUtf(a.password(), 32);
         buf.writeUtf(a.battleType(), 32);
         buf.writeVarInt(a.level());
         buf.writeVarInt(a.pick());
         buf.writeBoolean(a.fullHeal());
         buf.writeBoolean(a.hostEngine());
         buf.writeBoolean(a.legality());
         buf.writeUtf(a.inviteCode(), 32);
      },
      buf -> new RoomActionPayload(
         buf.readUtf(16),
         buf.readUtf(16),
         buf.readUtf(64),
         buf.readUtf(32),
         buf.readUtf(32),
         buf.readVarInt(),
         buf.readVarInt(),
         buf.readBoolean(),
         buf.readBoolean(),
         buf.readBoolean(),
         buf.readUtf(32)
      )
   );

   public static RoomActionPayload of(String action) {
      return new RoomActionPayload(action, "", "", "", "singles", -1, 6, true, false, true, "");
   }

   public static RoomActionPayload join(String roomId, String password, String battleType, boolean hostEngine, boolean legality) {
      return new RoomActionPayload("join", roomId, "", password, battleType, -1, 6, true, hostEngine, legality, "");
   }

   public static RoomActionPayload joinByCode(String inviteCode) {
      return new RoomActionPayload("join_code", "", "", "", "", -1, 6, true, false, true, inviteCode);
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
