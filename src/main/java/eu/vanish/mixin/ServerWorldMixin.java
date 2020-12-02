package eu.vanish.mixin;

import eu.vanish.Vanish;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(ServerWorld.class)
public class ServerWorldMixin {

    @Inject(at = @At("HEAD"), cancellable = true, method = "playSound")
    private void onPlaySound(@Nullable PlayerEntity player, double x, double y, double z, SoundEvent sound, SoundCategory category, float volume, float pitch, CallbackInfo ci) {
        if (!Vanish.INSTANCE.isActive()) return;

        AtomicBoolean cancel = new AtomicBoolean(false);

        Vanish.INSTANCE.getVanishedPlayers().forEach(playerEntity -> {
            if (playerEntity.equals(player)) {
                cancel.set(true);
            }
        });

        if (cancel.get()) ci.cancel();
    }
}
