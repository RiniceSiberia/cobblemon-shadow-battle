package xiaocaoawa.minecraft.mod.cobblebattle.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import io.github.rinicesiberia.shadowbattle.network.PayloadTypeCatalog;
import xiaocaoawa.minecraft.mod.cobblebattle.account.AuthMode;

public record OpenAuthScreenPayload(int mode, String suggestedId, boolean emailEnabled) implements CustomPacketPayload {
   public static final Type<OpenAuthScreenPayload> TYPE = PayloadTypeCatalog.named("open_auth");
   public static final StreamCodec<RegistryFriendlyByteBuf, OpenAuthScreenPayload> CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      OpenAuthScreenPayload::mode,
      ByteBufCodecs.stringUtf8(64),
      OpenAuthScreenPayload::suggestedId,
      ByteBufCodecs.BOOL,
      OpenAuthScreenPayload::emailEnabled,
      OpenAuthScreenPayload::new
   );

   public static OpenAuthScreenPayload of(AuthMode mode, String suggestedId, boolean emailEnabled) {
      return new OpenAuthScreenPayload(mode.ordinal(), suggestedId == null ? "" : suggestedId, emailEnabled);
   }

   public AuthMode authMode() {
      return AuthMode.byOrdinal(this.mode);
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
