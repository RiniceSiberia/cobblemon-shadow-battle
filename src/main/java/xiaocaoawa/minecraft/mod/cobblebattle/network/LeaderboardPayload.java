package xiaocaoawa.minecraft.mod.cobblebattle.network;

import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.resources.ResourceLocation;

public record LeaderboardPayload(String rankedId, String name, int players, List<LeaderboardPayload.Entry> top, LeaderboardPayload.Entry you)
   implements CustomPacketPayload {
   public static final Type<LeaderboardPayload> TYPE = new Type(ResourceLocation.fromNamespaceAndPath("cobblebattle", "leaderboard"));
   public static final StreamCodec<RegistryFriendlyByteBuf, LeaderboardPayload> CODEC = StreamCodec.composite(
      ByteBufCodecs.stringUtf8(64),
      LeaderboardPayload::rankedId,
      ByteBufCodecs.stringUtf8(64),
      LeaderboardPayload::name,
      ByteBufCodecs.VAR_INT,
      LeaderboardPayload::players,
      LeaderboardPayload.Entry.CODEC.apply(ByteBufCodecs.list(64)),
      LeaderboardPayload::top,
      LeaderboardPayload.Entry.CODEC,
      LeaderboardPayload::you,
      LeaderboardPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public record Entry(int rank, long uid, String name, long score, int wins, int losses, int streak, String favourite) {
      public static final StreamCodec<RegistryFriendlyByteBuf, LeaderboardPayload.Entry> CODEC = StreamCodec.of(
         (buf, e) -> {
            buf.writeVarInt(e.rank());
            buf.writeVarLong(e.uid());
            buf.writeUtf(e.name(), 64);
            buf.writeVarLong(e.score());
            buf.writeVarInt(e.wins());
            buf.writeVarInt(e.losses());
            buf.writeVarInt(e.streak());
            buf.writeUtf(e.favourite(), 64);
         },
         buf -> new LeaderboardPayload.Entry(
            buf.readVarInt(), buf.readVarLong(), buf.readUtf(64), buf.readVarLong(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readUtf(64)
         )
      );
      public static final LeaderboardPayload.Entry NONE = new LeaderboardPayload.Entry(0, 0L, "", 0L, 0, 0, 0, "");
   }
}
