package games.alejandrocoria.mapfrontiers.client.territory.collection;

import games.alejandrocoria.mapfrontiers.common.territory.CollectionData;
import games.alejandrocoria.mapfrontiers.client.territory.frontier.FrontierOverlay;
import journeymap.api.v2.client.display.Context;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
public class CollectionOverlay {
    private static final Context.MapType[] FULLSCREEN_MAP_TYPES = {
            Context.MapType.Day,
            Context.MapType.Night,
            Context.MapType.Underground,
            Context.MapType.Topo,
            Context.MapType.Biome
    };

    private final CollectionOverlayKey key;
    private @Nullable CollectionData collection;
    private List<FrontierOverlay> memberFrontiers;
    private boolean needUpdateOverlay = true;
    private boolean membershipDirty = true;
    private boolean geometryDirty = true;
    private boolean labelDirty = true;
    private List<CollectionVisibilityVariant> visibleVariants = List.of();
    private @Nullable Runnable dirtyOverlayListener;

    public CollectionOverlay(CollectionOverlayKey key, CollectionData collection, List<FrontierOverlay> members) {
        this.key = key;
        this.collection = collection;
        memberFrontiers = List.copyOf(members);
    }

    public CollectionOverlayKey getKey() {
        return key;
    }

    public @Nullable CollectionData getCollection() {
        return collection;
    }

    public List<FrontierOverlay> getMemberFrontiers() {
        return memberFrontiers;
    }

    public List<CollectionVisibilityVariant> getVisibleVariants() {
        return visibleVariants;
    }

    public void refreshMembersAndCollection(CollectionData collection, List<FrontierOverlay> members) {
        boolean dirty = false;

        if (this.collection != collection) {
            this.collection = collection;
            geometryDirty = true;
            labelDirty = true;
            dirty = true;
        }

        List<FrontierOverlay> updatedMembers = List.copyOf(members);
        if (!memberFrontiers.equals(updatedMembers)) {
            memberFrontiers = updatedMembers;
            membershipDirty = true;
            geometryDirty = true;
            labelDirty = true;
            dirty = true;
        }

        if (dirty) {
            invalidateOverlayRefresh();
        }
    }

    public void processDirtyOverlay() {
        if (needUpdateOverlay) {
            refreshOverlay();
        }
    }

    public void rebuildOverlayNow() {
        membershipDirty = true;
        geometryDirty = true;
        labelDirty = true;
        refreshOverlay();
    }

    public void deleted() {
        dirtyOverlayListener = null;
    }

    void setDirtyOverlayListener(@Nullable Runnable dirtyOverlayListener) {
        this.dirtyOverlayListener = dirtyOverlayListener;
    }

    private void refreshOverlay() {
        needUpdateOverlay = false;
        if (membershipDirty || geometryDirty) {
            rebuildVisibleVariants();
        }
        membershipDirty = false;
        geometryDirty = false;
        labelDirty = false;
    }

    private void rebuildVisibleVariants() {
        if (collection == null || !collection.isCollectionViewEnabled() || memberFrontiers.isEmpty()) {
            visibleVariants = List.of();
            return;
        }

        int collectionMaxZoom = collection.getCollectionViewZoom();
        List<CollectionVisibilityVariant> rebuiltVariants = new ArrayList<>();
        List<VisibleVariantBuilder> variantBuilders = new ArrayList<>();

        for (Context.MapType mapType : FULLSCREEN_MAP_TYPES) {
            List<VisibleMemberGeometry> visibleMembers = resolveVisibleMemberGeometries(mapType, collectionMaxZoom);
            if (visibleMembers.isEmpty()) {
                continue;
            }

            List<FrontierOverlay> visibleFrontiers = visibleMembers.stream()
                    .map(VisibleMemberGeometry::frontier)
                    .toList();
            VisibleVariantBuilder existingVariant = findVariantBuilder(variantBuilders, visibleFrontiers);
            if (existingVariant == null) {
                variantBuilders.add(new VisibleVariantBuilder(List.copyOf(visibleFrontiers), List.copyOf(visibleMembers), mapType));
            } else {
                existingVariant.addMapType(mapType);
            }
        }

        for (VisibleVariantBuilder builder : variantBuilders) {
            CollectionVisibilityVariant variant = buildVisibleVariant(builder);
            if (variant != null) {
                rebuiltVariants.add(variant);
            }
        }

        visibleVariants = List.copyOf(rebuiltVariants);
    }

    private List<VisibleMemberGeometry> resolveVisibleMemberGeometries(Context.MapType mapType, int collectionMaxZoom) {
        List<VisibleMemberGeometry> visibleMembers = new ArrayList<>();

        for (FrontierOverlay frontier : memberFrontiers) {
            if (!frontier.isVisibleOnFullscreenMap(mapType)) {
                continue;
            }

            FrontierOverlay.CollectionGeometrySnapshot snapshot = frontier.getCollectionGeometrySnapshot();
            if (snapshot == null || snapshot.isEmpty()) {
                continue;
            }

            List<FrontierOverlay.CollectionGeometryRegionSnapshot> visibleRegions = snapshot.getRegions().stream()
                    .filter(region -> region.getMinZoom() <= collectionMaxZoom)
                    .toList();
            if (visibleRegions.isEmpty()) {
                continue;
            }

            visibleMembers.add(new VisibleMemberGeometry(frontier, visibleRegions));
        }

        return List.copyOf(visibleMembers);
    }

