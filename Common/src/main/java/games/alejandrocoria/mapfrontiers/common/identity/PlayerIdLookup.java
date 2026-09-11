package games.alejandrocoria.mapfrontiers.common.identity;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@FunctionalInterface
@ParametersAreNonnullByDefault
public interface PlayerIdLookup {
    @Nullable
    PlayerId findByName(String username);
}
