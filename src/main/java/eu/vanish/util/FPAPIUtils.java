package eu.vanish.util;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.command.ServerCommandSource;
import java.util.function.Predicate;

public class FPAPIUtils {
    static Predicate<ServerCommandSource> require(String permission, boolean defaultValue) {
        return Permissions.require(permission, defaultValue);
    }

    static Predicate<ServerCommandSource> require(String permission, int level) {
        return Permissions.require(permission, level);
    }

    public static boolean check(ServerCommandSource source, String permission, boolean fallback) {
        return Permissions.check(source, permission, fallback);
    }
}
