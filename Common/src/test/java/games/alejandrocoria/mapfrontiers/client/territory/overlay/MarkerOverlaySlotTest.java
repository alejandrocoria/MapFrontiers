package games.alejandrocoria.mapfrontiers.client.territory.overlay;

import journeymap.api.v2.client.display.MarkerOverlay;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkerOverlaySlotTest {
    private static final String LAYER = "markers";

    @Test
    void reconcile_newVisibleSlot_createsAndPublishes() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        MarkerOverlaySlot slot = new MarkerOverlaySlot("mapfrontiers", publisher);

        slot.reconcile(OverlayTestStates.marker(1, "first"), true, new OverlayRefreshResult(), LAYER);

        MarkerOverlay overlay = slot.getOverlay();
        assertNotNull(overlay);
        assertSame(overlay, publisher.operations().getFirst().overlay());
        assertEquals(FakeOverlayPublisher.OperationType.SHOW, publisher.operations().getFirst().type());
        assertEquals(PublicationState.PUBLISHED, slot.getPublicationState());
    }

    @Test
    void reconcile_identicalState_preservesIdentityWithoutPublishing() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        MarkerOverlaySlot slot = new MarkerOverlaySlot("mapfrontiers", publisher);
        slot.reconcile(OverlayTestStates.marker(1, "first"), true, new OverlayRefreshResult(), LAYER);
        MarkerOverlay firstOverlay = slot.getOverlay();
        publisher.clearOperations();

        slot.reconcile(OverlayTestStates.marker(1, "first"), true, new OverlayRefreshResult(), LAYER);

        assertSame(firstOverlay, slot.getOverlay());
        assertTrue(publisher.operations().isEmpty());
    }

    @Test
    void reconcile_changedState_reappliesAndPublishesSameIdentity() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        MarkerOverlaySlot slot = new MarkerOverlaySlot("mapfrontiers", publisher);
        slot.reconcile(OverlayTestStates.marker(1, "first"), true, new OverlayRefreshResult(), LAYER);
        MarkerOverlay firstOverlay = slot.getOverlay();
        publisher.clearOperations();

        slot.reconcile(OverlayTestStates.marker(7, "second"), true, new OverlayRefreshResult(), LAYER);

        assertSame(firstOverlay, slot.getOverlay());
        assertEquals(7, firstOverlay.getPoint().getX());
        assertEquals("marker-7", firstOverlay.getLabel());
        assertEquals(1, publisher.operations().size());
        assertSame(firstOverlay, publisher.operations().getFirst().overlay());
    }

    @Test
    void reconcile_stateClearsOptionalValues_resetsPreviousPresentation() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        MarkerOverlaySlot slot = new MarkerOverlaySlot("mapfrontiers", publisher);
        slot.reconcile(OverlayTestStates.marker(1, "first"), true, new OverlayRefreshResult(), LAYER);

        slot.reconcile(OverlayTestStates.bareMarker(2, "bare"), true, new OverlayRefreshResult(), LAYER);

        MarkerOverlay overlay = slot.getOverlay();
        assertEquals(2, overlay.getMinZoom());
        assertEquals(16384, overlay.getMaxZoom());
        assertNull(overlay.getOverlayGroupName());
        assertNull(overlay.getTitle());
        assertNull(overlay.getLabel());
        assertNull(overlay.getTextProperties());

        slot.reconcile(OverlayTestStates.bareMarker(3, "bare"), true, new OverlayRefreshResult(), LAYER);
        assertEquals(3, overlay.getPoint().getX());
        assertNull(overlay.getTextProperties());
    }

    @Test
    void reconcile_hideThenShow_preservesSlotAndRepublishes() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        MarkerOverlaySlot slot = new MarkerOverlaySlot("mapfrontiers", publisher);
        MarkerOverlayState state = OverlayTestStates.marker(1, "first");
        slot.reconcile(state, true, new OverlayRefreshResult(), LAYER);
        MarkerOverlay overlay = slot.getOverlay();
        publisher.clearOperations();

        slot.reconcile(state, false, new OverlayRefreshResult(), LAYER);
        slot.reconcile(state, true, new OverlayRefreshResult(), LAYER);

        assertSame(overlay, slot.getOverlay());
        assertEquals(2, publisher.operations().size());
        assertEquals(FakeOverlayPublisher.OperationType.REMOVE, publisher.operations().get(0).type());
        assertEquals(FakeOverlayPublisher.OperationType.SHOW, publisher.operations().get(1).type());
    }

    @Test
    void reconcile_changedWhileHidden_appliesLocallyAndShowsOnlyFinalState() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        MarkerOverlaySlot slot = new MarkerOverlaySlot("mapfrontiers", publisher);
        slot.reconcile(OverlayTestStates.marker(1, "first"), true, new OverlayRefreshResult(), LAYER);
        slot.reconcile(OverlayTestStates.marker(2, "second"), false, new OverlayRefreshResult(), LAYER);
        publisher.clearOperations();

        slot.reconcile(OverlayTestStates.marker(4, "final"), false, new OverlayRefreshResult(), LAYER);
        assertEquals(4, slot.getOverlay().getPoint().getX());
        assertTrue(publisher.operations().isEmpty());

        slot.reconcile(OverlayTestStates.marker(4, "final"), true, new OverlayRefreshResult(), LAYER);
        assertEquals(1, publisher.operations().size());
        assertEquals(FakeOverlayPublisher.OperationType.SHOW, publisher.operations().getFirst().type());
    }

    @Test
    void reconcile_unavailablePublisher_updatesWithoutPublishing() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        publisher.setAvailable(false);
        MarkerOverlaySlot slot = new MarkerOverlaySlot("mapfrontiers", publisher);

        slot.reconcile(OverlayTestStates.marker(3, "preview"), true, new OverlayRefreshResult(), LAYER);

        assertEquals(3, slot.getOverlay().getPoint().getX());
        assertEquals(PublicationState.UNPUBLISHED, slot.getPublicationState());
        assertTrue(publisher.operations().isEmpty());
    }

    @Test
    void reconcile_showAndCleanupFail_marksUnknownAndConvergesLater() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        publisher.failNextShow();
        publisher.failNextRemove();
        MarkerOverlaySlot slot = new MarkerOverlaySlot("mapfrontiers", publisher);
        OverlayRefreshResult failedResult = new OverlayRefreshResult();

        slot.reconcile(OverlayTestStates.marker(1, "first"), true, failedResult, LAYER);

        assertEquals(PublicationState.UNKNOWN, slot.getPublicationState());
        assertTrue(failedResult.isRetryNeeded());
        assertEquals(1, failedResult.getFailureCount(OverlayRefreshResult.Operation.SHOW, LAYER));
        assertEquals(1, failedResult.getFailureCount(OverlayRefreshResult.Operation.REMOVE, LAYER));

        publisher.clearOperations();
        slot.reconcile(OverlayTestStates.marker(1, "first"), true, new OverlayRefreshResult(), LAYER);
        assertEquals(PublicationState.PUBLISHED, slot.getPublicationState());
        assertEquals(1, publisher.operations().size());
    }

    @Test
    void reconcile_removeFails_retriesRetainedHide() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        MarkerOverlaySlot slot = new MarkerOverlaySlot("mapfrontiers", publisher);
        MarkerOverlayState state = OverlayTestStates.marker(1, "first");
        slot.reconcile(state, true, new OverlayRefreshResult(), LAYER);
        publisher.clearOperations();
        publisher.failNextRemove();
        OverlayRefreshResult failedResult = new OverlayRefreshResult();

        slot.reconcile(state, false, failedResult, LAYER);
        assertEquals(PublicationState.UNKNOWN, slot.getPublicationState());
        assertTrue(failedResult.isRetryNeeded());

        slot.reconcile(state, false, new OverlayRefreshResult(), LAYER);
        assertEquals(PublicationState.UNPUBLISHED, slot.getPublicationState());
        assertEquals(2, publisher.operations().size());
    }
}
