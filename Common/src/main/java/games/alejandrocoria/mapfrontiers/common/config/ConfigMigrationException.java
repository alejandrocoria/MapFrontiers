package games.alejandrocoria.mapfrontiers.common.config;

public final class ConfigMigrationException extends Exception {
    public ConfigMigrationException(String message) {
        super(message);
    }

    public ConfigMigrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
