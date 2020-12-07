package eu.vanish.mixin;

import eu.vanish.Vanish;
import eu.vanish.mixinterface.IGameMessageS2CPacket;
import eu.vanish.mixinterface.IPlayerListS2CPacket;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.minecraft.network.MessageType.CHAT;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {

    @Inject(at = @At("HEAD"), cancellable = true, method = "send(Lnet/minecraft/network/Packet;Lio/netty/util/concurrent/GenericFutureListener;)V")
    private void onSend(Packet<?> packet, @Nullable GenericFutureListener<? extends Future<? super Void>> callback, CallbackInfo ci) {
        if (!Vanish.INSTANCE.isActive()) return;

        if (packet instanceof GameMessageS2CPacket) {
            if (shouldStopLeaveJoinMessage(packet)) {
                ci.cancel();
            }
        }

        if (packet instanceof PlayerListS2CPacket) {
            removeVanishedPlayers(packet);
        }
    }

    private boolean shouldStopLeaveJoinMessage(Packet<?> packet) {
        if(((GameMessageS2CPacket) packet).getLocation().equals(CHAT)) return false;
        Text textMessage = ((IGameMessageS2CPacket) packet).getMessageOnServer();
        if (textMessage instanceof TranslatableText){
            TranslatableText message = (TranslatableText) textMessage;
            String key = message.getKey();
            if(key.equals("multiplayer.player.joined") || key.equals("multiplayer.player.left")) {
                String messageString = message.toString();
                for (String vanishedPlayer : Vanish.INSTANCE.getVanishedPlayerNames()) {
                    if(messageString.contains(vanishedPlayer)){
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private void removeVanishedPlayers(Packet<?> packet) {
        IPlayerListS2CPacket playerListS2CPacket = (IPlayerListS2CPacket) packet;
        PlayerListS2CPacket.Action action = playerListS2CPacket.getActionOnServer();

        if (action.equals(PlayerListS2CPacket.Action.REMOVE_PLAYER) || action.equals(PlayerListS2CPacket.Action.UPDATE_LATENCY)) return;

        playerListS2CPacket.getEntriesOnServer().removeIf(entry -> Vanish.INSTANCE.getVanishedPlayersUUID().contains(entry.getProfile().getId()));
    }
}
