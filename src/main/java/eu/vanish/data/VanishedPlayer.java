package eu.vanish.data;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

public final class VanishedPlayer {
    private final String name;
    private final UUID uuid;
    private int entityId;

    public VanishedPlayer(ServerPlayerEntity player) {
        this.name = player.getEntityName();
        this.uuid = player.getUuid();
        this.entityId = player.getId();
    }

    public String getName() {
        return name;
    }

    public UUID getUUID() {
        return uuid;
    }

    public int getEntityId() {
        return entityId;
    }

    public void setEntityId(int entityId) {
        this.entityId = entityId;
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
