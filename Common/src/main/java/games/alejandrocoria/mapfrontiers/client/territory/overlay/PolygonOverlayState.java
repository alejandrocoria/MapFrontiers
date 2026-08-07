package games.alejandrocoria.mapfrontiers.client.territory.overlay;

import journeymap.api.v2.client.model.MapPolygon;
import journeymap.api.v2.client.model.ShapeProperties;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

/**
 * Desired polygon values. Geometry and style keys version the mutable JourneyMap model objects.
 */
public final class PolygonOverlayState {
    private final MapPolygon outerArea;
    private final @Nullable List<MapPolygon> holes;
    private final ShapeProperties shapeProperties;
    private final Object geometryKey;
    private final Object styleKey;
    private final OverlayDisplayState displayState;

    public PolygonOverlayState(MapPolygon outerArea, @Nullable List<MapPolygon> holes,
                               ShapeProperties shapeProperties, Object geometryKey, Object styleKey,
                               OverlayDisplayState displayState) {
        this.outerArea = Objects.requireNonNull(outerArea, "outerArea");
        this.holes = holes;
        this.shapeProperties = Objects.requireNonNull(shapeProperties, "shapeProperties");
        this.geometryKey = Objects.requireNonNull(geometryKey, "geometryKey");
        this.styleKey = Objects.requireNonNull(styleKey, "styleKey");
        this.displayState = Objects.requireNonNull(displayState, "displayState");
    }

    boolean sameAs(PolygonOverlayState other) {
        return geometryKey.equals(other.geometryKey)
                && styleKey.equals(other.styleKey)
                && displayState.sameAs(other.displayState);
    }

    MapPolygon getOuterArea() {
        return outerArea;
    }

    @Nullable List<MapPolygon> getHoles() {
        return holes;
    }

    ShapeProperties getShapeProperties() {
        return shapeProperties;
    }

    OverlayDisplayState getDisplayState() {
        return displayState;
    }
}
