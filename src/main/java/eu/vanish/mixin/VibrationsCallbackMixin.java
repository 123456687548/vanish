package eu.vanish.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import eu.vanish.Vanish;
import net.minecraft.entity.Entity;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.Vibrations;

@Mixin(Vibrations.Callback.class)
public interface VibrationsCallbackMixin {
    @Inject(
        at = @At("HEAD"),
        cancellable = true,
        method = "canAccept(Lnet/minecraft/world/event/GameEvent;Lnet/minecraft/world/event/GameEvent$Emitter;)Z"
    )
    private void canAccept(GameEvent gameEvent, GameEvent.Emitter emitter, CallbackInfoReturnable<Boolean> cir) {
        if(!Vanish.INSTANCE.getSettings().isDisableSculkSensorWhileVanished()){
            return;
        }
        
        Entity vibrationSourceEntity = emitter.sourceEntity();
        if (vibrationSourceEntity != null && Vanish.INSTANCE.vanishedPlayers.isVanished(vibrationSourceEntity.getUuid())) {
            cir.setReturnValue(false);
        }
    }
}
