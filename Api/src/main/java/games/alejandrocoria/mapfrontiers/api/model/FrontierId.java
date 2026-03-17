package games.alejandrocoria.mapfrontiers.api.model;

import java.util.UUID;

public record FrontierId(UUID value) {
    public FrontierId {
        if (value == null) {
            throw new IllegalArgumentException("FrontierId cannot be null");
        }
    }
}
