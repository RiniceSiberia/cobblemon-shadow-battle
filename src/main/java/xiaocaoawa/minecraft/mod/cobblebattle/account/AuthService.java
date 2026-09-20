package xiaocaoawa.minecraft.mod.cobblebattle.account;

import com.google.gson.JsonObject;
import io.github.rinicesiberia.shadowbattle.account.AccountSessions;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import xiaocaoawa.minecraft.mod.cobblebattle.net.BattleServerClient;

public final class AuthService {
    private final AccountSessions accountSessions = new AccountSessions();
    public String accountOf(UUID participantUuid) { return accountSessions.accountKey(participantUuid); }
    public String nicknameOf(UUID participantUuid) { return accountSessions.displayLabel(participantUuid); }
    public long uidOf(UUID participantUuid) { return accountSessions.numericIdentity(participantUuid); }
    public boolean isSignedIn(UUID participantUuid) { return accountSessions.containsParticipant(participantUuid); }
    public Set<UUID> signedInPlayers() { return accountSessions.participantSnapshot(); }
    public void clear() { accountSessions.resetSessions(); }
    public void forget(UUID participantUuid) { accountSessions.discardParticipant(participantUuid); }
    public Component submit(BattleServerClient serverClient, ServerPlayer participant, AuthMode authMode, String accountId, String email, String password, String verificationCode, boolean emailEnabled) {
        return accountSessions.submitCredentials(serverClient, participant, authMode, accountId, email, password, verificationCode, emailEnabled);
    }
    public void signOut(BattleServerClient serverClient, ServerPlayer participant) { accountSessions.signOutParticipant(serverClient, participant); }
    public UUID onAccountCodeOk(JsonObject responseDocument) { return accountSessions.acceptConfirmation(responseDocument); }
    public Outcome onAccountOk(JsonObject responseDocument) { return accountSessions.acceptAuthentication(responseDocument); }
    public UUID onAccountError(JsonObject responseDocument) { return accountSessions.rejectAuthentication(responseDocument); }
    public boolean isAwaiting(JsonObject responseDocument) { return accountSessions.hasPendingReference(responseDocument); }
    public record Outcome(UUID playerUuid, String accountId, String nickname, long uid, boolean registered) {}
    public record Signed(String accountId, String nickname, long uid) {}
}
