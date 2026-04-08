package games.alejandrocoria.mapfrontiers.client.frontier;

import net.minecraft.util.Mth;

import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public final class FrontierLabelPlacementSolver {
    private static final double SQRT_2 = Math.sqrt(2.0);
    private static final int CLEARANCE_ITERATIONS = 24;
    private static final double SQUARE_CONTAINS_EPSILON = 1.0e-4;

    private FrontierLabelPlacementSolver() {
    }

    public static LabelPlacement solve(Area area, int contentWidthPx, int contentHeightPx, double precision) {
        int safeContentWidth = Math.max(1, contentWidthPx);
        int safeContentHeight = Math.max(1, contentHeightPx);
        double safePrecision = Math.max(precision, Mth.EPSILON);
        double comparisonEpsilon = Math.max(Mth.EPSILON, safePrecision * 1.0e-3);

        if (area.isEmpty()) {
            Rectangle2D bounds = area.getBounds2D();
            return new LabelPlacement(bounds.getCenterX(), bounds.getCenterY(), 0.0, 0.0, safeContentWidth, safeContentHeight);
        }

        TransformedGeometry geometry = buildTransformedGeometry(area, safeContentWidth, safeContentHeight);
        SolverPoint bestPoint = solvePoleOfInaccessibility(geometry, safePrecision, comparisonEpsilon);

        if (bestPoint == null || !geometry.area().contains(bestPoint.x(), bestPoint.z())) {
            bestPoint = findFallbackPoint(geometry, safePrecision);
        }

        if (bestPoint == null) {
            Rectangle2D originalBounds = area.getBounds2D();
            return new LabelPlacement(originalBounds.getCenterX(), originalBounds.getCenterY(), 0.0, 0.0, safeContentWidth, safeContentHeight);
        }

        double squareClearance = computeCenteredSquareClearance(geometry.area(), geometry.bounds(), bestPoint.x(), bestPoint.z());
        double availableHeightBlocks = squareClearance * 2.0;
        double availableWidthBlocks = availableHeightBlocks * safeContentWidth / (double) safeContentHeight;

        return new LabelPlacement(bestPoint.x() / geometry.scaleX(),
                bestPoint.z(),
                availableWidthBlocks,
                availableHeightBlocks,
                safeContentWidth,
                safeContentHeight);
    }

    private static TransformedGeometry buildTransformedGeometry(Area area, int contentWidthPx, int contentHeightPx) {
        double scaleX = contentHeightPx / (double) contentWidthPx;
        Area transformedArea = new Area(area);
        transformedArea.transform(AffineTransform.getScaleInstance(scaleX, 1.0));
        Rectangle2D bounds = transformedArea.getBounds2D();
        List<List<Point2D.Double>> rings = extractRings(transformedArea);
        return new TransformedGeometry(transformedArea, scaleX, bounds, rings);
    }

    private static List<List<Point2D.Double>> extractRings(Area transformedArea) {
        List<List<Point2D.Double>> rings = new ArrayList<>();
        PathIterator pathIterator = transformedArea.getPathIterator(null);
        double[] coords = new double[6];
        List<Point2D.Double> currentRing = null;

        while (!pathIterator.isDone()) {
            switch (pathIterator.currentSegment(coords)) {
                case PathIterator.SEG_MOVETO -> {
                    if (currentRing != null) {
                        addRing(rings, currentRing);
                    }
                    currentRing = new ArrayList<>();
                    currentRing.add(new Point2D.Double(coords[0], coords[1]));
                }
                case PathIterator.SEG_LINETO -> {
                    if (currentRing == null) {
                        currentRing = new ArrayList<>();
                    }
                    Point2D.Double point = new Point2D.Double(coords[0], coords[1]);
                    if (currentRing.isEmpty() || !samePoint(currentRing.getLast(), point)) {
                        currentRing.add(point);
                    }
                }
                case PathIterator.SEG_CLOSE -> {
                    if (currentRing != null) {
                        addRing(rings, currentRing);
                        currentRing = null;
                    }
                }
            }

            pathIterator.next();
        }

        if (currentRing != null) {
            addRing(rings, currentRing);
        }

        return rings;
    }

    private static void addRing(List<List<Point2D.Double>> rings, List<Point2D.Double> ring) {
        if (ring.size() > 1 && samePoint(ring.getFirst(), ring.getLast())) {
            ring.removeLast();
        }
        if (!ring.isEmpty()) {
            rings.add(List.copyOf(ring));
        }
    }

    private static SolverPoint solvePoleOfInaccessibility(TransformedGeometry geometry, double precision, double comparisonEpsilon) {
        Rectangle2D bounds = geometry.bounds();
        double width = bounds.getWidth();
        double height = bounds.getHeight();
        double boundsCenterX = bounds.getCenterX();
        double boundsCenterZ = bounds.getCenterY();

        Comparator<Cell> comparator = (left, right) -> {
            int compare = Double.compare(right.maxDistance(), left.maxDistance());
            if (compare != 0) {
                return compare;
            }

            compare = Double.compare(distanceToCenterSquared(left.centerX(), left.centerZ(), boundsCenterX, boundsCenterZ),
                    distanceToCenterSquared(right.centerX(), right.centerZ(), boundsCenterX, boundsCenterZ));
            if (compare != 0) {
                return compare;
            }

            compare = Double.compare(left.centerZ(), right.centerZ());
            if (compare != 0) {
                return compare;
            }

            return Double.compare(left.centerX(), right.centerX());
        };

        PriorityQueue<Cell> queue = new PriorityQueue<>(comparator);
        double initialCenterX = boundsCenterX;
        double initialCenterZ = boundsCenterZ;
        SolverPoint best = geometry.area().contains(initialCenterX, initialCenterZ)
                ? new SolverPoint(initialCenterX, initialCenterZ, signedDistanceToBoundary(initialCenterX, initialCenterZ, geometry.area(), geometry.rings()))
                : null;

        if (width <= 0.0 || height <= 0.0) {
            Cell cell = createCell(initialCenterX, initialCenterZ, 0.0, geometry);
            queue.add(cell);
        } else {
            double cellSize = Math.min(width, height);
            double halfSize = cellSize * 0.5;
            int cols = Math.max(1, (int) Math.ceil(width / cellSize));
            int rows = Math.max(1, (int) Math.ceil(height / cellSize));

            for (int row = 0; row < rows; ++row) {
                for (int col = 0; col < cols; ++col) {
                    double centerX = bounds.getMinX() + col * cellSize + halfSize;
                    double centerZ = bounds.getMinY() + row * cellSize + halfSize;
                    queue.add(createCell(centerX, centerZ, halfSize, geometry));
                }
            }
        }

        while (!queue.isEmpty()) {
            Cell cell = queue.poll();

            if (cell.distance() >= 0.0 && isBetterPoint(cell.centerX(), cell.centerZ(), cell.distance(), best, boundsCenterX, boundsCenterZ, comparisonEpsilon)) {
                best = new SolverPoint(cell.centerX(), cell.centerZ(), cell.distance());
            }

            if (!greaterThan(cell.maxDistance() - (best == null ? 0.0 : best.signedDistance()), precision, comparisonEpsilon) || cell.halfSize() <= comparisonEpsilon) {
                continue;
            }

            double childHalfSize = cell.halfSize() * 0.5;
            queue.add(createCell(cell.centerX() - childHalfSize, cell.centerZ() - childHalfSize, childHalfSize, geometry));
            queue.add(createCell(cell.centerX() + childHalfSize, cell.centerZ() - childHalfSize, childHalfSize, geometry));
            queue.add(createCell(cell.centerX() - childHalfSize, cell.centerZ() + childHalfSize, childHalfSize, geometry));
            queue.add(createCell(cell.centerX() + childHalfSize, cell.centerZ() + childHalfSize, childHalfSize, geometry));
        }

        return best;
    }

    private static Cell createCell(double centerX, double centerZ, double halfSize, TransformedGeometry geometry) {
        double distance = signedDistanceToBoundary(centerX, centerZ, geometry.area(), geometry.rings());
        double maxDistance = distance + halfSize * SQRT_2;
        return new Cell(centerX, centerZ, halfSize, distance, maxDistance);
    }

    private static SolverPoint findFallbackPoint(TransformedGeometry geometry, double precision) {
        Rectangle2D bounds = geometry.bounds();
        double centerX = bounds.getCenterX();
        double centerZ = bounds.getCenterY();

        if (geometry.area().contains(centerX, centerZ)) {
            return new SolverPoint(centerX, centerZ, signedDistanceToBoundary(centerX, centerZ, geometry.area(), geometry.rings()));
        }

        double step = Math.max(precision, 1.0);
        for (double z = bounds.getMinY(); z <= bounds.getMaxY() + step * 0.5; z += step) {
            for (double x = bounds.getMinX(); x <= bounds.getMaxX() + step * 0.5; x += step) {
                if (geometry.area().contains(x, z)) {
                    return new SolverPoint(x, z, signedDistanceToBoundary(x, z, geometry.area(), geometry.rings()));
                }
            }
        }

        return null;
    }

    private static double computeCenteredSquareClearance(Area transformedArea, Rectangle2D bounds, double xPrime, double zPrime) {
        double hi = Math.min(
                Math.min(xPrime - bounds.getMinX(), bounds.getMaxX() - xPrime),
                Math.min(zPrime - bounds.getMinY(), bounds.getMaxY() - zPrime)
        );
        if (hi <= 0.0) {
            return 0.0;
        }

        double lo = 0.0;
        for (int i = 0; i < CLEARANCE_ITERATIONS; ++i) {
            double mid = (lo + hi) * 0.5;
            if (containsCenteredSquare(transformedArea, xPrime, zPrime, mid)) {
                lo = mid;
            } else {
                hi = mid;
            }
        }

        return lo;
    }

    private static boolean containsCenteredSquare(Area transformedArea, double xPrime, double zPrime, double halfSide) {
        double shrink = Math.min(SQUARE_CONTAINS_EPSILON, halfSide);
        double size = Math.max(0.0, halfSide * 2.0 - shrink * 2.0);
        return transformedArea.contains(xPrime - halfSide + shrink, zPrime - halfSide + shrink, size, size);
    }

    private static boolean isBetterPoint(double candidateX,
                                         double candidateZ,
                                         double candidateDistance,
                                         SolverPoint currentBest,
                                         double boundsCenterX,
                                         double boundsCenterZ,
                                         double comparisonEpsilon) {
        if (currentBest == null) {
            return true;
        }
        if (greaterThan(candidateDistance, currentBest.signedDistance(), comparisonEpsilon)) {
            return true;
        }
        if (!nearlyEqual(candidateDistance, currentBest.signedDistance(), comparisonEpsilon)) {
            return false;
        }

        double candidateDistanceToCenter = distanceToCenterSquared(candidateX, candidateZ, boundsCenterX, boundsCenterZ);
        double currentDistanceToCenter = distanceToCenterSquared(currentBest.x(), currentBest.z(), boundsCenterX, boundsCenterZ);
        if (greaterThan(currentDistanceToCenter, candidateDistanceToCenter, comparisonEpsilon)) {
            return true;
        }
        if (!nearlyEqual(candidateDistanceToCenter, currentDistanceToCenter, comparisonEpsilon)) {
            return false;
        }

        if (candidateZ < currentBest.z() - comparisonEpsilon) {
            return true;
        }
        if (Math.abs(candidateZ - currentBest.z()) <= comparisonEpsilon) {
            return candidateX < currentBest.x() - comparisonEpsilon;
        }

        return false;
    }

    private static double signedDistanceToBoundary(double x, double z, Area area, List<List<Point2D.Double>> rings) {
        double distance = distanceToNearestSegment(x, z, rings);
        return area.contains(x, z) ? distance : -distance;
    }

    private static double distanceToNearestSegment(double x, double z, List<List<Point2D.Double>> rings) {
        double distance = Double.POSITIVE_INFINITY;

        for (List<Point2D.Double> ring : rings) {
            if (ring.size() == 1) {
                Point2D.Double point = ring.getFirst();
                distance = Math.min(distance, Point2D.distance(x, z, point.x, point.y));
                continue;
            }

            for (int i = 0; i < ring.size(); ++i) {
                Point2D.Double from = ring.get(i);
                Point2D.Double to = ring.get((i + 1) % ring.size());
                distance = Math.min(distance, pointToSegmentDistance(x, z, from.x, from.y, to.x, to.y));
            }
        }

        return Double.isFinite(distance) ? distance : 0.0;
    }

    private static double pointToSegmentDistance(double px, double pz, double ax, double az, double bx, double bz) {
        double dx = bx - ax;
        double dz = bz - az;
        double lengthSquared = dx * dx + dz * dz;
        if (lengthSquared == 0.0) {
            return Point2D.distance(px, pz, ax, az);
        }

        double t = ((px - ax) * dx + (pz - az) * dz) / lengthSquared;
        t = Mth.clamp(t, 0.0, 1.0);
        double closestX = ax + t * dx;
        double closestZ = az + t * dz;
        return Point2D.distance(px, pz, closestX, closestZ);
    }

    private static double distanceToCenterSquared(double x, double z, double centerX, double centerZ) {
        double dx = x - centerX;
        double dz = z - centerZ;
        return dx * dx + dz * dz;
    }

    private static boolean nearlyEqual(double a, double b, double epsilon) {
        return Math.abs(a - b) <= epsilon;
    }

    private static boolean greaterThan(double a, double b, double epsilon) {
        return a > b + epsilon;
    }

    private static boolean samePoint(Point2D.Double left, Point2D.Double right) {
        return Double.compare(left.x, right.x) == 0 && Double.compare(left.y, right.y) == 0;
    }

    public record LabelPlacement(double centerX,
                                 double centerZ,
                                 double availableWidthBlocks,
                                 double availableHeightBlocks,
                                 int contentWidthPx,
                                 int contentHeightPx) {
    }

    private record TransformedGeometry(Area area,
                                       double scaleX,
                                       Rectangle2D bounds,
                                       List<List<Point2D.Double>> rings) {
    }

    private record Cell(double centerX,
                        double centerZ,
                        double halfSize,
                        double distance,
                        double maxDistance) {
    }

    private record SolverPoint(double x,
                               double z,
                               double signedDistance) {
    }
}
