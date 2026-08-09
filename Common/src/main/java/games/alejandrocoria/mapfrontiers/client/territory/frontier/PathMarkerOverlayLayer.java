package games.alejandrocoria.mapfrontiers.client.territory.frontier;

import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.territory.overlay.MarkerOverlaySlot;
import games.alejandrocoria.mapfrontiers.client.territory.overlay.OverlayActivation;
import games.alejandrocoria.mapfrontiers.client.territory.overlay.OverlayDisplayState;
import games.alejandrocoria.mapfrontiers.client.territory.overlay.OverlayPublisher;
import games.alejandrocoria.mapfrontiers.client.territory.overlay.OverlayRefreshResult;
import games.alejandrocoria.mapfrontiers.client.territory.overlay.ReconciledOverlayList;
import journeymap.api.v2.client.display.Context;
import journeymap.api.v2.client.display.MarkerOverlay;
import journeymap.api.v2.client.model.MapImage;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Owns persistent marker slots for one path representation. Point and segment visuals live above the UI buckets so
 * every enabled UI can share the same immutable-by-convention MapImage without sharing overlay identities.
 */
final class PathMarkerOverlayLayer {
    private static final int REPEATED_MARKER_MIN_ZOOM = 2;
    private static final int REPEATED_MARKER_MAX_ZOOM = 16384;
    private static final List<Context.UI> UI_ORDER = List.of(
            Context.UI.Fullscreen,
            Context.UI.Minimap,
            Context.UI.Webmap);

    enum Mode {
        BASE(99, 100),
        HIGHLIGHT(100, 101);

        private final int repeatedDisplayOrder;
        private final int pointDisplayOrder;

        Mode(int repeatedDisplayOrder, int pointDisplayOrder) {
            this.repeatedDisplayOrder = repeatedDisplayOrder;
            this.pointDisplayOrder = pointDisplayOrder;
        }
    }

    record UiState(Context.UI ui, boolean enabled, Context.MapType[] mapTypes) {
        UiState {
            Objects.requireNonNull(ui, "ui");
            Objects.requireNonNull(mapTypes, "mapTypes");
        }
    }

    private final Mode mode;
    private final List<UiBucket> uiBuckets;
    private final List<VisualReference> pointVisuals = new ArrayList<>();
    private final List<VisualReference> segmentVisuals = new ArrayList<>();

    PathMarkerOverlayLayer(String modId, String layerName, OverlayPublisher publisher, Mode mode) {
        Objects.requireNonNull(modId, "modId");
        Objects.requireNonNull(layerName, "layerName");
        Objects.requireNonNull(publisher, "publisher");
        this.mode = Objects.requireNonNull(mode, "mode");

        List<UiBucket> buckets = new ArrayList<>(UI_ORDER.size());
        for (Context.UI ui : UI_ORDER) {
            buckets.add(new UiBucket(modId, layerName, publisher, ui, mode));
        }
        uiBuckets = List.copyOf(buckets);
    }

    void reconcile(List<FrontierOverlay.PathPointLayout> pointLayouts,
                   List<FrontierOverlay.PathSegmentLayout> segmentLayouts,
                   List<UiState> uiStates,
                   ResourceKey<Level> dimension,
                   int tint,
                   float opacity,
                   int markerSize,
                   int markerScale,
                   boolean visible,
                   OverlayRefreshResult result) {
        Objects.requireNonNull(pointLayouts, "pointLayouts");
        Objects.requireNonNull(segmentLayouts, "segmentLayouts");
        Objects.requireNonNull(uiStates, "uiStates");
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(result, "result");
        if (segmentLayouts.size() != Math.max(0, pointLayouts.size() - 1)) {
            throw new IllegalArgumentException("A path must have exactly one segment between adjacent points");
        }

        resizeVisuals(pointVisuals, pointLayouts.size());
        for (int index = 0; index < pointLayouts.size(); index++) {
            FrontierOverlay.PathPointLayout layout = pointLayouts.get(index);
            updateVisual(pointVisuals.get(index), layout.markerId(), layout.rotation(), tint, opacity, markerSize);
        }
        resizeVisuals(segmentVisuals, segmentLayouts.size());
        for (int index = 0; index < segmentLayouts.size(); index++) {
            FrontierOverlay.PathSegmentLayout layout = segmentLayouts.get(index);
            updateVisual(segmentVisuals.get(index), layout.segmentMarkerId(), layout.rotation(), tint, opacity, markerSize);
        }

        for (UiBucket bucket : uiBuckets) {
            UiState uiState = findUiState(uiStates, bucket.ui);
            if (uiState == null || !uiState.enabled()) {
                bucket.clear(result);
            } else {
                bucket.reconcile(pointLayouts, segmentLayouts, pointVisuals, segmentVisuals,
                        dimension, uiState.mapTypes(), markerScale, visible, result);
            }
        }
    }

