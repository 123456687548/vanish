package eu.vanish.mixin;

import eu.vanish.mixinterface.IEntitySetHeadYawS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySetHeadYawS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(EntitySetHeadYawS2CPacket.class)
public class EntitySetHeadYawS2CPacketMixin implements IEntitySetHeadYawS2CPacket {
    @Shadow private int entity;

    @Override
    public int getIdOnServer() {
        return entity;
    }
}
