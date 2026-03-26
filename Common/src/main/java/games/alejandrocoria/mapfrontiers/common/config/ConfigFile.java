package games.alejandrocoria.mapfrontiers.common.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import games.alejandrocoria.mapfrontiers.MapFrontiers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ConfigFile {
    private final Path path;
    private final List<ConfigEntry<?, ?>> entries = new ArrayList<>();

    public ConfigFile(Path path) {
        this.path = path;
    }

    public <T extends ConfigEntry<?, ?>> T register(T entry) {
        entries.add(entry);
        return entry;
    }

    public boolean load() {
        boolean dirty = false;

        try (CommentedFileConfig config = TomlConfigIO.open(path)) {
            if (Files.exists(path)) {
                config.load();
            } else {
                dirty = true;
            }

            for (ConfigEntry<?, ?> entry : entries) {
                dirty |= entry.load(config);
            }
        } catch (Exception e) {
            MapFrontiers.LOGGER.error("Failed to load config file {}", path, e);
            for (ConfigEntry<?, ?> entry : entries) {
                entry.reset();
            }
            return true;
        }

        return dirty;
    }

    public void save() {
        try (CommentedFileConfig config = TomlConfigIO.open(path)) {
            config.clear();
            for (ConfigEntry<?, ?> entry : entries) {
                entry.save(config);
            }
            config.save();
        } catch (Exception e) {
            MapFrontiers.LOGGER.error("Failed to save config file {}", path, e);
        }
    }
}
