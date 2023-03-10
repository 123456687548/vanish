package eu.vanish.util;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.ServerCommandSource;

import java.util.function.Predicate;

public class FPAPIUtilsWrapper {
    private static boolean fpapiLoaded = false;
    private static boolean fpapiLoadedChecked = false;

    static boolean isFPAPILoaded() {
        if (!fpapiLoadedChecked) {
            fpapiLoadedChecked = true;
            fpapiLoaded = FabricLoader.getInstance().isModLoaded("fabric-permissions-api-v0");
        }
        return fpapiLoaded;
    }

    public static Predicate<ServerCommandSource> require(String permission, boolean defaultValue) {
        if (isFPAPILoaded()) {
            return FPAPIUtils.require(permission, defaultValue);
        }
        return _ignored -> defaultValue;
    }

    public static Predicate<ServerCommandSource> require(String permission, int level) {
        if (isFPAPILoaded()) {
            return FPAPIUtils.require(permission, level);
        }
        return player -> player.hasPermissionLevel(level);
    }

    public static boolean check(ServerCommandSource source, String permission, boolean fallback) {
        if (isFPAPILoaded()) {
            return FPAPIUtils.check(source, permission, fallback);
        }
        return fallback;
    }
}
