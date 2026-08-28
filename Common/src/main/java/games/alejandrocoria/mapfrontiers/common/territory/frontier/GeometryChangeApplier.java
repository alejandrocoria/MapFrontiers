package games.alejandrocoria.mapfrontiers.common.territory.frontier;

import net.minecraft.core.BlockPos;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.List;

@ParametersAreNonnullByDefault
final class GeometryChangeApplier {
    static void apply(FrontierData frontier, List<GeometryChange> changes) {
        for (GeometryChange change : changes) {
            apply(frontier, change);
        }
    }

    private static void apply(FrontierData frontier, GeometryChange change) {
        switch (change) {
            case GeometryChange.InsertPathPointAt value -> {
                requireShape(frontier, FrontierShape.Path, change);
                requireInsertIndex(value.index(), frontier.getPointCount(), change);
                frontier.addPoint(normalize(value.point()), value.index());
            }
            case GeometryChange.InsertPathPointBeforeFirst value -> {
                requireShape(frontier, FrontierShape.Path, change);
                frontier.addPoint(normalize(value.point()), 0);
            }
            case GeometryChange.InsertPathPointAfterLast value -> {
                requireShape(frontier, FrontierShape.Path, change);
                frontier.addPoint(normalize(value.point()));
            }
            case GeometryChange.InsertPathPointAutomatically value -> {
                requireShape(frontier, FrontierShape.Path, change);
                BlockPos point = normalize(value.point());
                frontier.addPoint(point, getAutomaticPathInsertIndex(frontier.getPoints(), point));
            }
            case GeometryChange.SetPathPointAt value -> {
                requireShape(frontier, FrontierShape.Path, change);
                requireExistingIndex(value.index(), frontier.getPointCount(), change);
                frontier.movePoint(normalize(value.point()), value.index());
            }
            case GeometryChange.RemovePathPointAt value -> {
                requireShape(frontier, FrontierShape.Path, change);
                requireExistingIndex(value.index(), frontier.getPointCount(), change);
                frontier.removePoint(value.index());
            }
            case GeometryChange.ReversePath ignored -> {
                requireShape(frontier, FrontierShape.Path, change);
                synchronized (frontier.points) {
                    Collections.reverse(frontier.points);
                }
                frontier.invalidateGeometryHash();
            }
            case GeometryChange.InsertVertexAt value -> {
                requireShape(frontier, FrontierShape.Vertex, change);
                requireInsertIndex(value.index(), frontier.getVertexCount(), change);
                frontier.addVertex(normalize(value.vertex()), value.index());
            }
            case GeometryChange.InsertVertexAutomatically value -> {
                requireShape(frontier, FrontierShape.Vertex, change);
                BlockPos vertex = normalize(value.vertex());
                frontier.addVertex(vertex, getAutomaticVertexInsertIndex(frontier.getVertices(), vertex));
            }
            case GeometryChange.SetVertexAt value -> {
                requireShape(frontier, FrontierShape.Vertex, change);
                requireExistingIndex(value.index(), frontier.getVertexCount(), change);
                frontier.moveVertex(normalize(value.vertex()), value.index());
            }
            case GeometryChange.RemoveVertexAt value -> {
                requireShape(frontier, FrontierShape.Vertex, change);
                requireExistingIndex(value.index(), frontier.getVertexCount(), change);
                frontier.removeVertex(value.index());
            }
            case GeometryChange.AddChunks value -> {
                requireShape(frontier, FrontierShape.Chunk, change);
                value.chunks().forEach(frontier::addChunk);
            }
            case GeometryChange.RemoveChunks value -> {
                requireShape(frontier, FrontierShape.Chunk, change);
                value.chunks().forEach(frontier::removeChunk);
            }
        }
    }

    static int getAutomaticPathInsertIndex(List<BlockPos> points, BlockPos point) {
        if (points.isEmpty()) {
            return 0;
        }
        if (points.size() == 1) {
            return 1;
        }

        double bestSegmentDistance = Double.POSITIVE_INFINITY;
        int bestSegmentInsertIndex = -1;
        for (int i = 0; i < points.size() - 1; ++i) {
            double distance = distanceToSegmentSquared(point, points.get(i), points.get(i + 1));
            if (distance < bestSegmentDistance) {
                bestSegmentDistance = distance;
                bestSegmentInsertIndex = i + 1;
            }
        }

        double startDistance = distanceSquared(point, points.getFirst());
        double endDistance = distanceSquared(point, points.getLast());
        if (bestSegmentDistance < Math.min(startDistance, endDistance)) {
            return bestSegmentInsertIndex;
        }
        return startDistance < endDistance ? 0 : points.size();
    }

    static int getAutomaticVertexInsertIndex(List<BlockPos> vertices, BlockPos vertex) {
        if (vertices.isEmpty()) {
            return 0;
        }
        if (vertices.size() == 1) {
            return 1;
        }

        double bestDistance = Double.POSITIVE_INFINITY;
        int bestInsertIndex = 1;
        for (int i = 0; i < vertices.size(); ++i) {
            BlockPos edgeStart = vertices.get(i);
            BlockPos edgeEnd = vertices.get((i + 1) % vertices.size());
            double distance = distanceToSegmentSquared(vertex, edgeStart, edgeEnd);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestInsertIndex = i + 1;
            }
        }
        return bestInsertIndex;
    }

    private static double distanceToSegmentSquared(BlockPos point, BlockPos edgeStart, BlockPos edgeEnd) {
        double edgeX = (double) edgeEnd.getX() - edgeStart.getX();
        double edgeZ = (double) edgeEnd.getZ() - edgeStart.getZ();
        double edgeLengthSquared = edgeX * edgeX + edgeZ * edgeZ;
        if (edgeLengthSquared == 0.0) {
            return distanceSquared(point, edgeStart);
        }

        double pointX = (double) point.getX() - edgeStart.getX();
        double pointZ = (double) point.getZ() - edgeStart.getZ();
        double projection = Math.max(0.0, Math.min(1.0, (pointX * edgeX + pointZ * edgeZ) / edgeLengthSquared));
        double closestX = edgeStart.getX() + projection * edgeX;
        double closestZ = edgeStart.getZ() + projection * edgeZ;
        double deltaX = point.getX() - closestX;
        double deltaZ = point.getZ() - closestZ;
        return deltaX * deltaX + deltaZ * deltaZ;
    }

    private static double distanceSquared(BlockPos left, BlockPos right) {
        double deltaX = (double) left.getX() - right.getX();
        double deltaZ = (double) left.getZ() - right.getZ();
        return deltaX * deltaX + deltaZ * deltaZ;
    }

    private static BlockPos normalize(BlockPos point) {
        return point.atY(70);
    }

    private static void requireShape(FrontierData frontier, FrontierShape expected, GeometryChange change) {
        if (frontier.getShape() != expected) {
            throw new IllegalArgumentException("Geometry change " + change.getClass().getSimpleName()
                    + " requires shape " + expected + " but frontier shape is " + frontier.getShape());
        }
    }

    private static void requireInsertIndex(int index, int size, GeometryChange change) {
        if (index < 0 || index > size) {
            throw new IllegalArgumentException("Invalid insert index " + index + " for size " + size
                    + " in " + change.getClass().getSimpleName());
        }
    }

    private static void requireExistingIndex(int index, int size, GeometryChange change) {
        if (index < 0 || index >= size) {
            throw new IllegalArgumentException("Invalid index " + index + " for size " + size
                    + " in " + change.getClass().getSimpleName());
        }
    }

    private GeometryChangeApplier() {
    }
}
