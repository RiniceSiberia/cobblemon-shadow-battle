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
      public static final StreamCodec<RegistryFriendlyByteBuf, OpenMainMenuPayload.RankedInfo> CODEC = StreamCodec.of((buffer, rankedInfo) -> {
         buffer.writeUtf(rankedInfo.id(), 64);
         buffer.writeUtf(rankedInfo.name(), 64);
         buffer.writeUtf(rankedInfo.battleType(), 32);
         buffer.writeVarInt(rankedInfo.slots());
         buffer.writeVarInt(rankedInfo.adjustLevel());
         buffer.writeBoolean(rankedInfo.fullHeal());
         buffer.writeUtf(rankedInfo.winScore(), 128);
         buffer.writeUtf(rankedInfo.failScore(), 128);
         buffer.writeVarInt(rankedInfo.rules().size());

         for (String ruleText : rankedInfo.rules()) {
            buffer.writeUtf(ruleText, 128);
         }
      }, buffer -> {
         String formatId = buffer.readUtf(64);
         String formatName = buffer.readUtf(64);
         String battleFormat = buffer.readUtf(32);
         int teamSlots = buffer.readVarInt();
         int levelAdjustment = buffer.readVarInt();
         boolean restoresHealth = buffer.readBoolean();
         String victoryScore = buffer.readUtf(128);
         String defeatScore = buffer.readUtf(128);
         int ruleCount = Math.min(buffer.readVarInt(), 256);
         ArrayList<String> ruleTexts = new ArrayList<>(ruleCount);

         for (int ruleIndex = 0; ruleIndex < ruleCount; ruleIndex++) {
            ruleTexts.add(buffer.readUtf(128));
         }

         return new OpenMainMenuPayload.RankedInfo(formatId, formatName, battleFormat, teamSlots, levelAdjustment, restoresHealth, victoryScore, defeatScore, List.copyOf(ruleTexts));
      });
   }
}
