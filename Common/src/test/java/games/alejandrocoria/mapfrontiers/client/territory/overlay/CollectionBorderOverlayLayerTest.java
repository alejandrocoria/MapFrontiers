package games.alejandrocoria.mapfrontiers.client.territory.overlay;

import journeymap.api.v2.client.display.PolygonOverlay;
import journeymap.api.v2.common.Context;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CollectionBorderOverlayLayerTest {
    @Test
    void reconcile_variantGrowth_doesNotShiftFollowingBuckets() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        CollectionBorderOverlayLayer layer = new CollectionBorderOverlayLayer(
                "mapfrontiers", "collection-border", publisher);
        reconcile(layer, List.of(
                variant(Context.UI.Fullscreen, state(Context.UI.Fullscreen, 0, "fs-0")),
                variant(Context.UI.Fullscreen, state(Context.UI.Fullscreen, 10, "fs-1")),
                variant(Context.UI.Minimap, state(Context.UI.Minimap, 20, "minimap-0"))));
        List<PolygonOverlay> original = layer.getOverlays();
        publisher.clearOperations();

        reconcile(layer, List.of(
                variant(Context.UI.Fullscreen, state(Context.UI.Fullscreen, 0, "fs-0"),
                        state(Context.UI.Fullscreen, 1, "fs-0-added")),
                variant(Context.UI.Fullscreen, state(Context.UI.Fullscreen, 10, "fs-1")),
                variant(Context.UI.Minimap, state(Context.UI.Minimap, 20, "minimap-0"))));

        List<PolygonOverlay> current = layer.getOverlays();
        assertEquals(4, current.size());
        assertSame(original.get(0), current.get(0));
        assertSame(original.get(1), current.get(2));
        assertSame(original.get(2), current.get(3));
        assertEquals(1, publisher.operations().size());
        assertSame(current.get(1), publisher.operations().getFirst().overlay());

        publisher.clearOperations();
        reconcile(layer, List.of(
                variant(Context.UI.Fullscreen, state(Context.UI.Fullscreen, 0, "fs-0")),
                variant(Context.UI.Fullscreen, state(Context.UI.Fullscreen, 10, "fs-1")),
                variant(Context.UI.Minimap, state(Context.UI.Minimap, 20, "minimap-0"))));

        assertEquals(3, layer.getOverlays().size());
        assertSame(original.get(1), layer.getOverlays().get(1));
        assertSame(original.get(2), layer.getOverlays().get(2));
        assertEquals(1, publisher.operations().size());
        assertEquals(FakeOverlayPublisher.OperationType.REMOVE, publisher.operations().getFirst().type());
    }

    @Test
    void reconcile_variantMaskChangesAndUiDisappears_reusesRetainedIdentity() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        CollectionBorderOverlayLayer layer = new CollectionBorderOverlayLayer(
                "mapfrontiers", "collection-border", publisher);
        reconcile(layer, List.of(
                variant(Context.UI.Fullscreen, state(Context.UI.Fullscreen, 0, "geometry")),
                variant(Context.UI.Fullscreen, state(Context.UI.Fullscreen, 5, "second-variant")),
                variant(Context.UI.Minimap, state(Context.UI.Minimap, 10, "minimap"))));
        PolygonOverlay retained = layer.getOverlays().getFirst();
        publisher.clearOperations();

        reconcile(layer, List.of(variant(Context.UI.Fullscreen,
                state(Context.UI.Fullscreen, 0, "geometry", Context.MapType.Day, Context.MapType.Night))));

        assertEquals(1, layer.getOverlays().size());
        assertSame(retained, layer.getOverlays().getFirst());
        assertEquals(List.of(
                        FakeOverlayPublisher.OperationType.SHOW,
                        FakeOverlayPublisher.OperationType.REMOVE,
                        FakeOverlayPublisher.OperationType.REMOVE),
                publisher.operations().stream().map(FakeOverlayPublisher.Operation::type).toList());
    }

    @Test
    void reconcile_publisherUnavailable_buildsOrderedPreviewModelsWithoutPublishing() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        publisher.setAvailable(false);
        CollectionBorderOverlayLayer layer = new CollectionBorderOverlayLayer(
                "mapfrontiers", "collection-border", publisher);

        reconcile(layer, List.of(
                variant(Context.UI.Fullscreen, state(Context.UI.Fullscreen, 0, "first"),
                        state(Context.UI.Fullscreen, 1, "second")),
                variant(Context.UI.Fullscreen, state(Context.UI.Fullscreen, 10, "third"))));

        assertEquals(3, layer.getOverlays().size());
        assertTrue(publisher.operations().isEmpty());
    }

    @Test
    void clear_removeFailure_attemptsEveryVariantWithoutRetry() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        CollectionBorderOverlayLayer layer = new CollectionBorderOverlayLayer(
                "mapfrontiers", "collection-border", publisher);
        reconcile(layer, List.of(
                variant(Context.UI.Fullscreen, state(Context.UI.Fullscreen, 0, "first")),
                variant(Context.UI.Minimap, state(Context.UI.Minimap, 10, "second")),
                variant(Context.UI.Webmap, state(Context.UI.Webmap, 20, "third"))));
        publisher.clearOperations();
        publisher.failNextRemove();
        OverlayRefreshResult result = new OverlayRefreshResult();

        layer.clear(result);

        assertTrue(layer.getOverlays().isEmpty());
        assertEquals(3, publisher.operations().size());
        assertTrue(result.hasFailures());
        assertFalse(result.isRetryNeeded());
    }

    private static Variant variant(Context.UI ui, PolygonOverlayState... states) {
        return new Variant(ui, List.of(states));
    }

    private static PolygonOverlayState state(Context.UI ui, int offset, String geometryKey,
                                             Context.MapType... mapTypes) {
        Context.MapType[] effectiveMapTypes = mapTypes.length == 0
                ? new Context.MapType[]{Context.MapType.Day}
                : mapTypes;
        return OverlayTestStates.barePolygon(offset, geometryKey, "style", false,
                OverlayActivation.of(ui, effectiveMapTypes), 2, 4096);
    }

    private static void reconcile(CollectionBorderOverlayLayer layer, List<Variant> variants) {
        OverlayRefreshResult result = new OverlayRefreshResult();
        layer.beginReconcile();
        for (Variant variant : variants) {
            layer.beginVariant(variant.ui());
            for (PolygonOverlayState state : variant.states()) {
                layer.reconcileNext(state, result);
            }
            layer.finishVariant(result);
        }
        layer.finishReconcile(result);
    }

    private record Variant(Context.UI ui, List<PolygonOverlayState> states) {
    }
}
