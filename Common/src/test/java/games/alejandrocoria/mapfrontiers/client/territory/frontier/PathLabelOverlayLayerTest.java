package games.alejandrocoria.mapfrontiers.client.territory.frontier;

import games.alejandrocoria.mapfrontiers.client.territory.overlay.FakeOverlayPublisher;
import games.alejandrocoria.mapfrontiers.client.territory.overlay.MarkerOverlayState;
import games.alejandrocoria.mapfrontiers.client.territory.overlay.OverlayActivation;
import games.alejandrocoria.mapfrontiers.client.territory.overlay.OverlayDisplayState;
import games.alejandrocoria.mapfrontiers.client.territory.overlay.OverlayRefreshResult;
import journeymap.api.v2.client.display.MarkerOverlay;
import journeymap.api.v2.client.model.MapImage;
import journeymap.api.v2.common.Context;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathLabelOverlayLayerTest {
    private static final ResourceKey<Level> DIMENSION = createTestDimension();

    @SuppressWarnings("unchecked")
    private static ResourceKey<Level> createTestDimension() {
        try {
            Method create = ResourceKey.class.getDeclaredMethod("create", ResourceLocation.class, ResourceLocation.class);
            create.setAccessible(true);
            return (ResourceKey<Level>) create.invoke(null, new ResourceLocation("minecraft", "dimension"),
                    new ResourceLocation("mapfrontiers", "path_label_test"));
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to create test dimension key", e);
        }
    }

    @Test
    void selectPathLabelRoles_optionalAndSinglePointLabels_preserveSemanticRoles() {
        assertTrue(FrontierOverlay.selectPathLabelRoles(0, true, true, true).isEmpty());
        assertTrue(FrontierOverlay.selectPathLabelRoles(3, false, false, false).isEmpty());
        assertEquals(List.of(PathLabelOverlayLayer.Role.SINGLE),
                FrontierOverlay.selectPathLabelRoles(1, true, true, true));
        assertEquals(List.of(PathLabelOverlayLayer.Role.SINGLE),
                FrontierOverlay.selectPathLabelRoles(1, false, true, false));
        assertEquals(List.of(PathLabelOverlayLayer.Role.START, PathLabelOverlayLayer.Role.END),
                FrontierOverlay.selectPathLabelRoles(3, true, false, true));
        assertEquals(List.of(PathLabelOverlayLayer.Role.START, PathLabelOverlayLayer.Role.MIDDLE,
                        PathLabelOverlayLayer.Role.END),
                FrontierOverlay.selectPathLabelRoles(3, true, true, true));
    }

    @Test
    void reconcile_sparseUiStates_flattensFixedUiAndRoleOrder() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        PathLabelOverlayLayer layer = createLayer(publisher);

        layer.reconcile(List.of(
                        uiState(Context.UI.Webmap, true, state(Context.UI.Webmap, 9), null, null,
                                state(Context.UI.Webmap, 12)),
                        uiState(Context.UI.Fullscreen, true, state(Context.UI.Fullscreen, 1),
                                state(Context.UI.Fullscreen, 2), state(Context.UI.Fullscreen, 2),
                                state(Context.UI.Fullscreen, 4)),
                        uiState(Context.UI.Minimap, true, null, state(Context.UI.Minimap, 6),
                                state(Context.UI.Minimap, 7), state(Context.UI.Minimap, 8))),
                true, new OverlayRefreshResult());

        assertEquals(List.of(1, 2, 2, 4, 6, 7, 8, 9, 12),
                layer.getOverlays().stream().map(overlay -> overlay.getPoint().getX()).toList());
        assertNotSame(layer.getOverlays().get(1), layer.getOverlays().get(2));
    }

    @Test
    void reconcile_roleChanges_preserveSiblingAndChangedSlotIdentities() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        PathLabelOverlayLayer layer = createLayer(publisher);
        layer.reconcile(List.of(uiState(Context.UI.Fullscreen, true, null,
                        state(Context.UI.Fullscreen, 1), state(Context.UI.Fullscreen, 2),
                        state(Context.UI.Fullscreen, 3))),
                true, new OverlayRefreshResult());
        List<MarkerOverlay> original = layer.getOverlays();
        publisher.clearOperations();

        layer.reconcile(List.of(uiState(Context.UI.Fullscreen, true, null,
                        state(Context.UI.Fullscreen, 11), null, state(Context.UI.Fullscreen, 3))),
                true, new OverlayRefreshResult());
        List<MarkerOverlay> changed = layer.getOverlays();

        assertEquals(2, changed.size());
        assertSame(original.get(0), changed.get(0));
        assertSame(original.get(2), changed.get(1));
        assertEquals(List.of(FakeOverlayPublisher.OperationType.SHOW,
                        FakeOverlayPublisher.OperationType.REMOVE),
                publisher.operations().stream().map(FakeOverlayPublisher.Operation::type).toList());
    }

    @Test
    void reconcile_disabledAndGloballyHiddenUi_reactivatesSameSlots() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        PathLabelOverlayLayer layer = createLayer(publisher);
        PathLabelOverlayLayer.UiState enabled = uiState(Context.UI.Fullscreen, true,
                state(Context.UI.Fullscreen, 1), state(Context.UI.Fullscreen, 2),
                state(Context.UI.Fullscreen, 3), state(Context.UI.Fullscreen, 4));
        layer.reconcile(List.of(enabled), true, new OverlayRefreshResult());
        List<MarkerOverlay> original = layer.getOverlays();

        layer.reconcile(List.of(uiState(Context.UI.Fullscreen, false, null, null, null, null)),
                false, new OverlayRefreshResult());
        layer.reconcile(List.of(enabled), false, new OverlayRefreshResult());
        assertSameOverlays(original, layer.getOverlays());
        publisher.clearOperations();

        layer.reconcile(List.of(enabled), true, new OverlayRefreshResult());

        assertSameOverlays(original, layer.getOverlays());
        assertEquals(4, publisher.operations().size());
        assertTrue(publisher.operations().stream().allMatch(operation ->
                operation.type() == FakeOverlayPublisher.OperationType.SHOW));
    }

    private static PathLabelOverlayLayer createLayer(FakeOverlayPublisher publisher) {
        return new PathLabelOverlayLayer("mapfrontiers", "path-label", publisher);
    }

    private static PathLabelOverlayLayer.UiState uiState(
            Context.UI ui,
            boolean enabled,
            MarkerOverlayState single,
            MarkerOverlayState start,
            MarkerOverlayState middle,
            MarkerOverlayState end) {
        return new PathLabelOverlayLayer.UiState(ui, enabled, single, start, middle, end);
    }

    private static MarkerOverlayState state(Context.UI ui, int x) {
        MapImage image = new MapImage(new ResourceLocation(
                "mapfrontiers", "textures/test/path_label_" + x + ".png"), 16, 16);
        OverlayDisplayState displayState = new OverlayDisplayState(DIMENSION,
                OverlayActivation.of(ui, Context.MapType.Day), 2, 16384,
                0, "frontier", null, "label-" + x, null, null, null);
        return new MarkerOverlayState(new BlockPos(x, 70, x), image, "visual-" + x, displayState);
    }

    private static void assertSameOverlays(List<MarkerOverlay> expected, List<MarkerOverlay> actual) {
        assertEquals(expected.size(), actual.size());
        for (int index = 0; index < expected.size(); index++) {
            assertSame(expected.get(index), actual.get(index));
        }
    }
}
