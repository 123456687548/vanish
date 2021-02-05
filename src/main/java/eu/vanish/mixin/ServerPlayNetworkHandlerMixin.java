package eu.vanish.mixin;

import eu.vanish.Vanish;
import eu.vanish.data.Settings;
import eu.vanish.exeptions.NoTranslateableMessageExeption;
import eu.vanish.mixinterface.*;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {
    private Settings settings = Vanish.INSTANCE.getSettings();

    @Shadow
    public ServerPlayerEntity player;

    @Inject(at = @At("HEAD"), cancellable = true, method = "sendPacket(Lnet/minecraft/network/Packet;Lio/netty/util/concurrent/GenericFutureListener;)V")
    private void onSendPacket(Packet<?> packet, @Nullable GenericFutureListener<? extends Future<? super Void>> callback, CallbackInfo ci) {
        if (!Vanish.INSTANCE.isActive()) return;

        if (packet instanceof GameMessageS2CPacket) {
            GameMessageS2CPacket gameMessagePacket = (GameMessageS2CPacket) packet;

            if (shouldStopMessage(gameMessagePacket)) {
                ci.cancel();
            }
        }

        if (packet instanceof EntityS2CPacket
                || packet instanceof EntityVelocityUpdateS2CPacket
                || packet instanceof EntitySetHeadYawS2CPacket
                || packet instanceof EntityStatusS2CPacket
                || packet instanceof EntityPositionS2CPacket
                || packet instanceof EntityAnimationS2CPacket
                || packet instanceof EntityAttributesS2CPacket
                || packet instanceof EntityTrackerUpdateS2CPacket
                || packet instanceof EntityEquipmentUpdateS2CPacket) {
            EntityIDProvider entityIDProvider = (EntityIDProvider) packet;
            if (Vanish.INSTANCE.getVanishedPlayers().stream().anyMatch(vanishedPlayer ->
                    vanishedPlayer.getEntityId() == entityIDProvider.getIdOnServer())) {
                ci.cancel();
            }
        }

        if (packet instanceof PlayerListS2CPacket) {
            removeVanishedPlayers(packet);
        }
    }

    private boolean shouldStopMessage(GameMessageS2CPacket packet) {
        try {
            TranslatableText message = getTranslateableTextFromPacket(packet);

            if (!settings.removeChatMessage() && message.getKey().contains("chat.type.text")) return false;
            if (!settings.removeWisperMessage() && message.getKey().contains("commands.message.display.incoming")) return false;
            if (!settings.removeCommandOPMessage() && message.getKey().contains("chat.type.admin")) return false;
            if (settings.showFakeJoinMessage() && !packet.isNonChat() && message.getKey().contains("multiplayer.player.joined")) return false;
            if (settings.showFakeLeaveMessage() && !packet.isNonChat() && message.getKey().contains("multiplayer.player.left")) return false;

            return Arrays.stream(message.getArgs()).anyMatch(arg -> {
                if (arg instanceof LiteralText) {
                    String name = ((LiteralText) arg).getRawString();
                    return !name.equals(player.getEntityName()) && Vanish.INSTANCE.isVanished(name);
                }
                return false;
            });
        } catch (NoTranslateableMessageExeption ignore) {
            return false;
        }
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

    private TranslatableText getTranslateableTextFromPacket(GameMessageS2CPacket packet) throws NoTranslateableMessageExeption {
        Text textMessage = ((IGameMessageS2CPacket) packet).getMessageOnServer();
        if (textMessage instanceof TranslatableText) {
            return (TranslatableText) textMessage;
        }
        throw new NoTranslateableMessageExeption();
    }

    @Inject(at = @At("HEAD"), method = "onDisconnected")
    private void onDisconnect(CallbackInfo ci) {
        Vanish.INSTANCE.onDisconnect(player);
    }
}
