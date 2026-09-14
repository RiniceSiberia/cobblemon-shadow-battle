package xiaocaoawa.minecraft.mod.cobblebattle.battle;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xiaocaoawa.minecraft.mod.cobblebattle.lang.Msg;
import xiaocaoawa.minecraft.mod.cobblebattle.net.BattleServerClient;
import xiaocaoawa.minecraft.mod.cobblebattle.network.CobbleBattleNetwork;
import xiaocaoawa.minecraft.mod.cobblebattle.network.TeamPreviewPayload;

final class TeamPreviews {
   private static final Logger LOGGER = LoggerFactory.getLogger("CobbleBattle");
   private final CrossServerBattleService service;
   private final Map<UUID, TeamPreviews.Session> open = new ConcurrentHashMap<>();

   TeamPreviews(CrossServerBattleService service) {
      this.service = service;
   }

   void clear() {
      this.open.clear();
   }

   void forget(UUID playerUuid) {
      this.open.remove(playerUuid);
   }

   void onOpen(JsonObject message) {
      String battleId = BattleServerClient.str(message, "battleId", null);
      String playerRaw = BattleServerClient.str(message, "yourPlayer", null);
      String seat = BattleServerClient.str(message, "yourSeat", null);
      if (battleId != null && playerRaw != null && seat != null) {
         UUID playerUuid;
         try {
            playerUuid = UUID.fromString(playerRaw);
         } catch (IllegalArgumentException var15) {
            LOGGER.error("preview_open named a player that is not a uuid: {}", playerRaw);
            return;
         }

         JsonObject rosters = message.getAsJsonObject("rosters");
         List<TeamPreviewPayload.Slot> mine = readRoster(rosters, seat);
         List<TeamPreviewPayload.Slot> theirs = readRoster(rosters, otherSeat(rosters, seat));
         int pick = BattleServerClient.integer(message, "pick", mine.size());
         int lead = Math.max(1, BattleServerClient.integer(message, "lead", 1));
         long deadline = deadlineOf(message);
         ServerPlayer player = this.service.playerOf(playerUuid);
         if (player == null) {
            LOGGER.warn("preview_open for {} but they are not on this server", playerUuid);
            this.send(battleId, playerUuid, frontOf(mine.size(), pick));
         } else {
            TeamPreviewPayload shown = new TeamPreviewPayload(
               battleId,
               player.getGameProfile().getName(),
               BattleServerClient.str(message, "opponent", ""),
               BattleServerClient.str(message, "opponentServer", ""),
               pick,
               lead,
               deadline,
               mine,
               theirs,
               false,
               false,
               ""
            );
            if (!CobbleBattleNetwork.sendTeamPreview(player, shown)) {
               LOGGER.info("{} has no CobbleBattle client; picking the first {} of their team", playerUuid, pick);
               this.service.tellPlayer(playerUuid, Msg.of(ChatFormatting.YELLOW, "preview.no_client", pick));
               this.send(battleId, playerUuid, frontOf(mine.size(), pick));
            } else {
               this.open.put(playerUuid, new TeamPreviews.Session(battleId, playerUuid, seat, pick, mine.size(), shown));
            }
         }
      } else {
         LOGGER.error("preview_open was missing required fields");
      }
   }

   void onState(JsonObject message) {
      String battleId = BattleServerClient.str(message, "battleId", null);
      if (battleId != null) {
         JsonObject ready = message.getAsJsonObject("ready");
         if (ready != null) {
            for (TeamPreviews.Session session : new ArrayList<>(this.open.values())) {
               if (session.battleId().equals(battleId)) {
                  ServerPlayer player = this.service.playerOf(session.player());
                  if (player != null) {
                     boolean mineReady = false;
                     boolean theirsReady = false;

                     for (Entry<String, JsonElement> entry : ready.entrySet()) {
                        boolean value = entry.getValue().isJsonPrimitive() && entry.getValue().getAsBoolean();
                        if (entry.getKey().equals(session.seat())) {
                           mineReady = value;
                        } else {
                           theirsReady = theirsReady || value;
                        }
                     }

                     TeamPreviewPayload shown = repaint(session.shown(), mineReady, theirsReady, "");
                     this.open
                        .put(
                           session.player(),
                           new TeamPreviews.Session(session.battleId(), session.player(), session.seat(), session.pick(), session.teamSize(), shown)
                        );
                     CobbleBattleNetwork.sendTeamPreview(player, shown);
                  }
               }
            }
         }
      }
   }

