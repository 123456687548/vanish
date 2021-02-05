package eu.vanish.mixin;

import eu.vanish.mixinterface.EntityIDProvider;
import net.minecraft.network.packet.s2c.play.EntityAttributesS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(EntityAttributesS2CPacket.class)
public class EntityAttributesS2CPacketMixin implements EntityIDProvider {
    @Shadow
    private int entityId;

    @Override
    public int getIdOnServer() {
        return entityId;
    }
}
