package xiaocaoawa.minecraft.mod.cobblebattle.network;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import io.github.rinicesiberia.shadowbattle.network.PayloadTypeCatalog;

public record OpenMainMenuPayload(String nickname, String favourite, List<OpenMainMenuPayload.RankedInfo> competitions) implements CustomPacketPayload {
   public static final Type<OpenMainMenuPayload> TYPE = PayloadTypeCatalog.named("open_main_menu");
   public static final StreamCodec<RegistryFriendlyByteBuf, OpenMainMenuPayload> CODEC = StreamCodec.composite(
      ByteBufCodecs.stringUtf8(64),
      OpenMainMenuPayload::nickname,
      ByteBufCodecs.stringUtf8(64),
      OpenMainMenuPayload::favourite,
      OpenMainMenuPayload.RankedInfo.CODEC.apply(ByteBufCodecs.list(256)),
      OpenMainMenuPayload::competitions,
      OpenMainMenuPayload::new
   );

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public record RankedInfo(
      String id, String name, String battleType, int slots, int adjustLevel, boolean fullHeal, String winScore, String failScore, List<String> rules
   ) {
      public static final StreamCodec<RegistryFriendlyByteBuf, OpenMainMenuPayload.RankedInfo> CODEC = StreamCodec.of((buf, r) -> {
         buf.writeUtf(r.id(), 64);
         buf.writeUtf(r.name(), 64);
         buf.writeUtf(r.battleType(), 32);
         buf.writeVarInt(r.slots());
         buf.writeVarInt(r.adjustLevel());
         buf.writeBoolean(r.fullHeal());
         buf.writeUtf(r.winScore(), 128);
         buf.writeUtf(r.failScore(), 128);
         buf.writeVarInt(r.rules().size());

         for (String rule : r.rules()) {
            buf.writeUtf(rule, 128);
         }
      }, buf -> {
         String id = buf.readUtf(64);
         String name = buf.readUtf(64);
         String type = buf.readUtf(32);
         int slots = buf.readVarInt();
         int level = buf.readVarInt();
         boolean heal = buf.readBoolean();
         String win = buf.readUtf(128);
         String fail = buf.readUtf(128);
         int count = Math.min(buf.readVarInt(), 256);
         ArrayList<String> rules = new ArrayList<>(count);

         for (int i = 0; i < count; i++) {
            rules.add(buf.readUtf(128));
         }

         return new OpenMainMenuPayload.RankedInfo(id, name, type, slots, level, heal, win, fail, List.copyOf(rules));
      });
   }
}
