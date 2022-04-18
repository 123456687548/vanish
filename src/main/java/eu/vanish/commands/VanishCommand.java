package eu.vanish.commands;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.datafixers.util.Pair;
import eu.vanish.Vanish;
import eu.vanish.data.FakeTranslatableText;
import eu.vanish.data.Settings;
import eu.vanish.data.VanishedList;
import eu.vanish.data.VanishedPlayer;
import net.minecraft.block.entity.CommandBlockBlockEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.MessageType;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static com.mojang.brigadier.arguments.BoolArgumentType.bool;
import static com.mojang.brigadier.arguments.BoolArgumentType.getBool;
import static net.minecraft.command.argument.EntityArgumentType.*;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.util.Util.NIL_UUID;

public final class VanishCommand {
    private static final Vanish vanish = Vanish.INSTANCE;
    private static final Settings settings = vanish.getSettings();
    private static ServerWorld world = null;
    private static ServerPlayerEntity vanishStatusEntity = null;

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register((
                literal("vanish")
                        .requires(source -> sourceIsCommandblock(source) || source.hasPermissionLevel(4))
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
        vanish.setServer(executor.getServer());
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

        if (world == null) {
            world = vanish.getServer().getWorlds().iterator().next();
        }
        if (settings.showStatusInPlayerlist() && vanishStatusEntity == null && world != null) {
            vanishStatusEntity = new ServerPlayerEntity(
                    vanish.getServer()
                    , world
                    , new GameProfile(UUID.randomUUID(), " You're Vanished")
            );
        }


        vanish.setActive(true);

        vanishedPlayers.add(vanishedPlayer);
        vanish.increaseAmountOfOnlineVanishedPlayers();

        vanish.getServer().getPlayerManager().getPlayerList().forEach(playerEntity -> {
            if (!playerEntity.equals(vanishingPlayer)) {
                playerEntity.networkHandler.sendPacket(new PlayerListS2CPacket(PlayerListS2CPacket.Action.REMOVE_PLAYER, vanishingPlayer));
                if (settings.showFakeLeaveMessage()) {
                    playerEntity.sendMessage(new FakeTranslatableText("multiplayer.player.left", vanishingPlayer.getDisplayName()).formatted(Formatting.YELLOW), MessageType.SYSTEM, NIL_UUID);
                }
                playerEntity.networkHandler.sendPacket(new EntitiesDestroyS2CPacket(vanishingPlayer.getId()));
            }
        });

        if (vanishStatusEntity != null) {
            vanishingPlayer.networkHandler.sendPacket(new PlayerListS2CPacket(PlayerListS2CPacket.Action.ADD_PLAYER, vanishStatusEntity));
        }
        vanishingPlayer.networkHandler.sendPacket(new GameMessageS2CPacket(new LiteralText("You are now Vanished").formatted(Formatting.GREEN), MessageType.CHAT, NIL_UUID));

        vanish.vanishedPlayers.saveToFile();
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
                playerEntity.networkHandler.sendPacket(new PlayerListS2CPacket(PlayerListS2CPacket.Action.ADD_PLAYER, vanishingPlayer));

                playerEntity.networkHandler.sendPacket(new PlayerSpawnS2CPacket(vanishingPlayer));
                updateEquipment(vanishingPlayer, playerEntity);
                if (settings.showFakeJoinMessage()) {
                    playerEntity.sendMessage(new FakeTranslatableText("multiplayer.player.joined", vanishingPlayer.getDisplayName()).formatted(Formatting.YELLOW), MessageType.SYSTEM, NIL_UUID);
                }
            }
        });

        if (vanishStatusEntity != null) {
            vanishingPlayer.networkHandler.sendPacket(new PlayerListS2CPacket(PlayerListS2CPacket.Action.REMOVE_PLAYER, vanishStatusEntity));
        }

        vanishingPlayer.networkHandler.sendPacket(new GameMessageS2CPacket(new LiteralText("You are no longer Vanished").formatted(Formatting.RED), MessageType.CHAT, NIL_UUID));

        if (vanishedPlayers.isEmpty()) {
            vanish.setActive(false);
        }

        vanish.vanishedPlayers.saveToFile();
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
        vanish.setServer(executor.getServer());

        if (world == null) {
            world = vanish.getServer().getWorlds().iterator().next();
        }

        if (settings.showStatusInPlayerlist() && vanishStatusEntity == null && world != null) {
            vanishStatusEntity = new ServerPlayerEntity(
                    vanish.getServer()
                    , world
                    , new GameProfile(UUID.randomUUID(), " You're Vanished")
            );
        }

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
                    playerEntity.networkHandler.sendPacket(new PlayerListS2CPacket(PlayerListS2CPacket.Action.REMOVE_PLAYER, player));
                    if (settings.showFakeLeaveMessage()) {
                        playerEntity.sendMessage(new FakeTranslatableText("multiplayer.player.left", player.getDisplayName()).formatted(Formatting.YELLOW), MessageType.SYSTEM, NIL_UUID);
                    }
                    playerEntity.networkHandler.sendPacket(new EntitiesDestroyS2CPacket(player.getId()));
                }
            });

            if (vanishStatusEntity != null) {
                player.networkHandler.sendPacket(new PlayerListS2CPacket(PlayerListS2CPacket.Action.ADD_PLAYER, vanishStatusEntity));
            }
            player.networkHandler.sendPacket(new GameMessageS2CPacket(new LiteralText("You are now Vanished").formatted(Formatting.GREEN), MessageType.CHAT, NIL_UUID));
        });

        vanish.vanishedPlayers.saveToFile();
        return 1;
    }

    private static int unvanishAll(ServerCommandSource executor) {
        vanish.setServer(executor.getServer());

        VanishedList vanishedPlayers = vanish.vanishedPlayers;

        List<ServerPlayerEntity> players = executor.getServer().getPlayerManager().getPlayerList();

        players.forEach(player -> {
            VanishedPlayer vanishedPlayer = new VanishedPlayer(player);

            if (vanishedPlayers.isVanished(vanishedPlayer)) { //unvanish
                vanishedPlayers.remove(vanishedPlayer);

                vanish.decreaseAmountOfOnlineVanishedPlayers();

                vanish.getServer().getPlayerManager().getPlayerList().forEach(playerEntity -> {
                    if (!playerEntity.equals(player)) {
                        playerEntity.networkHandler.sendPacket(new PlayerListS2CPacket(PlayerListS2CPacket.Action.ADD_PLAYER, player));

                        playerEntity.networkHandler.sendPacket(new PlayerSpawnS2CPacket(player));
                        updateEquipment(player, playerEntity);
                        if (settings.showFakeJoinMessage()) {
                            playerEntity.sendMessage(new FakeTranslatableText("multiplayer.player.joined", player.getDisplayName()).formatted(Formatting.YELLOW), MessageType.SYSTEM, NIL_UUID);
                        }
                    }
                });

                if (vanishStatusEntity != null) {
                    player.networkHandler.sendPacket(new PlayerListS2CPacket(PlayerListS2CPacket.Action.REMOVE_PLAYER, vanishStatusEntity));
                }

                player.networkHandler.sendPacket(new GameMessageS2CPacket(new LiteralText("You are no longer Vanished").formatted(Formatting.RED), MessageType.CHAT, NIL_UUID));

                if (vanishedPlayers.isEmpty()) {
                    vanish.setActive(false);
                }
            }

        });

        vanish.vanishedPlayers.saveToFile();
        return 1;
    }

    public static void sendFakePlayerListEntry(ServerPlayerEntity player) {
        if (!Vanish.INSTANCE.vanishedPlayers.isVanished(player)) return;
        if (vanishStatusEntity == null) return;
        player.networkHandler.sendPacket(new PlayerListS2CPacket(PlayerListS2CPacket.Action.ADD_PLAYER, vanishStatusEntity));
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
        return source.getName().equals("@") || source.getWorld().getBlockEntity(new BlockPos(source.getPosition())) instanceof CommandBlockBlockEntity;
    }
}
