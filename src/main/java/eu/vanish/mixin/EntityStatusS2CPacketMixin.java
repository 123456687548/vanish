package eu.vanish.mixin;

import eu.vanish.mixinterface.IEntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(EntityStatusS2CPacket.class)
public class EntityStatusS2CPacketMixin implements IEntityStatusS2CPacket {
    @Shadow private int id;

    @Override
    public int getIdOnServer() {
        return id;
    }
}