   void onClosed(JsonObject message) {
      String battleId = BattleServerClient.str(message, "battleId", null);
      String reason = BattleServerClient.str(message, "reason", "closed");
      if (battleId != null) {
         for (TeamPreviews.Session session : new ArrayList<>(this.open.values())) {
            if (session.battleId().equals(battleId)) {
               this.open.remove(session.player());
               ServerPlayer player = this.service.playerOf(session.player());
               if (player != null) {
                  CobbleBattleNetwork.sendTeamPreview(player, repaint(session.shown(), session.shown().mineReady(), session.shown().theirsReady(), reason));
               }

               this.service.tellPlayer(session.player(), Msg.of(ChatFormatting.YELLOW, "preview.closed"));
               this.service.battleQueue().drop(session.player());
            }
         }
      }
   }

   void onPicked(ServerPlayer player, String battleId, List<Integer> picks) {
      UUID playerUuid = player.getUUID();
      TeamPreviews.Session session = this.open.get(playerUuid);
      if (session != null && session.battleId().equals(battleId)) {
         if (picks.size() != session.pick()) {
            LOGGER.warn("{} picked {} Pokemon for a preview that wants {}", new Object[]{playerUuid, picks.size(), session.pick()});
         } else {
            Set<Integer> seen = new LinkedHashSet<>();

            for (int pick : picks) {
               if (pick < 0 || pick >= session.teamSize() || !seen.add(pick)) {
                  LOGGER.warn("{} sent a malformed team-preview pick: {}", playerUuid, picks);
                  return;
               }
            }

            TeamPreviewPayload shown = repaint(session.shown(), true, session.shown().theirsReady(), "");
            this.open.put(playerUuid, new TeamPreviews.Session(session.battleId(), playerUuid, session.seat(), session.pick(), session.teamSize(), shown));
            this.send(battleId, playerUuid, picks);
         }
      }
   }

   private void send(String battleId, UUID playerUuid, List<Integer> picks) {
      JsonArray array = new JsonArray();

      for (int pick : picks) {
         array.add(pick);
      }

      JsonObject msg = BattleServerClient.msg("preview_pick");
      msg.addProperty("battleId", battleId);
      msg.addProperty("player", playerUuid.toString());
      msg.add("picks", array);
      this.service.client().send(msg);
   }

   private static List<TeamPreviewPayload.Slot> readRoster(JsonObject rosters, String seat) {
      List<TeamPreviewPayload.Slot> out = new ArrayList<>();
      if (rosters != null && seat != null) {
         JsonElement element = rosters.get(seat);
         if (element != null && element.isJsonArray()) {
            for (JsonElement entry : element.getAsJsonArray()) {
               if (entry.isJsonObject()) {
                  JsonObject slot = entry.getAsJsonObject();
                  out.add(
                     new TeamPreviewPayload.Slot(
                        BattleServerClient.str(slot, "species", ""),
                        BattleServerClient.integer(slot, "level", 1),
                        BattleServerClient.str(slot, "gender", ""),
                        BattleServerClient.bool(slot, "shiny", false),
                        BattleServerClient.str(slot, "item", "")
                     )
                  );
               }
            }

            return out;
         } else {
            return out;
         }
      } else {
         return out;
      }
   }

   private static String otherSeat(JsonObject rosters, String seat) {
      if (rosters == null) {
         return null;
      } else {
         for (String key : rosters.keySet()) {
            if (!key.equals(seat)) {
               return key;
            }
         }

         return null;
      }
   }

   private static long deadlineOf(JsonObject message) {
      long deadline = BattleServerClient.longer(message, "deadline", 0L);
      long left = deadline - System.currentTimeMillis();
      return left > 0L && left <= 600000L ? deadline : System.currentTimeMillis() + 60000L;
   }

   private static List<Integer> frontOf(int teamSize, int pick) {
      List<Integer> out = new ArrayList<>();

      for (int i = 0; i < Math.min(pick, teamSize); i++) {
         out.add(i);
      }

      return out;
   }

   private static TeamPreviewPayload repaint(TeamPreviewPayload shown, boolean mineReady, boolean theirsReady, String closed) {
      return new TeamPreviewPayload(
         shown.battleId(),
         shown.you(),
         shown.opponent(),
         shown.opponentServer(),
         shown.pick(),
         shown.lead(),
         shown.deadlineMs(),
         shown.mine(),
         shown.theirs(),
         mineReady,
         theirsReady,
         closed
      );
   }

   private record Session(String battleId, UUID player, String seat, int pick, int teamSize, TeamPreviewPayload shown) {
   }
}
