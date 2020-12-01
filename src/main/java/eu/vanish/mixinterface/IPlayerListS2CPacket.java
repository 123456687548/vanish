package eu.vanish.mixinterface;

import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;

import java.util.List;

public interface IPlayerListS2CPacket {
    List<PlayerListS2CPacket.Entry> getEntriesOnServer();
    PlayerListS2CPacket.Action getActionOnServer();
}
