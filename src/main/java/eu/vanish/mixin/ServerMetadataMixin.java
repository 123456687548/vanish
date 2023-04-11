package eu.vanish.mixin;

import com.mojang.authlib.GameProfile;
import eu.vanish.Vanish;
import net.minecraft.server.ServerMetadata;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mixin(ServerMetadata.class)
public class ServerMetadataMixin {

    @Shadow
    Optional<ServerMetadata.Players> players;

    @Inject(method = "players", at = @At("RETURN"), cancellable = true)
    private void onGetPlayers(CallbackInfoReturnable<Optional<ServerMetadata.Players>> cir) {
        Optional<ServerMetadata.Players> newPlayers = null;
        if (Vanish.INSTANCE.isActive() && Vanish.INSTANCE.getSettings().fakePlayerCount()) {
            List<GameProfile> gameProfiles = new ArrayList<>();

            Vanish.INSTANCE.getServer().getPlayerManager().getPlayerList().forEach(player -> {
                GameProfile profile = player.getGameProfile();
                if (Vanish.INSTANCE.vanishedPlayers.isNotVanished(player)) {
                    gameProfiles.add(profile);
                }
            });
            newPlayers = Optional.of(new ServerMetadata.Players(players.get().max(), Vanish.INSTANCE.getFakePlayerCount(), gameProfiles));

        }
        if(newPlayers == null){
            cir.setReturnValue(players);
        } else {
            cir.setReturnValue(newPlayers);
        }
    }


  /*  @Inject(at = @At("HEAD"), method = "players")
    private void onGetPlayers(CallbackInfoReturnable<ServerMetadata.Players> ci) {
        if (Vanish.INSTANCE.isActive() && Vanish.INSTANCE.getSettings().fakePlayerCount()) {
            List<GameProfile> gameProfiles = new ArrayList<>();

            Vanish.INSTANCE.getServer().getPlayerManager().getPlayerList().forEach(player -> {
                GameProfile profile = player.getGameProfile();
                if (Vanish.INSTANCE.vanishedPlayers.isNotVanished(player)) {
                    gameProfiles.add(profile);
                }
            });
            players = Optional.of(new ServerMetadata.Players(players.get().max(), Vanish.INSTANCE.getFakePlayerCount(), gameProfiles));
        }
    } */
}
