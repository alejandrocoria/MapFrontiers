package games.alejandrocoria.mapfrontiers.common.territory.frontier;

import games.alejandrocoria.mapfrontiers.platform.services.WorldGeometry;
import net.minecraft.core.BlockPos;

import java.util.function.Predicate;

/** Queries copies of a position without changing the stored, planar geometry. */
public final class GeometryQueries {
    private GeometryQueries() {}

    public static boolean anyCopyInBounds(WorldGeometry geometry, BlockPos point,
            double minX, double maxX, double minZ, double maxZ, Predicate<BlockPos> test) {
        minX = Math.max(minX, Integer.MIN_VALUE);
        maxX = Math.min(maxX, Integer.MAX_VALUE);
        minZ = Math.max(minZ, Integer.MIN_VALUE);
        maxZ = Math.min(maxZ, Integer.MAX_VALUE);
        int periodX = geometry.periodX();
        int periodZ = geometry.periodZ();
        long firstX = firstCopy(point.getX(), minX, periodX);
        long firstZ = firstCopy(point.getZ(), minZ, periodZ);
        for (long x = firstX; x >= minX && x <= maxX; x += periodX) {
            for (long z = firstZ; z >= minZ && z <= maxZ; z += periodZ) {
                if (test.test(new BlockPos((int) x, point.getY(), (int) z))) {
                    return true;
                }
                if (periodZ == 0) break;
            }
            if (periodX == 0) break;
        }
        return false;
    }

    private static long firstCopy(int coordinate, double minimum, int period) {
        return period == 0 ? coordinate : coordinate + (long) Math.ceil((minimum - coordinate) / period) * period;
    }

    public static BlockPos nearestSegmentCopy(WorldGeometry geometry, BlockPos point, BlockPos start, BlockPos end) {
        if (!geometry.hasWrappedAxes()) {
            return point;
        }

        // An endpoint supplies an upper bound; every better copy is inside this expanded segment box.
        class Nearest {
            BlockPos position = geometry.nearestCopy(start, point);
            double distance = distanceToSegmentSquared(position, start, end);
        }
        Nearest nearest = new Nearest();
        double radius = Math.sqrt(nearest.distance);
        anyCopyInBounds(geometry, point,
                Math.min(start.getX(), end.getX()) - radius, Math.max(start.getX(), end.getX()) + radius,
                Math.min(start.getZ(), end.getZ()) - radius, Math.max(start.getZ(), end.getZ()) + radius, copy -> {
                    double distance = distanceToSegmentSquared(copy, start, end);
                    if (distance < nearest.distance) {
                        nearest.position = copy;
                        nearest.distance = distance;
                    }
                    return nearest.distance == 0.0;
                });
        return nearest.position;
    }

    public static double distanceToSegmentSquared(BlockPos point, BlockPos start, BlockPos end) {
        double edgeX = (double) end.getX() - start.getX();
        double edgeZ = (double) end.getZ() - start.getZ();
        double lengthSquared = edgeX * edgeX + edgeZ * edgeZ;
        double projection = lengthSquared == 0.0 ? 0.0 : Math.clamp(
                (((double) point.getX() - start.getX()) * edgeX
                + ((double) point.getZ() - start.getZ()) * edgeZ) / lengthSquared, 0.0, 1.0);
        double dx = point.getX() - (start.getX() + projection * edgeX);
        double dz = point.getZ() - (start.getZ() + projection * edgeZ);
        return dx * dx + dz * dz;
    }

    public record Insertion(int index, BlockPos position) {}
}
