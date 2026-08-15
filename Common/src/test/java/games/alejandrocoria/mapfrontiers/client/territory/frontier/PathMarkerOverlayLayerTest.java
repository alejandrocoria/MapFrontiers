package games.alejandrocoria.mapfrontiers.client.territory.frontier;

import games.alejandrocoria.mapfrontiers.client.territory.overlay.FakeOverlayPublisher;
import games.alejandrocoria.mapfrontiers.client.territory.overlay.OverlayRefreshResult;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierData;
import journeymap.api.v2.client.display.Context;
import journeymap.api.v2.client.display.MarkerOverlay;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathMarkerOverlayLayerTest {
    private static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION,
            Identifier.fromNamespaceAndPath("mapfrontiers", "path_test"));
    private static final int MARKER_SIZE = 12;

    @Test
    void reconcile_zeroThenOnePoint_handlesStructuralEndpoints() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        PathMarkerOverlayLayer layer = createLayer(publisher);

        reconcile(layer, new PathLayout(List.of(), List.of()), fullscreen(true),
                true, new OverlayRefreshResult());
        assertTrue(layer.getOverlays().isEmpty());
        assertTrue(publisher.operations().isEmpty());

        PathLayout onePoint = new PathLayout(
                List.of(point(7, FrontierData.PathStyle.BIG_DOT, 0.f)), List.of());
        reconcile(layer, onePoint, List.of(new PathMarkerOverlayLayer.UiState(
                        Context.UI.Fullscreen, true, new Context.MapType[0])),
                true, new OverlayRefreshResult());

        assertEquals(1, layer.getOverlays().size());
        assertEquals(7, layer.getOverlays().getFirst().getPoint().getX());
        assertTrue(layer.getOverlays().getFirst().getActiveMapTypes().isEmpty());
    }

    @Test
    void reconcile_identicalLayout_preservesIdentityWithoutPublishing() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        PathMarkerOverlayLayer layer = createLayer(publisher);
        PathLayout layout = oneSegment(FrontierData.PathStyle.BIG_DOT, 0.f, 0);

        reconcile(layer, layout, fullscreen(true), true, new OverlayRefreshResult());
        List<MarkerOverlay> original = layer.getOverlays();
        publisher.clearOperations();

        reconcile(layer, layout, fullscreen(true), true, new OverlayRefreshResult());

        assertSameOverlays(original, layer.getOverlays());
        assertTrue(publisher.operations().isEmpty());
    }

    @Test
    void reconcile_translatedLayout_preservesIdentityAndUpdatesEverySlot() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        PathMarkerOverlayLayer layer = createLayer(publisher);
        reconcile(layer, oneSegment(FrontierData.PathStyle.BIG_DOT, 0.f, 0),
                fullscreen(true), true, new OverlayRefreshResult());
        List<MarkerOverlay> original = layer.getOverlays();
        publisher.clearOperations();

        reconcile(layer, oneSegment(FrontierData.PathStyle.BIG_DOT, 0.f, 1),
                fullscreen(true), true, new OverlayRefreshResult());

        assertSameOverlays(original, layer.getOverlays());
        assertEquals(original.size(), publisher.operations().size());
        assertTrue(publisher.operations().stream().allMatch(operation ->
                operation.type() == FakeOverlayPublisher.OperationType.SHOW));
    }

    @Test
    void reconcile_oneRepeatedPositionChanges_publishesOnlyAffectedOrdinals() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        PathMarkerOverlayLayer layer = createLayer(publisher);
        PathLayout originalLayout = oneSegment(FrontierData.PathStyle.BIG_DOT, 0.f, 0);
        reconcile(layer, originalLayout, fullscreen(true), true, new OverlayRefreshResult());
        List<MarkerOverlay> original = layer.getOverlays();
        BlockPos changedPosition = original.stream()
                .filter(overlay -> overlay.getDisplayOrder() == 99)
                .findFirst()
                .orElseThrow()
                .getPoint();
        List<MarkerOverlay> affected = original.stream()
                .filter(overlay -> overlay.getDisplayOrder() == 99 && overlay.getPoint().equals(changedPosition))
                .toList();
        FrontierOverlay.PathSegmentLayout originalSegment = originalLayout.segments().getFirst();
        List<BlockPos> changedPositions = new ArrayList<>(originalSegment.repeatedMarkerPositions());
        int changedIndex = changedPositions.indexOf(changedPosition);
        changedPositions.set(changedIndex, changedPosition.offset(0, 0, 1));
        FrontierOverlay.PathSegmentLayout changedSegment = new FrontierOverlay.PathSegmentLayout(
                originalSegment.from(), originalSegment.to(), originalSegment.rotation(),
                originalSegment.segmentMarkerId(), originalSegment.segmentSpacingMultiplier(), originalSegment.length(),
                List.copyOf(changedPositions));
        PathLayout changedLayout = new PathLayout(originalLayout.points(), List.of(changedSegment));
        publisher.clearOperations();

        reconcile(layer, changedLayout, fullscreen(true), true, new OverlayRefreshResult());

        assertSameOverlays(original, layer.getOverlays());
        assertFalse(affected.isEmpty());
        assertEquals(affected.size(), publisher.operations().size());
        assertTrue(publisher.operations().stream().allMatch(operation ->
                operation.type() == FakeOverlayPublisher.OperationType.SHOW
                        && affected.contains(operation.overlay())));
    }

    @Test
    void reconcile_nonDirectionalRotationChange_doesNotPublish() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        PathMarkerOverlayLayer layer = createLayer(publisher);
        reconcile(layer, oneSegment(FrontierData.PathStyle.BIG_DOT, 0.f, 0),
                fullscreen(true), true, new OverlayRefreshResult());
        List<MarkerOverlay> original = layer.getOverlays();
        publisher.clearOperations();

        reconcile(layer, oneSegment(FrontierData.PathStyle.BIG_DOT, 83.4f, 0),
                fullscreen(true), true, new OverlayRefreshResult());

        assertSameOverlays(original, layer.getOverlays());
        assertTrue(publisher.operations().isEmpty());
    }

    @Test
    void reconcile_directionalRotationChange_republishesSameIdentity() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        PathMarkerOverlayLayer layer = createLayer(publisher);
        reconcile(layer, oneSegment(FrontierData.PathStyle.ARROW, 0.f, 0),
                fullscreen(true), true, new OverlayRefreshResult());
        List<MarkerOverlay> original = layer.getOverlays();
        publisher.clearOperations();

        reconcile(layer, oneSegment(FrontierData.PathStyle.ARROW, 45.f, 0),
                fullscreen(true), true, new OverlayRefreshResult());

        assertSameOverlays(original, layer.getOverlays());
        assertEquals(original.size(), publisher.operations().size());
        assertTrue(publisher.operations().stream().allMatch(operation ->
                operation.type() == FakeOverlayPublisher.OperationType.SHOW));
    }

    @Test
    void reconcile_colorChange_republishesSameIdentity() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        PathMarkerOverlayLayer layer = createLayer(publisher);
        PathLayout layout = oneSegment(FrontierData.PathStyle.BIG_DOT, 0.f, 0);
        reconcile(layer, layout, fullscreen(true), 0x112233, 0.8f, true, new OverlayRefreshResult());
        List<MarkerOverlay> original = layer.getOverlays();
        publisher.clearOperations();

        reconcile(layer, layout, fullscreen(true), 0x445566, 0.8f, true, new OverlayRefreshResult());

        assertSameOverlays(original, layer.getOverlays());
        assertEquals(original.size(), publisher.operations().size());
        assertTrue(publisher.operations().stream().allMatch(operation ->
                operation.type() == FakeOverlayPublisher.OperationType.SHOW));
    }

    @Test
    void reconcile_markerBecomesAbsent_retiresItsSlots() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        PathMarkerOverlayLayer layer = createLayer(publisher);
        reconcile(layer, oneSegment(FrontierData.PathStyle.BIG_DOT, 0.f, 0),
                fullscreen(true), true, new OverlayRefreshResult());
        int originalSize = layer.getOverlays().size();
        publisher.clearOperations();

        reconcile(layer, oneSegment(FrontierData.PathStyle.NONE, 0.f, 0),
                fullscreen(true), true, new OverlayRefreshResult());

        assertTrue(layer.getOverlays().isEmpty());
        assertEquals(originalSize, publisher.operations().size());
        assertTrue(publisher.operations().stream().allMatch(operation ->
                operation.type() == FakeOverlayPublisher.OperationType.REMOVE));
    }

    @Test
    void reconcile_firstSegmentShrinks_doesNotTouchSiblingSegment() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        PathMarkerOverlayLayer layer = createLayer(publisher);
        PathLayout originalLayout = twoSegments(24, 24);
        reconcile(layer, originalLayout, fullscreen(true), true, new OverlayRefreshResult());
        List<MarkerOverlay> secondSegment = layer.getOverlays().stream()
                .filter(overlay -> overlay.getDisplayOrder() == 99 && overlay.getPoint().getX() >= 1000)
                .toList();
        publisher.clearOperations();

        reconcile(layer, twoSegments(5, 24), fullscreen(true), true, new OverlayRefreshResult());

        assertFalse(secondSegment.isEmpty());
        assertTrue(publisher.operations().stream().noneMatch(operation ->
                secondSegment.contains(operation.overlay())));
        assertTrue(publisher.operations().stream().anyMatch(operation ->
                operation.type() == FakeOverlayPublisher.OperationType.REMOVE));
    }

    @Test
    void reconcile_manyRepeatedMarkers_avoidsWorkForIdenticalSecondPass() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        PathMarkerOverlayLayer layer = createLayer(publisher);
        PathLayout layout = oneSegmentWithVolume(256, 100_000.0);

        reconcile(layer, layout, fullscreen(true), true, new OverlayRefreshResult());
        List<MarkerOverlay> original = layer.getOverlays();
        assertTrue(original.size() > 500);
        assertEquals(original.size(), publisher.operations().size());
        publisher.clearOperations();

        reconcile(layer, layout, fullscreen(true), true, new OverlayRefreshResult());

        assertSameOverlays(original, layer.getOverlays());
        assertTrue(publisher.operations().isEmpty());
    }

    @Test
    void reconcile_saturatedBands_materializesOneCombinedRange() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        PathMarkerOverlayLayer layer = createLayer(publisher);
        PathLayout layout = oneSegmentWithVolume(256, 100_000.0);
        FrontierOverlay.PathSegmentLayout segment = layout.segments().getFirst();
        PathMarkerBandPlan plan = PathMarkerBandPlan.create(segment.length(),
                segment.repeatedMarkerPositions().size(), 1, segment.segmentSpacingMultiplier());

        reconcile(layer, layout, fullscreen(true), true, new OverlayRefreshResult());

        List<MarkerOverlay> repeated = repeatedOverlays(layer);
        assertEquals(plan.entries().stream().mapToInt(PathMarkerBandPlan.Entry::markerCount).sum(), repeated.size());
        PathMarkerBandPlan.Entry combined = plan.entries().getLast();
        List<MarkerOverlay> combinedOverlays = repeated.stream()
                .filter(overlay -> overlay.getMinZoom() == combined.minZoom()
                        && overlay.getMaxZoom() == 16384)
                .toList();
        assertEquals(combined.markerCount(), combinedOverlays.size());
        assertEquals(0, combined.maxZoom());
        for (int minZoom = 2; minZoom <= 16384; minZoom *= 2) {
            int zoom = minZoom;
            List<BlockPos> expectedPositions = expectedRepeatedPositions(segment, minZoom, 1);
            List<BlockPos> actualPositions = repeated.stream()
                    .filter(overlay -> zoom >= overlay.getMinZoom() && zoom <= overlay.getMaxZoom())
                    .map(MarkerOverlay::getPoint)
                    .toList();
            assertEquals(expectedPositions, actualPositions, "Unexpected positions at zoom " + minZoom);
        }
    }

    @Test
    void reconcile_earlyBandAppears_doesNotTouchStableLaterOwner() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        PathMarkerOverlayLayer layer = createLayer(publisher);
        reconcile(layer, oneSegmentWithVolume(256, 3_000.0), fullscreen(true),
                true, new OverlayRefreshResult());
        List<MarkerOverlay> stableBand = repeatedOverlays(layer).stream()
                .filter(overlay -> overlay.getMinZoom() == 4 && overlay.getMaxZoom() == 7)
                .toList();
        assertFalse(stableBand.isEmpty());
        publisher.clearOperations();

        reconcile(layer, oneSegmentWithVolume(256, 3_200.0), fullscreen(true),
                true, new OverlayRefreshResult());

        List<MarkerOverlay> currentStableBand = repeatedOverlays(layer).stream()
                .filter(overlay -> overlay.getMinZoom() == 4 && overlay.getMaxZoom() == 7)
                .toList();
        assertSameOverlays(stableBand, currentStableBand);
        assertTrue(publisher.operations().stream().noneMatch(operation ->
                stableBand.contains(operation.overlay())));
    }

    @Test
    void reconcile_saturationBoundaryMoves_reusesNewFirstOwnerAndRetiresAbsorbedOwner() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        PathMarkerOverlayLayer layer = createLayer(publisher);
        reconcile(layer, oneSegmentWithVolume(256, 500.0), fullscreen(true),
                true, new OverlayRefreshResult());
        List<MarkerOverlay> firstOwner = repeatedOverlays(layer).stream()
                .filter(overlay -> overlay.getMinZoom() == 2048 && overlay.getMaxZoom() == 4095)
                .toList();
        List<MarkerOverlay> absorbedOwner = repeatedOverlays(layer).stream()
                .filter(overlay -> overlay.getMinZoom() == 4096 && overlay.getMaxZoom() == 16384)
                .toList();
        assertEquals(249, firstOwner.size());
        assertEquals(256, absorbedOwner.size());
        publisher.clearOperations();

        reconcile(layer, oneSegmentWithVolume(256, 520.0), fullscreen(true),
                true, new OverlayRefreshResult());

        List<MarkerOverlay> combined = repeatedOverlays(layer).stream()
                .filter(overlay -> overlay.getMinZoom() == 2048 && overlay.getMaxZoom() == 16384)
                .toList();
        assertEquals(256, combined.size());
        for (int index = 0; index < firstOwner.size(); index++) {
            assertSame(firstOwner.get(index), combined.get(index));
        }
        assertTrue(publisher.operations().stream().filter(operation ->
                        absorbedOwner.contains(operation.overlay()))
                .allMatch(operation -> operation.type() == FakeOverlayPublisher.OperationType.REMOVE));
        assertEquals(absorbedOwner.size(), publisher.operations().stream().filter(operation ->
                absorbedOwner.contains(operation.overlay())).count());
    }

    @Test
    void reconcile_disabledUi_retiresOnlyThatUi() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        PathMarkerOverlayLayer layer = createLayer(publisher);
        PathLayout layout = oneSegment(FrontierData.PathStyle.BIG_DOT, 0.f, 0);
        List<PathMarkerOverlayLayer.UiState> bothUis = List.of(
                new PathMarkerOverlayLayer.UiState(Context.UI.Fullscreen, true, new Context.MapType[]{Context.MapType.Day}),
                new PathMarkerOverlayLayer.UiState(Context.UI.Minimap, true, new Context.MapType[]{Context.MapType.Day}));
        reconcile(layer, layout, bothUis, true, new OverlayRefreshResult());
        List<MarkerOverlay> minimap = overlaysForUi(layer, Context.UI.Minimap);
        publisher.clearOperations();

        reconcile(layer, layout, List.of(
                        new PathMarkerOverlayLayer.UiState(Context.UI.Fullscreen, false, new Context.MapType[]{Context.MapType.Day}),
                        new PathMarkerOverlayLayer.UiState(Context.UI.Minimap, true, new Context.MapType[]{Context.MapType.Day})),
                true, new OverlayRefreshResult());

        assertSameOverlays(minimap, overlaysForUi(layer, Context.UI.Minimap));
        assertTrue(overlaysForUi(layer, Context.UI.Fullscreen).isEmpty());
        assertTrue(publisher.operations().stream().allMatch(operation ->
                        operation.type() == FakeOverlayPublisher.OperationType.REMOVE
                        && operation.overlay() instanceof MarkerOverlay marker
                        && marker.getActiveUIs().equals(Set.of(Context.UI.Fullscreen))));
    }

    @Test
    void setVisible_hideThenShow_preservesAllIdentities() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        PathMarkerOverlayLayer layer = createLayer(publisher);
        reconcile(layer, oneSegment(FrontierData.PathStyle.BIG_DOT, 0.f, 0),
                fullscreen(true), true, new OverlayRefreshResult());
        List<MarkerOverlay> original = layer.getOverlays();
        publisher.clearOperations();

        layer.setVisible(false, new OverlayRefreshResult());
        layer.setVisible(true, new OverlayRefreshResult());

        assertSameOverlays(original, layer.getOverlays());
        assertEquals(original.size() * 2, publisher.operations().size());
        assertTrue(publisher.operations().subList(0, original.size()).stream().allMatch(operation ->
                operation.type() == FakeOverlayPublisher.OperationType.REMOVE));
        assertTrue(publisher.operations().subList(original.size(), original.size() * 2).stream().allMatch(operation ->
                operation.type() == FakeOverlayPublisher.OperationType.SHOW));
    }

    @Test
    void clear_publishedLayer_removesEverythingOnce() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        PathMarkerOverlayLayer layer = createLayer(publisher);
        reconcile(layer, oneSegment(FrontierData.PathStyle.BIG_DOT, 0.f, 0),
                fullscreen(true), true, new OverlayRefreshResult());
        int originalSize = layer.getOverlays().size();
        publisher.clearOperations();

        layer.clear(new OverlayRefreshResult());

        assertTrue(layer.getOverlays().isEmpty());
        assertEquals(originalSize, publisher.operations().size());
        assertTrue(publisher.operations().stream().allMatch(operation ->
                operation.type() == FakeOverlayPublisher.OperationType.REMOVE));
        publisher.clearOperations();
        layer.clear(new OverlayRefreshResult());
        assertTrue(publisher.operations().isEmpty());
    }

    @Test
    void reconcile_showFailure_retriesOnlyUnpublishedSlot() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        publisher.failNextShow();
        PathMarkerOverlayLayer layer = createLayer(publisher);
        PathLayout layout = oneSegment(FrontierData.PathStyle.BIG_DOT, 0.f, 0);
        OverlayRefreshResult failed = new OverlayRefreshResult();

        reconcile(layer, layout, fullscreen(true), true, failed);
        List<MarkerOverlay> original = layer.getOverlays();
        assertTrue(failed.isRetryNeeded());
        publisher.clearOperations();

        reconcile(layer, layout, fullscreen(true), true, new OverlayRefreshResult());

        assertSameOverlays(original, layer.getOverlays());
        assertEquals(1, publisher.operations().size());
        assertEquals(FakeOverlayPublisher.OperationType.SHOW, publisher.operations().getFirst().type());
    }

    @Test
    void getOverlays_twoSegments_preservesLegacyPreviewOrder() {
        FakeOverlayPublisher publisher = new FakeOverlayPublisher();
        publisher.setAvailable(false);
        PathMarkerOverlayLayer layer = createLayer(publisher);

        reconcile(layer, twoSegments(12, 12), fullscreen(true), true, new OverlayRefreshResult());

        List<MarkerOverlay> overlays = layer.getOverlays();
        int firstPoint = indexOfPoint(overlays, 0);
        int secondPoint = indexOfPoint(overlays, 1000);
        int lastPoint = indexOfPoint(overlays, 2000);
        assertTrue(firstPoint > 0);
        assertTrue(secondPoint > firstPoint + 1);
        assertEquals(secondPoint + 1, lastPoint);
        assertTrue(overlays.subList(0, firstPoint).stream().allMatch(overlay -> overlay.getDisplayOrder() == 99));
        assertTrue(overlays.subList(firstPoint + 1, secondPoint).stream().allMatch(overlay -> overlay.getDisplayOrder() == 99));
        assertTrue(publisher.operations().isEmpty());
    }

    @Test
    void resolvePointMarker_noneRoles_preservesEffectiveFallbacks() {
        FrontierData.PathStyle style = new FrontierData.PathStyle();
        style.startMarker = FrontierData.PathStyle.NONE;
        style.innerMarker = FrontierData.PathStyle.NONE;
        style.endMarker = FrontierData.PathStyle.NONE;
        style.segmentMarker = FrontierData.PathStyle.SMALL_DOT;

        assertEquals(FrontierData.PathStyle.SMALL_DOT,
                FrontierOverlay.resolvePathPointMarkerId(style, 3, 0));
        assertEquals(FrontierData.PathStyle.SMALL_DOT,
                FrontierOverlay.resolvePathPointMarkerId(style, 3, 1));

        style.segmentMarker = FrontierData.PathStyle.NONE;
        assertEquals(FrontierData.PathStyle.NONE,
                FrontierOverlay.resolvePathPointMarkerId(style, 3, 2));
        assertEquals(FrontierData.PathStyle.BIG_DOT,
                FrontierOverlay.resolvePathSinglePointMarkerId(style));

        style.startMarker = Identifier.fromNamespaceAndPath("mapfrontiers", "unknown_marker");
        assertEquals(FrontierData.PathStyle.BIG_DOT,
                FrontierOverlay.resolvePathSinglePointMarkerId(style));
    }

    private static PathMarkerOverlayLayer createLayer(FakeOverlayPublisher publisher) {
        return new PathMarkerOverlayLayer("mapfrontiers", "path-base", publisher,
                PathMarkerOverlayLayer.Mode.BASE);
    }

    private static void reconcile(PathMarkerOverlayLayer layer, PathLayout layout,
                                  List<PathMarkerOverlayLayer.UiState> uiStates,
                                  boolean visible, OverlayRefreshResult result) {
        reconcile(layer, layout, uiStates, 0x336699, 0.8f, visible, result);
    }

    private static void reconcile(PathMarkerOverlayLayer layer, PathLayout layout,
                                  List<PathMarkerOverlayLayer.UiState> uiStates,
                                  int tint, float opacity,
                                  boolean visible, OverlayRefreshResult result) {
        layer.reconcile(layout.points(), layout.segments(), uiStates, DIMENSION,
                tint, opacity, MARKER_SIZE, 1, visible, result);
    }

    private static List<PathMarkerOverlayLayer.UiState> fullscreen(boolean enabled) {
        return List.of(new PathMarkerOverlayLayer.UiState(
                Context.UI.Fullscreen, enabled, new Context.MapType[]{Context.MapType.Day}));
    }

    private static PathLayout oneSegment(Identifier markerId, float rotation, int offset) {
        return new PathLayout(
                List.of(
                        point(offset, markerId, rotation),
                        point(offset + 100, markerId, rotation)),
                List.of(segment(offset, 24, markerId, rotation)));
    }

    private static PathLayout twoSegments(int firstPositionCount, int secondPositionCount) {
        return new PathLayout(
                List.of(
                        point(0, FrontierData.PathStyle.BIG_DOT, 0.f),
                        point(1000, FrontierData.PathStyle.BIG_DOT, 0.f),
                        point(2000, FrontierData.PathStyle.BIG_DOT, 0.f)),
                List.of(
                        segment(0, firstPositionCount, FrontierData.PathStyle.SMALL_DOT, 0.f),
                        segment(1000, secondPositionCount, FrontierData.PathStyle.SMALL_DOT, 0.f)));
    }

    private static PathLayout oneSegmentWithVolume(int positionCount, double length) {
        return new PathLayout(
                List.of(
                        point(0, FrontierData.PathStyle.BIG_DOT, 0.f),
                        point(positionCount + 1, FrontierData.PathStyle.BIG_DOT, 0.f)),
                List.of(segment(0, positionCount, length, FrontierData.PathStyle.SMALL_DOT, 0.f)));
    }

    private static FrontierOverlay.PathPointLayout point(int x, Identifier markerId, float rotation) {
        return new FrontierOverlay.PathPointLayout(new BlockPos(x, 70, 0), markerId, rotation, 0.0);
    }

    private static FrontierOverlay.PathSegmentLayout segment(int offset, int positionCount,
                                                             Identifier markerId, float rotation) {
        return segment(offset, positionCount, 3_000.0, markerId, rotation);
    }

    private static FrontierOverlay.PathSegmentLayout segment(int offset, int positionCount, double length,
                                                             Identifier markerId, float rotation) {
        List<BlockPos> positions = new ArrayList<>(positionCount);
        for (int index = 1; index <= positionCount; index++) {
            positions.add(new BlockPos(offset + index, 70, 0));
        }
        return new FrontierOverlay.PathSegmentLayout(
                new BlockPos(offset, 70, 0),
                new BlockPos(offset + positionCount + 1, 70, 0),
                rotation, markerId, 1.0, length, List.copyOf(positions));
    }

    private static List<MarkerOverlay> overlaysForUi(PathMarkerOverlayLayer layer, Context.UI ui) {
        return layer.getOverlays().stream()
                .filter(overlay -> overlay.getActiveUIs().equals(Set.of(ui)))
                .toList();
    }

    private static List<MarkerOverlay> repeatedOverlays(PathMarkerOverlayLayer layer) {
        return layer.getOverlays().stream()
                .filter(overlay -> overlay.getDisplayOrder() == 99)
                .toList();
    }

    private static List<BlockPos> expectedRepeatedPositions(FrontierOverlay.PathSegmentLayout segment,
                                                            int minZoom, int markerScale) {
        List<BlockPos> available = segment.repeatedMarkerPositions();
        double targetSpacing = PathRepeatedMarkerSelector.getTargetSpacing(
                minZoom, markerScale, segment.segmentSpacingMultiplier());
        int markerCount = PathRepeatedMarkerSelector.getMarkerCount(
                targetSpacing, segment.length(), available.size());
        List<BlockPos> selected = new ArrayList<>(markerCount);
        for (int ordinal = 1; ordinal <= markerCount; ordinal++) {
            selected.add(available.get(PathRepeatedMarkerSelector.getMarkerIndex(
                    ordinal, markerCount, available.size())));
        }
        return selected;
    }

    private static int indexOfPoint(List<MarkerOverlay> overlays, int x) {
        for (int index = 0; index < overlays.size(); index++) {
            MarkerOverlay overlay = overlays.get(index);
            if (overlay.getDisplayOrder() == 100 && overlay.getPoint().getX() == x) {
                return index;
            }
        }
        return -1;
    }

    private static void assertSameOverlays(List<MarkerOverlay> expected, List<MarkerOverlay> actual) {
        assertEquals(expected.size(), actual.size());
        for (int index = 0; index < expected.size(); index++) {
            assertSame(expected.get(index), actual.get(index));
        }
    }

    private record PathLayout(List<FrontierOverlay.PathPointLayout> points,
                              List<FrontierOverlay.PathSegmentLayout> segments) {
    }
}
