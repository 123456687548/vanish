package eu.vanish.data;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.UUID;

public class VList {
    private final HashMap<UUID, VanishedPlayer> vanishedPlayers = new HashMap<>();

    public void add(VanishedPlayer vanishedPlayer){
        vanishedPlayers.put(vanishedPlayer.getUUID(), vanishedPlayer);
    }

    public void remove(VanishedPlayer vanishedPlayer){
        vanishedPlayers.remove(vanishedPlayer.getUUID());
    }

    public VanishedPlayer get(UUID uuid){
        return vanishedPlayers.get(uuid);
    }

    public VanishedPlayer get(GameProfile profile){
        return vanishedPlayers.get(profile.getId());
    }

    public boolean isEmpty(){
        return vanishedPlayers.isEmpty();
    }

    public boolean isVanished(VanishedPlayer player){
        return containsUUID(player.getUUID());
    }

    public boolean isNotVanished(VanishedPlayer player){
        return !containsUUID(player.getUUID());
    }

    public boolean isVanished(GameProfile profile){
        return containsUUID(profile.getId());
    }

    public boolean isNotVanished(GameProfile profile){
        return !containsUUID(profile.getId());
    }

    public boolean isVanished(String playerName){
        return containsName(playerName);
    }

    public boolean isNotVanished(String playerName) {
        return !containsName(playerName);
    }

    public boolean isVanished(int playerID){
        return containsID(playerID);
    }

    public boolean isNotVanished(int playerID){
        return !containsID(playerID);
    }

    public boolean isVanished(ServerPlayerEntity player){
        return containsUUID(player.getUuid());
    }

    public boolean isNotVanished(ServerPlayerEntity player){
        return !containsUUID(player.getUuid());
    }

    private boolean containsUUID(UUID uuid) {
        return vanishedPlayers.containsKey(uuid);
    }

    private boolean containsID(int id) {
        return vanishedPlayers
                .values()
                .stream()
                .anyMatch(vanishedPlayer -> vanishedPlayer.getEntityId() == id);
    }

    private boolean containsName(String name) {
        return vanishedPlayers
                .values()
                .stream()
                .anyMatch(vanishedPlayer -> vanishedPlayer.getName().equals(name));
    }
}
