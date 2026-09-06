package games.alejandrocoria.mapfrontiers.common.identity.nbt;

import games.alejandrocoria.mapfrontiers.common.identity.PlayerIdLookup;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerNameRepository;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;

@ParametersAreNonnullByDefault
public record PlayerReferenceNbtReadContext(PlayerNameRepository playerNames,
                                            @Nullable PlayerIdLookup nameOnlyLookup) {
    public PlayerReferenceNbtReadContext {
        Objects.requireNonNull(playerNames, "playerNames");
    }

    public static PlayerReferenceNbtReadContext uuidOnly(PlayerNameRepository playerNames) {
        return new PlayerReferenceNbtReadContext(playerNames, null);
    }
}
