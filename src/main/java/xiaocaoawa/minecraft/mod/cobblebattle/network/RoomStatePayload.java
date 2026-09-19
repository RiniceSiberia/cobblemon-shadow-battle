package xiaocaoawa.minecraft.mod.cobblebattle.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import io.github.rinicesiberia.shadowbattle.network.PayloadTypeCatalog;

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
   public static final Type<RoomStatePayload> TYPE = PayloadTypeCatalog.named("room_state");
   public static final StreamCodec<RegistryFriendlyByteBuf, RoomStatePayload> CODEC = StreamCodec.of(
      (encoderBuffer, payload) -> {
         encoderBuffer.writeUtf(payload.roomId(), 16);
         encoderBuffer.writeUtf(payload.inviteCode(), 32);
         encoderBuffer.writeUtf(payload.name(), 64);
         encoderBuffer.writeBoolean(payload.locked());
         encoderBuffer.writeUtf(payload.battleType(), 32);
         encoderBuffer.writeVarInt(payload.level());
         encoderBuffer.writeVarInt(payload.pick());
         encoderBuffer.writeBoolean(payload.fullHeal());
         encoderBuffer.writeBoolean(payload.hostEngine());
         encoderBuffer.writeBoolean(payload.legality());
         encoderBuffer.writeBoolean(payload.fighting());
         RoomStatePayload.Member.CODEC.encode(encoderBuffer, payload.host());
         encoderBuffer.writeBoolean(payload.hasGuest());
         RoomStatePayload.Member.CODEC.encode(encoderBuffer, payload.guest());
         encoderBuffer.writeVarInt(payload.watchers().size());

         for (RoomStatePayload.Member watcherMember : payload.watchers()) {
            RoomStatePayload.Member.CODEC.encode(encoderBuffer, watcherMember);
         }

         encoderBuffer.writeUtf(payload.youAre(), 16);
      },
      decoderBuffer -> {
         String roomIdentifier = decoderBuffer.readUtf(16);
         String invitationCode = decoderBuffer.readUtf(32);
         String displayName = decoderBuffer.readUtf(64);
         boolean roomLocked = decoderBuffer.readBoolean();
         String battleFormat = decoderBuffer.readUtf(32);
         int levelCap = decoderBuffer.readVarInt();
         int selectionCount = decoderBuffer.readVarInt();
         boolean fullRestore = decoderBuffer.readBoolean();
         boolean hostEngineEnabled = decoderBuffer.readBoolean();
         boolean legalityEnforced = decoderBuffer.readBoolean();
         boolean battleInProgress = decoderBuffer.readBoolean();
         RoomStatePayload.Member hostMember = (RoomStatePayload.Member)RoomStatePayload.Member.CODEC.decode(decoderBuffer);
         boolean guestPresent = decoderBuffer.readBoolean();
         RoomStatePayload.Member guestMember = (RoomStatePayload.Member)RoomStatePayload.Member.CODEC.decode(decoderBuffer);
         int watcherCount = Math.min(decoderBuffer.readVarInt(), 256);
         ArrayList<RoomStatePayload.Member> watcherMembers = new ArrayList<>(watcherCount);

         for (int watcherIndex = 0; watcherIndex < watcherCount; watcherIndex++) {
            watcherMembers.add((RoomStatePayload.Member)RoomStatePayload.Member.CODEC.decode(decoderBuffer));
         }

         return new RoomStatePayload(
            roomIdentifier,
            invitationCode,
            displayName,
            roomLocked,
            battleFormat,
            levelCap,
            selectionCount,
            fullRestore,
            hostEngineEnabled,
            legalityEnforced,
            battleInProgress,
            hostMember,
            guestPresent,
            guestMember,
            List.copyOf(watcherMembers),
            decoderBuffer.readUtf(16)
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
