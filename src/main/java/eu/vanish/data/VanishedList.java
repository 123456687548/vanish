package eu.vanish.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.authlib.GameProfile;
import eu.vanish.Vanish;
import eu.vanish.util.FileManager;
import net.minecraft.server.network.ServerPlayerEntity;

import java.lang.reflect.Type;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.UUID;

import static eu.vanish.util.FileManager.ROOT_FOLDER;

public class VanishedList {
    private static final Vanish vanish = Vanish.INSTANCE;
    private static final Path PERSISTENCE_FILE = ROOT_FOLDER.resolve("persistence.json");
    private final HashMap<UUID, VanishedPlayer> vanishedPlayers;

    public VanishedList() {
        vanishedPlayers = loadFromFile();
    }

    public void add(VanishedPlayer vanishedPlayer) {
        vanishedPlayers.put(vanishedPlayer.getUUID(), vanishedPlayer);
    }

    public void remove(VanishedPlayer vanishedPlayer) {
        vanishedPlayers.remove(vanishedPlayer.getUUID());
    }

    public VanishedPlayer get(UUID uuid) {
        return vanishedPlayers.get(uuid);
    }

    public VanishedPlayer get(GameProfile profile) {
        return vanishedPlayers.get(profile.getId());
    }

    public boolean isEmpty() {
        return vanishedPlayers.isEmpty();
    }

    public boolean isVanished(VanishedPlayer player) {
        return containsUUID(player.getUUID());
    }

    public boolean isNotVanished(VanishedPlayer player) {
        return !containsUUID(player.getUUID());
    }

    public boolean isVanished(GameProfile profile) {
        return containsUUID(profile.getId());
    }

    public boolean isNotVanished(GameProfile profile) {
        return !containsUUID(profile.getId());
    }

    public boolean isVanished(String playerName) {
        return containsName(playerName);
    }

    public boolean isNotVanished(String playerName) {
        return !containsName(playerName);
    }

    public boolean isVanished(int playerID) {
        return containsID(playerID);
    }

    public boolean isNotVanished(int playerID) {
        return !containsID(playerID);
    }

    public boolean isVanished(ServerPlayerEntity player) {
        return containsUUID(player.getUuid());
    }

    public boolean isNotVanished(ServerPlayerEntity player) {
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

    public void saveToFile() {
        if (!vanish.getSettings().isPersistent()) return;

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(vanishedPlayers);

        FileManager.createFile(PERSISTENCE_FILE, json.getBytes());
    }

    private HashMap<UUID, VanishedPlayer> loadFromFile() {
        if (!vanish.getSettings().isPersistent()) new HashMap<>();
        Type returnType = new TypeToken<HashMap<UUID, VanishedPlayer>>() {
        }.getType();

        try {
            return FileManager.readFile(PERSISTENCE_FILE, returnType);
        } catch (NoSuchFileException e) {
            return new HashMap<>();
        }
    }
}
