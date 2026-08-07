package games.alejandrocoria.mapfrontiers.client.territory.overlay;

import journeymap.api.v2.client.display.MarkerOverlay;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReconciledOverlayListTest {
    private static final String LAYER = "markers";

    @Test
    void reconcile_growth_createsOnlyAdditionalSlots() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        ReconciledOverlayList<MarkerOverlaySlot> slots = markerSlots(publisher);
        reconcile(slots, 2, true, new OverlayRefreshResult());
        MarkerOverlay first = slots.get(0).getOverlay();
        MarkerOverlay second = slots.get(1).getOverlay();
        publisher.clearOperations();

        reconcile(slots, 4, true, new OverlayRefreshResult());

        assertEquals(4, slots.size());
        assertSame(first, slots.get(0).getOverlay());
        assertSame(second, slots.get(1).getOverlay());
        assertEquals(2, publisher.operations().size());
    }

    @Test
    void reconcile_shrink_removesOnlyUnvisitedTail() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        ReconciledOverlayList<MarkerOverlaySlot> slots = markerSlots(publisher);
        reconcile(slots, 4, true, new OverlayRefreshResult());
        MarkerOverlay first = slots.get(0).getOverlay();
        MarkerOverlay second = slots.get(1).getOverlay();
        MarkerOverlay third = slots.get(2).getOverlay();
        MarkerOverlay fourth = slots.get(3).getOverlay();
        publisher.clearOperations();

        reconcile(slots, 2, true, new OverlayRefreshResult());

        assertEquals(2, slots.size());
        assertSame(first, slots.get(0).getOverlay());
        assertSame(second, slots.get(1).getOverlay());
        assertEquals(2, publisher.operations().size());
        assertSame(third, publisher.operations().get(0).overlay());
        assertSame(fourth, publisher.operations().get(1).overlay());
    }

    @Test
    void reconcile_showFailure_continuesWithLaterSlots() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        publisher.failNextShow();
        ReconciledOverlayList<MarkerOverlaySlot> slots = markerSlots(publisher);
        OverlayRefreshResult result = new OverlayRefreshResult();

        reconcile(slots, 2, true, result);

        assertEquals(PublicationState.UNPUBLISHED, slots.get(0).getPublicationState());
        assertEquals(PublicationState.PUBLISHED, slots.get(1).getPublicationState());
        assertTrue(result.isRetryNeeded());
        assertEquals(3, publisher.operations().size());

        publisher.clearOperations();
        reconcile(slots, 2, true, new OverlayRefreshResult());
        assertEquals(PublicationState.PUBLISHED, slots.get(0).getPublicationState());
        assertEquals(1, publisher.operations().size());
        assertSame(slots.get(0).getOverlay(), publisher.operations().getFirst().overlay());
    }

    @Test
    void clear_removeFailure_attemptsEveryPublishedSlotAndDropsOwnership() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        ReconciledOverlayList<MarkerOverlaySlot> slots = markerSlots(publisher);
        reconcile(slots, 3, true, new OverlayRefreshResult());
        publisher.clearOperations();
        publisher.failNextRemove();
        OverlayRefreshResult result = new OverlayRefreshResult();

        slots.clear(result, LAYER);

        assertEquals(0, slots.size());
        assertEquals(3, publisher.operations().size());
        assertEquals(1, result.getFailureCount(OverlayRefreshResult.Operation.REMOVE, LAYER));
        assertTrue(result.hasFailures());
        assertFalse(result.isRetryNeeded());
    }

    @Test
    void lifecycle_invalidBeginAcquireFinishAndClear_failFast() {
        ReconciledOverlayList<MarkerOverlaySlot> slots = markerSlots(new FakeOverlayPublisher());

        assertThrows(IllegalStateException.class, slots::acquire);
        assertThrows(IllegalStateException.class,
                () -> slots.finishReconcile(new OverlayRefreshResult(), LAYER));

        slots.beginReconcile();
        assertThrows(IllegalStateException.class, slots::beginReconcile);
        assertThrows(IllegalStateException.class,
                () -> slots.clear(new OverlayRefreshResult(), LAYER));
        slots.finishReconcile(new OverlayRefreshResult(), LAYER);

        assertThrows(IllegalStateException.class, slots::acquire);
    }

    private static ReconciledOverlayList<MarkerOverlaySlot> markerSlots(FakeOverlayPublisher publisher) {
        return new ReconciledOverlayList<>(() -> new MarkerOverlaySlot("mapfrontiers", publisher));
    }

    private static void reconcile(ReconciledOverlayList<MarkerOverlaySlot> slots, int count, boolean visible,
                                  OverlayRefreshResult result) {
        slots.beginReconcile();
        for (int index = 0; index < count; index++) {
            slots.acquire().reconcile(OverlayTestStates.marker(index, "marker-" + index), visible, result, LAYER);
        }
        slots.finishReconcile(result, LAYER);
    }
}
