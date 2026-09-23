package games.alejandrocoria.mapfrontiers.common.territory.frontier;

import games.alejandrocoria.mapfrontiers.platform.services.WorldGeometry;
import games.alejandrocoria.mapfrontiers.testutil.PeriodicGeometry;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.geom.Path2D;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static games.alejandrocoria.mapfrontiers.common.territory.frontier.GeometryQueries.anyCopyInBounds;
import static games.alejandrocoria.mapfrontiers.common.territory.frontier.GeometryQueries.distanceToSegmentSquared;
import static games.alejandrocoria.mapfrontiers.common.territory.frontier.GeometryQueries.nearestSegmentCopy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeometryQueriesTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void doesNotInventASegmentBetweenIndependentlyWrappedEndpoints() {
        WorldGeometry world = new PeriodicGeometry(1024, 0);
        BlockPos start = point(480, 0), end = point(544, 0), query = point(0, 0);
        BlockPos copy = nearestSegmentCopy(world, query, start, end);
        assertEquals(480.0 * 480.0, distanceToSegmentSquared(copy, start, end));
        assertFalse(anyCopyInBounds(world, query, 478, 546, -2, 2,
                p -> distanceToSegmentSquared(p, start, end) <= 4));
    }

    @Test
    void preservesLongStoredSegmentsInBothAxes() {
        for (WorldGeometry world : worlds()) {
            BlockPos start = point(0, 0), end = point(768, 768), query = point(384, 384);
            assertEquals(query, nearestSegmentCopy(world, query, start, end));
            assertTrue(anyCopyInBounds(world, query, 0, 768, 0, 768,
                    p -> distanceToSegmentSquared(p, start, end) == 0));
        }
    }

    @Test
    void searchesMoreThanTheCopyClosestToTheBoundsCenter() {
        WorldGeometry world = new PeriodicGeometry(1024, 0);
        Set<ChunkPos> chunks = Set.of(new ChunkPos(0, 0), new ChunkPos(65, 1));
        assertTrue(anyCopyInBounds(world, point(8, 8), 0, 1055, 0, 31,
                p -> chunks.contains(new ChunkPos(p))));
        assertTrue(anyCopyInBounds(world, point(24, 24), 0, 1055, 0, 31,
                p -> chunks.contains(new ChunkPos(p))));
        assertFalse(anyCopyInBounds(world, point(8, 24), 0, 1055, 0, 31,
                p -> chunks.contains(new ChunkPos(p))));
    }

    @Test
    void polygonQueriesRecognizeEveryCopyWithoutMovingItsEdges() {
        Path2D polygon = new Path2D.Double();
        polygon.moveTo(1000, 1000);
        polygon.lineTo(1060, 1000);
        polygon.lineTo(1060, 1060);
        polygon.lineTo(1000, 1060);
        polygon.closePath();
        for (WorldGeometry world : worlds()) {
            int x = world.periodX() == 0 ? 1030 : 6;
            int z = world.periodZ() == 0 ? 1030 : 6;
            assertTrue(anyCopyInBounds(world, point(x, z), 1000, 1060, 1000, 1060,
                    p -> polygon.contains(p.getX() + 0.5, p.getZ() + 0.5)));
            assertTrue(anyCopyInBounds(world, point(x + 3 * world.periodX(), z - 2 * world.periodZ()),
                    1000, 1060, 1000, 1060, p -> polygon.contains(p.getX() + 0.5, p.getZ() + 0.5)));
        }
    }

    @Test
    void flatSegmentQueryReturnsInputAndBoundsShortCircuit() {
        BlockPos query = point(5, 6);
        assertSame(query, nearestSegmentCopy(WorldGeometry.FLAT, query, point(0, 0), point(10, 10)));
        AtomicInteger calls = new AtomicInteger();
        assertFalse(anyCopyInBounds(WorldGeometry.FLAT, query, 20, 30, 0, 10, p -> {
            calls.incrementAndGet();
            return true;
        }));
        assertEquals(0, calls.get());
    }

    @Test
    void handlesNegativeCopiesUnwrappedAxesAndInclusiveLimits() {
        WorldGeometry world = new PeriodicGeometry(1024, 0);
        assertTrue(anyCopyInBounds(world, point(0, -7), -2048, -2048, -7, -7,
                p -> p.equals(point(-2048, -7))));
        assertFalse(anyCopyInBounds(world, point(0, -7), -2048, -2048, 1017, 1017, p -> true));
        assertTrue(anyCopyInBounds(world, point(Integer.MAX_VALUE, 0),
                Integer.MAX_VALUE, (double) Integer.MAX_VALUE + 100, 0, 0, p -> true));
    }

    private static WorldGeometry[] worlds() {
        return new WorldGeometry[]{new PeriodicGeometry(1024, 0), new PeriodicGeometry(0, 1024),
                new PeriodicGeometry(1024, 1024)};
    }

    private static BlockPos point(int x, int z) { return new BlockPos(x, 70, z); }
}
