/*package eu.vanish.mixin;

import eu.vanish.Vanish;
import eu.vanish.mixinterface.IServerPlayerInteractionManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(World.class)
public class WorldMixin {
    World W_instance = (World) (Object) this;

    @Redirect(method="breakBlock(Lnet/minecraft/util/math/BlockPos;ZLnet/minecraft/entity/Entity;I)Z",at=@At(value="INVOKE",target="Lnet/minecraft/world/World;syncWorldEvent(ILnet/minecraft/util/math/BlockPos;I)V"))
    private void vanishNoiseCanceling(World instance, int i, BlockPos blockPos, int i2) {
        for (ServerPlayerEntity SPE : instance.getServer().getPlayerManager().getPlayerList()) {
            System.out.println();
            System.out.println(Vanish.INSTANCE.vanishedPlayers.isVanished(SPE.getEntityName()));
            System.out.println(((IServerPlayerInteractionManager)instance.getServer().getPlayerInteractionManager(SPE)).getCurrentlyMining().equals(blockPos));
            if (!Vanish.INSTANCE.vanishedPlayers.isVanished(SPE.getEntityName()) || !((IServerPlayerInteractionManager)instance.getServer().getPlayerInteractionManager(SPE)).getCurrentlyMining().equals(blockPos)) {
                W_instance.syncWorldEvent(i, blockPos, i2);
            }
            else {
                System.out.println("noise cancelled bozo!");
            }

        }
    }
}*/
