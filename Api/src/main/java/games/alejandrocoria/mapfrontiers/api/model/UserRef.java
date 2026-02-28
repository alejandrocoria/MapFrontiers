package games.alejandrocoria.mapfrontiers.api.model;

import java.util.UUID;

public record UserRef(UUID id, String name) {
    public UserRef {
        if (id == null) {
            throw new IllegalArgumentException("UserRef id cannot be null");
        }
    }
}
