package games.alejandrocoria.mapfrontiers.client.territory.collection;

import com.mojang.blaze3d.platform.NativeImage;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.config.ClientConfig;
import games.alejandrocoria.mapfrontiers.client.config.TextColor;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.territory.frontier.FrontierLabelPlacementSolver;
import games.alejandrocoria.mapfrontiers.client.territory.frontier.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.common.territory.CollectionData;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.display.Context;
import journeymap.api.v2.client.display.MarkerOverlay;
import journeymap.api.v2.client.model.MapImage;
import journeymap.api.v2.client.model.TextProperties;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.awt.Color;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
public class CollectionOverlay {
    private static final int OVERLAY_Y = 70;
    private static final int TEXT_LINE_HEIGHT_PX = 9;
    private static final int LABEL_CONTENT_PADDING_PX = 6;
    private static final double LABEL_SOLVER_PRECISION = 2.0;
    private static final Context.MapType[] FULLSCREEN_MAP_TYPES = {
            Context.MapType.Day,
            Context.MapType.Night,
            Context.MapType.Underground,
            Context.MapType.Topo,
            Context.MapType.Biome
    };
    private static final MapImage transparentLabelMarker = createTransparentLabelMarker();

    private final CollectionOverlayKey key;
    private final @Nullable IClientAPI jmAPI;
    private @Nullable CollectionData collection;
    private List<FrontierOverlay> memberFrontiers;
    private boolean needUpdateOverlay = true;
    private boolean membershipDirty = true;
    private boolean geometryDirty = true;
    private boolean labelsDirty = true;
    private boolean labelVisibilityDirty = true;
    private List<CollectionVisibilityVariant> visibleVariants = List.of();
    private final List<MarkerOverlay> labelOverlays = new ArrayList<>();
    private final Map<CollectionLabelPlacementKey, FrontierLabelPlacementSolver.LabelPlacement> placementCache = new HashMap<>();
    private @Nullable Runnable dirtyOverlayListener;

    public CollectionOverlay(CollectionOverlayKey key, @Nullable IClientAPI jmAPI, CollectionData collection, List<FrontierOverlay> members) {
        this.key = key;
        this.jmAPI = jmAPI;
        this.collection = collection;
        memberFrontiers = List.copyOf(members);
    }

    public CollectionOverlayKey getKey() {
        return key;
    }

    public List<MarkerOverlay> getLabelOverlays() {
        return labelOverlays;
    }

