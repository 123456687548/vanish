package eu.vanish.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import eu.vanish.util.FileManager;

import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import static eu.vanish.util.FileManager.ROOT_FOLDER;

public final class Settings {
    private static final transient Path CONFIG_FILE = ROOT_FOLDER.resolve("config.json");

    private boolean showFakeLeaveMessage = true;
    private boolean showFakeJoinMessage = true;
    private boolean showStatusInPlayerlist = true;
    private boolean removeChatMessage = true;
    private boolean removeWisperMessage = true;
    private boolean removeCommandOPMessage = true;
    private boolean overwriteMsgCommand = true;
    private boolean overwriteListCommand = true;
    private boolean fakePlayerCount = true;
    private boolean persistent = true;
    private boolean logVanishToConsole = false;
    private boolean logUnvanishToConsole = false;

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

    public boolean removeChatMessage() {
        return removeChatMessage;
    }

    public boolean removeWisperMessage() {
        return removeWisperMessage;
    }

    public boolean removeCommandOPMessage() {
        return removeCommandOPMessage;
    }

    public boolean overwriteMsgCommand() {
        return overwriteMsgCommand;
    }

    public boolean overwriteListCommand() {
        return overwriteListCommand;
    }

    public boolean fakePlayerCount() {
        return fakePlayerCount;
    }

    public boolean isLogVanishToConsole() {
        return logVanishToConsole;
    }

    public boolean isLogUnvanishToConsole(){
        return logUnvanishToConsole;
    }

    public boolean isPersistent() {
        return persistent;
    }

    public static Settings loadSettings() {
        try {
            Settings settings = FileManager.readFile(CONFIG_FILE, Settings.class);
            saveSettings(settings);
            return settings;
        } catch (NoSuchFileException e) {
            return createDefaultSettings();
        }
    }

    private static void saveSettings(Settings settings){
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(settings);

        FileManager.createFile(CONFIG_FILE, json.getBytes());
    }

    private static Settings createDefaultSettings() {
        Settings settings = new Settings();

        saveSettings(settings);

        return settings;
    }
}
