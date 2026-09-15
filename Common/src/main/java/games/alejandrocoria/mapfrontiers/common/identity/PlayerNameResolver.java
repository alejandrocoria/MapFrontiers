package games.alejandrocoria.mapfrontiers.common.identity;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@FunctionalInterface
@ParametersAreNonnullByDefault
public interface PlayerNameResolver {
    @Nullable
    String resolveName(PlayerId playerId);

    default String resolveNameOrUuid(PlayerId playerId) {
        String name = resolveName(playerId);
        return name == null ? playerId.uuid().toString() : name;
    }
}
