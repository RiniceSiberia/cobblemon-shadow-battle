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
      (buffer, actionPayload) -> {
         buffer.writeUtf(actionPayload.action(), 16);
         buffer.writeUtf(actionPayload.roomId(), 16);
         buffer.writeUtf(actionPayload.name(), 64);
         buffer.writeUtf(actionPayload.password(), 32);
         buffer.writeUtf(actionPayload.battleType(), 32);
         buffer.writeVarInt(actionPayload.level());
         buffer.writeVarInt(actionPayload.pick());
         buffer.writeBoolean(actionPayload.fullHeal());
         buffer.writeBoolean(actionPayload.hostEngine());
         buffer.writeBoolean(actionPayload.legality());
         buffer.writeUtf(actionPayload.inviteCode(), 32);
      },
      buffer -> new RoomActionPayload(
         buffer.readUtf(16), buffer.readUtf(16), buffer.readUtf(64), buffer.readUtf(32), buffer.readUtf(32),
         buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(), buffer.readUtf(32)
      )
   );

   public static RoomActionPayload of(String actionName) {
      return new RoomActionPayload(actionName, "", "", "", "singles", -1, 6, true, false, true, "");
   }

   public static RoomActionPayload join(String roomIdentifier, String roomPassword, String battleFormat, boolean useHostEngine, boolean legalTeam) {
      return new RoomActionPayload("join", roomIdentifier, "", roomPassword, battleFormat, -1, 6, true, useHostEngine, legalTeam, "");
   }

   public static RoomActionPayload joinByCode(String invitationCode) {
      return new RoomActionPayload("join_code", "", "", "", "", -1, 6, true, false, true, invitationCode);
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
