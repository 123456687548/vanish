package eu.vanish.mixin;

import eu.vanish.mixinterface.IItemPickupAnimationS2CPacket;
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ItemPickupAnimationS2CPacket.class)
public class ItemPickupAnimationS2CPacketMixin implements IItemPickupAnimationS2CPacket {
    @Shadow
    private int collectorEntityId;

    @Shadow private int entityId;

    @Override
    public int getIdOnServer() {
        return collectorEntityId;
    }

    @Override
    public int getItemIdOnServer() {
        return entityId;
    }
}
