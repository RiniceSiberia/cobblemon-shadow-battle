package xiaocaoawa.minecraft.mod.cobblebattle.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import io.github.rinicesiberia.shadowbattle.network.PayloadTypeCatalog;

public record AuthResultPayload(boolean ok, String message) implements CustomPacketPayload {
   public static final Type<AuthResultPayload> TYPE = PayloadTypeCatalog.named("auth_result");
   public static final StreamCodec<RegistryFriendlyByteBuf, AuthResultPayload> CODEC = StreamCodec.composite(
      ByteBufCodecs.BOOL, AuthResultPayload::ok, ByteBufCodecs.stringUtf8(512), AuthResultPayload::message, AuthResultPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
