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

final class TeamPreviewCoordinator {
   private static final Logger LOG = LoggerFactory.getLogger("CobbleBattle");
   private final CrossServerBattleService battleService;
   private final TeamPreviewSessionDirectory sessions = new TeamPreviewSessionDirectory();

   TeamPreviewCoordinator(CrossServerBattleService battleService) {
      this.battleService = battleService;
   }

   void clearSessions() {
      this.sessions.clear();
   }

   void forgetParticipant(UUID participantUuid) {
      this.sessions.forget(participantUuid);
   }

   void handlePreviewOpened(JsonObject document) {
      TeamPreviewEventDecoding.OpenResult result = TeamPreviewEventDecoding.open(
         document, System.currentTimeMillis(), System.currentTimeMillis()
      );
      if (result instanceof TeamPreviewEventDecoding.OpenResult.Ready opened) {
         String externalBattleId = opened.getBattleId();
         UUID participantUuid = opened.getParticipant();
         String participantSeat = opened.getSeat();
         List<TeamPreviewPayload.Slot> participantRoster = opened.getMine();
         int requiredPickCount = opened.getPick();
         ServerPlayer participant = this.battleService.participantOf(participantUuid);
         if (participant == null) {
            LOG.warn("preview_open for {} but they are not on this server", participantUuid);
            this.sendSelectedSlots(
               externalBattleId, participantUuid, TeamPreviewRules.frontPicks(participantRoster.size(), requiredPickCount)
            );
         } else {
            TeamPreviewPayload displayedPreview = new TeamPreviewPayload(
               externalBattleId,
               participant.getGameProfile().getName(),
               opened.getOpponent(),
               opened.getOpponentServer(),
               requiredPickCount,
               opened.getLead(),
               opened.getDeadline(),
               participantRoster,
               opened.getTheirs(),
               false,
               false,
               ""
            );
            if (!CobbleBattleNetwork.sendTeamPreview(participant, displayedPreview)) {
               LOG.info("{} has no CobbleBattle client; picking the first {} of their team", participantUuid, requiredPickCount);
               this.battleService.tellParticipant(
                  participantUuid, Msg.of(ChatFormatting.YELLOW, "preview.no_client", requiredPickCount)
               );
               this.sendSelectedSlots(
                  externalBattleId, participantUuid, TeamPreviewRules.frontPicks(participantRoster.size(), requiredPickCount)
               );
            } else {
               this.sessions.save(
                  new TeamPreviewSession(
                     externalBattleId,
                     participantUuid,
                     participantSeat,
                     requiredPickCount,
                     participantRoster.size(),
                     displayedPreview
                  )
               );
            }
         }
      } else if (result instanceof TeamPreviewEventDecoding.OpenResult.InvalidParticipant invalid) {
         LOG.error("preview_open named a player that is not a uuid: {}", invalid.getRaw());
      } else {
         LOG.error("preview_open was missing required fields");
      }
   }

   void handlePreviewState(JsonObject document) {
      TeamPreviewEventDecoding.State state = TeamPreviewEventDecoding.state(document);
      if (state != null) {
         for (TeamPreviewSession previewSession : this.sessions.forBattle(state.getBattleId())) {
            ServerPlayer participant = this.battleService.participantOf(previewSession.getPlayer());
            if (participant != null) {
               TeamPreviewEventDecoding.Readiness readiness = TeamPreviewEventDecoding.readiness(
                  state.getReady(), previewSession.getSeat()
               );
               TeamPreviewPayload refreshedPreview = TeamPreviewEventDecoding.repaint(
                  previewSession.getShown(), readiness.getMine(), readiness.getTheirs(), ""
               );
               this.sessions.save(
                  new TeamPreviewSession(
                     previewSession.getBattleId(),
                     previewSession.getPlayer(),
                     previewSession.getSeat(),
                     previewSession.getPick(),
                     previewSession.getTeamSize(),
                     refreshedPreview
                  )
               );
               CobbleBattleNetwork.sendTeamPreview(participant, refreshedPreview);
            }
         }
      }
   }

   void handlePreviewClosed(JsonObject document) {
      TeamPreviewEventDecoding.Closed closed = TeamPreviewEventDecoding.closed(document);
      if (closed != null) {
         for (TeamPreviewSession previewSession : this.sessions.forBattle(closed.getBattleId())) {
            this.sessions.forget(previewSession.getPlayer());
            ServerPlayer participant = this.battleService.participantOf(previewSession.getPlayer());
            if (participant != null) {
               CobbleBattleNetwork.sendTeamPreview(
                  participant,
                  TeamPreviewEventDecoding.repaint(
                     previewSession.getShown(),
                     previewSession.getShown().mineReady(),
                     previewSession.getShown().theirsReady(),
                     closed.getReason()
                  )
               );
            }

            this.battleService.tellParticipant(previewSession.getPlayer(), Msg.of(ChatFormatting.YELLOW, "preview.closed"));
            this.battleService.queueCoordinator().discardWaitingTeam(previewSession.getPlayer());
         }
      }
   }

   void handleTeamPicked(ServerPlayer participant, String externalBattleId, List<Integer> selectedSlots) {
      UUID participantUuid = participant.getUUID();
      TeamPreviewSession previewSession = this.sessions.find(participantUuid);
      if (previewSession != null && previewSession.getBattleId().equals(externalBattleId)) {
         PreviewPickValidation validation = TeamPreviewRules.validatePicks(
            selectedSlots, previewSession.getPick(), previewSession.getTeamSize()
         );
         if (validation == PreviewPickValidation.WRONG_COUNT) {
            LOG.warn(
               "{} picked {} Pokemon for a preview that wants {}",
               new Object[]{participantUuid, selectedSlots.size(), previewSession.getPick()}
            );
         } else if (validation == PreviewPickValidation.INVALID_SLOT) {
            LOG.warn("{} sent a malformed team-preview pick: {}", participantUuid, selectedSlots);
         } else {
            TeamPreviewPayload confirmedPreview = TeamPreviewEventDecoding.repaint(
               previewSession.getShown(), true, previewSession.getShown().theirsReady(), ""
            );
            this.sessions.save(
               new TeamPreviewSession(
                  previewSession.getBattleId(),
                  participantUuid,
                  previewSession.getSeat(),
                  previewSession.getPick(),
                  previewSession.getTeamSize(),
                  confirmedPreview
               )
            );
            this.sendSelectedSlots(externalBattleId, participantUuid, selectedSlots);
         }
      }
   }

   private void sendSelectedSlots(String externalBattleId, UUID participantUuid, List<Integer> selectedSlots) {
      this.battleService.serverClient().send(TeamPreviewMessages.pick(externalBattleId, participantUuid, selectedSlots));
   }

}
