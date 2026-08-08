package games.alejandrocoria.mapfrontiers.client.territory.overlay;

import journeymap.api.v2.client.display.Context;
import journeymap.api.v2.client.display.PolygonOverlay;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolygonOverlayLayerTest {
    private static final OverlayActivation ACTIVATION = OverlayActivation.of(
            new Context.UI[]{Context.UI.Fullscreen}, new Context.MapType[]{Context.MapType.Day});

    @Test
    void reconcile_sameSequence_reusesIdentityWithoutPublishingAgain() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        PolygonOverlayLayer layer = new PolygonOverlayLayer("mapfrontiers", "base", publisher);
        reconcile(layer, 3, true, new OverlayRefreshResult(), 2, 4096);
        List<PolygonOverlay> original = layer.getOverlays();
        publisher.clearOperations();

        reconcile(layer, 3, true, new OverlayRefreshResult(), 2, 4096);

        List<PolygonOverlay> current = layer.getOverlays();
        assertEquals(3, current.size());
        assertSame(original.get(0), current.get(0));
        assertSame(original.get(1), current.get(1));
        assertSame(original.get(2), current.get(2));
        assertTrue(publisher.operations().isEmpty());
    }

    @Test
    void reconcile_displayChanges_republishesSameIdentity() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        PolygonOverlayLayer layer = new PolygonOverlayLayer("mapfrontiers", "collection", publisher);
        reconcile(layer, 1, true, new OverlayRefreshResult(), 512, 4096);
        PolygonOverlay overlay = layer.getOverlays().getFirst();
        publisher.clearOperations();

        OverlayRefreshResult result = new OverlayRefreshResult();
        layer.beginReconcile();
        layer.reconcileNext(OverlayTestStates.barePolygon(0, 0, 0, false,
                OverlayActivation.of(new Context.UI[]{Context.UI.Fullscreen, Context.UI.Webmap},
                        new Context.MapType[]{Context.MapType.Day, Context.MapType.Night}),
                0, 256), true, result);
        layer.finishReconcile(result);

        assertSame(overlay, layer.getOverlays().getFirst());
        assertEquals(2, overlay.getMinZoom());
        assertEquals(256, overlay.getMaxZoom());
        assertNotNull(overlay.getTextProperties());
        assertEquals(1, publisher.operations().size());
        assertSame(overlay, publisher.operations().getFirst().overlay());
    }

    @Test
    void reconcile_holesRemoved_clearsExistingHoles() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        PolygonOverlayLayer layer = new PolygonOverlayLayer("mapfrontiers", "base", publisher);
        OverlayRefreshResult result = new OverlayRefreshResult();
        layer.beginReconcile();
        layer.reconcileNext(OverlayTestStates.barePolygon(0, 0, 0, true, ACTIVATION, 2, 4096), true, result);
        layer.finishReconcile(result);
        PolygonOverlay overlay = layer.getOverlays().getFirst();

        result = new OverlayRefreshResult();
        layer.beginReconcile();
        layer.reconcileNext(OverlayTestStates.barePolygon(0, 1, 0, false, ACTIVATION, 2, 4096), true, result);
        layer.finishReconcile(result);

        assertSame(overlay, layer.getOverlays().getFirst());
        assertNull(overlay.getHoles());
    }

    @Test
    void reconcile_visibilityAndSizeChanges_reusesPrefixAndRetiresTail() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        PolygonOverlayLayer layer = new PolygonOverlayLayer("mapfrontiers", "base", publisher);
        reconcile(layer, 3, true, new OverlayRefreshResult(), 2, 4096);
        PolygonOverlay first = layer.getOverlays().getFirst();
        publisher.clearOperations();

        reconcile(layer, 1, false, new OverlayRefreshResult(), 2, 4096);

        assertEquals(1, layer.getOverlays().size());
        assertSame(first, layer.getOverlays().getFirst());
        assertEquals(3, publisher.operations().size());

        publisher.clearOperations();
        reconcile(layer, 2, true, new OverlayRefreshResult(), 2, 4096);
        assertSame(first, layer.getOverlays().getFirst());
        assertEquals(2, publisher.operations().size());
    }

    @Test
    void clear_removeFailure_attemptsAllAndDoesNotRequestRetry() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        PolygonOverlayLayer layer = new PolygonOverlayLayer("mapfrontiers", "base", publisher);
        reconcile(layer, 3, true, new OverlayRefreshResult(), 2, 4096);
        publisher.clearOperations();
        publisher.failNextRemove();
        OverlayRefreshResult result = new OverlayRefreshResult();

        layer.clear(result);

        assertTrue(layer.getOverlays().isEmpty());
        assertEquals(3, publisher.operations().size());
        assertTrue(result.hasFailures());
        assertFalse(result.isRetryNeeded());
    }

    @Test
    void getOverlays_returnsOrderedIndependentSnapshot() {
        PolygonOverlayLayer layer = new PolygonOverlayLayer("mapfrontiers", "base", new FakeOverlayPublisher());
        reconcile(layer, 2, true, new OverlayRefreshResult(), 2, 4096);

        List<PolygonOverlay> snapshot = layer.getOverlays();
        PolygonOverlay first = snapshot.getFirst();
        snapshot.clear();

        List<PolygonOverlay> next = layer.getOverlays();
        assertEquals(2, next.size());
        assertSame(first, next.getFirst());
    }

    private static void reconcile(PolygonOverlayLayer layer, int count, boolean visible,
                                  OverlayRefreshResult result, int minZoom, int maxZoom) {
        layer.beginReconcile();
        for (int index = 0; index < count; index++) {
            layer.reconcileNext(OverlayTestStates.barePolygon(index * 10, index, 0, false,
                    ACTIVATION, minZoom, maxZoom), visible, result);
        }
        layer.finishReconcile(result);
    }
}
