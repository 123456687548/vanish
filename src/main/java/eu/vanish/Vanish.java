package eu.vanish;

import eu.vanish.commands.OverwrittenListCommand;
import eu.vanish.commands.OverwrittenMsgCommand;
import eu.vanish.commands.VanishCommand;
import eu.vanish.data.Settings;
import eu.vanish.data.VanishedList;
import eu.vanish.data.VanishedPlayer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import eu.vanish.util.FileManager;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public enum Vanish {
    INSTANCE;

    private boolean active = false;

    public VanishedList vanishedPlayers;

    private MinecraftServer server = null;

    private int amountOfVanishedPlayersOnline = 0;

    private Settings settings;

    public void init() {
        FileManager.init();
        settings = Settings.loadSettings();
        vanishedPlayers = new VanishedList();
        registerCommands();
    }

    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated, registrationEnvironment) -> {
                    VanishCommand.register(dispatcher);
                    if (settings.overwriteListCommand()) {
                        OverwrittenListCommand.register(dispatcher);
                    }
                    if (settings.overwriteMsgCommand()) {
                        OverwrittenMsgCommand.register(dispatcher);
                    }
                }
        );
    }

    public void onDisconnect(ServerPlayerEntity player) {
        setServer(player.getServer());

        if (vanishedPlayers.isVanished(player)) {
            decreaseAmountOfOnlineVanishedPlayers();
        }
    }

    public void onPlayerConnect(ServerPlayerEntity player) {
        setServer(player.getServer());

        VanishedPlayer vanishedPlayer = vanishedPlayers.get(player.getUuid());

        if (vanishedPlayer == null) return;

        vanishedPlayer.setEntityId(player.getId());

        server.getPlayerManager().getPlayerList().forEach(playerEntity -> {
            if (!vanishedPlayer.getUUID().equals(playerEntity.getUuid())) {
                playerEntity.networkHandler.sendPacket(new EntitiesDestroyS2CPacket(vanishedPlayer.getEntityId()));
            }
        });

        increaseAmountOfOnlineVanishedPlayers();
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

    public MinecraftServer getServer() {
        return server;
    }

    public void setServer(MinecraftServer server) {
        if (this.server != null) {
            return;
        }
        this.server = server;
    }

    public Settings getSettings() {
        return settings;
    }

    public void reloadSettings(){
        settings = Settings.loadSettings();
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }
}
