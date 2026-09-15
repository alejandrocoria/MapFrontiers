package games.alejandrocoria.mapfrontiers.client.network;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DelayedClientPacketQueueTest {
    @Test
    void zeroDelayExecutesSynchronously() {
        DelayedClientPacketQueue queue = new DelayedClientPacketQueue();
        List<Integer> applied = new ArrayList<>();

        queue.submit(() -> applied.add(1));

        assertEquals(List.of(1), applied);
        assertEquals(0, queue.getPendingCount());
    }

    @Test
    void laterEntryNeverPassesDelayedHead() {
        DelayedClientPacketQueue queue = new DelayedClientPacketQueue();
        List<Integer> applied = new ArrayList<>();
        queue.setDelayTicks(3);
        queue.submit(() -> applied.add(1));
        queue.tick();
        queue.setDelayTicks(0);
        queue.submit(() -> applied.add(2));

        assertTrue(applied.isEmpty());
        queue.tick();
        assertTrue(applied.isEmpty());
        queue.tick();

        assertEquals(List.of(1, 2), applied);
    }

    @Test
    void nextReleasesExactlyOneEntryWhileHolding() {
        DelayedClientPacketQueue queue = new DelayedClientPacketQueue();
        List<Integer> applied = new ArrayList<>();
        queue.hold();
        queue.submit(() -> applied.add(1));
        queue.submit(() -> applied.add(2));

        assertTrue(queue.runNext());

        assertEquals(List.of(1), applied);
        assertEquals(1, queue.getPendingCount());
        assertTrue(queue.isHolding());
    }

    @Test
    void resumeReleasesDueEntriesAndLeavesAutomaticDeliveryEnabled() {
        DelayedClientPacketQueue queue = new DelayedClientPacketQueue();
        List<Integer> applied = new ArrayList<>();
        queue.hold();
        queue.submit(() -> applied.add(1));

        queue.resume();

        assertEquals(List.of(1), applied);
        assertFalse(queue.isHolding());
        assertEquals(0, queue.getPendingCount());
    }

    @Test
    void changingDelayDoesNotChangeHoldState() {
        DelayedClientPacketQueue queue = new DelayedClientPacketQueue();
        queue.hold();

        queue.setDelayTicks(20);

        assertTrue(queue.isHolding());
        assertEquals(20, queue.getDelayTicks());
    }

    @Test
    void flushReleasesEntriesAddedDuringExecutionInOrder() {
        DelayedClientPacketQueue queue = new DelayedClientPacketQueue();
        List<Integer> applied = new ArrayList<>();
        queue.hold();
        queue.submit(() -> {
            applied.add(1);
            queue.submit(() -> applied.add(3));
        });
        queue.submit(() -> applied.add(2));

        assertEquals(3, queue.flush());

        assertEquals(List.of(1, 2, 3), applied);
        assertEquals(0, queue.getPendingCount());
        assertTrue(queue.isHolding());
    }

    @Test
    void clearDiscardsEntriesWithoutExecutingThem() {
        DelayedClientPacketQueue queue = new DelayedClientPacketQueue();
        List<Integer> applied = new ArrayList<>();
        queue.hold();
        queue.submit(() -> applied.add(1));

        assertEquals(1, queue.clear());

        assertTrue(applied.isEmpty());
        assertEquals(0, queue.getPendingCount());
    }

    @Test
    void discardingPendingPacketsRemovesQueuedEntries() {
        DelayedClientPacketQueue queue = new DelayedClientPacketQueue();
        List<Integer> applied = new ArrayList<>();
        queue.hold();
        queue.submit(() -> applied.add(1));

        queue.discardPendingPackets();

        assertEquals(0, queue.getPendingCount());
        assertFalse(queue.runNext());
        assertTrue(applied.isEmpty());
    }

    @Test
    void failingActionIsRemovedAndExceptionPropagates() {
        DelayedClientPacketQueue queue = new DelayedClientPacketQueue();
        queue.hold();
        queue.submit(() -> {
            throw new IllegalStateException("failure");
        });

        assertThrows(IllegalStateException.class, queue::runNext);

        assertEquals(0, queue.getPendingCount());
    }
}
