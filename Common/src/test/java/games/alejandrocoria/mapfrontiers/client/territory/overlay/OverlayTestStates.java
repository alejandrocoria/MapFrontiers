package games.alejandrocoria.mapfrontiers.client.territory.overlay;

import journeymap.api.v2.client.display.Context;
import journeymap.api.v2.client.model.MapImage;
import journeymap.api.v2.client.model.MapPolygon;
import journeymap.api.v2.client.model.ShapeProperties;
import journeymap.api.v2.client.model.TextProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.List;

final class OverlayTestStates {
    private static final ResourceKey<Level> TEST_DIMENSION = ResourceKey.create(Registries.DIMENSION,
            Identifier.fromNamespaceAndPath("mapfrontiers", "test_dimension"));

    private OverlayTestStates() {
    }

    static MarkerOverlayState marker(int x, String visualKey) {
        MapImage icon = new MapImage(Identifier.fromNamespaceAndPath("mapfrontiers", "textures/test/" + visualKey + ".png"), 16, 16);
        return new MarkerOverlayState(new BlockPos(x, 70, x), icon, visualKey,
                displayState(x, "marker-" + x));
    }

    static MarkerOverlayState bareMarker(int x, String visualKey) {
        MapImage icon = new MapImage(Identifier.fromNamespaceAndPath("mapfrontiers", "textures/test/" + visualKey + ".png"), 16, 16);
        OverlayDisplayState displayState = new OverlayDisplayState(TEST_DIMENSION,
                OverlayActivation.of(Context.UI.Minimap, Context.MapType.Topo),
                0, 0, x, null, null, null, null, null, null);
        return new MarkerOverlayState(new BlockPos(x, 70, x), icon, visualKey, displayState);
    }

    static MarkerOverlayState labelMarker(int x, MapImage icon, Object visualKey, String label,
                                          TextProperties textProperties, Object textPropertiesKey) {
        OverlayDisplayState displayState = new OverlayDisplayState(TEST_DIMENSION,
                OverlayActivation.of(Context.UI.Fullscreen, Context.MapType.Day),
                textProperties.getMinZoom(), textProperties.getMaxZoom(), 0,
                "frontier", null, label, textProperties, textPropertiesKey, null);
        return new MarkerOverlayState(new BlockPos(x, 70, x), icon, visualKey, displayState);
    }

    static PolygonOverlayState polygon(int offset, int geometryRevision, int styleRevision) {
        return createPolygon(offset, geometryRevision, styleRevision, true,
                displayState(offset, "polygon-" + offset));
    }

    static PolygonOverlayState barePolygon(int offset, Object geometryKey, Object styleKey, boolean withHole,
                                           OverlayActivation activation, int minZoom, int maxZoom) {
        OverlayDisplayState displayState = new OverlayDisplayState(TEST_DIMENSION, activation,
                minZoom, maxZoom, 0, null, null, null, null, null, null);
        return createPolygon(offset, geometryKey, styleKey, withHole, displayState);
    }

    private static PolygonOverlayState createPolygon(int offset, Object geometryKey, Object styleKey,
                                                     boolean withHole, OverlayDisplayState displayState) {
        MapPolygon outerArea = new MapPolygon(
                new BlockPos(offset, 70, offset),
                new BlockPos(offset + 4, 70, offset),
                new BlockPos(offset + 4, 70, offset + 4),
                new BlockPos(offset, 70, offset + 4));
        List<MapPolygon> holes = withHole ? List.of(new MapPolygon(
                new BlockPos(offset + 1, 70, offset + 1),
                new BlockPos(offset + 2, 70, offset + 1),
                new BlockPos(offset + 2, 70, offset + 2))) : null;
        ShapeProperties shapeProperties = new ShapeProperties()
                .setStrokeColor(styleKey.hashCode())
                .setFillColor(styleKey.hashCode() + 1)
                .setStrokeOpacity(0.8f)
                .setFillOpacity(0.3f)
                .setStrokeWidth(2f);
        return new PolygonOverlayState(outerArea, holes, shapeProperties, geometryKey, styleKey, displayState);
    }

    private static OverlayDisplayState displayState(int revision, String label) {
        TextProperties textProperties = new TextProperties()
                .setScale(1f + revision)
                .setColor(0x123456 + revision)
                .setMinZoom(2)
                .setMaxZoom(4096);
        return new OverlayDisplayState(TEST_DIMENSION,
                OverlayActivation.of(new Context.UI[]{Context.UI.Fullscreen, Context.UI.Webmap},
                        new Context.MapType[]{Context.MapType.Day, Context.MapType.Night}),
                2, 4096, revision, "test", "title-" + revision, label,
                textProperties, revision, null);
    }
}
