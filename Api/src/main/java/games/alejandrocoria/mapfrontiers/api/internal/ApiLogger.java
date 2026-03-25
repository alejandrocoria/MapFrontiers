package games.alejandrocoria.mapfrontiers.api.internal;

/**
 * Internal logging abstraction used by the API to avoid depending on a specific logging backend.
 */
public interface ApiLogger {
    void info(String message);

    void warn(String message);

    void error(String message);

    void error(String message, Throwable throwable);
}
