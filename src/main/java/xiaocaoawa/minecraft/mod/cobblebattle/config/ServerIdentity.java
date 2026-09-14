package xiaocaoawa.minecraft.mod.cobblebattle.config;

import io.github.rinicesiberia.shadowbattle.configuration.PersistentServerIdentity;

public final class ServerIdentity {
    private ServerIdentity() {}

    public static String get() {
        return PersistentServerIdentity.server.resolve();
    }
}
