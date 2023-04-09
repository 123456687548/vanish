package eu.vanish.mixin;

import eu.vanish.Vanish;
import eu.vanish.data.FakeTranslatableTextContent;
import eu.vanish.data.Settings;
import eu.vanish.data.VanishedPlayer;
import eu.vanish.exeptions.NoTranslateableMessageException;
import eu.vanish.mixinterface.EntityIDProvider;
import eu.vanish.mixinterface.IItemPickupAnimationS2CPacket;
import eu.vanish.mixinterface.IPlayerListS2CPacket;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {
    private final Settings settings = Vanish.INSTANCE.getSettings();

    @Shadow
    public ServerPlayerEntity player;

    @Inject(at = @At("HEAD"), cancellable = true, method = "sendPacket(Lnet/minecraft/network/packet/Packet;)V")
    private void onSendPacket(Packet<?> packet,  CallbackInfo ci) {
        if (!Vanish.INSTANCE.isActive()) {
            return;
        }

        if (packet instanceof GameMessageS2CPacket gameMessagePacket) {
            if (shouldStopMessage(gameMessagePacket)) {
                ci.cancel();
            }
        }

        if (settings.removeChatMessage()) {
            if (packet instanceof ChatMessageS2CPacket chatMessagePacket) {
                if (Vanish.INSTANCE.vanishedPlayers.isVanished(chatMessagePacket.sender())) {
                    ci.cancel();
                }
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

            if (Vanish.INSTANCE.vanishedPlayers.isVanished(entityIDProvider.getIdOnServer())) {
                if (player.getId() != entityIDProvider.getIdOnServer()) {
                    ci.cancel();
                }
            }
        }

        if (packet instanceof ItemPickupAnimationS2CPacket) {
            IItemPickupAnimationS2CPacket entityIDProvider = (IItemPickupAnimationS2CPacket) packet;

            if (Vanish.INSTANCE.vanishedPlayers.isVanished(entityIDProvider.getIdOnServer())) {
                player.networkHandler.sendPacket(new EntitiesDestroyS2CPacket(entityIDProvider.getItemIdOnServer()));
                ci.cancel();
            }
        }

        if (packet instanceof PlayerListS2CPacket) {
            removeVanishedPlayers(packet);
        }
    }

    private boolean shouldStopMessage(GameMessageS2CPacket packet) {
        try {
            TranslatableTextContent message = getTranslateableTextFromPacket(packet);

            if (!settings.removeChatMessage() && message.getKey().contains("chat.type.text")) {
                return false;
            }
            if (!settings.removeWisperMessage() && message.getKey().contains("commands.message.display.incoming")) {
                return false;
            }
            if (!settings.removeCommandOPMessage() && message.getKey().contains("chat.type.admin")) {
                return false;
            }
            if (settings.showFakeJoinMessage() && message instanceof FakeTranslatableTextContent && message.getKey().contains("multiplayer.player.joined")) {
                return false;
            }
            if (settings.showFakeLeaveMessage() && message instanceof FakeTranslatableTextContent && message.getKey().contains("multiplayer.player.left")) {
                return false;
            }

            return Arrays.stream(message.getArgs()).anyMatch(arg -> {
                if (!(arg instanceof MutableText)) return false;

                MutableText text = (MutableText) arg;
                String name = text.getString();
                if (!name.equals(player.getEntityName()) && Vanish.INSTANCE.vanishedPlayers.isVanished(name)) {
                    return true;
                }

                return text.getSiblings().stream().anyMatch(sibling -> {
                    String name2 = sibling.getString();
                    return !name2.equals(player.getEntityName()) && Vanish.INSTANCE.vanishedPlayers.isVanished(name2);
                });
            });
        } catch (NoTranslateableMessageException ignore) {
            return false;
        }
    }

    private void removeVanishedPlayers(Packet<?> packet) {
        IPlayerListS2CPacket playerListS2CPacket = (IPlayerListS2CPacket) packet;
        PlayerListS2CPacket.Action action = playerListS2CPacket.getActionOnServer();

        if (action.equals(PlayerListS2CPacket.Action.UPDATE_LISTED) || action.equals(PlayerListS2CPacket.Action.UPDATE_LATENCY) || action.equals(PlayerListS2CPacket.Action.UPDATE_GAME_MODE)) {
            return;
        }

        //todo maybe error
     /*   playerListS2CPacket. getEntriesOnServer().removeIf(entry -> {
            VanishedPlayer entryPlayer = Vanish.INSTANCE.vanishedPlayers.get(entry.profile());
            if (entryPlayer == null) return false;
            return entryPlayer.getUUID().equals(entry.profile().getId()) && !entryPlayer.getUUID().equals(player.getUuid());
        }); */
    }

    private TranslatableTextContent getTranslateableTextFromPacket(GameMessageS2CPacket packet) throws NoTranslateableMessageException {
        Text textMessage = packet.content();
        TextContent content = textMessage.getContent();
        if (content instanceof TranslatableTextContent) {
            return (TranslatableTextContent) content;
        }
        throw new NoTranslateableMessageException();
    }

    @Inject(at = @At("HEAD"), method = "onDisconnected")
    private void onDisconnect(CallbackInfo ci) {
        Vanish.INSTANCE.onDisconnect(player);
    }
}
