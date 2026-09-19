package xiaocaoawa.minecraft.mod.cobblebattle.net;

import com.google.gson.JsonObject;
import io.github.rinicesiberia.shadowbattle.transport.MessageFields;
import io.github.rinicesiberia.shadowbattle.transport.RemoteBattleConnection;
import java.util.function.Consumer;
import xiaocaoawa.minecraft.mod.cobblebattle.config.CobbleBattleConfig;

public final class BattleServerClient {
    private final RemoteBattleConnection transport;

    public BattleServerClient(
        CobbleBattleConfig connectionConfig,
        Consumer<JsonObject> inboundMessageHandler,
        Runnable connectedCallback,
        Consumer<String> disconnectedCallback
    ) {
        transport = new RemoteBattleConnection(
            connectionConfig,
            inboundMessageHandler,
            connectedCallback,
            disconnectedCallback
        );
    }

    public boolean isConnected() { return transport.isSocketOpen(); }
    public boolean isHandshaken() { return transport.isSessionReady(); }
    public void setHandshaken(boolean sessionReady) { transport.markSessionReady(sessionReady); }
    public int nextRef() { return transport.allocateReference(); }
    public void start() { transport.launch(); }
    public void stop() { transport.shutdown(); }
    public void connect() { transport.requestConnection(); }
    public void disconnect(String reason) { transport.closeByRequest(reason); }
    public void suspend(String refusalReason) { transport.pauseAfterRefusal(refusalReason); }
    public void setOnConnectFailed(Consumer<String> failureHandler) { transport.observeConnectionFailure(failureHandler); }
    public String refusedReason() { return transport.refusalCause(); }
    public boolean isWanted() { return transport.hasConnectionDemand(); }
    public void reconnect(String reason) { transport.restartConnection(reason); }
    public boolean send(JsonObject document) { return transport.sendMessage(document); }
    public boolean sendHandshake(JsonObject document) { return transport.sendGreeting(document); }
    public static JsonObject msg(String messageType) { return MessageFields.envelope(messageType); }
    public static String str(JsonObject document, String fieldName, String defaultValue) {
        return MessageFields.text(document, fieldName, defaultValue);
    }
    public static int integer(JsonObject document, String fieldName, int defaultValue) {
        return MessageFields.number(document, fieldName, defaultValue);
    }
    public static boolean bool(JsonObject document, String fieldName, boolean defaultValue) {
        return MessageFields.flag(document, fieldName, defaultValue);
    }
    public static long longer(JsonObject document, String fieldName, long defaultValue) {
        return MessageFields.longNumber(document, fieldName, defaultValue);
    }
}
