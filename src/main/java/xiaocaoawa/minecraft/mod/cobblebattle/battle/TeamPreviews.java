package xiaocaoawa.minecraft.mod.cobblebattle.battle;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.rinicesiberia.shadowbattle.battle.PreviewPickValidation;
import io.github.rinicesiberia.shadowbattle.battle.TeamPreviewRules;
import io.github.rinicesiberia.shadowbattle.battle.TeamPreviewSession;
import io.github.rinicesiberia.shadowbattle.battle.TeamPreviewSessionDirectory;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Map.Entry;
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
   private final TeamPreviewSessionDirectory sessions = new TeamPreviewSessionDirectory();

   TeamPreviews(CrossServerBattleService service) {
      this.service = service;
   }

   void clear() {
      this.sessions.clear();
   }

   void forget(UUID participantUuid) {
      this.sessions.forget(participantUuid);
   }

   void onOpen(JsonObject document) {
      String battleId = BattleServerClient.str(document, "battleId", null);
      String participantRaw = BattleServerClient.str(document, "yourPlayer", null);
      String seat = BattleServerClient.str(document, "yourSeat", null);
      if (battleId != null && participantRaw != null && seat != null) {
         UUID participantUuid;
         try {
            participantUuid = UUID.fromString(participantRaw);
         } catch (IllegalArgumentException failure) {
            LOGGER.error("preview_open named a player that is not a uuid: {}", participantRaw);
            return;
         }

         JsonObject rosters = document.getAsJsonObject("rosters");
         List<TeamPreviewPayload.Slot> mine = readRoster(rosters, seat);
         List<TeamPreviewPayload.Slot> theirs = readRoster(rosters, otherSeat(rosters, seat));
         int pick = BattleServerClient.integer(document, "pick", mine.size());
         int lead = Math.max(1, BattleServerClient.integer(document, "lead", 1));
         long deadline = deadlineOf(document);
         ServerPlayer participant = this.service.participantOf(participantUuid);
         if (participant == null) {
            LOGGER.warn("preview_open for {} but they are not on this server", participantUuid);
            this.send(battleId, participantUuid, TeamPreviewRules.frontPicks(mine.size(), pick));
         } else {
            TeamPreviewPayload shown = new TeamPreviewPayload(
               battleId,
               participant.getGameProfile().getName(),
               BattleServerClient.str(document, "opponent", ""),
               BattleServerClient.str(document, "opponentServer", ""),
               pick,
               lead,
               deadline,
               mine,
               theirs,
               false,
               false,
               ""
            );
            if (!CobbleBattleNetwork.sendTeamPreview(participant, shown)) {
               LOGGER.info("{} has no CobbleBattle client; picking the first {} of their team", participantUuid, pick);
               this.service.tellParticipant(participantUuid, Msg.of(ChatFormatting.YELLOW, "preview.no_client", pick));
               this.send(battleId, participantUuid, TeamPreviewRules.frontPicks(mine.size(), pick));
            } else {
               this.sessions.save(new TeamPreviewSession(battleId, participantUuid, seat, pick, mine.size(), shown));
            }
         }
      } else {
         LOGGER.error("preview_open was missing required fields");
      }
   }

   void onState(JsonObject document) {
      String battleId = BattleServerClient.str(document, "battleId", null);
      if (battleId != null) {
         JsonObject ready = document.getAsJsonObject("ready");
         if (ready != null) {
            for (TeamPreviewSession session : this.sessions.forBattle(battleId)) {
                  ServerPlayer participant = this.service.participantOf(session.getPlayer());
                  if (participant != null) {
                     boolean mineReady = false;
                     boolean theirsReady = false;

                     for (Entry<String, JsonElement> entry : ready.entrySet()) {
                        boolean value = entry.getValue().isJsonPrimitive() && entry.getValue().getAsBoolean();
                        if (entry.getKey().equals(session.getSeat())) {
                           mineReady = value;
                        } else {
                           theirsReady = theirsReady || value;
                        }
                     }

                     TeamPreviewPayload shown = repaint(session.getShown(), mineReady, theirsReady, "");
                     this.sessions.save(
                        new TeamPreviewSession(
                           session.getBattleId(), session.getPlayer(), session.getSeat(), session.getPick(), session.getTeamSize(), shown
                        )
                     );
                     CobbleBattleNetwork.sendTeamPreview(participant, shown);
                  }
            }
         }
      }
   }

   void onClosed(JsonObject document) {
      String battleId = BattleServerClient.str(document, "battleId", null);
      String reason = BattleServerClient.str(document, "reason", "closed");
      if (battleId != null) {
         for (TeamPreviewSession session : this.sessions.forBattle(battleId)) {
               this.sessions.forget(session.getPlayer());
               ServerPlayer participant = this.service.participantOf(session.getPlayer());
               if (participant != null) {
                  CobbleBattleNetwork.sendTeamPreview(
                     participant, repaint(session.getShown(), session.getShown().mineReady(), session.getShown().theirsReady(), reason)
                  );
               }

               this.service.tellParticipant(session.getPlayer(), Msg.of(ChatFormatting.YELLOW, "preview.closed"));
               this.service.battleQueue().drop(session.getPlayer());
         }
      }
   }

   void onPicked(ServerPlayer participant, String battleId, List<Integer> picks) {
      UUID participantUuid = participant.getUUID();
      TeamPreviewSession session = this.sessions.find(participantUuid);
      if (session != null && session.getBattleId().equals(battleId)) {
         PreviewPickValidation validation = TeamPreviewRules.validatePicks(picks, session.getPick(), session.getTeamSize());
         if (validation == PreviewPickValidation.WRONG_COUNT) {
            LOGGER.warn("{} picked {} Pokemon for a preview that wants {}", new Object[]{participantUuid, picks.size(), session.getPick()});
         } else if (validation == PreviewPickValidation.INVALID_SLOT) {
            LOGGER.warn("{} sent a malformed team-preview pick: {}", participantUuid, picks);
         } else {
            TeamPreviewPayload shown = repaint(session.getShown(), true, session.getShown().theirsReady(), "");
            this.sessions.save(
               new TeamPreviewSession(session.getBattleId(), participantUuid, session.getSeat(), session.getPick(), session.getTeamSize(), shown)
            );
            this.send(battleId, participantUuid, picks);
         }
      }
   }

   private void send(String battleId, UUID participantUuid, List<Integer> picks) {
      JsonArray array = new JsonArray();

      for (int pick : picks) {
         array.add(pick);
      }

      JsonObject msg = BattleServerClient.msg("preview_pick");
      msg.addProperty("battleId", battleId);
      msg.addProperty("player", participantUuid.toString());
      msg.add("picks", array);
      this.service.client().send(msg);
   }

   private static List<TeamPreviewPayload.Slot> readRoster(JsonObject rosters, String seat) {
      List<TeamPreviewPayload.Slot> outputStream = new ArrayList<>();
      if (rosters != null && seat != null) {
         JsonElement element = rosters.get(seat);
         if (element != null && element.isJsonArray()) {
            for (JsonElement entry : element.getAsJsonArray()) {
               if (entry.isJsonObject()) {
                  JsonObject slot = entry.getAsJsonObject();
                  outputStream.add(
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

            return outputStream;
         } else {
            return outputStream;
         }
      } else {
         return outputStream;
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

   private static long deadlineOf(JsonObject document) {
      long deadline = BattleServerClient.longer(document, "deadline", 0L);
      long left = deadline - System.currentTimeMillis();
      return left > 0L && left <= 600000L ? deadline : System.currentTimeMillis() + 60000L;
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
}
