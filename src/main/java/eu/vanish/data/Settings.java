package eu.vanish.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

public final class Settings {
    private static transient Path configFile;

    private boolean showFakeLeaveMessage = true;
    private boolean showFakeJoinMessage = true;
    private boolean showStatusInPlayerlist = true;

    private Settings() {

    }

    public boolean showFakeLeaveMessage() {
        return showFakeLeaveMessage;
    }

    public boolean showFakeJoinMessage() {
        return showFakeJoinMessage;
    }

    public boolean showStatusInPlayerlist() {
        return showStatusInPlayerlist;
    }

    public static Settings loadSettings() {
        loadConfigFilePath();

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try {
            String json = new String(Files.readAllBytes(configFile));
            return gson.fromJson(json, Settings.class);
        } catch (NoSuchFileException noFile) {
            return createDefaultSettings();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Settings createDefaultSettings() {
        Settings settings = new Settings();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(settings);

        try {
            Files.write(configFile, json.getBytes());
        } catch (IOException e) {
            throw new RuntimeException("Could not create Vanish config file");
        }

        return settings;
    }

    private static void loadConfigFilePath() {
        Path serverFolder = new File(".").toPath().normalize();
        Path vanishConfigFolder = serverFolder.resolve("mods/vanish");

        try {
            Files.createDirectory(vanishConfigFolder);
        } catch (FileAlreadyExistsException ignore) {

        } catch (IOException e) {
            throw new RuntimeException("Could not create Vanish config folder");
        }

        configFile = vanishConfigFolder.resolve("config.json");
    }
}
