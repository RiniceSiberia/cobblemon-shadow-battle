package xiaocaoawa.minecraft.mod.cobblebattle.network;

import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;

public record ServerDexPayload(String digest, boolean unchanged, List<ServerDexPayload.Entry> entries) implements CustomPacketPayload {
   public static final Type<ServerDexPayload> TYPE = new Type(ResourceLocation.fromNamespaceAndPath("cobblebattle", "server_dex"));
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
         (buf, e) -> {
            buf.writeUtf(e.id(), 64);
            buf.writeVarInt(e.hp());
            buf.writeVarInt(e.atk());
            buf.writeVarInt(e.def());
            buf.writeVarInt(e.spa());
            buf.writeVarInt(e.spd());
            buf.writeVarInt(e.spe());
         },
         buf -> new ServerDexPayload.Entry(
            buf.readUtf(64), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt()
         )
      );
   }
}
