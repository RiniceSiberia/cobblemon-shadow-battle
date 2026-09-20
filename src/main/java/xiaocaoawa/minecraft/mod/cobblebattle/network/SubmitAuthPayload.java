package xiaocaoawa.minecraft.mod.cobblebattle.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import io.github.rinicesiberia.shadowbattle.network.PayloadTypeCatalog;
import xiaocaoawa.minecraft.mod.cobblebattle.account.AuthMode;

public record SubmitAuthPayload(int mode, String accountId, String email, String password, String verificationCode) implements CustomPacketPayload {
   public static final Type<SubmitAuthPayload> TYPE = PayloadTypeCatalog.named("submit_auth");
   public static final StreamCodec<RegistryFriendlyByteBuf, SubmitAuthPayload> CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      SubmitAuthPayload::mode,
      ByteBufCodecs.stringUtf8(64),
      SubmitAuthPayload::accountId,
      ByteBufCodecs.stringUtf8(254),
      SubmitAuthPayload::email,
      ByteBufCodecs.stringUtf8(256),
      SubmitAuthPayload::password,
      ByteBufCodecs.stringUtf8(16),
      SubmitAuthPayload::verificationCode,
      SubmitAuthPayload::new
   );

   public static SubmitAuthPayload of(AuthMode authMode, String accountId, String email, String password, String verificationCode) {
      return new SubmitAuthPayload(
         authMode.ordinal(), accountId == null ? "" : accountId, email == null ? "" : email, password == null ? "" : password, verificationCode == null ? "" : verificationCode
      );
   }

   public AuthMode authMode() {
      return AuthMode.byOrdinal(this.mode);
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
