package eu.vanish.mixin;

import com.mojang.authlib.GameProfile;
import eu.vanish.Vanish;
import eu.vanish.data.Settings;
import net.minecraft.server.ServerMetadata;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(ServerMetadata.class)
public class ServerMetadataMixin {

    @Shadow
    private ServerMetadata.Players players;

    @Inject(at = @At("HEAD"), method = "getPlayers")
    private void onGetPlayers(CallbackInfoReturnable<ServerMetadata.Players> ci) {
        if (Vanish.INSTANCE.isActive() && Vanish.INSTANCE.getSettings().fakePlayerCount()) {
            List<GameProfile> gameProfiles = new ArrayList<>();

            Vanish.INSTANCE.getServer().getPlayerManager().getPlayerList().forEach(player -> {
                GameProfile profile = player.getGameProfile();
                if (Vanish.INSTANCE.vanishedPlayers.isNotVanished(player)) {
                    gameProfiles.add(profile);
                }
            });

            players = new ServerMetadata.Players(players.getPlayerLimit(), Vanish.INSTANCE.getFakePlayerCount());
            players.setSample(gameProfiles.toArray(new GameProfile[0]));
        }
    }
}
