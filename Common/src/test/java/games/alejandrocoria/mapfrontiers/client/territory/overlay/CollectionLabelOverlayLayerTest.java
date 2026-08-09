package games.alejandrocoria.mapfrontiers.client.territory.overlay;

import journeymap.api.v2.client.display.MarkerOverlay;
import journeymap.api.v2.common.Context;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CollectionLabelOverlayLayerTest {
    @Test
    void reconcile_gapChanges_doNotShiftFollowingBuckets() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        CollectionLabelOverlayLayer layer = new CollectionLabelOverlayLayer(
                "mapfrontiers", "collection-label", publisher);
        reconcile(layer, List.of(
                variant(Context.UI.Fullscreen, state(1), null, state(3)),
                variant(Context.UI.Fullscreen, state(10)),
                variant(Context.UI.Minimap, state(20))));
        List<MarkerOverlay> original = layer.getOverlays();
        publisher.clearOperations();

        reconcile(layer, List.of(
                variant(Context.UI.Fullscreen, state(1), state(2), state(3)),
                variant(Context.UI.Fullscreen, state(10)),
                variant(Context.UI.Minimap, state(20))));

        List<MarkerOverlay> expanded = layer.getOverlays();
        assertEquals(5, expanded.size());
        assertSame(original.get(0), expanded.get(0));
        assertSame(original.get(1), expanded.get(2));
        assertSame(original.get(2), expanded.get(3));
        assertSame(original.get(3), expanded.get(4));
        assertEquals(1, publisher.operations().size());
        assertSame(expanded.get(1), publisher.operations().get(0).overlay());

        publisher.clearOperations();
        reconcile(layer, List.of(
                variant(Context.UI.Fullscreen, state(1)),
                variant(Context.UI.Fullscreen, state(10)),
                variant(Context.UI.Minimap, state(20))));

        List<MarkerOverlay> reduced = layer.getOverlays();
        assertEquals(3, reduced.size());
        assertSame(original.get(0), reduced.get(0));
        assertSame(original.get(2), reduced.get(1));
        assertSame(original.get(3), reduced.get(2));
        assertEquals(2, publisher.operations().size());
        assertTrue(publisher.operations().stream().allMatch(operation ->
                operation.type() == FakeOverlayPublisher.OperationType.REMOVE));
    }

    @Test
    void reconcile_variantAndUiDisappear_retiresOnlyRemovedBuckets() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        CollectionLabelOverlayLayer layer = new CollectionLabelOverlayLayer(
                "mapfrontiers", "collection-label", publisher);
        reconcile(layer, List.of(
                variant(Context.UI.Fullscreen, state(1)),
                variant(Context.UI.Fullscreen, state(10)),
                variant(Context.UI.Minimap, state(20))));
        MarkerOverlay retained = layer.getOverlays().get(0);
        publisher.clearOperations();

        reconcile(layer, List.of(variant(Context.UI.Fullscreen, state(1))));

        assertEquals(1, layer.getOverlays().size());
        assertSame(retained, layer.getOverlays().get(0));
        assertEquals(2, publisher.operations().size());
        assertTrue(publisher.operations().stream().allMatch(operation ->
                operation.type() == FakeOverlayPublisher.OperationType.REMOVE));
    }

    @Test
    void reconcile_publisherUnavailable_buildsOrderedPreviewModels() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        publisher.setAvailable(false);
        CollectionLabelOverlayLayer layer = new CollectionLabelOverlayLayer(
                "mapfrontiers", "collection-label", publisher);

        reconcile(layer, List.of(
                variant(Context.UI.Fullscreen, state(1), null, state(3)),
                variant(Context.UI.Webmap, state(20))));

        assertEquals(List.of(1, 3, 20), layer.getOverlays().stream()
                .map(overlay -> overlay.getPoint().getX())
                .toList());
        assertTrue(publisher.operations().isEmpty());
    }

    @Test
    void clear_removeFailure_attemptsEveryVariantWithoutRetry() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        CollectionLabelOverlayLayer layer = new CollectionLabelOverlayLayer(
                "mapfrontiers", "collection-label", publisher);
        reconcile(layer, List.of(
                variant(Context.UI.Fullscreen, state(1)),
                variant(Context.UI.Minimap, state(10)),
                variant(Context.UI.Webmap, state(20))));
        publisher.clearOperations();
        publisher.failNextRemove();
        OverlayRefreshResult result = new OverlayRefreshResult();

        layer.clear(result);

        assertTrue(layer.getOverlays().isEmpty());
        assertEquals(3, publisher.operations().size());
        assertTrue(result.hasFailures());
        assertFalse(result.isRetryNeeded());
    }

    private static Variant variant(Context.UI ui, @Nullable MarkerOverlayState... states) {
        return new Variant(ui, Arrays.asList(states));
    }

    private static MarkerOverlayState state(int offset) {
        return OverlayTestStates.marker(offset, "label-" + offset);
    }

    private static void reconcile(CollectionLabelOverlayLayer layer, List<Variant> variants) {
        OverlayRefreshResult result = new OverlayRefreshResult();
        layer.beginReconcile();
        for (Variant variant : variants) {
            layer.beginVariant(variant.ui());
            for (MarkerOverlayState state : variant.states()) {
                layer.reconcileNext(state, result);
            }
            layer.finishVariant(result);
        }
        layer.finishReconcile(result);
    }

    private record Variant(Context.UI ui, List<MarkerOverlayState> states) {
    }
}
