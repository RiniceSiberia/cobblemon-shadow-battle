package xiaocaoawa.minecraft.mod.cobblebattle.network;

import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;

public record ChatLinePayload(String channel, long uid, String id, String name, UUID sender, String text) implements CustomPacketPayload {
   public static final Type<ChatLinePayload> TYPE = new Type(ResourceLocation.fromNamespaceAndPath("cobblebattle", "chat_line"));
   public static final StreamCodec<RegistryFriendlyByteBuf, ChatLinePayload> CODEC = StreamCodec.composite(
      ByteBufCodecs.stringUtf8(16),
      ChatLinePayload::channel,
      ByteBufCodecs.VAR_LONG,
      ChatLinePayload::uid,
      ByteBufCodecs.stringUtf8(64),
      ChatLinePayload::id,
      ByteBufCodecs.stringUtf8(64),
      ChatLinePayload::name,
      UUIDUtil.STREAM_CODEC,
      ChatLinePayload::sender,
      ByteBufCodecs.stringUtf8(256),
      ChatLinePayload::text,
      ChatLinePayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
