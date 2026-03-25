package games.alejandrocoria.mapfrontiers.common.util;

public class InvalidNbtFormatException extends RuntimeException {
    public InvalidNbtFormatException(String message) {
        super(message);
    }

    public InvalidNbtFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
