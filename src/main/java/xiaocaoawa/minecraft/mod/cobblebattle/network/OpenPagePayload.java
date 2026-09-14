package xiaocaoawa.minecraft.mod.cobblebattle.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import io.github.rinicesiberia.shadowbattle.network.PayloadTypeCatalog;

public record OpenPagePayload(String page, String ranked, String have) implements CustomPacketPayload {
   public static final String MAIN = "main";
   public static final String LEADERBOARD = "leaderboard";
   public static final String DEX = "dex";
   public static final String ROOMS = "rooms";
   public static final Type<OpenPagePayload> TYPE = PayloadTypeCatalog.named("open_page");
   public static final StreamCodec<RegistryFriendlyByteBuf, OpenPagePayload> CODEC = StreamCodec.composite(
      ByteBufCodecs.stringUtf8(16),
      OpenPagePayload::page,
      ByteBufCodecs.stringUtf8(64),
      OpenPagePayload::ranked,
      ByteBufCodecs.stringUtf8(128),
      OpenPagePayload::have,
      OpenPagePayload::new
   );

   public static OpenPagePayload of(String page) {
      return new OpenPagePayload(page, "", "");
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
