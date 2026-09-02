package games.alejandrocoria.mapfrontiers.client.territory;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PendingOptimisticFrontierUpdatesTest {
    private static final UUID FRONTIER_ID = new UUID(1L, 2L);

    @Test
    void acknowledgesUpdatesInSubmissionOrder() {
        PendingOptimisticFrontierUpdates updates = new PendingOptimisticFrontierUpdates();
        updates.expect(FRONTIER_ID, 10L);
        updates.expect(FRONTIER_ID, 20L);

        assertTrue(updates.acknowledge(FRONTIER_ID, 10L));
        assertFalse(updates.acknowledge(FRONTIER_ID, 10L));
        assertTrue(updates.acknowledge(FRONTIER_ID, 20L));
        assertFalse(updates.acknowledge(FRONTIER_ID, 20L));
    }

    @Test
    void mismatchedAcknowledgementDoesNotConsumePendingUpdate() {
        PendingOptimisticFrontierUpdates updates = new PendingOptimisticFrontierUpdates();
        updates.expect(FRONTIER_ID, 10L);

        assertFalse(updates.acknowledge(FRONTIER_ID, 20L));
        assertTrue(updates.acknowledge(FRONTIER_ID, 10L));
    }

    @Test
    void clearDiscardsAllPendingUpdatesForFrontier() {
        PendingOptimisticFrontierUpdates updates = new PendingOptimisticFrontierUpdates();
        updates.expect(FRONTIER_ID, 10L);
        updates.expect(FRONTIER_ID, 20L);

        updates.clear(FRONTIER_ID);

        assertFalse(updates.acknowledge(FRONTIER_ID, 10L));
        assertFalse(updates.acknowledge(FRONTIER_ID, 20L));
    }
}
