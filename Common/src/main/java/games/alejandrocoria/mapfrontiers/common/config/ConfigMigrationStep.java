package games.alejandrocoria.mapfrontiers.common.config;

import com.electronwill.nightconfig.core.CommentedConfig;

@FunctionalInterface
public interface ConfigMigrationStep {
    void apply(CommentedConfig config) throws ConfigMigrationException;
}
