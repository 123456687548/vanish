package eu.vanish;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.network.MessageType;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;

import java.util.HashSet;
import java.util.UUID;

import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.util.Util.NIL_UUID;

public enum Vanish {
    INSTANCE;

    private boolean active = false;

    private HashSet<UUID> vanishedPlayersUUID;
    private HashSet<String> vanishedPlayerNames;

    private MinecraftServer server = null;

    public void init() {
        vanishedPlayersUUID = new HashSet<>();
        vanishedPlayerNames = new HashSet<>();

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) ->
                dispatcher.register(literal("vanish")
                        .requires(source -> source.hasPermissionLevel(4))
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayer();
                            GameProfile profile = player.getGameProfile();

                            server = context.getSource().getMinecraftServer();

                            if (vanishedPlayersUUID.contains(profile.getId())) {
                                vanishedPlayersUUID.remove(profile.getId());
                                vanishedPlayerNames.remove(player.getEntityName());
                                server.getPlayerManager().sendToAll(new PlayerListS2CPacket(PlayerListS2CPacket.Action.ADD_PLAYER, player));
                                server.getPlayerManager().sendToAll(new GameMessageS2CPacket(new TranslatableText("multiplayer.player.joined", new LiteralText(player.getEntityName())).formatted(Formatting.YELLOW), MessageType.CHAT, NIL_UUID));
                                server.getPlayerManager().getPlayerList().forEach(playerEntity -> {
                                    if (!playerEntity.equals(player)) {
                                        playerEntity.networkHandler.sendPacket(new PlayerSpawnS2CPacket(player));
                                    }
                                });
                                if(vanishedPlayersUUID.isEmpty()){
                                    active = false;
                                }
                            } else {
                                active = true;
                                vanishedPlayersUUID.add(profile.getId());
                                vanishedPlayerNames.add(player.getEntityName());
                                server.getPlayerManager().sendToAll(new PlayerListS2CPacket(PlayerListS2CPacket.Action.REMOVE_PLAYER, player));
                                server.getPlayerManager().sendToAll(new GameMessageS2CPacket(new TranslatableText("multiplayer.player.left", new LiteralText(player.getEntityName())).formatted(Formatting.YELLOW), MessageType.CHAT, NIL_UUID));
                                server.getPlayerManager().getPlayerList().forEach(playerEntity -> {
                                    if (!playerEntity.equals(player)) {
                                        playerEntity.networkHandler.sendPacket(new EntitiesDestroyS2CPacket(player.getEntityId()));
                                    }
                                });
                            }
                            return 1;
                        })
                )
        );
    }

    public HashSet<UUID> getVanishedPlayersUUID() {
        return vanishedPlayersUUID;
    }

    public HashSet<String> getVanishedPlayerNames() {
        return vanishedPlayerNames;
    }

    public MinecraftServer getServer() {
        return server;
    }

    public boolean isActive() {
        return active;
    }
}
