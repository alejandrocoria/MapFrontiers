package games.alejandrocoria.mapfrontiers.common.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import games.alejandrocoria.mapfrontiers.MapFrontiers;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class ConfigFile {
    private static final String VERSION_KEY = "configVersion";
    private static final DateTimeFormatter BACKUP_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd--HH-mm-ss");

    private final Path path;
    @Nullable
    private final ConfigMigrations migrations;
    private final List<ConfigEntry<?, ?>> entries = new ArrayList<>();

    public ConfigFile(Path path) {
        this(path, null);
    }

    public ConfigFile(Path path, @Nullable ConfigMigrations migrations) {
        this.path = path;
        this.migrations = migrations;
    }

    public <T extends ConfigEntry<?, ?>> T register(T entry) {
        entries.add(entry);
        return entry;
    }

    public boolean load() {
        if (migrations == null) {
            return loadWithoutMigrations();
        }

        return loadWithMigrations();
    }

    private boolean loadWithoutMigrations() {
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

    private boolean loadWithMigrations() {
        try {
            if (!Files.exists(path)) {
                return loadDefaults(true);
            }

            ConfigVersionStatus versionStatus;
            try {
                versionStatus = inspectVersion();
            } catch (Exception e) {
                MapFrontiers.LOGGER.warn("Config file {} is malformed or unreadable. Resetting to defaults.", path, e);
                createBackup(BackupMode.COPY);
                return loadDefaults(true);
            }

            BackupMode backupMode = backupMode(versionStatus);
            boolean dirty = isDirtyRecovery(versionStatus);
            boolean backupCreated = applyRecoveryPlan(versionStatus, backupMode);

            if (shouldLoadDefaults(versionStatus)) {
                return loadDefaults(dirty);
            }

            ConfigMigrationException migrationFailure = null;
            try (CommentedFileConfig config = TomlConfigIO.open(path)) {
                config.load();
                try {
                    dirty |= applyMigrations(config, versionStatus);
                } catch (ConfigMigrationException e) {
                    migrationFailure = e;
                }

                if (migrationFailure == null) {
                    return loadEntries(config, dirty);
                }
            }

            MapFrontiers.LOGGER.warn("Failed to migrate config file {} from version {}. Resetting to defaults.",
                    path, versionStatus.effectiveVersion(), migrationFailure);

            if (!backupCreated) {
                createBackup(BackupMode.COPY);
            }

            return loadDefaults(true);
        } catch (Exception e) {
            MapFrontiers.LOGGER.error("Failed to load config file {}", path, e);
            resetEntries();
            return true;
        }
    }

    public void save() {
        try (CommentedFileConfig config = TomlConfigIO.open(path)) {
            config.clear();
            if (migrations != null) {
                migrations.writeCurrentVersion(config);
            }
            for (ConfigEntry<?, ?> entry : entries) {
                entry.save(config);
            }
            config.save();
        } catch (Exception e) {
            MapFrontiers.LOGGER.error("Failed to save config file {}", path, e);
        }
    }

    private ConfigVersionStatus inspectVersion() throws Exception {
        try (CommentedFileConfig config = TomlConfigIO.open(path)) {
            config.load();
            return inspectVersion(config);
        }
    }

    private ConfigVersionStatus inspectVersion(CommentedConfig config) {
        Object rawValue = config.get(VERSION_KEY);
        if (rawValue == null) {
            return new ConfigVersionStatus(ConfigVersionState.MISSING, 0, null);
        }

        Integer parsedVersion = parseVersion(rawValue);
        if (parsedVersion == null) {
            return new ConfigVersionStatus(ConfigVersionState.INVALID, -1, rawValue);
        }

        if (parsedVersion > migrations.currentVersion()) {
            return new ConfigVersionStatus(ConfigVersionState.FUTURE, parsedVersion, rawValue);
        }

        if (parsedVersion == migrations.currentVersion()) {
            return new ConfigVersionStatus(ConfigVersionState.CURRENT, parsedVersion, rawValue);
        }

        return new ConfigVersionStatus(ConfigVersionState.OUTDATED, parsedVersion, rawValue);
    }

    @Nullable
    private Integer parseVersion(Object rawValue) {
        if (!(rawValue instanceof Byte || rawValue instanceof Short || rawValue instanceof Integer || rawValue instanceof Long)) {
            return null;
        }

        long version = ((Number) rawValue).longValue();
        if (version < 0 || version > Integer.MAX_VALUE) {
            return null;
        }

        return (int) version;
    }

    private BackupMode backupMode(ConfigVersionStatus versionStatus) {
        return switch (versionStatus.state()) {
            case MISSING, OUTDATED, CURRENT -> BackupMode.NONE;
            case INVALID -> BackupMode.COPY;
            case FUTURE -> BackupMode.MOVE;
        };
    }

    private boolean shouldLoadDefaults(ConfigVersionStatus versionStatus) {
        return versionStatus.state() == ConfigVersionState.FUTURE;
    }

    private boolean isDirtyRecovery(ConfigVersionStatus versionStatus) {
        return versionStatus.state() != ConfigVersionState.CURRENT;
    }

    private boolean applyRecoveryPlan(ConfigVersionStatus versionStatus, BackupMode backupMode) {
        if (versionStatus.state() == ConfigVersionState.INVALID) {
            MapFrontiers.LOGGER.warn("Config file {} has invalid configVersion {}. Attempting best-effort recovery.",
                    path, versionStatus.rawValue());
        } else if (versionStatus.state() == ConfigVersionState.FUTURE) {
            MapFrontiers.LOGGER.warn("Config file {} uses unsupported future configVersion {}. Latest supported version is {}. Resetting to defaults.",
                    path, versionStatus.effectiveVersion(), migrations.currentVersion());
        }

        return createBackup(backupMode);
    }

    private boolean applyMigrations(CommentedConfig config, ConfigVersionStatus versionStatus) throws ConfigMigrationException {
        if (versionStatus.state() != ConfigVersionState.MISSING && versionStatus.state() != ConfigVersionState.OUTDATED) {
            return false;
        }

        int version = versionStatus.effectiveVersion();
        if (versionStatus.state() == ConfigVersionState.MISSING) {
            MapFrontiers.LOGGER.warn("Config file {} has no configVersion. Treating it as legacy version 0 and applying migrations up to {}.",
                    path, migrations.currentVersion());
        }

        while (version < migrations.currentVersion()) {
            ConfigMigrationStep step = migrations.step(version);
            if (step == null) {
                throw new ConfigMigrationException("Missing migration step from version " + version + " to " + (version + 1));
            }

            try {
                step.apply(config);
            } catch (ConfigMigrationException e) {
                throw e;
            } catch (RuntimeException e) {
                throw new ConfigMigrationException("Migration step from version " + version + " to " + (version + 1) + " failed", e);
            }
            version++;
        }

        return true;
    }

    private boolean createBackup(BackupMode mode) {
        if (mode == BackupMode.NONE) {
            return false;
        }

        Path backupPath;
        try {
            backupPath = nextBackupPath();
        } catch (IOException e) {
            MapFrontiers.LOGGER.warn("Failed to resolve backup path for config file {}", path, e);
            return false;
        }

        String action = mode == BackupMode.COPY ? "Copying" : "Moving";
        MapFrontiers.LOGGER.warn("{} config file {} to backup {}", action, path, backupPath);

        try {
            if (mode == BackupMode.COPY) {
                Files.copy(path, backupPath, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.move(path, backupPath, StandardCopyOption.REPLACE_EXISTING);
            }

            return true;
        } catch (IOException e) {
            MapFrontiers.LOGGER.warn("Failed to create backup for config file {} at {}", path, backupPath, e);
            return false;
        }
    }

    private Path nextBackupPath() throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        } else {
            parent = path.toAbsolutePath().getParent();
        }

        String fileName = path.getFileName().toString();
        String timestamp = BACKUP_TIMESTAMP_FORMAT.format(LocalDateTime.now());
        Path backupPath = parent.resolve(fileName + "." + timestamp + ".bak");
        int suffix = 2;
        while (Files.exists(backupPath)) {
            backupPath = parent.resolve(fileName + "." + timestamp + "-" + suffix + ".bak");
            suffix++;
        }

        return backupPath;
    }

    private boolean loadDefaults(boolean dirty) throws Exception {
        try (CommentedFileConfig config = TomlConfigIO.open(path)) {
            config.clear();
            return loadEntries(config, dirty);
        }
    }

    private boolean loadEntries(CommentedConfig config, boolean dirty) {
        boolean currentDirty = dirty;
        for (ConfigEntry<?, ?> entry : entries) {
            currentDirty |= entry.load(config);
        }
        return currentDirty;
    }

    private void resetEntries() {
        for (ConfigEntry<?, ?> entry : entries) {
            entry.reset();
        }
    }

    private enum ConfigVersionState {
        MISSING,
        CURRENT,
        OUTDATED,
        INVALID,
        FUTURE
    }

    private enum BackupMode {
        NONE,
        COPY,
        MOVE
    }

    private record ConfigVersionStatus(ConfigVersionState state, int effectiveVersion, @Nullable Object rawValue) {
    }
}
