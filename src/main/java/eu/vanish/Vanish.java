package eu.vanish;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.network.MessageType;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.util.Util.NIL_UUID;

public enum Vanish {
    INSTANCE;

    private boolean active = false;

    private HashSet<UUID> vanishedPlayersUUID;
    private List<ServerPlayerEntity> vanishedPlayers;

    private MinecraftServer server = null;

    public void init() {
        vanishedPlayersUUID = new HashSet<>();
        vanishedPlayers = new ArrayList<>();

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) ->
                dispatcher.register(literal("vanish")
                        .requires(source -> source.hasPermissionLevel(4))
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayer();
                            GameProfile profile = player.getGameProfile();

                            server = context.getSource().getMinecraftServer();

                            active = true;

                            if (vanishedPlayersUUID.contains(profile.getId())) {
                                vanishedPlayersUUID.remove(profile.getId());
                                vanishedPlayers.remove(player);
                                server.getPlayerManager().sendToAll(new PlayerListS2CPacket(PlayerListS2CPacket.Action.ADD_PLAYER, player));
                                server.getPlayerManager().sendToAll(new GameMessageS2CPacket(new LiteralText(String.format("\u00a7e%s joined the game", player.getEntityName())), MessageType.CHAT, NIL_UUID));
                            } else {
                                vanishedPlayersUUID.add(profile.getId());
                                vanishedPlayers.add(player);
                                server.getPlayerManager().sendToAll(new PlayerListS2CPacket(PlayerListS2CPacket.Action.REMOVE_PLAYER, player));
                                server.getPlayerManager().sendToAll(new GameMessageS2CPacket(new LiteralText(String.format("\u00a7e%s left the game", player.getEntityName())), MessageType.CHAT, NIL_UUID));
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

    public List<ServerPlayerEntity> getVanishedPlayers() {
        return vanishedPlayers;
    }

    public MinecraftServer getServer() {
        return server;
    }

    public boolean isActive() {
        return active;
    }
}
