package eu.vanish.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import eu.vanish.Vanish;
import eu.vanish.data.VanishedPlayer;
import net.minecraft.network.MessageType;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerSpawnS2CPacket;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;

import java.util.HashSet;
import java.util.UUID;

import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.util.Util.NIL_UUID;

public final class VanishCommand {
    private static final Vanish vanish = Vanish.INSTANCE;

    private static ServerWorld world = null;

    private static ServerPlayerEntity vanishStatusEntity = null;

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralCommandNode<ServerCommandSource> commandNode = dispatcher.register(literal("vanish")
                .requires(source -> source.hasPermissionLevel(4))
                .executes(context -> vanish(context.getSource().getPlayer())));
//        dispatcher.register(literal("v").redirect(commandNode));
    }

    private static int vanish(ServerPlayerEntity vanishingPlayer) {
        vanish.setServer(vanishingPlayer.getServer());

        if (world == null) {
            world = vanish.getServer().getWorlds().iterator().next();
        }
        if (vanishStatusEntity == null && world != null) {
            vanishStatusEntity = new ServerPlayerEntity(
                    vanish.getServer()
                    , world
                    , new GameProfile(UUID.randomUUID(), " You're Vanished")
                    , new ServerPlayerInteractionManager(world)
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
                    playerEntity.networkHandler.sendPacket(new GameMessageS2CPacket(new TranslatableText("multiplayer.player.joined", new LiteralText(vanishingPlayer.getEntityName())).formatted(Formatting.YELLOW), MessageType.CHAT, NIL_UUID));
                    playerEntity.networkHandler.sendPacket(new PlayerSpawnS2CPacket(vanishingPlayer));
                }
            });

            if (vanishStatusEntity != null) {
                vanishingPlayer.networkHandler.sendPacket(new PlayerListS2CPacket(PlayerListS2CPacket.Action.REMOVE_PLAYER, vanishStatusEntity));
            }

            vanishingPlayer.networkHandler.sendPacket(new GameMessageS2CPacket(new LiteralText("You are no longer Vanished").formatted(Formatting.RED), MessageType.CHAT, NIL_UUID));

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
                    playerEntity.networkHandler.sendPacket(new GameMessageS2CPacket(new TranslatableText("multiplayer.player.left", new LiteralText(vanishingPlayer.getEntityName())).formatted(Formatting.YELLOW), MessageType.CHAT, NIL_UUID));
                    playerEntity.networkHandler.sendPacket(new EntitiesDestroyS2CPacket(vanishingPlayer.getEntityId()));
                }
            });

            if (vanishStatusEntity != null) {
                vanishingPlayer.networkHandler.sendPacket(new PlayerListS2CPacket(PlayerListS2CPacket.Action.ADD_PLAYER, vanishStatusEntity));
            }
            vanishingPlayer.networkHandler.sendPacket(new GameMessageS2CPacket(new LiteralText("You are now Vanished").formatted(Formatting.GREEN), MessageType.CHAT, NIL_UUID));
        }
        return 1;
    }

    public static void sendFakePlayerListEntry(ServerPlayerEntity player){
        if(Vanish.INSTANCE.getVanishedPlayers().stream().noneMatch(vanishedPlayer -> vanishedPlayer.getUuid().equals(player.getUuid()))) return;
        if(vanishStatusEntity == null) return;
        player.networkHandler.sendPacket(new PlayerListS2CPacket(PlayerListS2CPacket.Action.ADD_PLAYER, vanishStatusEntity));
    }
}
