package xiaocaoawa.minecraft.mod.cobblebattle.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import io.github.rinicesiberia.shadowbattle.network.PayloadTypeCatalog;

public record ChatStatePayload(boolean signedIn, boolean inBattle, long uid, String name, boolean enabled) implements CustomPacketPayload {
   public static final Type<ChatStatePayload> TYPE = PayloadTypeCatalog.named("chat_state");
   public static final StreamCodec<RegistryFriendlyByteBuf, ChatStatePayload> CODEC = StreamCodec.composite(
      ByteBufCodecs.BOOL,
      ChatStatePayload::signedIn,
      ByteBufCodecs.BOOL,
      ChatStatePayload::inBattle,
      ByteBufCodecs.VAR_LONG,
      ChatStatePayload::uid,
      ByteBufCodecs.stringUtf8(64),
      ChatStatePayload::name,
      ByteBufCodecs.BOOL,
      ChatStatePayload::enabled,
      ChatStatePayload::new
   );

   public static ChatStatePayload signedOut() {
      return new ChatStatePayload(false, false, 0L, "", true);
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
