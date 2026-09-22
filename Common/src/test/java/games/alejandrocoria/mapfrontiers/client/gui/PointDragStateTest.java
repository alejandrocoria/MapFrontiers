package games.alejandrocoria.mapfrontiers.client.gui;

import games.alejandrocoria.mapfrontiers.platform.services.WorldGeometry;
import games.alejandrocoria.mapfrontiers.testutil.PeriodicGeometry;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class PointDragStateTest {
    @Test
    void snappingDoesNotTrapTheCursorOrLoseMovement() {
        WorldGeometry world = new PeriodicGeometry(1024, 1024);
        PointDragState drag = new PointDragState(point(510, 510));
        for (int coordinate = 511; coordinate <= 525; coordinate++) {
            int canonical = coordinate > 511 ? coordinate - 1024 : coordinate;
            BlockPos unsnapped = drag.update(point(canonical, canonical), world);
            // Applying a snapped position must not feed back into the cursor state.
            BlockPos applied = coordinate < 520 ? point(510, 510) : unsnapped;
            assertEquals(point(coordinate, coordinate), unsnapped);
            if (coordinate >= 520) assertEquals(point(coordinate, coordinate), applied);
        }
        assertEquals(point(510, 510), drag.update(point(510, 510), world));
    }

    @Test
    void startsInTheSelectedPointsStoredCopyAndDoesNotWrapTheOtherAxis() {
        PointDragState drag = new PointDragState(point(1530, 1530));
        assertEquals(point(1538, -510), drag.update(point(-510, -510), new PeriodicGeometry(1024, 0)));
        drag = new PointDragState(point(1530, 1530));
        assertEquals(point(-510, 1538), drag.update(point(-510, -510), new PeriodicGeometry(0, 1024)));
    }

    @Test
    void flatMovementUsesAbsoluteCursorCoordinates() {
        PointDragState drag = new PointDragState(point(100, 100));
        BlockPos cursor = point(105, 106);
        assertSame(cursor, drag.update(cursor, WorldGeometry.FLAT));
        assertEquals(point(110, 111), drag.update(point(110, 111), WorldGeometry.FLAT));
    }

    private static BlockPos point(int x, int z) { return new BlockPos(x, 70, z); }
}
