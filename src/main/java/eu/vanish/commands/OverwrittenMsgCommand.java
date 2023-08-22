package eu.vanish.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import eu.vanish.Vanish;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.MessageArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;

import java.util.Collection;
import java.util.UUID;
import java.util.function.Consumer;

import static net.minecraft.server.command.CommandManager.literal;

public final class OverwrittenMsgCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralCommandNode<ServerCommandSource> literalCommandNode = dispatcher.register(literal("msg").then(CommandManager.argument("targets", EntityArgumentType.players()).then(CommandManager.argument("message", MessageArgumentType.message()).executes((commandContext) -> {
            return execute(commandContext.getSource(), EntityArgumentType.getPlayers(commandContext, "targets"), MessageArgumentType.getMessage(commandContext, "message"));
        }))));
        dispatcher.register(literal("tell").redirect(literalCommandNode));
        dispatcher.register(literal("w").redirect(literalCommandNode));
    }

    private static int execute(ServerCommandSource source, Collection<ServerPlayerEntity> targets, Text message) {
        UUID uUID = source.getEntity() == null ? Util.NIL_UUID : source.getEntity().getUuid();
        Entity entity = source.getEntity();
        Consumer<Text> consumer2;
        if (entity instanceof ServerPlayerEntity serverPlayerEntity) {
            consumer2 = (text2) -> {
                serverPlayerEntity.sendMessage(Text.translatable("commands.message.display.outgoing", text2, message).formatted(Formatting.GRAY, Formatting.ITALIC));
            };
        } else {
            consumer2 = (text2) -> {
                source.sendFeedback(() -> (Text.translatable("commands.message.display.outgoing", text2, message).formatted(Formatting.GRAY, Formatting.ITALIC)), false);
            };
        }

        for (ServerPlayerEntity target : targets) {
            if (Vanish.INSTANCE.vanishedPlayers.isVanished(target)) {
                if (targets.size() == 1) {
                    source.sendFeedback(() -> (Text.translatable("argument.entity.notfound.player").formatted(Formatting.RED)), false);
                }
            } else {
                consumer2.accept(target.getDisplayName());
                target.sendMessage(Text.translatable("commands.message.display.incoming", source.getDisplayName(), message).formatted(Formatting.GRAY, Formatting.ITALIC));
            }
        }

        return targets.size();
    }
}
