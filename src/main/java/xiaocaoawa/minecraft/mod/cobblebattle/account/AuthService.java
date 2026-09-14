package xiaocaoawa.minecraft.mod.cobblebattle.account;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import xiaocaoawa.minecraft.mod.cobblebattle.lang.Msg;
import xiaocaoawa.minecraft.mod.cobblebattle.net.BattleServerClient;

public final class AuthService {
   private final Map<UUID, AuthService.Signed> signedIn = new ConcurrentHashMap<>();
   private final Map<Integer, AuthService.Pending> pending = new ConcurrentHashMap<>();

   public String accountOf(UUID playerUuid) {
      AuthService.Signed signed = this.signedIn.get(playerUuid);
      return signed == null ? null : signed.accountId();
   }

   public String nicknameOf(UUID playerUuid) {
      AuthService.Signed signed = this.signedIn.get(playerUuid);
      return signed == null ? null : signed.nickname();
   }

   public long uidOf(UUID playerUuid) {
      AuthService.Signed signed = this.signedIn.get(playerUuid);
      return signed == null ? 0L : signed.uid();
   }

   public boolean isSignedIn(UUID playerUuid) {
      return this.signedIn.containsKey(playerUuid);
   }

   public Set<UUID> signedInPlayers() {
      return Set.copyOf(this.signedIn.keySet());
   }

   public void clear() {
      this.signedIn.clear();
      this.pending.clear();
   }

   public void forget(UUID playerUuid) {
      this.signedIn.remove(playerUuid);
   }

   public Component submit(
      BattleServerClient client,
      ServerPlayer player,
      AuthMode mode,
      String accountId,
      String email,
      String password,
      String verificationCode,
      boolean emailEnabled
   ) {
      if (client == null || !client.isHandshaken()) {
         return Msg.of(ChatFormatting.RED, "auth.not_connected");
      } else if (mode.isBindEmail()) {
         int ref = client.nextRef();
         JsonObject request = BattleServerClient.msg("account_bind_email");
         request.addProperty("ref", ref);
         request.addProperty("accountId", accountId);
         request.addProperty("email", email);
         request.addProperty("password", password);
         request.addProperty("verificationCode", verificationCode);
         JsonObject who = new JsonObject();
         who.addProperty("uuid", player.getUUID().toString());
         who.addProperty("name", player.getGameProfile().getName());
         request.add("player", who);
         this.pending.put(ref, new AuthService.Pending(player.getUUID(), mode));
         if (!client.send(request)) {
            this.pending.remove(ref);
            return Msg.of(ChatFormatting.RED, "auth.send_failed");
         } else {
            return null;
         }
      } else if (mode.isCodeRequest()) {
         if (email != null && !email.isBlank()) {
            int ref = client.nextRef();
            JsonObject request = BattleServerClient.msg("account_code");
            request.addProperty("ref", ref);
            request.addProperty("email", email.trim());
            request.addProperty("purpose", "__bind".equals(accountId) ? "bind" : "register");
            this.pending.put(ref, new AuthService.Pending(player.getUUID(), mode));
            if (!client.send(request)) {
               this.pending.remove(ref);
               return Msg.of(ChatFormatting.RED, "auth.send_failed");
            } else {
               return null;
            }
         } else {
            return Msg.of(ChatFormatting.RED, "auth.need_email");
         }
      } else if (!mode.isRegister() || !emailEnabled || email != null && !email.isBlank()) {
         if (password != null && !password.isEmpty()) {
            int ref = client.nextRef();
            JsonObject request = BattleServerClient.msg(mode.isRegister() ? "account_register" : "account_login");
            request.addProperty("ref", ref);
            request.addProperty("accountId", accountId == null ? "" : accountId.trim());
            request.addProperty("email", email == null ? "" : email.trim());
            request.addProperty("password", password);
            request.addProperty("verificationCode", verificationCode == null ? "" : verificationCode.trim());
            JsonObject who = new JsonObject();
            who.addProperty("uuid", player.getUUID().toString());
            who.addProperty("name", player.getGameProfile().getName());
            request.add("player", who);
            this.pending.put(ref, new AuthService.Pending(player.getUUID(), mode));
            if (!client.send(request)) {
               this.pending.remove(ref);
               return Msg.of(ChatFormatting.RED, "auth.send_failed");
            } else {
               return null;
            }
         } else {
            return Msg.of(ChatFormatting.RED, "auth.need_password");
         }
      } else {
         return Msg.of(ChatFormatting.RED, "auth.need_email");
      }
   }

   public void signOut(BattleServerClient client, ServerPlayer player) {
      this.signedIn.remove(player.getUUID());
      if (client != null && client.isHandshaken()) {
         JsonObject request = BattleServerClient.msg("account_logout");
         JsonObject who = new JsonObject();
         who.addProperty("uuid", player.getUUID().toString());
         request.add("player", who);
         client.send(request);
      }
   }

   public UUID onAccountCodeOk(JsonObject message) {
      AuthService.Pending waiting = this.claim(message.get("ref"));
      return waiting == null ? null : waiting.playerUuid();
   }

   public AuthService.Outcome onAccountOk(JsonObject message) {
      UUID playerUuid = parseUuid(BattleServerClient.str(message, "player", ""));
      String accountId = BattleServerClient.str(message, "accountId", "");
      String nickname = BattleServerClient.str(message, "nickname", "");
      boolean registered = BattleServerClient.bool(message, "registered", false);
      AuthService.Pending waiting = this.claim(message.get("ref"));
      if (playerUuid == null && waiting != null) {
         playerUuid = waiting.playerUuid();
      }

      if (playerUuid == null) {
         return null;
      } else {
         long uid = message.get("uid").getAsLong();
         this.signedIn.put(playerUuid, new AuthService.Signed(accountId, nickname, uid));
         return new AuthService.Outcome(playerUuid, accountId, nickname, uid, registered);
      }
   }

   public UUID onAccountError(JsonObject message) {
      AuthService.Pending waiting = this.claim(message.get("ref"));
      return waiting == null ? null : waiting.playerUuid();
   }

   public boolean isAwaiting(JsonObject message) {
      JsonElement ref = message.get("ref");
      return ref != null && !ref.isJsonNull() && this.pending.containsKey(ref.getAsInt());
   }

   private AuthService.Pending claim(JsonElement ref) {
      return ref != null && !ref.isJsonNull() ? this.pending.remove(ref.getAsInt()) : null;
   }

   private static UUID parseUuid(String raw) {
      try {
         return raw != null && !raw.isBlank() ? UUID.fromString(raw) : null;
      } catch (IllegalArgumentException var2) {
         return null;
      }
   }

   public record Outcome(UUID playerUuid, String accountId, String nickname, long uid, boolean registered) {
   }

   private record Pending(UUID playerUuid, AuthMode mode) {
   }

   public record Signed(String accountId, String nickname, long uid) {
   }
}
