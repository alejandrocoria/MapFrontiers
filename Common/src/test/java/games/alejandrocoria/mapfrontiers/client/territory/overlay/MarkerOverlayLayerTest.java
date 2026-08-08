package games.alejandrocoria.mapfrontiers.client.territory.overlay;

import journeymap.api.v2.client.display.MarkerOverlay;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkerOverlayLayerTest {
    @Test
    void reconcile_initiallyAbsentPosition_doesNotCreatePlaceholder() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        MarkerOverlayLayer layer = new MarkerOverlayLayer("mapfrontiers", "labels", publisher);

        reconcile(layer, Collections.singletonList(null), true, new OverlayRefreshResult());

        assertTrue(layer.getOverlays().isEmpty());
        assertTrue(publisher.operations().isEmpty());

        reconcile(layer, List.of(OverlayTestStates.marker(1, "label")), true, new OverlayRefreshResult());
        assertEquals(1, layer.getOverlays().size());
        assertEquals(1, publisher.operations().size());
        assertEquals(FakeOverlayPublisher.OperationType.SHOW, publisher.operations().getFirst().type());
    }

    @Test
    void reconcile_internalGap_preservesLogicalIdentityAndFlattenedOrder() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        MarkerOverlayLayer layer = new MarkerOverlayLayer("mapfrontiers", "labels", publisher);
        reconcile(layer, Arrays.asList(
                OverlayTestStates.marker(1, "first"),
                null,
                OverlayTestStates.marker(3, "third")), true, new OverlayRefreshResult());
        List<MarkerOverlay> original = layer.getOverlays();
        publisher.clearOperations();

        reconcile(layer, Arrays.asList(
                OverlayTestStates.marker(1, "first"),
                null,
                OverlayTestStates.marker(3, "third")), true, new OverlayRefreshResult());

        List<MarkerOverlay> current = layer.getOverlays();
        assertEquals(2, current.size());
        assertSame(original.get(0), current.get(0));
        assertSame(original.get(1), current.get(1));
        assertTrue(publisher.operations().isEmpty());
    }

    @Test
    void reconcile_presentAbsentPresent_retainsHiddenIdentity() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        MarkerOverlayLayer layer = new MarkerOverlayLayer("mapfrontiers", "labels", publisher);
        reconcile(layer, List.of(OverlayTestStates.marker(1, "label")), true, new OverlayRefreshResult());
        MarkerOverlay overlay = layer.getOverlays().getFirst();
        publisher.clearOperations();

        reconcile(layer, Collections.singletonList(null), true, new OverlayRefreshResult());
        assertTrue(layer.getOverlays().isEmpty());
        reconcile(layer, List.of(OverlayTestStates.marker(1, "label")), true, new OverlayRefreshResult());

        assertSame(overlay, layer.getOverlays().getFirst());
        assertEquals(List.of(FakeOverlayPublisher.OperationType.REMOVE, FakeOverlayPublisher.OperationType.SHOW),
                publisher.operations().stream().map(FakeOverlayPublisher.Operation::type).toList());
    }

    @Test
    void setVisible_absentRetainedSlot_neverRepublishesIt() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        MarkerOverlayLayer layer = new MarkerOverlayLayer("mapfrontiers", "labels", publisher);
        reconcile(layer, List.of(
                OverlayTestStates.marker(1, "first"),
                OverlayTestStates.marker(2, "second")), true, new OverlayRefreshResult());
        MarkerOverlay second = layer.getOverlays().get(1);
        reconcile(layer, Arrays.asList(null, OverlayTestStates.marker(2, "second")),
                true, new OverlayRefreshResult());
        publisher.clearOperations();

        layer.setVisible(false, new OverlayRefreshResult());
        layer.setVisible(true, new OverlayRefreshResult());

        assertEquals(1, layer.getOverlays().size());
        assertSame(second, layer.getOverlays().getFirst());
        assertEquals(List.of(FakeOverlayPublisher.OperationType.REMOVE, FakeOverlayPublisher.OperationType.SHOW),
                publisher.operations().stream().map(FakeOverlayPublisher.Operation::type).toList());
    }

    @Test
    void finishReconcile_shorterPlan_retiresOnlyTail() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        MarkerOverlayLayer layer = new MarkerOverlayLayer("mapfrontiers", "labels", publisher);
        reconcile(layer, List.of(
                OverlayTestStates.marker(1, "first"),
                OverlayTestStates.marker(2, "second"),
                OverlayTestStates.marker(3, "third")), true, new OverlayRefreshResult());
        MarkerOverlay first = layer.getOverlays().getFirst();
        publisher.clearOperations();

        reconcile(layer, List.of(OverlayTestStates.marker(1, "first")), true, new OverlayRefreshResult());

        assertEquals(1, layer.getOverlays().size());
        assertSame(first, layer.getOverlays().getFirst());
        assertEquals(2, publisher.operations().size());
        assertTrue(publisher.operations().stream().allMatch(operation ->
                operation.type() == FakeOverlayPublisher.OperationType.REMOVE));
    }

    @Test
    void clear_removeFailure_attemptsAllAndDoesNotRequestRetry() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        MarkerOverlayLayer layer = new MarkerOverlayLayer("mapfrontiers", "labels", publisher);
        reconcile(layer, List.of(
                OverlayTestStates.marker(1, "first"),
                OverlayTestStates.marker(2, "second")), true, new OverlayRefreshResult());
        publisher.clearOperations();
        publisher.failNextRemove();
        OverlayRefreshResult result = new OverlayRefreshResult();

        layer.clear(result);

        assertTrue(layer.getOverlays().isEmpty());
        assertEquals(2, publisher.operations().size());
        assertTrue(result.hasFailures());
        assertFalse(result.isRetryNeeded());
    }

    @Test
    void reconcile_showFailure_retriesPresentPositionWithoutReplacingIt() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        publisher.failNextShow();
        MarkerOverlayLayer layer = new MarkerOverlayLayer("mapfrontiers", "labels", publisher);
        OverlayRefreshResult failedResult = new OverlayRefreshResult();

        reconcile(layer, List.of(OverlayTestStates.marker(1, "label")), true, failedResult);
        MarkerOverlay overlay = layer.getOverlays().getFirst();
        publisher.clearOperations();

        layer.setVisible(true, new OverlayRefreshResult());

        assertSame(overlay, layer.getOverlays().getFirst());
        assertEquals(List.of(FakeOverlayPublisher.OperationType.SHOW),
                publisher.operations().stream().map(FakeOverlayPublisher.Operation::type).toList());
    }

    @Test
    void reconcile_absentPositionWithFailedRemove_retriesRemovalInsteadOfRepublishing() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        MarkerOverlayLayer layer = new MarkerOverlayLayer("mapfrontiers", "labels", publisher);
        reconcile(layer, List.of(OverlayTestStates.marker(1, "label")), true, new OverlayRefreshResult());
        publisher.clearOperations();
        publisher.failNextRemove();
        OverlayRefreshResult failedResult = new OverlayRefreshResult();

        reconcile(layer, Collections.singletonList(null), true, failedResult);
        assertTrue(failedResult.isRetryNeeded());
        publisher.clearOperations();

        layer.setVisible(true, new OverlayRefreshResult());

        assertTrue(layer.getOverlays().isEmpty());
        assertEquals(List.of(FakeOverlayPublisher.OperationType.REMOVE),
                publisher.operations().stream().map(FakeOverlayPublisher.Operation::type).toList());
    }

    private static void reconcile(MarkerOverlayLayer layer, List<MarkerOverlayState> states,
                                  boolean visible, OverlayRefreshResult result) {
        layer.beginReconcile();
        for (MarkerOverlayState state : states) {
            layer.reconcileNext(state, visible, result);
        }
        layer.finishReconcile(result);
    }
}
