package eu.vanish.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import eu.vanish.data.Settings;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

public class FileManager {
    public static final Path ROOT_FOLDER = new File(".").toPath().normalize().resolve("mods/vanish");

    public static void init() {
        createFolder();
    }

    private static void createFolder() {
        try {
            Files.createDirectory(ROOT_FOLDER);
        } catch (FileAlreadyExistsException ignore) {

        } catch (IOException e) {
            throw new RuntimeException(String.format("Could not create folder %s", ROOT_FOLDER));
        }
    }

    public static <T> T readFile(Path path, Type clazz) throws NoSuchFileException {
        try {
            String json = new String(Files.readAllBytes(path));
            return new Gson().fromJson(json, clazz);
        } catch (NoSuchFileException noFile) {
            throw noFile;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T readFile(Path path, Class<T> clazz) throws NoSuchFileException {
        try {
            String json = new String(Files.readAllBytes(path));
            return new Gson().fromJson(json, clazz);
        } catch (NoSuchFileException noFile) {
            throw noFile;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void createFile(Path path, byte[] content) {
        try {
            Files.write(path, content);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Could not create file %s", path));
        }
    }
}
