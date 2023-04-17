package eu.vanish.mixin;

import eu.vanish.Vanish;
import eu.vanish.data.FakeTranslatableTextContent;
import eu.vanish.data.Settings;
import eu.vanish.exeptions.NoTranslateableMessageException;
import eu.vanish.mixinterface.EntityIDProvider;
import eu.vanish.mixinterface.IItemPickupAnimationS2CPacket;
import eu.vanish.mixinterface.IPlayerListS2CPacket;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.*;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Debug(export = true)
@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {
    private final Settings settings = Vanish.INSTANCE.getSettings();

    @Shadow
    public ServerPlayerEntity player;

    @Inject(at = @At("HEAD"), cancellable = true, method = "sendPacket(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/PacketCallbacks;)V")
    private void onSendPacket(Packet<?> packet, @Nullable PacketCallbacks arg, CallbackInfo ci) {
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

        List<PlayerListS2CPacket.Entry> fakeEntries = new ArrayList<>();

        playerListS2CPacket.getEntriesOnServer().forEach(entry -> {
            if (player.getUuid().equals(entry.profileId())) { // allawys see yourself -> show
                fakeEntries.add(entry);
                return;
            }

            if (Vanish.INSTANCE.vanishedPlayers.isVanished(entry.profileId())) return; // entry is vanished -> don't show

            fakeEntries.add(entry); // everything else -> show
        });

        playerListS2CPacket.setEntriesOnServer(fakeEntries);
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
