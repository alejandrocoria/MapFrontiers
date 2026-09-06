package games.alejandrocoria.mapfrontiers.server.identity;

import games.alejandrocoria.mapfrontiers.common.identity.PlayerId;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ServerPlayerIdLookupTest {
    private static final PlayerId LOCAL = new PlayerId(UUID.fromString("22527f7f-b0ec-4a63-93b8-df8b6a33399c"));
    private static final PlayerId OFFICIAL = new PlayerId(UUID.fromString("4b5d6407-cb58-4f3a-bf06-d9ef017d1fc8"));

    @Test
    void localProfileWinsWithoutRemoteLookup() {
        AtomicInteger remoteCalls = new AtomicInteger();
        ServerPlayerIdLookup lookup = new ServerPlayerIdLookup(username -> LOCAL, username -> {
            remoteCalls.incrementAndGet();
            return OFFICIAL;
        });

        assertEquals(LOCAL, lookup.findByName("Alice"));
        assertEquals(0, remoteCalls.get());
    }

    @Test
    void officialLookupIsUsedWhenProfileIsNotLocal() {
        ServerPlayerIdLookup lookup = new ServerPlayerIdLookup(username -> null,
                username -> username.equals("Alice") ? OFFICIAL : null);

        assertEquals(OFFICIAL, lookup.findByName("Alice"));
        assertNull(lookup.findByName("Unknown"));
    }

    @Test
    void invalidUsernameDoesNotReachEitherSource() {
        AtomicInteger calls = new AtomicInteger();
        ServerPlayerIdLookup lookup = new ServerPlayerIdLookup(username -> {
            calls.incrementAndGet();
            return LOCAL;
        }, username -> {
            calls.incrementAndGet();
            return OFFICIAL;
        });

        assertNull(lookup.findByName("not a player"));
        assertEquals(0, calls.get());
    }

    @Test
    void sourceFailureLeavesNameUnresolved() {
        ServerPlayerIdLookup lookup = new ServerPlayerIdLookup(username -> null, username -> {
            throw new IllegalStateException("service unavailable");
        });

        assertNull(lookup.findByName("Alice"));
    }
}