    void setVisible(boolean visible, OverlayRefreshResult result) {
        Objects.requireNonNull(result, "result");
        for (UiBucket bucket : uiBuckets) {
            bucket.setVisible(visible, result);
        }
    }

    void clear(OverlayRefreshResult result) {
        Objects.requireNonNull(result, "result");
        for (UiBucket bucket : uiBuckets) {
            bucket.clear(result);
        }
        pointVisuals.clear();
        segmentVisuals.clear();
    }

    List<MarkerOverlay> getOverlays() {
        List<MarkerOverlay> overlays = new ArrayList<>();
        for (UiBucket bucket : uiBuckets) {
            bucket.addOverlays(overlays);
        }
        return overlays;
    }

    private @Nullable UiState findUiState(List<UiState> uiStates, Context.UI ui) {
        for (UiState uiState : uiStates) {
            if (uiState.ui() == ui) {
                return uiState;
            }
        }
        return null;
    }

    private static void resizeVisuals(List<VisualReference> visuals, int desiredSize) {
        while (visuals.size() < desiredSize) {
            visuals.add(new VisualReference());
        }
        if (visuals.size() > desiredSize) {
            visuals.subList(desiredSize, visuals.size()).clear();
        }
    }

    private void updateVisual(VisualReference visual, Identifier markerId, float rotation,
                              int tint, float opacity, int markerSize) {
        PathMarkerCatalog.Entry entry = PathMarkerCatalog.get(markerId);
        if (entry == null || entry.texture() == null) {
            visual.clear();
            return;
        }

        Identifier texture;
        int effectiveTint;
        float effectiveOpacity;
        if (mode == Mode.HIGHLIGHT) {
            texture = PathMarkerCatalog.getHighlightTexture(markerId);
            effectiveTint = ColorConstants.TEXTURE_TINT_NONE;
            effectiveOpacity = 1.f;
        } else {
            texture = entry.texture();
            effectiveTint = tint;
            effectiveOpacity = opacity;
        }
        if (texture == null) {
            visual.clear();
            return;
        }

        int effectiveRotation = entry.directional() ? Math.round(rotation) : 0;
        visual.update(texture, effectiveTint, effectiveOpacity, markerSize, effectiveRotation);
    }

    private static MapImage createImage(PathMarkerVisualKey key) {
        MapImage image = new MapImage(key.texture(), 0, 0,
                MarkerImageConstants.TEXTURE_SIZE, MarkerImageConstants.TEXTURE_SIZE,
                key.tint(), key.opacity());
        image.setDisplayWidth(key.markerSize());
        image.setDisplayHeight(key.markerSize());
        image.setAnchorX(key.markerSize() / 2.0);
        image.setAnchorY(key.markerSize() / 2.0);
        if (key.rotation() != 0) {
            image.setRotation(key.rotation());
        }
        return image;
    }

    private record PathMarkerVisualKey(Identifier texture, int tint, float opacity, int markerSize, int rotation) {
    }

