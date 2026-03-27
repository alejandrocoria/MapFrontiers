package games.alejandrocoria.mapfrontiers.common.config;

import javax.annotation.Nullable;

public interface ConfigMigrations {
    @Nullable
    ConfigMigrationStep step(int fromVersion);
}
