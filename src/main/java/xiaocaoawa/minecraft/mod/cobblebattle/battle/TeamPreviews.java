package xiaocaoawa.minecraft.mod.cobblebattle.battle;

import com.google.gson.JsonObject;
import io.github.rinicesiberia.shadowbattle.battle.PreviewPickValidation;
import io.github.rinicesiberia.shadowbattle.battle.TeamPreviewRules;
import io.github.rinicesiberia.shadowbattle.battle.TeamPreviewEventDecoding;
import io.github.rinicesiberia.shadowbattle.battle.TeamPreviewSession;
import io.github.rinicesiberia.shadowbattle.battle.TeamPreviewSessionDirectory;
import io.github.rinicesiberia.shadowbattle.transport.TeamPreviewMessages;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xiaocaoawa.minecraft.mod.cobblebattle.lang.Msg;
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
      TeamPreviewEventDecoding.OpenResult result = TeamPreviewEventDecoding.open(
         document, System.currentTimeMillis(), System.currentTimeMillis()
      );
      if (result instanceof TeamPreviewEventDecoding.OpenResult.Ready opened) {
         String battleId = opened.getBattleId();
         UUID participantUuid = opened.getParticipant();
         String seat = opened.getSeat();
         List<TeamPreviewPayload.Slot> mine = opened.getMine();
         int pick = opened.getPick();
         ServerPlayer participant = this.service.participantOf(participantUuid);
         if (participant == null) {
            LOGGER.warn("preview_open for {} but they are not on this server", participantUuid);
            this.send(battleId, participantUuid, TeamPreviewRules.frontPicks(mine.size(), pick));
         } else {
            TeamPreviewPayload shown = new TeamPreviewPayload(
               battleId,
               participant.getGameProfile().getName(),
               opened.getOpponent(),
               opened.getOpponentServer(),
               pick,
               opened.getLead(),
               opened.getDeadline(),
               mine,
               opened.getTheirs(),
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
      } else if (result instanceof TeamPreviewEventDecoding.OpenResult.InvalidParticipant invalid) {
         LOGGER.error("preview_open named a player that is not a uuid: {}", invalid.getRaw());
      } else {
         LOGGER.error("preview_open was missing required fields");
      }
   }

   void onState(JsonObject document) {
      TeamPreviewEventDecoding.State state = TeamPreviewEventDecoding.state(document);
      if (state != null) {
            for (TeamPreviewSession session : this.sessions.forBattle(state.getBattleId())) {
                  ServerPlayer participant = this.service.participantOf(session.getPlayer());
                  if (participant != null) {
                     TeamPreviewEventDecoding.Readiness readiness = TeamPreviewEventDecoding.readiness(state.getReady(), session.getSeat());
                     TeamPreviewPayload shown = TeamPreviewEventDecoding.repaint(session.getShown(), readiness.getMine(), readiness.getTheirs(), "");
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

   void onClosed(JsonObject document) {
      TeamPreviewEventDecoding.Closed closed = TeamPreviewEventDecoding.closed(document);
      if (closed != null) {
         for (TeamPreviewSession session : this.sessions.forBattle(closed.getBattleId())) {
               this.sessions.forget(session.getPlayer());
               ServerPlayer participant = this.service.participantOf(session.getPlayer());
               if (participant != null) {
                  CobbleBattleNetwork.sendTeamPreview(
                     participant, TeamPreviewEventDecoding.repaint(session.getShown(), session.getShown().mineReady(), session.getShown().theirsReady(), closed.getReason())
                  );
               }

               this.service.tellParticipant(session.getPlayer(), Msg.of(ChatFormatting.YELLOW, "preview.closed"));
               this.service.queueCoordinator().discardWaitingTeam(session.getPlayer());
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
            TeamPreviewPayload shown = TeamPreviewEventDecoding.repaint(session.getShown(), true, session.getShown().theirsReady(), "");
            this.sessions.save(
               new TeamPreviewSession(session.getBattleId(), participantUuid, session.getSeat(), session.getPick(), session.getTeamSize(), shown)
            );
            this.send(battleId, participantUuid, picks);
         }
      }
   }

   private void send(String battleId, UUID participantUuid, List<Integer> picks) {
      this.service.serverClient().send(TeamPreviewMessages.pick(battleId, participantUuid, picks));
   }

}