    public void refreshMembersAndCollection(CollectionData collection, List<FrontierOverlay> members) {
        boolean dirty = false;

        if (this.collection != collection) {
            this.collection = collection;
            geometryDirty = true;
            labelsDirty = true;
            labelVisibilityDirty = true;
            dirty = true;
        }

        List<FrontierOverlay> updatedMembers = List.copyOf(members);
        if (!memberFrontiers.equals(updatedMembers)) {
            memberFrontiers = updatedMembers;
            membershipDirty = true;
            geometryDirty = true;
            labelsDirty = true;
            labelVisibilityDirty = true;
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
        labelsDirty = true;
        labelVisibilityDirty = true;
        refreshOverlay();
    }

    public void deleted() {
        hideMarkerOverlays(labelOverlays);
        labelOverlays.clear();
        placementCache.clear();
        visibleVariants = List.of();
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

        if (labelsDirty || labelVisibilityDirty) {
            rebuildLabelOverlays();
        }

        membershipDirty = false;
        geometryDirty = false;
        labelsDirty = false;
        labelVisibilityDirty = false;
    }

    private void rebuildVisibleVariants() {
        placementCache.clear();
        if (collection == null || !collection.isCollectionViewEnabled() || memberFrontiers.isEmpty()) {
            visibleVariants = List.of();
            labelsDirty = true;
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
        labelsDirty = true;
    }

    private void rebuildLabelOverlays() {
        hideMarkerOverlays(labelOverlays);
        labelOverlays.clear();

        if (collection == null || !collection.isCollectionViewEnabled() || visibleVariants.isEmpty()) {
            return;
        }

        String effectiveName = getEffectiveCollectionName();
        if (effectiveName == null) {
            return;
        }

        CollectionLabelContentMetrics metrics = buildLabelContentMetrics(effectiveName);
        int collectionMaxZoom = collection.getCollectionViewZoom();
        for (CollectionVisibilityVariant variant : visibleVariants) {
            PlacedRegionCandidate bestCandidate = null;

            for (CollectionGeometryRegion region : variant.getRegions()) {
                CollectionLabelPlacementKey placementKey = new CollectionLabelPlacementKey(region, metrics.contentWidthPx(), metrics.contentHeightPx());
                FrontierLabelPlacementSolver.LabelPlacement placement = placementCache.computeIfAbsent(placementKey,
                        ignored -> FrontierLabelPlacementSolver.solve(region.copyEffectiveArea(),
                                metrics.contentWidthPx(),
                                metrics.contentHeightPx(),
                                LABEL_SOLVER_PRECISION));

                if (bestCandidate == null
                        || placement.availableWidthBlocks() > bestCandidate.placement().availableWidthBlocks()
                        || placement.availableWidthBlocks() == bestCandidate.placement().availableWidthBlocks()
                        && placement.availableHeightBlocks() > bestCandidate.placement().availableHeightBlocks()) {
                    bestCandidate = new PlacedRegionCandidate(region, placement);
                }
            }

            if (bestCandidate == null) {
                continue;
            }

            addLabelOverlay(variant.getMapTypes().toArray(Context.MapType[]::new), metrics, bestCandidate, collectionMaxZoom);
        }
        showMarkerOverlaysQuietly(labelOverlays);
    }

    private void addLabelOverlay(Context.MapType[] mapTypes, CollectionLabelContentMetrics metrics, PlacedRegionCandidate candidate,
                                 int collectionMaxZoom) {
        TextProperties textProperties = createBaseTextProperties();
        int minZoom = Math.max(2, candidate.region().getMinZoom());
        BlockPos anchor = BlockPos.containing(candidate.placement().centerX(), OVERLAY_Y, candidate.placement().centerZ());
        MarkerOverlay labelOverlay = new MarkerOverlay(MapFrontiers.MODID, anchor, transparentLabelMarker);
        labelOverlay.setActiveUIs(Context.UI.Fullscreen);
        labelOverlay.setActiveMapTypes(mapTypes);
        labelOverlay.setDimension(key.dimension());
        labelOverlay.setMinZoom(minZoom);
        labelOverlay.setMaxZoom(collectionMaxZoom);
        labelOverlay.setOverlayGroupName("collection");
        labelOverlay.setTextProperties(textProperties.setMinZoom(minZoom).setMaxZoom(collectionMaxZoom)).setLabel(metrics.label());
        labelOverlays.add(labelOverlay);
    }

    private @Nullable String getEffectiveCollectionName() {
        if (collection == null) {
            return null;
        }

        String name = collection.getName().trim();
        return name.isEmpty() ? null : name;
    }

    private CollectionLabelContentMetrics buildLabelContentMetrics(String effectiveName) {
        String label = ChatFormatting.BOLD + effectiveName + ChatFormatting.RESET;
        int textSize = ClientConfig.COLLECTION_TEXT_SIZE.get();
        int textWidthPx = Minecraft.getInstance().font.width(Component.literal(effectiveName).withStyle(ChatFormatting.BOLD)) * textSize;
        int textHeightPx = TEXT_LINE_HEIGHT_PX * textSize;
        int paddedWidthPx = textWidthPx + LABEL_CONTENT_PADDING_PX;
        int paddedHeightPx = textHeightPx + LABEL_CONTENT_PADDING_PX;
        return new CollectionLabelContentMetrics(label, paddedWidthPx, paddedHeightPx);
    }

    private TextProperties createBaseTextProperties() {
        TextProperties textProperties = new TextProperties()
                .setOpacity(ClientConfig.COLLECTION_TEXT_OPACITY.get().floatValue())
                .setScale(ClientConfig.COLLECTION_TEXT_SIZE.get())
                .setBackgroundOpacity(0.f);
        int collectionColor = collection == null ? ColorConstants.WHITE : collection.getColor();
        switch (ClientConfig.COLLECTION_TEXT_COLOR.get()) {
            case TextColor.FrontierColor -> textProperties.setColor(collectionColor);
            case TextColor.FrontierColorBright -> textProperties.setColor(colorMaxBrightness(collectionColor));
            case TextColor.White -> textProperties.setColor(ColorConstants.WHITE);
        }
        return textProperties;
    }

    private void hideMarkerOverlays(List<MarkerOverlay> overlays) {
        for (MarkerOverlay marker : overlays) {
            removeMarkerOverlay(marker);
        }
    }

    private void showMarkerOverlaysQuietly(List<MarkerOverlay> overlays) {
        try {
            showMarkerOverlays(overlays);
        } catch (Exception e) {
            MapFrontiers.LOGGER.error("Error showing collection label overlays", e);
        }
    }

    private void showMarkerOverlays(List<MarkerOverlay> overlays) throws Exception {
        if (jmAPI == null) {
            return;
        }

        for (MarkerOverlay marker : overlays) {
            jmAPI.show(marker);
        }
    }

    private void removeMarkerOverlay(MarkerOverlay marker) {
        if (jmAPI == null) {
            return;
        }

        try {
            jmAPI.remove(marker);
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error("Failed to remove collection label overlay for {}", key, t);
        }
    }

    private static int colorMaxBrightness(int color) {
        float[] hsv = Color.RGBtoHSB((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, null);
        return Color.HSBtoRGB(hsv[0], hsv[1], 1.f);
    }

    private static MapImage createTransparentLabelMarker() {
        NativeImage image = new NativeImage(1, 1, false);
        image.setPixel(0, 0, 0);
        MapImage mapImage = new MapImage(image);
        mapImage.setAnchorX(0.5);
        mapImage.setAnchorY(0.5);
        mapImage.setDisplayWidth(1);
        mapImage.setDisplayHeight(1);
        mapImage.setOpacity(0.f);
        return mapImage;
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
        return new CollectionVisibilityVariant(builder.mapTypes(), regions);
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

    private static final class CollectionVisibilityVariant {
        private final List<Context.MapType> mapTypes;
        private final List<CollectionGeometryRegion> regions;

        public CollectionVisibilityVariant(List<Context.MapType> mapTypes, List<CollectionGeometryRegion> regions) {
            this.mapTypes = List.copyOf(mapTypes);
            this.regions = List.copyOf(regions);
        }

        public List<Context.MapType> getMapTypes() {
            return mapTypes;
        }

        public List<CollectionGeometryRegion> getRegions() {
            return regions;
        }
    }

    private static final class CollectionGeometryRegion {
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

    private record CollectionLabelPlacementKey(CollectionGeometryRegion region,
                                               int contentWidthPx,
                                               int contentHeightPx) {
    }

    private record CollectionLabelContentMetrics(String label,
                                                 int contentWidthPx,
                                                 int contentHeightPx) {
    }

    private record PlacedRegionCandidate(CollectionGeometryRegion region,
                                         FrontierLabelPlacementSolver.LabelPlacement placement) {
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
