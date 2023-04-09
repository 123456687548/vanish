package eu.vanish.mixin;

import com.mojang.authlib.GameProfile;
import eu.vanish.Vanish;
import net.minecraft.server.ServerMetadata;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(ServerMetadata.class)
public class ServerMetadataMixin {

    @Inject(at = @At("HEAD"), method = "players")
    private void onGetPlayers(CallbackInfoReturnable<ServerMetadata.Players> ci) {
        if (Vanish.INSTANCE.isActive() && Vanish.INSTANCE.getSettings().fakePlayerCount()) {
            List<GameProfile> gameProfiles = new ArrayList<>();

            Vanish.INSTANCE.getServer().getPlayerManager().getPlayerList().forEach(player -> {
                GameProfile profile = player.getGameProfile();
                if (Vanish.INSTANCE.vanishedPlayers.isNotVanished(player)) {
                    gameProfiles.add(profile);
                }
            });
        }
    }
}
