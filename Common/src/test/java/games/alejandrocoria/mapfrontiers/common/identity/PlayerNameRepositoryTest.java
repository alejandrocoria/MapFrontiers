package games.alejandrocoria.mapfrontiers.common.identity;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerNameRepositoryTest {
    private static final PlayerId PLAYER_ID = new PlayerId(
            UUID.fromString("22527f7f-b0ec-4a63-93b8-df8b6a33399c"));

    @Test
    void unknownPlayerFallsBackToUuid() {
        PlayerNameRepository repository = new PlayerNameRepository(message -> {
        });

        assertNull(repository.resolveName(PLAYER_ID));
        assertEquals(PLAYER_ID.uuid().toString(), repository.resolveNameOrUuid(PLAYER_ID));
    }

    @Test
    void firstObservationStoresNormalizedNameAndEmitsEvent() {
        PlayerNameRepository repository = new PlayerNameRepository(message -> {
        });
        List<PlayerId> changed = new ArrayList<>();
        repository.getEvents().subscribeChanged(this, changed::add);

        assertTrue(repository.observe(PLAYER_ID, "  Alice  ", PlayerNameSource.HINT));
        assertEquals("Alice", repository.resolveName(PLAYER_ID));
        assertEquals(List.of(PLAYER_ID), changed);
    }

    @Test
    void identicalHigherPriorityObservationPromotesWithoutEvent() {
        List<String> warnings = new ArrayList<>();
        PlayerNameRepository repository = new PlayerNameRepository(warnings::add);
        List<PlayerId> changed = new ArrayList<>();
        repository.getEvents().subscribeChanged(this, changed::add);

        assertTrue(repository.observe(PLAYER_ID, "Alice", PlayerNameSource.HINT));
        assertFalse(repository.observe(PLAYER_ID, "Alice", PlayerNameSource.MINECRAFT_CACHE));
        assertFalse(repository.observe(PLAYER_ID, "Bob", PlayerNameSource.HINT));

        assertEquals("Alice", repository.resolveName(PLAYER_ID));
        assertEquals(List.of(PLAYER_ID), changed);
        assertTrue(warnings.isEmpty());
    }

    @Test
    void higherPriorityReplacesAndLowerPriorityDoesNot() {
        PlayerNameRepository repository = new PlayerNameRepository(message -> {
        });
        List<PlayerId> changed = new ArrayList<>();
        repository.getEvents().subscribeChanged(this, changed::add);

        assertTrue(repository.observe(PLAYER_ID, "OldName", PlayerNameSource.HINT));
        assertTrue(repository.observe(PLAYER_ID, "CurrentName", PlayerNameSource.CONNECTED_PROFILE));
        assertFalse(repository.observe(PLAYER_ID, "CachedName", PlayerNameSource.MINECRAFT_CACHE));

        assertEquals("CurrentName", repository.resolveName(PLAYER_ID));
        assertEquals(List.of(PLAYER_ID, PLAYER_ID), changed);
    }

    @Test
    void latestServerSyncObservationRepresentsRename() {
        PlayerNameRepository repository = new PlayerNameRepository(message -> {
        });

        assertTrue(repository.observe(PLAYER_ID, "OldName", PlayerNameSource.SERVER_SYNC));
        assertTrue(repository.observe(PLAYER_ID, "NewName", PlayerNameSource.SERVER_SYNC));
        assertEquals("NewName", repository.resolveName(PLAYER_ID));
    }

    @Test
    void latestMinecraftCacheObservationRepresentsRename() {
        PlayerNameRepository repository = new PlayerNameRepository(message -> {
        });

        assertTrue(repository.observe(PLAYER_ID, "OldName", PlayerNameSource.MINECRAFT_CACHE));
        assertTrue(repository.observe(PLAYER_ID, "NewName", PlayerNameSource.MINECRAFT_CACHE));
        assertEquals("NewName", repository.resolveName(PLAYER_ID));
    }

    @Test
    void cacheAndConnectedProfileUpdatePersistedHintOnlyWhenTheNameChanges() {
        PlayerNameRepository repository = new PlayerNameRepository(message -> {
        });

        assertTrue(repository.observe(PLAYER_ID, "OldName", PlayerNameSource.HINT));
        assertTrue(repository.observe(PLAYER_ID, "CurrentName", PlayerNameSource.MINECRAFT_CACHE));
        assertFalse(repository.observe(PLAYER_ID, "CurrentName", PlayerNameSource.CONNECTED_PROFILE));
        assertTrue(repository.observe(PLAYER_ID, "RenamedName", PlayerNameSource.CONNECTED_PROFILE));

        assertEquals("RenamedName", repository.resolveName(PLAYER_ID));
    }

    @Test
    void firstHintWinsAndOnlyFirstConflictWarns() {
        List<String> warnings = new ArrayList<>();
        PlayerNameRepository repository = new PlayerNameRepository(warnings::add);

        assertTrue(repository.observe(PLAYER_ID, "Alice", PlayerNameSource.HINT));
        assertFalse(repository.observe(PLAYER_ID, "Bob", PlayerNameSource.HINT));
        assertFalse(repository.observe(PLAYER_ID, "Carol", PlayerNameSource.HINT));

        assertEquals("Alice", repository.resolveName(PLAYER_ID));
        assertEquals(List.of("Conflicting username hints for UUID " + PLAYER_ID.uuid()
                + ". Keeping \"Alice\" and ignoring \"Bob\"."), warnings);
    }

    @Test
    void contradictoryHintAgainstHigherPriorityEntryDoesNotWarn() {
        List<String> warnings = new ArrayList<>();
        PlayerNameRepository repository = new PlayerNameRepository(warnings::add);

        assertTrue(repository.observe(PLAYER_ID, "Alice", PlayerNameSource.MINECRAFT_CACHE));
        assertFalse(repository.observe(PLAYER_ID, "Bob", PlayerNameSource.HINT));

        assertEquals("Alice", repository.resolveName(PLAYER_ID));
        assertTrue(warnings.isEmpty());
    }

    @Test
    void nullAndBlankNamesAreIgnored() {
        PlayerNameRepository repository = new PlayerNameRepository(message -> {
        });

        assertFalse(repository.observe(PLAYER_ID, null, PlayerNameSource.HINT));
        assertFalse(repository.observe(PLAYER_ID, "", PlayerNameSource.HINT));
        assertFalse(repository.observe(PLAYER_ID, "   ", PlayerNameSource.HINT));
        assertNull(repository.resolveName(PLAYER_ID));
    }

    @Test
    void unsubscribeAndCloseClearListenersAndState() {
        PlayerNameRepository repository = new PlayerNameRepository(message -> {
        });
        List<PlayerId> changed = new ArrayList<>();
        Object owner = new Object();
        repository.getEvents().subscribeChanged(owner, changed::add);

        repository.getEvents().unsubscribe(owner);
        repository.observe(PLAYER_ID, "Alice", PlayerNameSource.HINT);
        assertTrue(changed.isEmpty());

        repository.getEvents().subscribeChanged(owner, changed::add);
        repository.close();
        assertNull(repository.resolveName(PLAYER_ID));

        repository.observe(PLAYER_ID, "Bob", PlayerNameSource.HINT);
        assertTrue(changed.isEmpty());
    }
}
