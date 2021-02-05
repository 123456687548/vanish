package eu.vanish.mixin;

import eu.vanish.mixinterface.IEntityS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(EntityS2CPacket.class)
public class EntityS2CPacketMixin implements IEntityS2CPacket {
    @Shadow
    protected int id;

    @Override
    public int getId() {
        return id;
    }
}
