package eu.vanish.commands;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.datafixers.util.Pair;
import eu.vanish.Vanish;
import eu.vanish.data.FakeTranslatableTextContent;
import eu.vanish.data.Settings;
import eu.vanish.data.VanishedPlayer;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static net.minecraft.server.command.CommandManager.literal;

public final class VanishCommand {
    private static final Vanish vanish = Vanish.INSTANCE;
    private static final Settings settings = vanish.getSettings();

    private static ServerWorld world = null;

    private static ServerPlayerEntity vanishStatusEntity = null;

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralCommandNode<ServerCommandSource> commandNode = dispatcher.register(literal("vanish")
                .requires(source -> source.hasPermissionLevel(4))
                .executes(context -> vanish(context.getSource().getPlayerOrThrow())));
//        dispatcher.register(literal("v").redirect(commandNode));
    }

    private static int vanish(ServerPlayerEntity vanishingPlayer) {
        vanish.setServer(vanishingPlayer.getServer());

        if (world == null) {
            world = vanish.getServer().getWorlds().iterator().next();
        }
        if (settings.showStatusInPlayerlist() && vanishStatusEntity == null && world != null) {
            vanishStatusEntity = new ServerPlayerEntity(
                    vanish.getServer()
                    , world
                    , new GameProfile(UUID.randomUUID(), " You're Vanished")
                    , null
            );
        }

        HashSet<VanishedPlayer> vanishedPlayers = vanish.getVanishedPlayers();
        VanishedPlayer vanishedPlayer = new VanishedPlayer(vanishingPlayer);

        if (vanishedPlayers.contains(vanishedPlayer)) { //unvanish
            vanishedPlayers.remove(vanishedPlayer);

            vanish.decreaseAmountOfOnlineVanishedPlayers();

            vanish.getServer().getPlayerManager().getPlayerList().forEach(playerEntity -> {
                if (!playerEntity.equals(vanishingPlayer)) {
                    playerEntity.networkHandler.sendPacket(new PlayerListS2CPacket(PlayerListS2CPacket.Action.ADD_PLAYER, vanishingPlayer));

                    playerEntity.networkHandler.sendPacket(new PlayerSpawnS2CPacket(vanishingPlayer));
                    updateEquipment(vanishingPlayer, playerEntity);
                    if (settings.showFakeJoinMessage()) {
                        playerEntity.sendMessage(MutableText.of(new FakeTranslatableTextContent("multiplayer.player.joined", vanishingPlayer.getDisplayName())).formatted(Formatting.YELLOW));
                    }
                }
            });

            if (vanishStatusEntity != null) {
                vanishingPlayer.networkHandler.sendPacket(new PlayerListS2CPacket(PlayerListS2CPacket.Action.REMOVE_PLAYER, vanishStatusEntity));
            }

            vanishingPlayer.networkHandler.sendPacket(new GameMessageS2CPacket(Text.literal("You are no longer Vanished").formatted(Formatting.RED), 1));

            if (vanishedPlayers.isEmpty()) {
                vanish.setActive(false);
            }
        } else { //vanish
            vanish.setActive(true);

            vanishedPlayers.add(vanishedPlayer);
            vanish.increaseAmountOfOnlineVanishedPlayers();

            vanish.getServer().getPlayerManager().getPlayerList().forEach(playerEntity -> {
                if (!playerEntity.equals(vanishingPlayer)) {
                    playerEntity.networkHandler.sendPacket(new PlayerListS2CPacket(PlayerListS2CPacket.Action.REMOVE_PLAYER, vanishingPlayer));
                    if (settings.showFakeLeaveMessage()) {
                        playerEntity.sendMessage(MutableText.of(new FakeTranslatableTextContent("multiplayer.player.left", vanishingPlayer.getDisplayName())).formatted(Formatting.YELLOW));
                    }
                    playerEntity.networkHandler.sendPacket(new EntitiesDestroyS2CPacket(vanishingPlayer.getId()));
                }
            });

            if (vanishStatusEntity != null) {
                vanishingPlayer.networkHandler.sendPacket(new PlayerListS2CPacket(PlayerListS2CPacket.Action.ADD_PLAYER, vanishStatusEntity));
            }
            vanishingPlayer.networkHandler.sendPacket(new GameMessageS2CPacket(Text.literal("You are now Vanished").formatted(Formatting.GREEN), 1));
        }
        return 1;
    }

    public static void sendFakePlayerListEntry(ServerPlayerEntity player) {
        if (Vanish.INSTANCE.getVanishedPlayers().stream().noneMatch(vanishedPlayer -> vanishedPlayer.getUuid().equals(player.getUuid()))) return;
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
}
