package xiaocaoawa.minecraft.mod.cobblebattle.net;

public final class Protocol {
   public static final int VERSION = 10;

   private Protocol() {
   }

   public static final class C2S {
      public static final String HELLO = "hello";
      public static final String DEX_QUERY = "dex_query";
      public static final String ROOM_CREATE = "room_create";
      public static final String ROOM_LIST = "room_list";
      public static final String ROOM_JOIN = "room_join";
      public static final String ROOM_LOOKUP = "room_lookup";
      public static final String ROOM_LEAVE = "room_leave";
      public static final String ROOM_START = "room_start";
      public static final String ACCOUNT_REGISTER = "account_register";
      public static final String ACCOUNT_CODE = "account_code";
      public static final String ACCOUNT_BIND_EMAIL = "account_bind_email";
      public static final String ACCOUNT_LOGIN = "account_login";
      public static final String ACCOUNT_LOGOUT = "account_logout";
      public static final String QUEUE_JOIN = "queue_join";
      public static final String QUEUE_LEAVE = "queue_leave";
      public static final String PREVIEW_PICK = "preview_pick";
      public static final String BATTLE_ACK = "battle_ack";
      public static final String CHOICE = "choice";
      public static final String FORFEIT = "forfeit";
      public static final String BATTLE_ABORT = "battle_abort";
      public static final String BATTLE_OUTPUT = "battle_output";
      public static final String CHAT_SEND = "chat_send";
      public static final String CHAT_WATCH = "chat_watch";
      public static final String LEADERBOARD_QUERY = "leaderboard_query";
      public static final String PONG = "pong";

      private C2S() {
      }
   }

   public static final class Err {
      public static final String BAD_AUTH = "BAD_AUTH";
      public static final String BAD_PROTOCOL = "BAD_PROTOCOL";
      public static final String NOT_HANDSHAKEN = "NOT_HANDSHAKEN";
      public static final String DEX_NOT_READY = "DEX_NOT_READY";
      public static final String UNKNOWN_SPECIES = "UNKNOWN_SPECIES";
      public static final String DEX_MISMATCH = "DEX_MISMATCH";
      public static final String ALREADY_QUEUED = "ALREADY_QUEUED";
      public static final String ALREADY_IN_BATTLE = "ALREADY_IN_BATTLE";
      public static final String UNKNOWN_RANKED = "UNKNOWN_RANKED";
      public static final String TEAM_RULE = "TEAM_RULE";
      public static final String BAD_PICK = "BAD_PICK";
      public static final String BANNED_ACTION = "BANNED_ACTION";
      public static final String CHAT_TOO_FAST = "CHAT_TOO_FAST";
      public static final String NOT_IN_BATTLE = "NOT_IN_BATTLE";
      public static final String CHAT_DISABLED = "CHAT_DISABLED";
      public static final String MUTED = "MUTED";
      public static final String BANNED = "BANNED";
      public static final String NO_SUCH_ROOM = "NO_SUCH_ROOM";
      public static final String ROOM_LOCKED = "ROOM_LOCKED";
      public static final String BAD_INVITE_CODE = "BAD_INVITE_CODE";
      public static final String OWN_ROOM = "OWN_ROOM";
      public static final String NOT_ROOM_HOST = "NOT_ROOM_HOST";
      public static final String ROOM_NOT_READY = "ROOM_NOT_READY";
      public static final String BAD_TEAM = "BAD_TEAM";
      public static final String NO_SUCH_BATTLE = "NO_SUCH_BATTLE";
      public static final String NOT_YOUR_SEAT = "NOT_YOUR_SEAT";
      public static final String NOT_BATTLE_HOST = "NOT_BATTLE_HOST";
      public static final String NOT_LOGGED_IN = "NOT_LOGGED_IN";
      public static final String BAD_CREDENTIALS = "BAD_CREDENTIALS";
      public static final String BAD_EMAIL = "BAD_EMAIL";
      public static final String BAD_CODE = "BAD_CODE";
      public static final String EMAIL_EXISTS = "EMAIL_EXISTS";
      public static final String CODE_TOO_SOON = "CODE_TOO_SOON";
      public static final String SMTP_FAILED = "SMTP_FAILED";
      public static final String BAD_ACCOUNT_ID = "BAD_ACCOUNT_ID";
      public static final String ACCOUNT_EXISTS = "ACCOUNT_EXISTS";
      public static final String BAD_PASSWORD = "BAD_PASSWORD";
      public static final String ACCOUNT_IN_USE = "ACCOUNT_IN_USE";
      public static final String NOT_ACCOUNT_OWNER = "NOT_ACCOUNT_OWNER";
      public static final String TOO_MANY_ATTEMPTS = "TOO_MANY_ATTEMPTS";
      public static final String SERVER_BUSY = "SERVER_BUSY";
      public static final String INTERNAL = "INTERNAL";

      private Err() {
      }
   }

   public static final class S2C {
      public static final String HELLO_ACK = "hello_ack";
      public static final String RANKED_UPDATE = "ranked_update";
      public static final String DEX_SNAPSHOT = "dex_snapshot";
      public static final String ACCOUNT_OK = "account_ok";
      public static final String ACCOUNT_CODE_OK = "account_code_ok";
      public static final String QUEUE_ACK = "queue_ack";
      public static final String QUEUE_LEFT = "queue_left";
      public static final String QUEUE_WAIT = "queue_wait";
      public static final String ROOM_CREATED = "room_created";
      public static final String ROOM_LIST = "room_list";
      public static final String ROOM_INFO = "room_info";
      public static final String ROOM_STATE = "room_state";
      public static final String ROOM_CLOSED = "room_closed";
      public static final String PREVIEW_OPEN = "preview_open";
      public static final String PREVIEW_STATE = "preview_state";
      public static final String PREVIEW_CLOSED = "preview_closed";
      public static final String MATCH_FOUND = "match_found";
      public static final String BATTLE_START = "battle_start";
      public static final String OUTPUT = "output";
      public static final String BATTLE_CHOICE = "battle_choice";
      public static final String SPECTATE_START = "spectate_start";
      public static final String SPECTATE_END = "spectate_end";
      public static final String BATTLE_END = "battle_end";
      public static final String FORFEIT = "forfeit";
      public static final String CHAT = "chat";
      public static final String LEADERBOARD = "leaderboard";
      public static final String ERROR = "error";
      public static final String PING = "ping";

      private S2C() {
      }
   }
}
