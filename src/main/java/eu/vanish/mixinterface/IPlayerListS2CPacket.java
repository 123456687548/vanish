package eu.vanish.mixinterface;

import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;

import java.util.EnumSet;
import java.util.List;

public interface IPlayerListS2CPacket {
    List<PlayerListS2CPacket.Entry> getEntriesOnServer();
    EnumSet<PlayerListS2CPacket.Action> getActionsOnServer();
}
