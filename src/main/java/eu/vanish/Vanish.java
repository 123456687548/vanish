package eu.vanish;

import eu.vanish.commands.OverwrittenListCommand;
import eu.vanish.commands.OverwrittenMsgCommand;
import eu.vanish.commands.VanishCommand;
import eu.vanish.data.Settings;
import eu.vanish.data.VanishedPlayer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashSet;

public enum Vanish {
    INSTANCE;

    private boolean active = false;

    private final HashSet<VanishedPlayer> vanishedPlayers = new HashSet<>();

    private MinecraftServer server = null;

    private int amountOfVanishedPlayersOnline = 0;

    private Settings settings;

    public void init() {
        settings = Settings.loadSettings();
        registerCommands();
    }

    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
                    VanishCommand.register(dispatcher);
                    OverwrittenListCommand.register(dispatcher);
                    OverwrittenMsgCommand.register(dispatcher);
                }
        );
    }

    public void onDisconnect(ServerPlayerEntity player) {
        setServer(player.getServer());

        if (vanishedPlayers.stream().anyMatch(vanishedPlayer -> vanishedPlayer.getUuid().equals(player.getUuid()))) {
            decreaseAmountOfOnlineVanishedPlayers();
        }
    }

    public void onPlayerConnect(ServerPlayerEntity player) {
        setServer(player.getServer());

        if (vanishedPlayers.stream().anyMatch(vanishedPlayer -> vanishedPlayer.getUuid().equals(player.getUuid()))) {
            increaseAmountOfOnlineVanishedPlayers();
        }
    }

    public int getFakePlayerCount() {
        return Math.max(server.getCurrentPlayerCount() - amountOfVanishedPlayersOnline, 0); //wrong
    }

    public void decreaseAmountOfOnlineVanishedPlayers() {
        amountOfVanishedPlayersOnline--;
    }

    public void increaseAmountOfOnlineVanishedPlayers() {
        amountOfVanishedPlayersOnline++;
    }

    public HashSet<VanishedPlayer> getVanishedPlayers() {
        return vanishedPlayers;
    }

    public MinecraftServer getServer() {
        return server;
    }

    public void setServer(MinecraftServer server) {
        if (this.server != null) return;
        this.server = server;
    }

    public Settings getSettings() {
        return settings;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }
}