    private static final class VisualReference {
        private @Nullable PathMarkerVisualKey key;
        private @Nullable MapImage image;

        private void update(Identifier texture, int tint, float opacity, int markerSize, int rotation) {
            if (key == null
                    || !texture.equals(key.texture())
                    || tint != key.tint()
                    || Float.compare(opacity, key.opacity()) != 0
                    || markerSize != key.markerSize()
                    || rotation != key.rotation()) {
                key = new PathMarkerVisualKey(texture, tint, opacity, markerSize, rotation);
                image = createImage(key);
            }
        }

        private void clear() {
            key = null;
            image = null;
        }
    }

    private static final class UiBucket {
        private final Context.UI ui;
        private final String layerName;
        private final OverlayPublisher publisher;
        private final Mode mode;
        private final String modId;
        private final List<PointBucket> pointBuckets = new ArrayList<>();
        private final List<SegmentBucket> segmentBuckets = new ArrayList<>();
        private @Nullable OverlayActivation activation;
        private @Nullable ResourceKey<Level> pointDisplayDimension;
        private @Nullable OverlayActivation pointDisplayActivation;
        private @Nullable OverlayDisplayState pointDisplayState;

        private UiBucket(String modId, String layerName, OverlayPublisher publisher, Context.UI ui, Mode mode) {
            this.modId = modId;
            this.layerName = layerName;
            this.publisher = publisher;
            this.ui = ui;
            this.mode = mode;
        }

        private void reconcile(List<FrontierOverlay.PathPointLayout> pointLayouts,
                               List<FrontierOverlay.PathSegmentLayout> segmentLayouts,
                               List<VisualReference> pointVisuals,
                               List<VisualReference> segmentVisuals,
                               ResourceKey<Level> dimension,
                               Context.MapType[] mapTypes,
                               int markerScale,
                               boolean visible,
                               OverlayRefreshResult result) {
            OverlayActivation desiredActivation = OverlayActivation.of(ui, mapTypes);
            if (!desiredActivation.equals(activation)) {
                activation = desiredActivation;
            }

            resizePointBuckets(pointLayouts.size(), result);
            resizeSegmentBuckets(segmentLayouts.size(), result);
            OverlayDisplayState desiredPointDisplayState = getPointDisplayState(dimension);
            for (int index = 0; index < segmentLayouts.size(); index++) {
                segmentBuckets.get(index).reconcile(segmentLayouts.get(index), segmentVisuals.get(index),
                        dimension, activation, markerScale, visible, result);
                pointBuckets.get(index).reconcile(pointLayouts.get(index).pos(), pointVisuals.get(index),
                        desiredPointDisplayState, visible, result);
            }
            if (!pointLayouts.isEmpty()) {
                int lastIndex = pointLayouts.size() - 1;
                pointBuckets.get(lastIndex).reconcile(pointLayouts.get(lastIndex).pos(), pointVisuals.get(lastIndex),
                        desiredPointDisplayState, visible, result);
            }
        }

        private OverlayDisplayState getPointDisplayState(ResourceKey<Level> dimension) {
            if (pointDisplayState == null
                    || !dimension.equals(pointDisplayDimension)
                    || !Objects.equals(activation, pointDisplayActivation)) {
                pointDisplayDimension = dimension;
                pointDisplayActivation = activation;
                pointDisplayState = new OverlayDisplayState(dimension, Objects.requireNonNull(activation),
                        0, 0, mode.pointDisplayOrder, null, null, null, null, null, null);
            }
            return Objects.requireNonNull(pointDisplayState);
        }

        private void resizePointBuckets(int desiredSize, OverlayRefreshResult result) {
            while (pointBuckets.size() < desiredSize) {
                pointBuckets.add(new PointBucket(modId, layerName, publisher));
            }
            for (int index = desiredSize; index < pointBuckets.size(); index++) {
                pointBuckets.get(index).clear(result);
            }
            if (pointBuckets.size() > desiredSize) {
                pointBuckets.subList(desiredSize, pointBuckets.size()).clear();
            }
        }

