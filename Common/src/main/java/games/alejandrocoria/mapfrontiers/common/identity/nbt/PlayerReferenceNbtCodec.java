package games.alejandrocoria.mapfrontiers.common.identity.nbt;

import games.alejandrocoria.mapfrontiers.common.identity.PlayerId;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerIdLookup;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerNameResolver;
import games.alejandrocoria.mapfrontiers.common.identity.PlayerNameSource;
import games.alejandrocoria.mapfrontiers.common.util.InvalidNbtFormatException;
import games.alejandrocoria.mapfrontiers.common.util.NbtReadHelper;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;
import java.util.UUID;

@ParametersAreNonnullByDefault
public final class PlayerReferenceNbtCodec {
    private static final String UUID_KEY = "UUID";
    private static final String USERNAME_KEY = "username";

    public record ReadResult(PlayerId playerId, boolean repaired) {
        public ReadResult {
            Objects.requireNonNull(playerId, "playerId");
        }
    }

    public static ReadResult read(CompoundTag nbt, PlayerReferenceNbtReadContext context) {
        String username = normalize(nbt.getStringOr(USERNAME_KEY, ""));
        PlayerId playerId;
        boolean repaired = false;

        if (nbt.contains(UUID_KEY)) {
            String uuidValue = NbtReadHelper.requireString(nbt, UUID_KEY);
            try {
                playerId = new PlayerId(UUID.fromString(uuidValue));
            } catch (IllegalArgumentException e) {
                throw new InvalidNbtFormatException("Invalid UUID field '" + UUID_KEY + "': '" + uuidValue + "'.", e);
            }
        } else {
            if (username == null) {
                throw new InvalidNbtFormatException("Player reference is missing both UUID and username.");
            }

            PlayerIdLookup lookup = context.nameOnlyLookup();
            if (lookup == null) {
                throw new InvalidNbtFormatException("Player reference is missing UUID and name-only resolution is unavailable.");
            }

            playerId = lookup.findByName(username);
            if (playerId == null) {
                throw new InvalidNbtFormatException("Could not resolve username '" + username + "' to a UUID.");
            }
            repaired = true;
        }

        if (username != null) {
            context.playerNames().observe(playerId, username, PlayerNameSource.HINT);
        }
        return new ReadResult(playerId, repaired);
    }

    public static void write(CompoundTag nbt, PlayerId playerId, PlayerNameResolver resolver) {
        nbt.putString(UUID_KEY, playerId.uuid().toString());

        String username = normalize(resolver.resolveName(playerId));
        if (username == null) {
            nbt.remove(USERNAME_KEY);
        } else {
            nbt.putString(USERNAME_KEY, username);
        }
    }

    @Nullable
    private static String normalize(@Nullable String username) {
        if (username == null) {
            return null;
        }
        String normalized = username.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private PlayerReferenceNbtCodec() {
    }
}
