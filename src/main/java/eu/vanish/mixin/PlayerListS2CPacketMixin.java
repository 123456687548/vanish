package eu.vanish.mixin;

import eu.vanish.mixinterface.IPlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(PlayerListS2CPacket.class)
public class PlayerListS2CPacketMixin implements IPlayerListS2CPacket {
    @Mutable
    @Shadow
    @Final
    private List<PlayerListS2CPacket.Entry> entries;

    @Override
    public List<PlayerListS2CPacket.Entry> getEntriesOnServer() {
        return entries;
    }

    @Override
    public void setEntriesOnServer(List<PlayerListS2CPacket.Entry> entries) {
        this.entries = entries;
    }
}
