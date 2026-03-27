package games.alejandrocoria.mapfrontiers.common.config;

import com.electronwill.nightconfig.core.CommentedConfig;

import javax.annotation.Nullable;

public interface ConfigMigrations {
    int currentVersion();

    @Nullable
    ConfigMigrationStep step(int fromVersion);

    void writeCurrentVersion(CommentedConfig config);
}
