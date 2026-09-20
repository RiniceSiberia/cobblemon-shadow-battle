package xiaocaoawa.minecraft.mod.cobblebattle.network;

import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import io.github.rinicesiberia.shadowbattle.network.PayloadTypeCatalog;

public record ServerDexPayload(String digest, boolean unchanged, List<ServerDexPayload.Entry> entries) implements CustomPacketPayload {
   public static final Type<ServerDexPayload> TYPE = PayloadTypeCatalog.named("server_dex");
   public static final StreamCodec<RegistryFriendlyByteBuf, ServerDexPayload> CODEC = StreamCodec.composite(
      ByteBufCodecs.stringUtf8(128),
      ServerDexPayload::digest,
      ByteBufCodecs.BOOL,
      ServerDexPayload::unchanged,
      ServerDexPayload.Entry.CODEC.apply(ByteBufCodecs.list(16384)),
      ServerDexPayload::entries,
      ServerDexPayload::new
   );

   public static ServerDexPayload of(String digest, List<ServerDexPayload.Entry> entries) {
      return new ServerDexPayload(digest, false, entries);
   }

   public static ServerDexPayload unchanged(String digest) {
      return new ServerDexPayload(digest, true, List.of());
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public record Entry(String id, int hp, int atk, int def, int spa, int spd, int spe) {
      public static final StreamCodec<RegistryFriendlyByteBuf, ServerDexPayload.Entry> CODEC = StreamCodec.of(
         (buffer, entry) -> {
            buffer.writeUtf(entry.id(), 64);
            buffer.writeVarInt(entry.hp());
            buffer.writeVarInt(entry.atk());
            buffer.writeVarInt(entry.def());
            buffer.writeVarInt(entry.spa());
            buffer.writeVarInt(entry.spd());
            buffer.writeVarInt(entry.spe());
         },
         buffer -> new ServerDexPayload.Entry(
            buffer.readUtf(64), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt()
         )
      );
   }
}
