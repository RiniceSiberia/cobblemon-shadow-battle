package xiaocaoawa.minecraft.mod.cobblebattle.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import io.github.rinicesiberia.shadowbattle.network.PayloadTypeCatalog;

public record MenuActionPayload(String action, String arg) implements CustomPacketPayload {
   public static final String QUEUE = "queue";
   public static final String LOGOUT = "logout";
   public static final Type<MenuActionPayload> TYPE = PayloadTypeCatalog.named("menu_action");
   public static final StreamCodec<RegistryFriendlyByteBuf, MenuActionPayload> CODEC = StreamCodec.composite(
      ByteBufCodecs.stringUtf8(16), MenuActionPayload::action, ByteBufCodecs.stringUtf8(64), MenuActionPayload::arg, MenuActionPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
