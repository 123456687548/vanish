package eu.vanish.mixin;

import eu.vanish.Vanish;
import net.minecraft.server.ServerMetadata;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerMetadata.class)
public class ServerMetadataMixin {

    @Shadow
    private ServerMetadata.Players players;

    @Inject(at = @At("HEAD"), method = "getPlayers")
    private void onGetPlayers(CallbackInfoReturnable<ServerMetadata.Players> ci) {
        if (Vanish.INSTANCE.isActive()) {
            int amountOfVanishedPlayers = Vanish.INSTANCE.getVanishedPlayersUUID().size();
            int realAmoungOfPlayers = Vanish.INSTANCE.getServer().getCurrentPlayerCount();
            players = new ServerMetadata.Players(players.getPlayerLimit(), realAmoungOfPlayers - amountOfVanishedPlayers);
        }
    }
}
