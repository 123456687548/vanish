package eu.vanish.mixin;

import eu.vanish.Vanish;
import eu.vanish.mixinterface.IServerWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerWorld.class)
public class ServerWorldMixin implements IServerWorld {
    ServerWorld SW_Instance = (ServerWorld) (Object) this;

    private boolean silenceBlockBreaking = false;

    public void setSilentBlockBreaking(boolean bool) {
        silenceBlockBreaking = bool;
    }

    @Inject(at = @At("HEAD"), cancellable = true, method = "playSound")
    private void onPlaySound(@Nullable PlayerEntity player, double x, double y, double z, RegistryEntry<SoundEvent> sound, SoundCategory category, float volume, float pitch, long seed, CallbackInfo ci) {
        if (!Vanish.INSTANCE.isActive()) return;
        if (player == null) return;

        if(Vanish.INSTANCE.vanishedPlayers.isVanished(player.getEntityName()))
            ci.cancel();
    }

    @Inject(at=@At("HEAD"),method="setBlockBreakingInfo",cancellable = true)
    private void onUpdateBreakingInfo(int entityId, BlockPos pos, int progress, CallbackInfo ci) {
        if (!Vanish.INSTANCE.isActive()) return;
        Entity entity = SW_Instance.getEntityById(entityId);
        if (!(entity instanceof PlayerEntity)) return;


        if(Vanish.INSTANCE.vanishedPlayers.isVanished(entity.getEntityName()))
            ci.cancel();
    }

    @Inject(at=@At("HEAD"),method="syncWorldEvent",cancellable = true)
    private void onBreakBlock(PlayerEntity player, int eventId, BlockPos pos, int data, CallbackInfo ci) {
        if (silenceBlockBreaking) {
            ci.cancel();
        }
    }
}
