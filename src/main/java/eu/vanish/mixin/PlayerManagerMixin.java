package eu.vanish.mixin;

import eu.vanish.Vanish;
import eu.vanish.commands.VanishCommand;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.ServerMetadata;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;


@Mixin(PlayerManager.class)
public class PlayerManagerMixin {

    @Shadow
    List<ServerPlayerEntity> players;

    @Inject(at = @At("TAIL"), method = "onPlayerConnect")
    private void onPlayerConnect(ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci) {
        System.out.println("Player connecting: " + player.getEntityName());
        Vanish.INSTANCE.onPlayerConnect(player);
        VanishCommand.sendFakePlayerListEntry(player, players.stream().filter( playerfd -> Vanish.INSTANCE.vanishedPlayers.isNotVanished(playerfd.getUuid())).toList());
        }

    @Inject(method = "getPlayerList", at = @At("RETURN"), cancellable = true)
            private void onGetPlayerList(CallbackInfoReturnable<java.util.List<ServerPlayerEntity>> cir){
        List<ServerPlayerEntity> newPlayers =  players.stream().filter( player -> Vanish.INSTANCE.vanishedPlayers.isNotVanished(player.getUuid())).toList();
        cir.setReturnValue( newPlayers);
        }
}