        private void resizeSegmentBuckets(int desiredSize, OverlayRefreshResult result) {
            while (segmentBuckets.size() < desiredSize) {
                segmentBuckets.add(new SegmentBucket(modId, layerName, publisher, mode.repeatedDisplayOrder));
            }
            for (int index = desiredSize; index < segmentBuckets.size(); index++) {
                segmentBuckets.get(index).clear(result);
            }
            if (segmentBuckets.size() > desiredSize) {
                segmentBuckets.subList(desiredSize, segmentBuckets.size()).clear();
            }
        }

        private void setVisible(boolean visible, OverlayRefreshResult result) {
            for (PointBucket pointBucket : pointBuckets) {
                pointBucket.setVisible(visible, result);
            }
            for (SegmentBucket segmentBucket : segmentBuckets) {
                segmentBucket.setVisible(visible, result);
            }
        }

        private void clear(OverlayRefreshResult result) {
            for (PointBucket pointBucket : pointBuckets) {
                pointBucket.clear(result);
            }
            for (SegmentBucket segmentBucket : segmentBuckets) {
                segmentBucket.clear(result);
            }
            pointBuckets.clear();
            segmentBuckets.clear();
            activation = null;
            pointDisplayDimension = null;
            pointDisplayActivation = null;
            pointDisplayState = null;
        }

        private void addOverlays(List<MarkerOverlay> overlays) {
            // Preview consumers expect the legacy order: segment bands, its starting point, then the final point.
            for (int index = 0; index < segmentBuckets.size(); index++) {
                segmentBuckets.get(index).addOverlays(overlays);
                pointBuckets.get(index).addOverlay(overlays);
            }
            if (!pointBuckets.isEmpty()) {
                pointBuckets.getLast().addOverlay(overlays);
            }
        }
    }

    private static final class PointBucket {
        private final String modId;
        private final String layerName;
        private final OverlayPublisher publisher;
        private @Nullable MarkerOverlaySlot slot;

        private PointBucket(String modId, String layerName, OverlayPublisher publisher) {
            this.modId = modId;
            this.layerName = layerName;
            this.publisher = publisher;
        }

        private void reconcile(BlockPos point, VisualReference visual, OverlayDisplayState displayState,
                               boolean visible, OverlayRefreshResult result) {
            if (visual.key == null || visual.image == null) {
                clear(result);
                return;
            }
            if (slot == null) {
                slot = new MarkerOverlaySlot(modId, publisher);
            }
            slot.reconcile(point, visual.image, visual.key, displayState, visible, result, layerName);
        }

        private void setVisible(boolean visible, OverlayRefreshResult result) {
            if (slot != null) {
                slot.reconcileVisibility(visible, result, layerName);
            }
        }

        private void clear(OverlayRefreshResult result) {
            if (slot != null) {
                slot.retire(result, layerName);
                slot = null;
            }
        }

        private void addOverlay(List<MarkerOverlay> overlays) {
            if (slot != null && slot.getOverlay() != null) {
                overlays.add(slot.getOverlay());
            }
        }
    }

    private static final class SegmentBucket {
        private final List<BandBucket> bands;

        private SegmentBucket(String modId, String layerName, OverlayPublisher publisher, int displayOrder) {
            List<BandBucket> createdBands = new ArrayList<>();
            for (int minZoom = REPEATED_MARKER_MIN_ZOOM; minZoom <= REPEATED_MARKER_MAX_ZOOM; minZoom *= 2) {
                int maxZoom = minZoom == REPEATED_MARKER_MAX_ZOOM ? 0 : minZoom * 2 - 1;
                createdBands.add(new BandBucket(modId, layerName, publisher, minZoom, maxZoom, displayOrder));
            }
            bands = List.copyOf(createdBands);
        }

