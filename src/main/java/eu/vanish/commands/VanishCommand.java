package eu.vanish.commands;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.datafixers.util.Pair;
import eu.vanish.Vanish;
import eu.vanish.data.FakeTranslatableTextContent;
import eu.vanish.data.Settings;
import eu.vanish.data.VanishedList;
import eu.vanish.data.VanishedPlayer;
import eu.vanish.util.FPAPIUtilsWrapper;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.block.entity.CommandBlockBlockEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.apache.logging.log4j.LogManager;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import static com.mojang.brigadier.arguments.BoolArgumentType.bool;
import static com.mojang.brigadier.arguments.BoolArgumentType.getBool;
import static net.minecraft.command.argument.EntityArgumentType.*;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class VanishCommand {
    private static final Vanish vanish = Vanish.INSTANCE;
    private static final Settings settings = vanish.getSettings();
    private static ServerWorld world = null;
    private static ServerPlayerEntity vanishStatusEntity = null;

    public static void init() {
        world = vanish.getServer().getWorlds().iterator().next();
        vanishStatusEntity = new ServerPlayerEntity(
                vanish.getServer()
                , world
                , new GameProfile(UUID.randomUUID(), " You're Vanished")
        );
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register((
                literal("vanish")
//                        .requires(source -> sourceIsCommandblock(source) || source.hasPermissionLevel(4))
                        .requires(FPAPIUtilsWrapper.require("vanish.vanish", true))
                        .executes(context -> toggleVanish(context.getSource().getPlayer())))
                .then((
                        argument("target", player())
                                .executes(context -> toggleVanish(getPlayer(context, "target"))))
                        .then(argument("on", bool())
                                .executes(context -> toggleVanish(getPlayer(context, "target"), getBool(context, "on")))))
                .then((
                        argument("targets", players())
                                .executes(context -> toggleVanish(getPlayers(context, "targets"))))
                        .then(argument("on", bool())
                                .executes(context -> toggleVanish(getPlayers(context, "targets"), getBool(context, "on")))))
                .then((
                        literal("all")
                                .executes(context -> vanishAllToggle(context.getSource(), true)))
                        .then(argument("on", bool())
                                .executes(context -> vanishAllToggle(context.getSource(), getBool(context, "on")))))
                .then((literal("reload")
                        .executes(context -> reloadSettings(context.getSource()))))
                .then((literal("list")
                        .executes(context -> listVanishedPlayers(context.getSource()))))
        );
    }

    private static int toggleVanish(Collection<ServerPlayerEntity> players, boolean enable) {
        players.forEach(player -> toggleVanish(player, enable));
        return 1;
    }

    private static int toggleVanish(Collection<ServerPlayerEntity> executor) {
        executor.forEach(VanishCommand::toggleVanish);

        return 1;
    }

    private static int toggleVanish(ServerPlayerEntity executor, boolean enable) {
        if (enable) {
            return vanish(executor);
        } else {
            return unvanish(executor);
        }
    }

    private static int toggleVanish(ServerPlayerEntity executor) {
        VanishedList vanishedPlayers = vanish.vanishedPlayers;
        VanishedPlayer executorPlayer = new VanishedPlayer(executor);

        return toggleVanish(executor, vanishedPlayers.isNotVanished(executorPlayer));
    }

    private static int vanish(ServerPlayerEntity vanishingPlayer) {
        VanishedList vanishedPlayers = vanish.vanishedPlayers;
        VanishedPlayer vanishedPlayer = new VanishedPlayer(vanishingPlayer);
        if (vanishedPlayers.isVanished(vanishingPlayer)) return 1;

        vanish.setActive(true);

        vanishedPlayers.add(vanishedPlayer);
        vanish.increaseAmountOfOnlineVanishedPlayers();

        vanish.getServer().getPlayerManager().getPlayerList().forEach(playerEntity -> {
            if (!playerEntity.equals(vanishingPlayer)) {
                playerEntity.networkHandler.sendPacket(new PlayerRemoveS2CPacket(List.of(vanishingPlayer.getUuid())));
                playerEntity.networkHandler.sendPacket(new EntitiesDestroyS2CPacket(vanishingPlayer.getId()));
            }
        });

        if (settings.showFakeLeaveMessage()) {
            vanish.getServer().getPlayerManager().broadcast(MutableText.of(new FakeTranslatableTextContent("multiplayer.player.left", vanishingPlayer.getDisplayName())).formatted(Formatting.YELLOW), false);
        }

        sendFakePlayerListEntry(vanishingPlayer);

        vanishingPlayer.networkHandler.sendPacket(new GameMessageS2CPacket(Text.literal("You are now Vanished").formatted(Formatting.GREEN), true));

        vanish.vanishedPlayers.saveToFile();
        logVanish(vanishingPlayer);
        return 1;
    }

    private static int unvanish(ServerPlayerEntity vanishingPlayer) {
        VanishedList vanishedPlayers = vanish.vanishedPlayers;
        VanishedPlayer vanishedPlayer = new VanishedPlayer(vanishingPlayer);
        if (vanishedPlayers.isNotVanished(vanishingPlayer)) return 1;

        vanishedPlayers.remove(vanishedPlayer);

        vanish.decreaseAmountOfOnlineVanishedPlayers();

        vanish.getServer().getPlayerManager().getPlayerList().forEach(playerEntity -> {
            if (!playerEntity.equals(vanishingPlayer)) {
                playerEntity.networkHandler.sendPacket(PlayerListS2CPacket.entryFromPlayer(List.of(vanishingPlayer)));
                playerEntity.networkHandler.sendPacket(new PlayerSpawnS2CPacket(vanishingPlayer));
                updateEquipment(vanishingPlayer, playerEntity);
            }
        });

        if (settings.showFakeJoinMessage()) {
            vanish.getServer().getPlayerManager().broadcast(MutableText.of(new FakeTranslatableTextContent("multiplayer.player.joined", vanishingPlayer.getDisplayName())).formatted(Formatting.YELLOW), false);
        }

        removeFakePlayerListEntry(vanishingPlayer);

        vanishingPlayer.networkHandler.sendPacket(new GameMessageS2CPacket(Text.literal("You are no longer Vanished").formatted(Formatting.RED), true));

        if (vanishedPlayers.isEmpty()) {
            vanish.setActive(false);
        }

        vanish.vanishedPlayers.saveToFile();
        logUnvanish(vanishingPlayer);
        return 1;
    }

    private static int vanishAllToggle(ServerCommandSource source, boolean enable) {
        if (enable) {
            return vanishAll(source);
        } else {
            return unvanishAll(source);
        }
    }

    private static int vanishAll(ServerCommandSource executor) {
        VanishedList vanishedPlayers = vanish.vanishedPlayers;

        List<ServerPlayerEntity> players = executor.getServer().getPlayerManager().getPlayerList();

        players.forEach(player -> {
            VanishedPlayer vanishedPlayer = new VanishedPlayer(player);
            if (vanishedPlayers.isVanished(vanishedPlayer)) return;

            vanish.setActive(true);

            vanishedPlayers.add(vanishedPlayer);
            vanish.increaseAmountOfOnlineVanishedPlayers();

            vanish.getServer().getPlayerManager().getPlayerList().forEach(playerEntity -> {
                if (!playerEntity.equals(player)) {
                    playerEntity.networkHandler.sendPacket(new PlayerRemoveS2CPacket(List.of(player.getUuid())));
                    playerEntity.networkHandler.sendPacket(new EntitiesDestroyS2CPacket(player.getId()));
                }
            });

            if (settings.showFakeLeaveMessage()) {
                vanish.getServer().getPlayerManager().broadcast(MutableText.of(new FakeTranslatableTextContent("multiplayer.player.left", player.getDisplayName())).formatted(Formatting.YELLOW), false);
            }

            sendFakePlayerListEntry(player);

            player.networkHandler.sendPacket(new GameMessageS2CPacket(Text.literal("You are now Vanished").formatted(Formatting.GREEN), true));

            logVanish(player);
        });

        vanish.vanishedPlayers.saveToFile();
        return 1;
    }

    private static int unvanishAll(ServerCommandSource executor) {
        VanishedList vanishedPlayers = vanish.vanishedPlayers;

        List<ServerPlayerEntity> players = executor.getServer().getPlayerManager().getPlayerList();

        players.forEach(player -> {
            VanishedPlayer vanishedPlayer = new VanishedPlayer(player);

            if (vanishedPlayers.isVanished(vanishedPlayer)) { //unvanish
                vanishedPlayers.remove(vanishedPlayer);

                vanish.decreaseAmountOfOnlineVanishedPlayers();

                vanish.getServer().getPlayerManager().getPlayerList().forEach(playerEntity -> {
                    if (!playerEntity.equals(player)) {
                        playerEntity.networkHandler.sendPacket(PlayerListS2CPacket.entryFromPlayer(List.of(player)));
                        playerEntity.networkHandler.sendPacket(new PlayerSpawnS2CPacket(player));
                        updateEquipment(player, playerEntity);
                    }
                });

                if (settings.showFakeJoinMessage()) {
                    vanish.getServer().getPlayerManager().broadcast(MutableText.of(new FakeTranslatableTextContent("multiplayer.player.joined", player.getDisplayName())).formatted(Formatting.YELLOW), false);
                }

                removeFakePlayerListEntry(player);

                player.networkHandler.sendPacket(new GameMessageS2CPacket(Text.literal("You are no longer Vanished").formatted(Formatting.RED), true));

                if (vanishedPlayers.isEmpty()) {
                    vanish.setActive(false);
                }

                logUnvanish(player);
            }
        });

        vanish.vanishedPlayers.saveToFile();
        return 1;
    }

    public static void sendFakePlayerListEntry(ServerPlayerEntity player) {
        if (!Vanish.INSTANCE.vanishedPlayers.isVanished(player)) return;
        if (vanishStatusEntity == null) return;
        player.networkHandler.sendPacket(PlayerListS2CPacket.entryFromPlayer(List.of(vanishStatusEntity)));
    }

    public static void removeFakePlayerListEntry(ServerPlayerEntity player) {
        if (vanishStatusEntity == null) return;
        player.networkHandler.sendPacket(new PlayerRemoveS2CPacket(List.of(vanishStatusEntity.getUuid())));
    }

    private static void updateEquipment(ServerPlayerEntity vanishingPlayer, ServerPlayerEntity receiver) {
        List<Pair<EquipmentSlot, ItemStack>> equipmentList = Lists.newArrayList();

        for (EquipmentSlot equipmentSlot : EquipmentSlot.values()) {
            ItemStack itemStack = vanishingPlayer.getEquippedStack(equipmentSlot);
            if (!itemStack.isEmpty()) {
                equipmentList.add(Pair.of(equipmentSlot, itemStack.copy()));
            }
        }

        if (!equipmentList.isEmpty()) {
            receiver.networkHandler.sendPacket(new EntityEquipmentUpdateS2CPacket(vanishingPlayer.getId(), equipmentList));
        }
    }

    private static boolean sourceIsCommandblock(ServerCommandSource source) {
        return source.getName().equals("@") || source.getWorld().getBlockEntity(new BlockPos((int) source.getPosition().x, (int) source.getPosition().y, (int) source.getPosition().z)) instanceof CommandBlockBlockEntity;
    }

    private static int reloadSettings(ServerCommandSource source) {
        vanish.reloadSettings();

        ServerPlayerEntity executor = source.getPlayer();
        executor.networkHandler.sendPacket(new GameMessageS2CPacket(Text.literal("Reloaded vanish settings").formatted(Formatting.YELLOW), true));
        return 1;
    }

    private static int listVanishedPlayers(ServerCommandSource source) {
        ServerPlayerEntity executor = source.getPlayer();

        if (executor == null) return 0;

        if (!vanish.getSettings().isVanishListCommand()) {
            executor.sendMessage(Text.of("Please enable vanishListCommand inside the settings for this command"));
            return 1;
        }

        Collection<VanishedPlayer> vanishedPlayers = vanish.vanishedPlayers.getVanishedPlayers();

        Iterator<VanishedPlayer> iterator = vanishedPlayers.iterator();

        StringBuilder vanishedPlayersString = new StringBuilder("Vanished Players:\n");

        while (iterator.hasNext()) {
            VanishedPlayer vanishedPlayer = iterator.next();
            vanishedPlayersString.append(vanishedPlayer.getName());
            if (iterator.hasNext()) {
                vanishedPlayersString.append(",\n");
            }
        }

        executor.sendMessage(Text.of(vanishedPlayersString.toString()));

        return 1;
    }

    private static void logVanish(ServerPlayerEntity player) {
        if (vanish.getSettings().isLogVanishToConsole()) {
            LogManager.getLogger().info("{} vanished!", player.getEntityName());
        }
    }

    private static void logUnvanish(ServerPlayerEntity player) {
        if (vanish.getSettings().isLogUnvanishToConsole()) {
            LogManager.getLogger().info("{} unvanished!", player.getEntityName());
        }
    }
}
