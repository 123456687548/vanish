package eu.vanish.mixin;

import eu.vanish.mixinterface.IEntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(EntityVelocityUpdateS2CPacket.class)
public class EntityVelocityUpdateS2CPacketMixin implements IEntityVelocityUpdateS2CPacket {
    @Shadow private int id;

    @Override
    public int getIdOnServer() {
        return id;
    }
}
