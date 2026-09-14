package xiaocaoawa.minecraft.mod.cobblebattle.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import io.github.rinicesiberia.shadowbattle.network.PayloadTypeCatalog;

public record SendChatPayload(String channel, String text) implements CustomPacketPayload {
   public static final Type<SendChatPayload> TYPE = PayloadTypeCatalog.named("send_chat");
   public static final StreamCodec<RegistryFriendlyByteBuf, SendChatPayload> CODEC = StreamCodec.composite(
      ByteBufCodecs.stringUtf8(16), SendChatPayload::channel, ByteBufCodecs.stringUtf8(256), SendChatPayload::text, SendChatPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
