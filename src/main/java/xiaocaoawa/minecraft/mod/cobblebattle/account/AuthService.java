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
    public String accountOf(UUID playerUuid) { return sessions.accountKey(playerUuid); }
    public String nicknameOf(UUID playerUuid) { return sessions.displayLabel(playerUuid); }
    public long uidOf(UUID playerUuid) { return sessions.numericIdentity(playerUuid); }
    public boolean isSignedIn(UUID playerUuid) { return sessions.containsParticipant(playerUuid); }
    public Set<UUID> signedInPlayers() { return sessions.participantSnapshot(); }
    public void clear() { sessions.resetSessions(); }
    public void forget(UUID playerUuid) { sessions.discardParticipant(playerUuid); }
    public Component submit(BattleServerClient client, ServerPlayer player, AuthMode mode, String accountId, String email, String password, String verificationCode, boolean emailEnabled) {
        return sessions.submitCredentials(client, player, mode, accountId, email, password, verificationCode, emailEnabled);
    }
    public void signOut(BattleServerClient client, ServerPlayer player) { sessions.signOutParticipant(client, player); }
    public UUID onAccountCodeOk(JsonObject message) { return sessions.acceptConfirmation(message); }
    public Outcome onAccountOk(JsonObject message) { return sessions.acceptAuthentication(message); }
    public UUID onAccountError(JsonObject message) { return sessions.rejectAuthentication(message); }
    public boolean isAwaiting(JsonObject message) { return sessions.hasPendingReference(message); }
    public record Outcome(UUID playerUuid, String accountId, String nickname, long uid, boolean registered) {}
    public record Signed(String accountId, String nickname, long uid) {}
}