    private @Nullable CollectionVisibilityVariant buildVisibleVariant(VisibleVariantBuilder builder) {
        Area unionArea = new Area();
        List<CollectionSourceRegion> sourceRegions = new ArrayList<>();

        for (VisibleMemberGeometry memberGeometry : builder.visibleMemberGeometries()) {
            for (FrontierOverlay.CollectionGeometryRegionSnapshot regionSnapshot : memberGeometry.regions()) {
                Area regionArea = regionSnapshot.copyEffectiveArea();
                if (regionArea.isEmpty()) {
                    continue;
                }

                unionArea.add(new Area(regionArea));
                sourceRegions.add(new CollectionSourceRegion(regionArea, regionSnapshot.getMinZoom()));
            }
        }

        if (unionArea.isEmpty() || sourceRegions.isEmpty()) {
            return null;
        }

        List<CollectionGeometryRegion> regions = extractGeometryRegions(unionArea, sourceRegions);
        return new CollectionVisibilityVariant(builder.mapTypes(), builder.visibleFrontiers(), unionArea, regions);
    }

    private static VisibleVariantBuilder findVariantBuilder(List<VisibleVariantBuilder> builders, List<FrontierOverlay> visibleFrontiers) {
        for (VisibleVariantBuilder builder : builders) {
            if (builder.visibleFrontiers().equals(visibleFrontiers)) {
                return builder;
            }
        }

        return null;
    }

    private static List<CollectionGeometryRegion> extractGeometryRegions(Area unionArea, List<CollectionSourceRegion> sourceRegions) {
        List<Area> extractedAreas = extractRegionAreas(unionArea);
        List<CollectionGeometryRegion> regions = new ArrayList<>();

        for (Area extractedArea : extractedAreas) {
            if (extractedArea.isEmpty()) {
                continue;
            }

            int minZoom = Integer.MAX_VALUE;
            for (CollectionSourceRegion sourceRegion : sourceRegions) {
                if (intersects(extractedArea, sourceRegion.effectiveArea())) {
                    minZoom = Math.min(minZoom, sourceRegion.minZoom());
                }
            }

            if (minZoom != Integer.MAX_VALUE) {
                regions.add(new CollectionGeometryRegion(extractedArea, minZoom));
            }
        }

        return List.copyOf(regions);
    }

    private static List<Area> extractRegionAreas(Area area) {
        List<RingPath> rings = extractRingPaths(area);
        if (rings.isEmpty()) {
            return List.of();
        }

        double outerSign = resolveOuterRingSign(rings);
        List<Area> regionAreas = new ArrayList<>();

        for (RingPath ring : rings) {
            if (!hasSameSign(ring.signedArea(), outerSign)) {
                continue;
            }

            Area regionArea = new Area(ring.path());
            Rectangle2D outerBounds = regionArea.getBounds2D();
            for (RingPath holeCandidate : rings) {
                if (hasSameSign(holeCandidate.signedArea(), outerSign)) {
                    continue;
                }

                Rectangle2D holeBounds = holeCandidate.area().getBounds2D();
                if (outerBounds.contains(holeBounds)) {
                    regionArea.subtract(new Area(holeCandidate.path()));
                }
            }

            if (!regionArea.isEmpty()) {
                regionAreas.add(regionArea);
            }
        }

        return List.copyOf(regionAreas);
    }

    private static List<RingPath> extractRingPaths(Area area) {
        List<RingPath> rings = new ArrayList<>();
        PathIterator pathIterator = area.getPathIterator(null);
        double[] coords = new double[6];
        Path2D.Double currentPath = null;
        List<Point2D.Double> currentPoints = null;

        while (!pathIterator.isDone()) {
            switch (pathIterator.currentSegment(coords)) {
                case PathIterator.SEG_MOVETO -> {
                    if (currentPath != null && currentPoints != null) {
                        addRingPath(rings, currentPath, currentPoints);
                    }
                    currentPath = new Path2D.Double(Path2D.WIND_NON_ZERO);
                    currentPath.moveTo(coords[0], coords[1]);
                    currentPoints = new ArrayList<>();
                    currentPoints.add(new Point2D.Double(coords[0], coords[1]));
                }
                case PathIterator.SEG_LINETO -> {
                    if (currentPath == null) {
                        currentPath = new Path2D.Double(Path2D.WIND_NON_ZERO);
                        currentPath.moveTo(coords[0], coords[1]);
                        currentPoints = new ArrayList<>();
                        currentPoints.add(new Point2D.Double(coords[0], coords[1]));
                    } else {
                        currentPath.lineTo(coords[0], coords[1]);
                        if (currentPoints != null) {
                            currentPoints.add(new Point2D.Double(coords[0], coords[1]));
                        }
                    }
                }
                case PathIterator.SEG_CLOSE -> {
                    if (currentPath != null && currentPoints != null) {
                        currentPath.closePath();
                        addRingPath(rings, currentPath, currentPoints);
                        currentPath = null;
                        currentPoints = null;
                    }
                }
            }

            pathIterator.next();
        }

        if (currentPath != null && currentPoints != null) {
            currentPath.closePath();
            addRingPath(rings, currentPath, currentPoints);
        }

        return List.copyOf(rings);
    }

