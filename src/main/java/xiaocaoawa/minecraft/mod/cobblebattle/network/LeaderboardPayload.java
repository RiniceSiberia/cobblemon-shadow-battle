package xiaocaoawa.minecraft.mod.cobblebattle.network;

import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import io.github.rinicesiberia.shadowbattle.network.PayloadTypeCatalog;

public record LeaderboardPayload(String rankedId, String name, int players, List<LeaderboardPayload.Entry> top, LeaderboardPayload.Entry you)
   implements CustomPacketPayload {
   public static final Type<LeaderboardPayload> TYPE = PayloadTypeCatalog.named("leaderboard");
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
         (buffer, entry) -> {
            buffer.writeVarInt(entry.rank());
            buffer.writeVarLong(entry.uid());
            buffer.writeUtf(entry.name(), 64);
            buffer.writeVarLong(entry.score());
            buffer.writeVarInt(entry.wins());
            buffer.writeVarInt(entry.losses());
            buffer.writeVarInt(entry.streak());
            buffer.writeUtf(entry.favourite(), 64);
         },
         buffer -> new LeaderboardPayload.Entry(
            buffer.readVarInt(), buffer.readVarLong(), buffer.readUtf(64), buffer.readVarLong(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readUtf(64)
         )
      );
      public static final LeaderboardPayload.Entry NONE = new LeaderboardPayload.Entry(0, 0L, "", 0L, 0, 0, 0, "");
   }
}
