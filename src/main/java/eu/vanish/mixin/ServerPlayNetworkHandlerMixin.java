package eu.vanish.mixin;

import eu.vanish.Vanish;
import eu.vanish.data.Settings;
import eu.vanish.data.VanishedPlayer;
import eu.vanish.mixinterface.IGameMessageS2CPacket;
import eu.vanish.mixinterface.IPlayerListS2CPacket;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.minecraft.network.MessageType.CHAT;
import static net.minecraft.network.MessageType.SYSTEM;

//todo cleanup (code repetition)

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {
    private Settings settings = Vanish.INSTANCE.getSettings();

    @Shadow
    public ServerPlayerEntity player;

    @Inject(at = @At("HEAD"), cancellable = true, method = "sendPacket(Lnet/minecraft/network/Packet;Lio/netty/util/concurrent/GenericFutureListener;)V")
    private void onSendPacket(Packet<?> packet, @Nullable GenericFutureListener<? extends Future<? super Void>> callback, CallbackInfo ci) {
        if (!Vanish.INSTANCE.isActive()) return;

        if (packet instanceof GameMessageS2CPacket) {
            if (shouldStopLeaveJoinMessage(packet)
                    || shouldStopAdvancementMessage(packet)
                    || shouldStopDeathMessage(packet)
                    || shouldStopCommandMessage(packet)) {
                ci.cancel();
            }
        }

        if (packet instanceof PlayerListS2CPacket) {
            removeVanishedPlayers(packet);
        }
    }

    private boolean shouldStopCommandMessage(Packet<?> packet) {
        if (!settings.removeCommandOPMessage()) return false;
        if (((GameMessageS2CPacket) packet).getLocation().equals(CHAT)) return false;
        Text textMessage = ((IGameMessageS2CPacket) packet).getMessageOnServer();
        if (textMessage instanceof TranslatableText) {
            TranslatableText message = (TranslatableText) textMessage;
            String key = message.getKey();
            if (key.equals("chat.type.admin")) {
                String messageString = message.toString();
                for (VanishedPlayer vanishedPlayer : Vanish.INSTANCE.getVanishedPlayers()) {
                    if (messageString.contains(vanishedPlayer.getName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean shouldStopDeathMessage(Packet<?> packet) {
        if(!settings.removeDeathMessage()) return false;
        if (((GameMessageS2CPacket) packet).getLocation().equals(CHAT)) return false;
        Text textMessage = ((IGameMessageS2CPacket) packet).getMessageOnServer();
        if (textMessage instanceof TranslatableText) {
            TranslatableText message = (TranslatableText) textMessage;
            String key = message.getKey();
            if (key.contains("death")) {
                String messageString = message.toString();
                for (VanishedPlayer vanishedPlayer : Vanish.INSTANCE.getVanishedPlayers()) {
                    if (messageString.contains(vanishedPlayer.getName())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean shouldStopLeaveJoinMessage(Packet<?> packet) {
        if (((GameMessageS2CPacket) packet).getLocation().equals(CHAT)) return false;
        Text textMessage = ((IGameMessageS2CPacket) packet).getMessageOnServer();
        if (textMessage instanceof TranslatableText) {
            TranslatableText message = (TranslatableText) textMessage;
            String key = message.getKey();
            if ((settings.removeJoinMessage() && key.equals("multiplayer.player.joined"))
                    || (settings.removeLeaveMessage() && key.equals("multiplayer.player.left"))) {
                String messageString = message.toString();
                for (VanishedPlayer vanishedPlayer : Vanish.INSTANCE.getVanishedPlayers()) {
                    if (messageString.contains(vanishedPlayer.getName())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean shouldStopAdvancementMessage(Packet<?> packet) {
        if(!settings.removeAdvancementMessage()) return false;
        if (!((GameMessageS2CPacket) packet).getLocation().equals(SYSTEM)) return false;
        Text textMessage = ((IGameMessageS2CPacket) packet).getMessageOnServer();
        if (textMessage instanceof TranslatableText) {
            TranslatableText message = (TranslatableText) textMessage;
            String key = message.getKey();
            if (key.contains("chat.type.advancement.")) {
                String messageString = message.toString();
                for (VanishedPlayer vanishedPlayer : Vanish.INSTANCE.getVanishedPlayers()) {
                    if (messageString.contains(vanishedPlayer.getName())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private void removeVanishedPlayers(Packet<?> packet) {
        if (Vanish.INSTANCE.getVanishedPlayers().stream().anyMatch(vanishedPlayer -> vanishedPlayer.getUuid().equals(player.getUuid()))) return;
        IPlayerListS2CPacket playerListS2CPacket = (IPlayerListS2CPacket) packet;
        PlayerListS2CPacket.Action action = playerListS2CPacket.getActionOnServer();

        if (action.equals(PlayerListS2CPacket.Action.REMOVE_PLAYER) || action.equals(PlayerListS2CPacket.Action.UPDATE_LATENCY) || action.equals(PlayerListS2CPacket.Action.UPDATE_GAME_MODE))
            return;

        playerListS2CPacket.getEntriesOnServer().removeIf(entry ->
                Vanish.INSTANCE.getVanishedPlayers().stream().anyMatch(vanishedPlayer ->
                        vanishedPlayer.getUuid().equals(entry.getProfile().getId())
                )
        );
    }

    @Inject(at = @At("HEAD"), method = "onDisconnected")
    private void onDisconnect(CallbackInfo ci) {
        Vanish.INSTANCE.onDisconnect(player);
    }
}