        private void reconcile(FrontierOverlay.PathSegmentLayout layout, VisualReference visual,
                               ResourceKey<Level> dimension, OverlayActivation activation,
                               int markerScale, boolean visible, OverlayRefreshResult result) {
            for (BandBucket band : bands) {
                band.reconcile(layout, visual, dimension, activation, markerScale, visible, result);
            }
        }

        private void setVisible(boolean visible, OverlayRefreshResult result) {
            for (BandBucket band : bands) {
                band.setVisible(visible, result);
            }
        }

        private void clear(OverlayRefreshResult result) {
            for (BandBucket band : bands) {
                band.clear(result);
            }
        }

        private void addOverlays(List<MarkerOverlay> overlays) {
            for (BandBucket band : bands) {
                band.addOverlays(overlays);
            }
        }
    }

    private static final class BandBucket {
        private final String layerName;
        private final int minZoom;
        private final int maxZoom;
        private final int displayOrder;
        private final ReconciledOverlayList<MarkerOverlaySlot> slots;
        private @Nullable ResourceKey<Level> displayDimension;
        private @Nullable OverlayActivation displayActivation;
        private @Nullable OverlayDisplayState displayState;

        private BandBucket(String modId, String layerName, OverlayPublisher publisher,
                           int minZoom, int maxZoom, int displayOrder) {
            this.layerName = layerName;
            this.minZoom = minZoom;
            this.maxZoom = maxZoom;
            this.displayOrder = displayOrder;
            slots = new ReconciledOverlayList<>(() -> new MarkerOverlaySlot(modId, publisher));
        }

        private void reconcile(FrontierOverlay.PathSegmentLayout layout, VisualReference visual,
                               ResourceKey<Level> dimension, OverlayActivation activation,
                               int markerScale, boolean visible, OverlayRefreshResult result) {
            List<BlockPos> positions = layout.repeatedMarkerPositions();
            if (visual.key == null || visual.image == null || positions.isEmpty()) {
                clear(result);
                return;
            }

            double targetSpacing = PathRepeatedMarkerSelector.getTargetSpacing(
                    minZoom, markerScale, layout.segmentSpacingMultiplier());
            int markerCount = PathRepeatedMarkerSelector.getMarkerCount(targetSpacing, layout.length(), positions.size());
            if (markerCount == 0) {
                clear(result);
                return;
            }

            if (displayState == null
                    || !dimension.equals(displayDimension)
                    || !activation.equals(displayActivation)) {
                displayDimension = dimension;
                displayActivation = activation;
                displayState = new OverlayDisplayState(dimension, activation, minZoom, maxZoom,
                        displayOrder, null, null, null, null, null, null);
            }
            OverlayDisplayState desiredDisplayState = Objects.requireNonNull(displayState);

            // Each band owns its reconcile boundary; finishing it must never retire slots from sibling bands.
            slots.beginReconcile();
            int previousIndex = -1;
            for (int markerOrdinal = 1; markerOrdinal <= markerCount; markerOrdinal++) {
                int markerIndex = PathRepeatedMarkerSelector.getMarkerIndex(markerOrdinal, markerCount, positions.size());
                if (markerIndex == previousIndex) {
                    continue;
                }
                previousIndex = markerIndex;
                slots.acquire().reconcile(positions.get(markerIndex), visual.image, visual.key,
                        desiredDisplayState, visible, result, layerName);
            }
            slots.finishReconcile(result, layerName);
        }

        private void setVisible(boolean visible, OverlayRefreshResult result) {
            for (int index = 0; index < slots.size(); index++) {
                slots.get(index).reconcileVisibility(visible, result, layerName);
            }
        }

        private void clear(OverlayRefreshResult result) {
            slots.clear(result, layerName);
            displayDimension = null;
            displayActivation = null;
            displayState = null;
        }

        private void addOverlays(List<MarkerOverlay> overlays) {
            for (int index = 0; index < slots.size(); index++) {
                MarkerOverlay overlay = slots.get(index).getOverlay();
                if (overlay != null) {
                    overlays.add(overlay);
                }
            }
        }
    }

}
