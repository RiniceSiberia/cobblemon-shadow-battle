package xiaocaoawa.minecraft.mod.cobblebattle.account;

import com.google.gson.JsonObject;
import io.github.rinicesiberia.shadowbattle.account.AccountSessions;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import xiaocaoawa.minecraft.mod.cobblebattle.net.BattleServerClient;

public final class AuthService {
    private final AccountSessions sessions = new AccountSessions();
    public String accountOf(UUID participantUuid) { return sessions.accountKey(participantUuid); }
    public String nicknameOf(UUID participantUuid) { return sessions.displayLabel(participantUuid); }
    public long uidOf(UUID participantUuid) { return sessions.numericIdentity(participantUuid); }
    public boolean isSignedIn(UUID participantUuid) { return sessions.containsParticipant(participantUuid); }
    public Set<UUID> signedInPlayers() { return sessions.participantSnapshot(); }
    public void clear() { sessions.resetSessions(); }
    public void forget(UUID participantUuid) { sessions.discardParticipant(participantUuid); }
    public Component submit(BattleServerClient client, ServerPlayer participant, AuthMode mode, String accountId, String email, String password, String verificationCode, boolean emailEnabled) {
        return sessions.submitCredentials(client, participant, mode, accountId, email, password, verificationCode, emailEnabled);
    }
    public void signOut(BattleServerClient client, ServerPlayer participant) { sessions.signOutParticipant(client, participant); }
    public UUID onAccountCodeOk(JsonObject document) { return sessions.acceptConfirmation(document); }
    public Outcome onAccountOk(JsonObject document) { return sessions.acceptAuthentication(document); }
    public UUID onAccountError(JsonObject document) { return sessions.rejectAuthentication(document); }
    public boolean isAwaiting(JsonObject document) { return sessions.hasPendingReference(document); }
    public record Outcome(UUID playerUuid, String accountId, String nickname, long uid, boolean registered) {}
    public record Signed(String accountId, String nickname, long uid) {}
}
