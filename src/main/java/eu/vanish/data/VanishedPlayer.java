package eu.vanish.data;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

public final class VanishedPlayer {
    private final String name;
    private final UUID uuid;

    public VanishedPlayer(ServerPlayerEntity player) {
        this.name = player.getEntityName();
        this.uuid = player.getUuid();
    }

    public String getName() {
        return name;
    }

    public UUID getUuid() {
        return uuid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VanishedPlayer that = (VanishedPlayer) o;
        return name.equals(that.name) && uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        return name.hashCode() * uuid.hashCode();
    }
}
