package games.alejandrocoria.mapfrontiers.client.territory.collection;

import journeymap.api.v2.client.model.MapPolygon;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Path2D;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CollectionOverlayGeometryTest {
    @Test
    void mergeTouchingRings_sharedEdges_reconstructsSingleBoundary() {
        CollectionOverlay.RingPath left = ring(0, 0, 0, 4, 4, 4, 4, 0);
        CollectionOverlay.RingPath right = ring(4, 0, 4, 4, 8, 4, 8, 0);

        List<CollectionOverlay.RingPath> merged = CollectionOverlay.mergeTouchingRings(List.of(left, right));

        Area expected = new Area(left.path());
        expected.add(new Area(right.path()));
        assertEquals(1, merged.size());
        assertTrue(areasEqual(merged.get(0).area(), expected));
    }

    @Test
    void mergeTouchingRings_disconnectedComponents_keepsSeparateBoundaries() {
        CollectionOverlay.RingPath left = ring(0, 0, 0, 4, 4, 4, 4, 0);
        CollectionOverlay.RingPath right = ring(8, 0, 8, 4, 12, 4, 12, 0);

        List<CollectionOverlay.RingPath> merged = CollectionOverlay.mergeTouchingRings(List.of(left, right));

        assertEquals(2, merged.size());
    }

    @Test
    void mergeTouchingRings_pointContact_keepsOriginalBoundaries() {
        CollectionOverlay.RingPath lowerLeft = ring(0, 0, 0, 4, 4, 4, 4, 0);
        CollectionOverlay.RingPath upperRight = ring(4, 4, 4, 8, 8, 8, 8, 4);

        List<CollectionOverlay.RingPath> merged = CollectionOverlay.mergeTouchingRings(
                List.of(lowerLeft, upperRight));

        assertEquals(2, merged.size());
        assertTrue(areasEqual(merged.get(0).area(), lowerLeft.area()));
        assertTrue(areasEqual(merged.get(1).area(), upperRight.area()));
    }

    @Test
    void extractGeometryIslands_overlappingSources_reconstructsSingleBoundary() {
        MapPolygon firstPolygon = polygon(0, 0, 6, 0, 6, 6, 0, 6);
        MapPolygon secondPolygon = polygon(4, 0, 10, 0, 10, 6, 4, 6);
        Area firstArea = polygonArea(firstPolygon);
        Area secondArea = polygonArea(secondPolygon);
        Area unionArea = new Area(firstArea);
        unionArea.add(secondArea);

        List<CollectionOverlay.CollectionGeometryIsland> islands = CollectionOverlay.extractGeometryIslands(unionArea,
                List.of(source(firstArea), source(secondArea)));

        assertEquals(1, islands.size());
        assertTrue(areasEqual(polygonArea(islands.get(0).getRenderGeometry().polygon()), unionArea));
    }

    private static CollectionOverlay.CollectionSourceIsland source(Area area) {
        return new CollectionOverlay.CollectionSourceIsland(area, 0);
    }

    private static boolean areasEqual(Area first, Area second) {
        Area difference = new Area(first);
        difference.exclusiveOr(second);
        return difference.isEmpty();
    }

    private static CollectionOverlay.RingPath ring(double... coordinates) {
        java.util.ArrayList<Point2D.Double> points = new java.util.ArrayList<>();
        for (int i = 0; i < coordinates.length; i += 2) {
            points.add(new Point2D.Double(coordinates[i], coordinates[i + 1]));
        }
        return CollectionOverlay.createRingPath(points);
    }

    private static MapPolygon polygon(int... coordinates) {
        java.util.ArrayList<BlockPos> points = new java.util.ArrayList<>();
        for (int i = 0; i < coordinates.length; i += 2) {
            points.add(new BlockPos(coordinates[i], 70, coordinates[i + 1]));
        }
        return new MapPolygon(points);
    }

    private static Area polygonArea(MapPolygon polygon) {
        List<BlockPos> points = polygon.getPoints();
        Path2D.Double path = new Path2D.Double(Path2D.WIND_NON_ZERO);
        BlockPos first = points.get(0);
        path.moveTo(first.getX(), first.getZ());
        for (int i = 1; i < points.size(); i++) {
            BlockPos point = points.get(i);
            path.lineTo(point.getX(), point.getZ());
        }
        path.closePath();
        return new Area(path);
    }
}
