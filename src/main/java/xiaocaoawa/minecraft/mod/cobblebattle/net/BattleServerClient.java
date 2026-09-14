package xiaocaoawa.minecraft.mod.cobblebattle.net;

import com.google.gson.JsonObject;
import io.github.rinicesiberia.shadowbattle.transport.MessageFields;
import io.github.rinicesiberia.shadowbattle.transport.RemoteBattleConnection;
import java.util.function.Consumer;
import xiaocaoawa.minecraft.mod.cobblebattle.config.CobbleBattleConfig;

public final class BattleServerClient {
    private final RemoteBattleConnection connection;

    public BattleServerClient(CobbleBattleConfig config, Consumer<JsonObject> inboundHandler, Runnable onConnected, Consumer<String> onDisconnected) {
        connection = new RemoteBattleConnection(config, inboundHandler, onConnected, onDisconnected);
    }
    public boolean isConnected() { return connection.isSocketOpen(); }
    public boolean isHandshaken() { return connection.isSessionReady(); }
    public void setHandshaken(boolean value) { connection.markSessionReady(value); }
    public int nextRef() { return connection.allocateReference(); }
    public void start() { connection.launch(); }
    public void stop() { connection.shutdown(); }
    public void connect() { connection.requestConnection(); }
    public void disconnect(String why) { connection.closeByRequest(why); }
    public void suspend(String why) { connection.pauseAfterRefusal(why); }
    public void setOnConnectFailed(Consumer<String> handler) { connection.observeConnectionFailure(handler); }
    public String refusedReason() { return connection.refusalCause(); }
    public boolean isWanted() { return connection.hasConnectionDemand(); }
    public void reconnect(String why) { connection.restartConnection(why); }
    public boolean send(JsonObject message) { return connection.sendMessage(message); }
    public boolean sendHandshake(JsonObject message) { return connection.sendGreeting(message); }
    public static JsonObject msg(String type) { return MessageFields.envelope(type); }
    public static String str(JsonObject object, String key, String fallback) { return MessageFields.text(object, key, fallback); }
    public static int integer(JsonObject object, String key, int fallback) { return MessageFields.number(object, key, fallback); }
    public static boolean bool(JsonObject object, String key, boolean fallback) { return MessageFields.flag(object, key, fallback); }
    public static long longer(JsonObject object, String key, long fallback) { return MessageFields.longNumber(object, key, fallback); }
}
