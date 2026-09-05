package games.alejandrocoria.mapfrontiers.common.identity;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;
import java.util.UUID;

@ParametersAreNonnullByDefault
public record PlayerId(UUID uuid) {
    public PlayerId {
        Objects.requireNonNull(uuid, "uuid");
    }
}