    private static void addRingPath(List<RingPath> rings, Path2D.Double path, List<Point2D.Double> points) {
        if (points.size() < 3) {
            return;
        }

        rings.add(new RingPath((Path2D.Double) path.clone(), new Area(path), computeSignedArea(points)));
    }

    private static double resolveOuterRingSign(List<RingPath> rings) {
        RingPath outerRing = rings.getFirst();
        double maxAreaMagnitude = Math.abs(outerRing.signedArea());

        for (RingPath ring : rings) {
            double areaMagnitude = Math.abs(ring.signedArea());
            if (areaMagnitude > maxAreaMagnitude) {
                outerRing = ring;
                maxAreaMagnitude = areaMagnitude;
            }
        }

        return outerRing.signedArea();
    }

    private static boolean hasSameSign(double value, double reference) {
        return value == 0.0 || reference == 0.0 || value > 0.0 == reference > 0.0;
    }

    private static double computeSignedArea(List<Point2D.Double> points) {
        double area = 0.0;
        for (int i = 0; i < points.size(); ++i) {
            Point2D.Double current = points.get(i);
            Point2D.Double next = points.get((i + 1) % points.size());
            area += current.x * next.y - next.x * current.y;
        }

        return area * 0.5;
    }

    private static boolean intersects(Area first, Area second) {
        Area intersection = new Area(first);
        intersection.intersect(second);
        return !intersection.isEmpty();
    }

    private void invalidateOverlayRefresh() {
        if (!needUpdateOverlay && dirtyOverlayListener != null) {
            dirtyOverlayListener.run();
        }
        needUpdateOverlay = true;
    }

    public static final class CollectionVisibilityVariant {
        private final List<Context.MapType> mapTypes;
        private final List<FrontierOverlay> visibleMembers;
        private final Area unionArea;
        private final List<CollectionGeometryRegion> regions;

        public CollectionVisibilityVariant(List<Context.MapType> mapTypes, List<FrontierOverlay> visibleMembers, Area unionArea,
                                           List<CollectionGeometryRegion> regions) {
            this.mapTypes = List.copyOf(mapTypes);
            this.visibleMembers = List.copyOf(visibleMembers);
            this.unionArea = new Area(unionArea);
            this.regions = List.copyOf(regions);
        }

        public List<Context.MapType> getMapTypes() {
            return mapTypes;
        }

        public List<FrontierOverlay> getVisibleMembers() {
            return visibleMembers;
        }

        public Area copyUnionArea() {
            return new Area(unionArea);
        }

        public List<CollectionGeometryRegion> getRegions() {
            return regions;
        }
    }

    public static final class CollectionGeometryRegion {
        private final Area effectiveArea;
        private final int minZoom;

        public CollectionGeometryRegion(Area effectiveArea, int minZoom) {
            this.effectiveArea = new Area(effectiveArea);
            this.minZoom = minZoom;
        }

        public Area copyEffectiveArea() {
            return new Area(effectiveArea);
        }

        public int getMinZoom() {
            return minZoom;
        }
    }

    private record VisibleMemberGeometry(FrontierOverlay frontier, List<FrontierOverlay.CollectionGeometryRegionSnapshot> regions) {
    }

    private record CollectionSourceRegion(Area effectiveArea, int minZoom) {
    }

    private record RingPath(Path2D.Double path, Area area, double signedArea) {
    }

    private static final class VisibleVariantBuilder {
        private final List<FrontierOverlay> visibleFrontiers;
        private final List<VisibleMemberGeometry> visibleMemberGeometries;
        private final List<Context.MapType> mapTypes = new ArrayList<>();

        private VisibleVariantBuilder(List<FrontierOverlay> visibleFrontiers, List<VisibleMemberGeometry> visibleMemberGeometries,
                                      Context.MapType initialMapType) {
            this.visibleFrontiers = visibleFrontiers;
            this.visibleMemberGeometries = visibleMemberGeometries;
            mapTypes.add(initialMapType);
        }

        public List<FrontierOverlay> visibleFrontiers() {
            return visibleFrontiers;
        }

        public List<VisibleMemberGeometry> visibleMemberGeometries() {
            return visibleMemberGeometries;
        }

        public List<Context.MapType> mapTypes() {
            return List.copyOf(mapTypes);
        }

        public void addMapType(Context.MapType mapType) {
            mapTypes.add(mapType);
        }
    }
}
