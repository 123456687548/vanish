package eu.vanish.mixin;

import eu.vanish.Vanish;
import eu.vanish.mixinterface.IServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerInteractionManager.class)
public class ServerPlayerInteractionManagerMixin {
    @Shadow private BlockPos miningPos;
    @Shadow @Final protected ServerPlayerEntity player;
    @Shadow protected ServerWorld world;

    public BlockPos getCurrentlyMining() {
        return this.miningPos;
    }

    @Inject(at=@At("HEAD"),method = "tryBreakBlock")
    private void onStartBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (player != null && Vanish.INSTANCE.vanishedPlayers.isVanished(player.getEntityName())) {
            ((IServerWorld)world).setSilentBlockBreaking(true);
        }
    }

    @Inject(at=@At("RETURN"),method = "tryBreakBlock")
    private void onEndBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (player != null && Vanish.INSTANCE.vanishedPlayers.isVanished(player.getEntityName())) {
            ((IServerWorld)world).setSilentBlockBreaking(false);
        }
    }
}
