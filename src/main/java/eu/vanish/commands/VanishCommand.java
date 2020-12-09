package eu.vanish.commands;

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
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;

import java.util.HashSet;

import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.util.Util.NIL_UUID;

public final class VanishCommand {

    private static final Vanish vanish = Vanish.INSTANCE;

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralCommandNode<ServerCommandSource> commandNode = dispatcher.register(literal("vanish")
                .requires(source -> source.hasPermissionLevel(4))
                .executes(context -> vanish(context.getSource().getPlayer())));
//        dispatcher.register(literal("v").redirect(commandNode));
    }

    private static int vanish(ServerPlayerEntity player) {
        vanish.setServer(player.getServer());

        HashSet<VanishedPlayer> vanishedPlayers = vanish.getVanishedPlayers();
        VanishedPlayer vanishedPlayer = new VanishedPlayer(player);

        if (vanishedPlayers.contains(vanishedPlayer)) { //unvanish
            vanishedPlayers.remove(vanishedPlayer);

            vanish.decreaseAmountOfOnlineVanishedPlayers();

            vanish.getServer().getPlayerManager().getPlayerList().forEach(playerEntity -> {
                if (!playerEntity.equals(player)) {
                    playerEntity.networkHandler.sendPacket(new PlayerListS2CPacket(PlayerListS2CPacket.Action.ADD_PLAYER, player));
                    playerEntity.networkHandler.sendPacket(new GameMessageS2CPacket(new TranslatableText("multiplayer.player.joined", new LiteralText(player.getEntityName())).formatted(Formatting.YELLOW), MessageType.CHAT, NIL_UUID));
                    playerEntity.networkHandler.sendPacket(new PlayerSpawnS2CPacket(player));
                }
            });

            player.networkHandler.sendPacket(new GameMessageS2CPacket(new LiteralText("Your aren't Vanished anymore").formatted(Formatting.RED), MessageType.CHAT, NIL_UUID));

            if (vanishedPlayers.isEmpty()) {
                vanish.setActive(false);
            }
        } else { //vanish
            vanish.setActive(true);

            vanishedPlayers.add(vanishedPlayer);
            vanish.increaseAmountOfOnlineVanishedPlayers();

            vanish.getServer().getPlayerManager().getPlayerList().forEach(playerEntity -> {
                if (!playerEntity.equals(player)) {
                    playerEntity.networkHandler.sendPacket(new PlayerListS2CPacket(PlayerListS2CPacket.Action.REMOVE_PLAYER, player));
                    playerEntity.networkHandler.sendPacket(new GameMessageS2CPacket(new TranslatableText("multiplayer.player.left", new LiteralText(player.getEntityName())).formatted(Formatting.YELLOW), MessageType.CHAT, NIL_UUID));
                    playerEntity.networkHandler.sendPacket(new EntitiesDestroyS2CPacket(player.getEntityId()));
                }
            });

            player.networkHandler.sendPacket(new GameMessageS2CPacket(new LiteralText("Your are now Vanished").formatted(Formatting.GREEN), MessageType.CHAT, NIL_UUID));
        }
        return 1;
    }
}
