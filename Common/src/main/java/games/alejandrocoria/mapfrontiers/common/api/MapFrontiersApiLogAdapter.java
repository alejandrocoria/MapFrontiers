package games.alejandrocoria.mapfrontiers.common.api;

import games.alejandrocoria.mapfrontiers.api.internal.ApiLogger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class MapFrontiersApiLogAdapter implements ApiLogger {
    private static final Logger LOGGER = LogManager.getLogger("MapFrontiers-API");

    @Override
    public void info(String message) {
        LOGGER.info(message);
    }

    @Override
    public void warn(String message) {
        LOGGER.warn(message);
    }

    @Override
    public void error(String message) {
        LOGGER.error(message);
    }

    @Override
    public void error(String message, Throwable throwable) {
        LOGGER.error(message, throwable);
    }
}
