package eu.vanish.mixin;

import eu.vanish.Vanish;
import eu.vanish.mixinterface.IPlayerListS2CPacket;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {

    @Inject(at = @At("HEAD"), cancellable = true, method = "sendPacket(Lnet/minecraft/network/Packet;Lio/netty/util/concurrent/GenericFutureListener;)V")
    private void onSendPacket(Packet<?> packet, GenericFutureListener<? extends Future<? super Void>> listener, CallbackInfo ci) {
        removeVanishedPlayers(packet);
    }

    private void removeVanishedPlayers(Packet<?> packet) {
        if (!Vanish.INSTANCE.isActive()) return;
        if (packet instanceof PlayerListS2CPacket) {
            IPlayerListS2CPacket playerListS2CPacket = (IPlayerListS2CPacket) packet;
            PlayerListS2CPacket.Action action = playerListS2CPacket.getActionOnServer();

            if (action.equals(PlayerListS2CPacket.Action.REMOVE_PLAYER) || action.equals(PlayerListS2CPacket.Action.UPDATE_LATENCY)) return;

            playerListS2CPacket.getEntriesOnServer().removeIf(entry -> Vanish.INSTANCE.getVanishedPlayersUUID().contains(entry.getProfile().getId()));
        }
    }
}
