package eu.vanish.mixin;

import eu.vanish.Vanish;
import net.minecraft.server.ServerMetadata;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;
import java.util.List;

@Mixin(ServerMetadata.class)
public class ServerMetadataMixin {

    @Shadow
    private ServerMetadata.Players players;

    @Inject(at = @At("HEAD"), method = "getPlayers")
    private void onGetPlayers(CallbackInfoReturnable<ServerMetadata.Players> ci) {
        if (Vanish.INSTANCE.isActive()) {
            int amountOfVanishedPlayers = Vanish.INSTANCE.getVanishedPlayersUUID().size();
            int realAmountOfPlayers = Vanish.INSTANCE.getServer().getCurrentPlayerCount();

            List<String> onlinePlayerNames = Arrays.asList(Vanish.INSTANCE.getServer().getPlayerManager().getPlayerNames());

            for (String name : Vanish.INSTANCE.getVanishedPlayerNames()) {
                if (!onlinePlayerNames.contains(name)) {
                    amountOfVanishedPlayers--;
                }
            }

            int fakePlayerAmount = Math.max(realAmountOfPlayers - amountOfVanishedPlayers, 0);

            players = new ServerMetadata.Players(players.getPlayerLimit(), fakePlayerAmount);
        }
    }
}
