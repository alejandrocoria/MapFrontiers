package games.alejandrocoria.mapfrontiers.common.territory.frontier;

import games.alejandrocoria.mapfrontiers.common.territory.frontier.GeometryQueries.Insertion;
import games.alejandrocoria.mapfrontiers.platform.services.WorldGeometry;
import net.minecraft.core.BlockPos;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static games.alejandrocoria.mapfrontiers.common.territory.frontier.GeometryQueries.distanceToSegmentSquared;
import static games.alejandrocoria.mapfrontiers.common.territory.frontier.GeometryQueries.nearestSegmentCopy;

@ParametersAreNonnullByDefault
final class GeometryChangeApplier {
    static List<GeometryChange> apply(FrontierData frontier, List<GeometryChange> changes, WorldGeometry geometry) {
        List<GeometryChange> resolved = null;
        for (int i = 0; i < changes.size(); ++i) {
            GeometryChange change = changes.get(i);
            GeometryChange explicit = change;
            if (change instanceof GeometryChange.InsertPathPointAutomatically value) {
                requireShape(frontier, FrontierShape.Path, change);
                Insertion insertion = automaticPathInsertion(frontier.getPoints(), normalize(value.point()), geometry);
                explicit = new GeometryChange.InsertPathPointAt(insertion.index(), insertion.position());
            } else if (change instanceof GeometryChange.InsertVertexAutomatically value) {
                requireShape(frontier, FrontierShape.Vertex, change);
                Insertion insertion = automaticVertexInsertion(frontier.getVertices(), normalize(value.vertex()), geometry);
                explicit = new GeometryChange.InsertVertexAt(insertion.index(), insertion.position());
            }
            apply(frontier, explicit);
            if (explicit != change) {
                if (resolved == null) resolved = new ArrayList<>(changes);
                resolved.set(i, explicit);
            }
        }
        return resolved == null ? changes : resolved;
    }

    private static void apply(FrontierData frontier, GeometryChange change) {
        if (change instanceof GeometryChange.InsertPathPointAt value) {
                requireShape(frontier, FrontierShape.Path, change);
                requireInsertIndex(value.index(), frontier.getPointCount(), change);
                frontier.addPoint(normalize(value.point()), value.index());
        } else if (change instanceof GeometryChange.InsertPathPointBeforeFirst value) {
                requireShape(frontier, FrontierShape.Path, change);
                frontier.addPoint(normalize(value.point()), 0);
        } else if (change instanceof GeometryChange.InsertPathPointAfterLast value) {
                requireShape(frontier, FrontierShape.Path, change);
                frontier.addPoint(normalize(value.point()));
        } else if (change instanceof GeometryChange.InsertPathPointAutomatically) {
                throw new IllegalArgumentException("Unresolved automatic path insertion");
        } else if (change instanceof GeometryChange.SetPathPointAt value) {
                requireShape(frontier, FrontierShape.Path, change);
                requireExistingIndex(value.index(), frontier.getPointCount(), change);
                frontier.movePoint(normalize(value.point()), value.index());
        } else if (change instanceof GeometryChange.RemovePathPointAt value) {
                requireShape(frontier, FrontierShape.Path, change);
                requireExistingIndex(value.index(), frontier.getPointCount(), change);
                frontier.removePoint(value.index());
        } else if (change instanceof GeometryChange.ReversePath) {
                requireShape(frontier, FrontierShape.Path, change);
                synchronized (frontier.points) {
                    Collections.reverse(frontier.points);
                }
                frontier.invalidateGeometryHash();
        } else if (change instanceof GeometryChange.InsertVertexAt value) {
                requireShape(frontier, FrontierShape.Vertex, change);
                requireInsertIndex(value.index(), frontier.getVertexCount(), change);
                frontier.addVertex(normalize(value.vertex()), value.index());
        } else if (change instanceof GeometryChange.InsertVertexAutomatically) {
                throw new IllegalArgumentException("Unresolved automatic vertex insertion");
        } else if (change instanceof GeometryChange.SetVertexAt value) {
                requireShape(frontier, FrontierShape.Vertex, change);
                requireExistingIndex(value.index(), frontier.getVertexCount(), change);
                frontier.moveVertex(normalize(value.vertex()), value.index());
        } else if (change instanceof GeometryChange.RemoveVertexAt value) {
                requireShape(frontier, FrontierShape.Vertex, change);
                requireExistingIndex(value.index(), frontier.getVertexCount(), change);
                frontier.removeVertex(value.index());
        } else if (change instanceof GeometryChange.AddChunks value) {
                requireShape(frontier, FrontierShape.Chunk, change);
                value.chunks().forEach(frontier::addChunk);
        } else if (change instanceof GeometryChange.RemoveChunks value) {
                requireShape(frontier, FrontierShape.Chunk, change);
                value.chunks().forEach(frontier::removeChunk);
        } else {
            throw new IllegalArgumentException("Unknown geometry change: " + change);
        }
    }

    private static Insertion automaticPathInsertion(List<BlockPos> points, BlockPos point, WorldGeometry geometry) {
        if (points.isEmpty()) return new Insertion(0, point);
        BlockPos endCopy = geometry.nearestCopy(points.get(points.size() - 1), point);
        if (points.size() == 1) return new Insertion(1, endCopy);

        double bestDistance = Double.POSITIVE_INFINITY;
        int bestIndex = -1;
        BlockPos bestCopy = point;
        for (int i = 0; i < points.size() - 1; ++i) {
            BlockPos start = points.get(i);
            BlockPos end = points.get(i + 1);
            BlockPos copy = nearestSegmentCopy(geometry, point, start, end);
            double distance = distanceToSegmentSquared(copy, start, end);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = i + 1;
                bestCopy = copy;
            }
        }
        BlockPos startCopy = geometry.nearestCopy(points.get(0), point);
        double startDistance = distanceSquared(startCopy, points.get(0));
        double endDistance = distanceSquared(endCopy, points.get(points.size() - 1));
        if (bestDistance < Math.min(startDistance, endDistance)) return new Insertion(bestIndex, bestCopy);
        return startDistance < endDistance ? new Insertion(0, startCopy) : new Insertion(points.size(), endCopy);
    }

    private static Insertion automaticVertexInsertion(List<BlockPos> vertices, BlockPos vertex, WorldGeometry geometry) {
        if (vertices.isEmpty()) return new Insertion(0, vertex);
        double bestDistance = Double.POSITIVE_INFINITY;
        int bestIndex = -1;
        BlockPos bestCopy = vertex;
        for (int i = 0; i < vertices.size(); ++i) {
            BlockPos start = vertices.get(i);
            BlockPos end = vertices.get((i + 1) % vertices.size());
            BlockPos copy = nearestSegmentCopy(geometry, vertex, start, end);
            double distance = distanceToSegmentSquared(copy, start, end);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = i + 1;
                bestCopy = copy;
            }
        }
        return new Insertion(bestIndex, bestCopy);
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
