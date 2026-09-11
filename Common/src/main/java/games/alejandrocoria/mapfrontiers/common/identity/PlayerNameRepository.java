package games.alejandrocoria.mapfrontiers.common.identity;

import games.alejandrocoria.mapfrontiers.MapFrontiers;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
public final class PlayerNameRepository implements PlayerNameResolver, AutoCloseable {
    private record Entry(String username, PlayerNameSource source) {
    }

    private final Map<PlayerId, Entry> entries = new HashMap<>();
    private final Set<PlayerId> warnedHintConflicts = new HashSet<>();
    private final PlayerNameEvents events = new PlayerNameEvents();
    private final Consumer<String> warningSink;

    public PlayerNameRepository() {
        this(MapFrontiers.LOGGER::warn);
    }

    PlayerNameRepository(Consumer<String> warningSink) {
        this.warningSink = Objects.requireNonNull(warningSink, "warningSink");
    }

    public boolean observe(PlayerId playerId, @Nullable String username, PlayerNameSource source) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(source, "source");

        if (username == null) {
            return false;
        }

        String candidate = username.trim();
        if (candidate.isEmpty()) {
            return false;
        }

        Entry current = entries.get(playerId);
        if (current == null) {
            return replace(playerId, candidate, source);
        }

        if (current.username().equals(candidate)) {
            if (source.priority() > current.source().priority()) {
                entries.put(playerId, new Entry(candidate, source));
            }
            return false;
        }

        if (source.priority() > current.source().priority()) {
            return replace(playerId, candidate, source);
        }

        if (source.priority() < current.source().priority()) {
            return false;
        }

        if (source == PlayerNameSource.HINT) {
            if (warnedHintConflicts.add(playerId)) {
                warningSink.accept("Conflicting username hints for UUID " + playerId.uuid()
                        + ". Keeping \"" + current.username() + "\" and ignoring \"" + candidate + "\".");
            }
            return false;
        }

        return replace(playerId, candidate, source);
    }

    public boolean isKnownOnlyFromHint(PlayerId playerId) {
        Entry entry = entries.get(Objects.requireNonNull(playerId, "playerId"));
        return entry != null && entry.source() == PlayerNameSource.HINT;
    }

    @Override
    @Nullable
    public String resolveName(PlayerId playerId) {
        Entry entry = entries.get(playerId);
        return entry == null ? null : entry.username();
    }

    public PlayerNameEvents getEvents() {
        return events;
    }

    @Override
    public void close() {
        entries.clear();
        warnedHintConflicts.clear();
        events.close();
    }

    private boolean replace(PlayerId playerId, String username, PlayerNameSource source) {
        entries.put(playerId, new Entry(username, source));
        events.postChanged(playerId);
        return true;
    }
}
