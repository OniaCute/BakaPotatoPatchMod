package cc.oniacute.bakapotatopatcher.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class BakaPotatoClientConfigManager {
    public static final String CONFIG_FILE_NAME = "bakapotatopatcher-client.json";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static Path configFile;
    private static BakaPotatoClientConfig config = BakaPotatoClientConfig.defaults();

    private BakaPotatoClientConfigManager() {
    }

    public static synchronized void initialize(Path configDirectory) {
        configFile = configDirectory.resolve(CONFIG_FILE_NAME);
        load();
    }

    public static synchronized BakaPotatoClientConfig get() {
        return config;
    }

    public static synchronized BakaPotatoClientConfig copy() {
        return config.copy();
    }

    public static synchronized void replaceAndSave(BakaPotatoClientConfig replacement) {
        replacement.normalize();
        config = replacement.copy();
        save();
    }

    public static synchronized void load() {
        if (configFile == null) {
            config = BakaPotatoClientConfig.defaults();
            return;
        }
        try {
            Files.createDirectories(configFile.getParent());
            if (!Files.exists(configFile)) {
                config = BakaPotatoClientConfig.defaults();
                save();
                return;
            }
            try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
                BakaPotatoClientConfig loaded = GSON.fromJson(reader, BakaPotatoClientConfig.class);
                config = loaded == null ? BakaPotatoClientConfig.defaults() : loaded;
                config.normalize();
            }
        } catch (IOException | RuntimeException exception) {
            config = BakaPotatoClientConfig.defaults();
        }
    }

    public static synchronized void save() {
        if (configFile == null) {
            return;
        }
        try {
            Files.createDirectories(configFile.getParent());
            try (Writer writer = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException ignored) {
            // The game can continue with the in-memory config.
        }
    }
}
