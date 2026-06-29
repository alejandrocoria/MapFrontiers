package games.alejandrocoria.mapfrontiers.client.territory.frontier;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.blaze3d.platform.NativeImage;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.client.config.ClientConfig;
import games.alejandrocoria.mapfrontiers.client.config.TextColor;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.territory.BannerRenderer;
import games.alejandrocoria.mapfrontiers.client.territory.collection.CollectionLocalOverrides;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import games.alejandrocoria.mapfrontiers.common.territory.BannerData;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionData;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionVisibilityData;
import games.alejandrocoria.mapfrontiers.common.territory.collection.CollectionVisibilityMask;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierChange;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierShape;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierSharingChange;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierVisibility;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierVisibilityData;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierVisibilityMask;
import it.unimi.dsi.fastutil.Pair;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.display.Context;
import journeymap.api.v2.client.display.MarkerOverlay;
import journeymap.api.v2.client.display.PolygonOverlay;
import journeymap.api.v2.client.model.MapImage;
import journeymap.api.v2.client.model.MapPolygon;
import journeymap.api.v2.client.model.ShapeProperties;
import journeymap.api.v2.client.model.TextProperties;
import journeymap.api.v2.client.util.PolygonHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.awt.Color;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static java.lang.Math.abs;

@ParametersAreNonnullByDefault
public class FrontierOverlay extends FrontierData {
    private static final int OVERLAY_Y = 70;
    private static final int TEXT_LINE_HEIGHT_PX = 9;
    private static final int BANNER_BASE_WIDTH_PX = 20;
    private static final int BANNER_BASE_HEIGHT_PX = 40;
    private static final int LABEL_CONTENT_PADDING_PX = 6;
    private static final int BANNER_MULTILINE_TEXT_OFFSET_Y = -6;
    private static final int BANNER_SINGLE_LINE_TEXT_OFFSET_Y = 5;
    private static final double VERTEX_LABEL_SOLVER_PRECISION = 0.5;
    private static final double CHUNK_LABEL_SOLVER_PRECISION = 2.0;
    private static final int PATH_LABEL_OFFSET_PADDING_PX = 4;
    private static final int INCOMPLETE_VERTEX_FRONTIER_MIN_ZOOM = 512;
    private static final int PATH_REPEATED_MARKER_BASE_MARKER_SIZE = 2;
    private static final int PATH_REPEATED_MARKER_MIN_ZOOM = 2;
    private static final int PATH_REPEATED_MARKER_MAX_ZOOM = 16384;
    private static final double PATH_REPEATED_MARKER_SPACING_ZOOM_REFERENCE = 8192.0;
    private static final Context.MapType[] HIGHLIGHT_MAP_TYPES = {
            Context.MapType.Day,
            Context.MapType.Night,
            Context.MapType.Underground,
            Context.MapType.Topo,
            Context.MapType.Biome
    };
    private static final MapImage transparentLabelMarker = createTransparentLabelMarker();

    public BlockPos topLeft;
    public BlockPos bottomRight;
    public float perimeter = 0.f;
    public float area = 0.f;
    private int selectedPointIndex = -1;
    protected FrontierVisibilityData effectiveVisibilityData;

    private boolean highlighted = false;

    private final IClientAPI jmAPI;
    private final List<PolygonOverlay> polygonOverlays = new ArrayList<>();
    private final List<PolygonOverlay> collectionPolygonOverlays = new ArrayList<>();
    private final List<PolygonOverlay> highlightPolygonOverlays = new ArrayList<>();
    private final List<PolygonRenderGeometry> polygonRenderGeometries = new ArrayList<>();
    private Area polygonArea;
    private final List<MarkerOverlay> markerOverlays = new ArrayList<>();
    private final List<MarkerOverlay> highlightMarkerOverlays = new ArrayList<>();
    private final List<MarkerOverlay> labelOverlays = new ArrayList<>();
    private final BannerRenderer bannerRenderer = new BannerRenderer();
    private int previewTextSize = -1;
    private int previewBannerSize = -1;
    private boolean previewCollectionStyleEnabled = false;
    private int previewCollectionColor = ColorConstants.WHITE;

    private int hash;
    private boolean hashDirty = true;

    private @Nullable Runnable dirtyOverlayListener;
    private boolean needUpdateOverlay = true;
    private boolean geometryCacheDirty = true;
    private boolean pathLayoutDirty = true;
    private boolean visualConfigDirty = true;
    private boolean polygonUiPlanDirty = true;
    private boolean baseOverlaysDirty = true;
    private boolean collectionBaseOverlaysDirty = true;
    private boolean labelsDirty = true;
    private boolean highlightStructureDirty = true;
    private boolean highlightVisibilityDirty = true;
    private boolean suppressDirtyOverlayListener = false;
    private @Nullable PathLayoutCache pathLayoutCache;
    private @Nullable VisualConfigSnapshot visualConfigSnapshot;
    private @Nullable PolygonUiPlanCache polygonUiPlanCache;
    private boolean editingActivationSuppressed = false;
    private boolean pendingActivationDirty = false;

    public FrontierOverlay(FrontierData data, @Nullable IClientAPI jmAPI) {
        super(data);
        this.jmAPI = jmAPI;
        setVisibilityOverride(MapFrontiersClient.getLocalOverrides().getVisibility(id));
        refreshEffectiveBannerRenderer();
        rebuildOverlayNow();
    }

    @Override
    public void updateFromData(FrontierData other) {
        suppressDirtyOverlayListener = true;
        try {
            super.updateFromData(other);
            setVisibilityOverride(MapFrontiersClient.getLocalOverrides().getVisibility(id));

            clampSelectedEditablePoint();
            refreshEffectiveBannerRenderer();

            invalidateAllOverlayLayers();
            processDirtyOverlay();
            hashDirty = true;
            markFrontierActivationDirty();
        } finally {
            suppressDirtyOverlayListener = false;
        }
    }

    public void applyChange(FrontierChange change) {
        suppressDirtyOverlayListener = true;
        try {
            super.applyChange(change);
            setVisibilityOverride(MapFrontiersClient.getLocalOverrides().getVisibility(id));

            clampSelectedEditablePoint();

            if (change.hasBannerChange() || change.hasCollectionIdChange()) {
                refreshEffectiveBannerRenderer();
            }

            if (change.hasShapeChange()) {
                invalidateFromGeometry();
            } else {
                if (change.hasColorChange() || change.hasVisibilityChange()) {
                    invalidateBasePresentation();
                }
                if (change.hasPathStyleChange()) {
                    invalidateFromDiscretization();
                }
                if (change.hasNameChange() || change.hasCollectionIdChange() || change.hasBannerChange()) {
                    invalidateLabels();
                }
            }
            processDirtyOverlay();
            if (change.hasShapeChange() || change.hasVisibilityChange()) {
                markFrontierActivationDirty();
            }
        } finally {
            suppressDirtyOverlayListener = false;
        }
    }

    public void applySharingChange(FrontierSharingChange sharingChange) {
        super.applySharingChange(sharingChange);
        hashDirty = true;
    }

    public int getHash() {
        if (hashDirty) {
            hashDirty = false;
            CollectionData collection = getCollection();
            hash = Objects.hash(id, color, dimension, name1, name2, visibilityData, vertices, chunks, points, frontierShape, pathStyle, banner, usersShared,
                    copiedFrom, inheritCollectionBanner, collectionId, sourcePluginId,
                    collection == null ? null : collection.getName(),
                    collection == null ? null : collection.getColor(),
                    collection == null ? null : collection.getBannerData(),
                    collection == null ? null : collection.getModified());
        }

        return hash;
    }

    @Override
    public void setCollectionId(@Nullable UUID collectionId) {
        super.setCollectionId(collectionId);
        markCollectionPresentationDirty();
    }

    public void markCollectionPresentationDirty() {
        hashDirty = true;
        refreshEffectiveBannerRenderer();
        baseOverlaysDirty = true;
        collectionBaseOverlaysDirty = true;
        labelsDirty = true;
        invalidateOverlayRefresh();
    }

    public List<PolygonOverlay> getPolygonOverlays() {
        return polygonOverlays;
    }

    public List<MarkerOverlay> getMarkerOverlays() {
        return markerOverlays;
    }

    public List<MarkerOverlay> getLabelOverlays() {
        return labelOverlays;
    }

    public @Nullable CollectionGeometrySnapshot getCollectionGeometrySnapshot() {
        if (frontierShape == FrontierShape.Path || frontierShape == FrontierShape.Vertex && getVertexCount() < 3) {
            return null;
        }

        if (geometryCacheDirty) {
            rebuildGeometryCache();
            geometryCacheDirty = false;
        }

        if (polygonRenderGeometries.isEmpty()) {
            return null;
        }

        List<CollectionGeometryIslandSnapshot> islands = new ArrayList<>();
        for (PolygonRenderGeometry geometry : polygonRenderGeometries) {
            islands.add(new CollectionGeometryIslandSnapshot(buildOverlayArea(geometry.polygon(), geometry.holes()), geometry.minZoom()));
        }

        return islands.isEmpty() ? null : new CollectionGeometrySnapshot(islands);
    }

    public void setPreviewLabelSizes(int textSize, int bannerSize) {
        previewTextSize = Math.max(1, textSize);
        previewBannerSize = Math.max(1, bannerSize);
        invalidateLabels();
    }

    public void setPreviewCollectionStyle(@Nullable Integer collectionColor) {
        boolean enabled = collectionColor != null;
        int resolvedColor = collectionColor == null ? ColorConstants.WHITE : collectionColor;
        if (previewCollectionStyleEnabled == enabled && (!enabled || previewCollectionColor == resolvedColor)) {
            return;
        }

        previewCollectionStyleEnabled = enabled;
        previewCollectionColor = resolvedColor;
        invalidateBasePresentation();
    }

    public void processDirtyOverlay() {
        if (needUpdateOverlay) {
            refreshOverlay();
        }
    }

    public void rebuildOverlayNow() {
        suppressDirtyOverlayListener = true;
        try {
            hashDirty = true;
            invalidateAllOverlayLayers();
            refreshOverlay();
        } finally {
            suppressDirtyOverlayListener = false;
        }
    }

    private void refreshOverlay() {
        needUpdateOverlay = false;
        if (geometryCacheDirty) {
            rebuildGeometryCache();
            geometryCacheDirty = false;
        }

        if (baseOverlaysDirty) {
            rebuildBaseOverlays();
            baseOverlaysDirty = false;
        }

        if (collectionBaseOverlaysDirty) {
            rebuildCollectionBaseOverlays();
            collectionBaseOverlaysDirty = false;
        }

        if (labelsDirty) {
            rebuildLabels();
            labelsDirty = false;
        }

        if (highlighted && highlightStructureDirty) {
            rebuildHighlightOverlays();
            highlightStructureDirty = false;
            highlightVisibilityDirty = false;
        } else if (highlightVisibilityDirty) {
            refreshHighlightVisibility();
            highlightVisibilityDirty = false;
        }
    }

    private void invalidateOverlayRefresh() {
        if (!needUpdateOverlay && dirtyOverlayListener != null && !suppressDirtyOverlayListener) {
            dirtyOverlayListener.run();
        }
        needUpdateOverlay = true;
    }

    void setDirtyOverlayListener(@Nullable Runnable dirtyOverlayListener) {
        this.dirtyOverlayListener = dirtyOverlayListener;
    }

    private void invalidateAllOverlayLayers() {
        geometryCacheDirty = true;
        pathLayoutDirty = true;
        pathLayoutCache = null;
        visualConfigDirty = true;
        visualConfigSnapshot = null;
        polygonUiPlanDirty = true;
        polygonUiPlanCache = null;
        baseOverlaysDirty = true;
        collectionBaseOverlaysDirty = true;
        labelsDirty = true;
        highlightStructureDirty = true;
        highlightVisibilityDirty = true;
        invalidateOverlayRefresh();
    }

    private void invalidateFromGeometry() {
        geometryCacheDirty = true;
        pathLayoutDirty = true;
        pathLayoutCache = null;
        polygonUiPlanDirty = true;
        polygonUiPlanCache = null;
        baseOverlaysDirty = true;
        collectionBaseOverlaysDirty = true;
        labelsDirty = true;
        highlightStructureDirty = true;
        invalidateOverlayRefresh();
        if (jmAPI != null) {
            MapFrontiersClient.notifyCollectionOverlayFrontierGeometryChanged(this);
        }
    }

    private void invalidateFromDiscretization() {
        pathLayoutDirty = true;
        pathLayoutCache = null;
        baseOverlaysDirty = true;
        collectionBaseOverlaysDirty = false;
        labelsDirty = true;
        highlightStructureDirty = true;
        invalidateOverlayRefresh();
    }

    private void invalidateBasePresentation() {
        visualConfigDirty = true;
        visualConfigSnapshot = null;
        polygonUiPlanDirty = true;
        polygonUiPlanCache = null;
        baseOverlaysDirty = true;
        collectionBaseOverlaysDirty = true;
        labelsDirty = true;
        invalidateOverlayRefresh();
    }

    private void invalidateLabels() {
        labelsDirty = true;
        invalidateOverlayRefresh();
    }

    private void invalidateHighlightVisibility() {
        highlightVisibilityDirty = true;
        invalidateOverlayRefresh();
    }

    public void beginInteractiveEdit() {
        editingActivationSuppressed = true;
    }

    public void endInteractiveEdit() {
        editingActivationSuppressed = false;
        if (pendingActivationDirty) {
            pendingActivationDirty = false;
            if (jmAPI != null) {
                MapFrontiersClient.markFrontierActivationDirty();
            }
        }
    }

    private void markFrontierActivationDirty() {
        if (editingActivationSuppressed) {
            pendingActivationDirty = true;
            return;
        }

        if (jmAPI != null) {
            MapFrontiersClient.markFrontierActivationDirty();
        }
    }

    private boolean isFrontierVisible() {
        return ClientConfig.resolveVisibilityValue(ClientConfig.FRONTIER_VISIBILITY.get(), getVisibility(FrontierVisibility.Frontier));
    }

    private void showPolygonOverlays(List<PolygonOverlay> overlays) throws Exception {
        if (jmAPI == null) {
            return;
        }

        for (PolygonOverlay polygon : overlays) {
            jmAPI.show(polygon);
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

    private void removeOverlay() {
        try {
            for (PolygonOverlay polygon : polygonOverlays) {
                removePolygonOverlay(polygon);
            }

            for (PolygonOverlay polygon : collectionPolygonOverlays) {
                removePolygonOverlay(polygon);
            }

            for (PolygonOverlay polygon : highlightPolygonOverlays) {
                removePolygonOverlay(polygon);
            }

            for (MarkerOverlay marker : markerOverlays) {
                removeMarkerOverlay(marker);
            }

            for (MarkerOverlay marker : highlightMarkerOverlays) {
                removeMarkerOverlay(marker);
            }

            for (MarkerOverlay label : labelOverlays) {
                removeMarkerOverlay(label);
            }
        } finally {
            polygonOverlays.clear();
            collectionPolygonOverlays.clear();
            highlightPolygonOverlays.clear();
            markerOverlays.clear();
            highlightMarkerOverlays.clear();
            labelOverlays.clear();
        }
    }

    public void deleted() {
        removeOverlay();
        try {
            bannerRenderer.releaseTexture();
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error("Failed to release banner texture for frontier {}", id, t);
        }
    }

    private void removePolygonOverlay(PolygonOverlay polygon) {
        if (jmAPI == null) {
            return;
        }

        try {
            jmAPI.remove(polygon);
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error("Failed to remove polygon overlay for frontier {}", id, t);
        }
    }

    private void removeMarkerOverlay(MarkerOverlay marker) {
        if (jmAPI == null) {
            return;
        }

        try {
            jmAPI.remove(marker);
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error("Failed to remove marker overlay for frontier {}", id, t);
        }
    }

    public boolean isInsideBoundingBox(BlockPos pos, double padding) {
        if (topLeft == null || bottomRight == null) {
            return false;
        }

        return pos.getX() >= topLeft.getX() - padding
                && pos.getX() <= bottomRight.getX() + padding
                && pos.getZ() >= topLeft.getZ() - padding
                && pos.getZ() <= bottomRight.getZ() + padding;
    }

    public boolean pointIsInside(BlockPos pos, double maxDistanceToOpen) {
        if (frontierShape == FrontierShape.Vertex) {
            if (vertices.size() > 2) {
                return polygonArea != null && polygonArea.contains(pos.getX() + 0.5, pos.getZ() + 0.5);
            } else if (maxDistanceToOpen > 0.0) {
                return distanceToPolylineSq(pos, vertices, true) <= maxDistanceToOpen * maxDistanceToOpen;
            }
        } else if (frontierShape == FrontierShape.Path) {
            if (points.isEmpty()) {
                return false;
            }

            double maxDistanceSq = maxDistanceToOpen * maxDistanceToOpen;
            if (maxDistanceToOpen == 0.0) {
                maxDistanceSq = 0.0;
            }

            return distanceToPolylineSq(pos, points, false) <= maxDistanceSq;
        } else if (pos.getX() >= topLeft.getX() && pos.getX() <= bottomRight.getX() && pos.getZ() >= topLeft.getZ() && pos.getZ() <= bottomRight.getZ()) {
            return chunks.contains(new ChunkPos(pos));
        }

        return false;
    }

    public void selectClosestVertex(BlockPos pos, double limit) {
        if (frontierShape != FrontierShape.Vertex) {
            selectedPointIndex = -1;
            return;
        }

        double distance = limit * limit;
        int closest = -1;

        if (!vertices.isEmpty()) {
            synchronized (vertices) {
                for (int i = 0; i < vertices.size(); ++i) {
                    BlockPos vertex = vertices.get(i);
                    int y = vertex.getY();
                    double dist = vertex.distSqr(pos.atY(y));
                    if (dist <= distance) {
                        distance = dist;
                        closest = i;
                    }
                }
            }
        }

        selectedPointIndex = closest;
        MapFrontiersClient.updateSelectedFrontierMarker(personal, getDimension(), this);
    }

    public void selectClosestEdge(BlockPos pos) {
        if (frontierShape != FrontierShape.Vertex) {
            selectedPointIndex = -1;
            return;
        }

        double distance = Double.MAX_VALUE;
        int closest = -1;
        double angleSimilarity = -1.0;

        if (vertices.size() == 1) {
            closest = 0;
        } else if (vertices.size() > 1) {
            synchronized (vertices) {
                for (int i = 0; i < vertices.size(); ++i) {
                    Vec3 point = Vec3.atLowerCornerOf(pos);
                    int y1 = pos.getY();
                    Vec3 edge1 = Vec3.atLowerCornerOf(vertices.get(i).atY(y1));
                    int y = pos.getY();
                    Vec3 edge2 = Vec3.atLowerCornerOf(vertices.get((i + 1) % vertices.size()).atY(y));
                    double dist;
                    double dot;

                    if (edge1.equals(edge2)) {
                        dot = -1;
                        dist = point.distanceToSqr(edge1);
                    } else {
                        Vec3 closestPoint = closestPointToEdge(point, edge1, edge2);

                        if (!closestPoint.equals(edge1) && !closestPoint.equals(edge2)) {
                            dot = -1;
                        } else {
                            Vec3 edge = edge2.subtract(edge1);
                            Vec2 edgeDirection = new Vec2((float) edge.x, (float) edge.z).normalized();
                            Vec3 toPos;

                            if (closestPoint.equals(edge1)) {
                                toPos = point.subtract(edge1);
                            } else {
                                edgeDirection = edgeDirection.negated();
                                toPos = point.subtract(edge2);
                            }

                            Vec2 toPosDirection = new Vec2((float) toPos.x, (float) toPos.z).normalized();
                            dot = toPosDirection.dot(edgeDirection);
                        }

                        dist = point.distanceToSqr(closestPoint);
                    }

                    if (dist < distance) {
                        distance = dist;
                        closest = i;
                        angleSimilarity = dot;
                    } else if (dist == distance && dot > angleSimilarity) {
                        closest = i;
                        angleSimilarity = dot;
                    }
                }
            }
        }

        selectedPointIndex = closest;
        MapFrontiersClient.updateSelectedFrontierMarker(personal, getDimension(), this);
    }

    public void selectClosestPoint(BlockPos pos, double limit) {
        if (frontierShape != FrontierShape.Path) {
            selectedPointIndex = -1;
            return;
        }

        double distance = limit * limit;
        int closest = -1;

        if (!points.isEmpty()) {
            synchronized (points) {
                for (int i = 0; i < points.size(); ++i) {
                    BlockPos point = points.get(i);
                    int y = point.getY();
                    double dist = point.distSqr(pos.atY(y));
                    if (dist <= distance) {
                        distance = dist;
                        closest = i;
                    }
                }
            }
        }

        selectedPointIndex = closest;
        MapFrontiersClient.updateSelectedFrontierMarker(personal, getDimension(), this);
    }

    private static Vec3 closestPointToEdge(Vec3 point, Vec3 edge1, Vec3 edge2) {
        Vec3 edge = edge2.subtract(edge1);

        if ((edge.x == 0) && (edge.z == 0)) {
            return edge1;
        } else {
            double u = ((point.x - edge1.x) * edge.x + (point.z - edge1.z) * edge.z) / (edge.x * edge.x + edge.z * edge.z);

            if (u < 0.0) {
                return edge1;
            } else if (u > 1.0) {
                return edge2;
            } else {
                return new Vec3(edge1.x + u * edge.x, point.y, edge1.z + u * edge.z);
            }
        }
    }

    public void setCurrentPlayerAsOwner() {
        if (Minecraft.getInstance().player != null) {
            owner = new SettingsUser(Minecraft.getInstance().player);
        }
    }

    @Override
    public void setId(UUID id) {
        super.setId(id);
        hashDirty = true;
        invalidateAllOverlayLayers();
    }

    @Override
    public void addVertex(BlockPos pos) {
        addVertex(pos, selectedPointIndex + 1, ClientConfig.SNAP_DISTANCE.get());
        selectNextVertex();
    }

    public void addVertex(BlockPos pos, int index, int snapDistance) {
        if (snapDistance != 0) {
            pos = snapVertex(pos, snapDistance);
        }

        super.addVertex(pos, index);
        hashDirty = true;
        invalidateFromGeometry();
        markFrontierActivationDirty();
    }

    @Override
    public void removeVertex(int index) {
        super.removeVertex(index);
        hashDirty = true;
        invalidateFromGeometry();
        markFrontierActivationDirty();
    }

    @Override
    public void moveAllVertices(BlockPos delta) {
        super.moveAllVertices(delta);
        hashDirty = true;
        invalidateFromGeometry();
        markFrontierActivationDirty();
        MapFrontiersClient.updateSelectedFrontierMarker(personal, getDimension(), this);
    }

    @Override
    public boolean toggleChunk(ChunkPos chunk) {
        boolean added = super.toggleChunk(chunk);
        hashDirty = true;
        invalidateFromGeometry();
        markFrontierActivationDirty();
        return added;
    }

    @Override
    public boolean addChunk(ChunkPos chunk) {
        if (super.addChunk(chunk)) {
            hashDirty = true;
            invalidateFromGeometry();
            markFrontierActivationDirty();
            return true;
        }

        return false;
    }

    @Override
    public boolean removeChunk(ChunkPos chunk) {
        if (super.removeChunk(chunk)) {
            hashDirty = true;
            invalidateFromGeometry();
            markFrontierActivationDirty();
            return true;
        }

        return false;
    }

    @Override
    public void moveAllChunks(ChunkPos delta) {
        super.moveAllChunks(delta);
        hashDirty = true;
        invalidateFromGeometry();
        markFrontierActivationDirty();
    }

    public boolean hasChunk(ChunkPos chunk) {
        return chunks.contains(chunk);
    }

    public List<ChunkPos> getConnectedChunks(ChunkPos chunk) {
        List<ChunkPos> connected = new ArrayList<>();

        if (!hasChunk(chunk)) {
            return connected;
        }

        Set<ChunkPos> visited = new HashSet<>();
        visited.add(chunk);
        Set<ChunkPos> toCheck = new HashSet<>();
        toCheck.add(chunk);

        while (!toCheck.isEmpty()) {
            ChunkPos pos = toCheck.iterator().next();
            toCheck.remove(pos);
            connected.add(pos);

            ChunkPos posUp = new ChunkPos(pos.x, pos.z - 1);
            if (!visited.contains(posUp) && hasChunk(posUp)) {
                toCheck.add(posUp);
            }
            visited.add(posUp);

            ChunkPos posDown = new ChunkPos(pos.x, pos.z + 1);
            if (!visited.contains(posDown) && hasChunk(posDown)) {
                toCheck.add(posDown);
            }
            visited.add(posDown);

            ChunkPos posRight = new ChunkPos(pos.x + 1, pos.z);
            if (!visited.contains(posRight) && hasChunk(posRight)) {
                toCheck.add(posRight);
            }
            visited.add(posRight);

            ChunkPos posLeft = new ChunkPos(pos.x - 1, pos.z);
            if (!visited.contains(posLeft) && hasChunk(posLeft)) {
                toCheck.add(posLeft);
            }
            visited.add(posLeft);
        }

        return connected;
    }

    public List<ChunkPos> getClosedRegion(ChunkPos chunk) {
        List<ChunkPos> region = new ArrayList<>();

        if (hasChunk(chunk) || chunks.isEmpty()) {
            return region;
        }

        ChunkPos topLeft = new ChunkPos(this.topLeft);
        ChunkPos bottomRight = new ChunkPos(this.bottomRight);

        if (chunk.x <= topLeft.x || chunk.x >= bottomRight.x || chunk.z <= topLeft.z || chunk.z >= bottomRight.z) {
            return region;
        }

        Set<ChunkPos> visited = new HashSet<>();
        visited.add(chunk);
        Set<ChunkPos> toCheck = new HashSet<>();
        toCheck.add(chunk);

        while (!toCheck.isEmpty()) {
            ChunkPos pos = toCheck.iterator().next();
            toCheck.remove(pos);
            region.add(pos);

            ChunkPos posUp = new ChunkPos(pos.x, pos.z - 1);
            if (!visited.contains(posUp) && !hasChunk(posUp)) {
                if (posUp.z == topLeft.z) {
                    return new ArrayList<>();
                }
                toCheck.add(posUp);
            }
            visited.add(posUp);

            ChunkPos posDown = new ChunkPos(pos.x, pos.z + 1);
            if (!visited.contains(posDown) && !hasChunk(posDown)) {
                if (posUp.z == bottomRight.z) {
                    return new ArrayList<>();
                }
                toCheck.add(posDown);
            }
            visited.add(posDown);

            ChunkPos posRight = new ChunkPos(pos.x + 1, pos.z);
            if (!visited.contains(posRight) && !hasChunk(posRight)) {
                if (posUp.x == bottomRight.x) {
                    return new ArrayList<>();
                }
                toCheck.add(posRight);
            }
            visited.add(posRight);

            ChunkPos posLeft = new ChunkPos(pos.x - 1, pos.z);
            if (!visited.contains(posLeft) && !hasChunk(posLeft)) {
                if (posUp.x == topLeft.x) {
                    return new ArrayList<>();
                }
                toCheck.add(posLeft);
            }
            visited.add(posLeft);
        }

        return region;
    }

    public void moveSelectedVertex(BlockPos pos, float snapDistance) {
        if (selectedPointIndex < 0 || selectedPointIndex >= vertices.size()) {
            return;
        }

        if (snapDistance != 0) {
            pos = snapVertex(pos, snapDistance);
        }

        super.moveVertex(pos, selectedPointIndex);
        hashDirty = true;
        invalidateFromGeometry();
        markFrontierActivationDirty();
        MapFrontiersClient.updateSelectedFrontierMarker(personal, getDimension(), this);
    }

    public void moveSelectedPoint(BlockPos pos, float snapDistance) {
        if (selectedPointIndex < 0 || selectedPointIndex >= points.size()) {
            return;
        }

        if (snapDistance != 0) {
            pos = snapVertex(pos, snapDistance);
        }

        super.movePoint(pos, selectedPointIndex);
        hashDirty = true;
        invalidateFromGeometry();
        markFrontierActivationDirty();
        MapFrontiersClient.updateSelectedFrontierMarker(personal, getDimension(), this);
    }

    @Override
    public void setName1(String name) {
        super.setName1(name);
        hashDirty = true;
        invalidateLabels();
    }

    @Override
    public void setName2(String name) {
        super.setName2(name);
        hashDirty = true;
        invalidateLabels();
    }

    @Override
    public void setVisibility(FrontierVisibility visibility, boolean enable) {
        super.setVisibility(visibility, enable);
        setVisibilityOverride(MapFrontiersClient.getLocalOverrides().getVisibility(id));
        hashDirty = true;
        invalidateBasePresentation();
        markFrontierActivationDirty();
    }

    @Override
    public void toggleVisibility(FrontierVisibility visibility) {
        super.toggleVisibility(visibility);
        setVisibilityOverride(MapFrontiersClient.getLocalOverrides().getVisibility(id));
        hashDirty = true;
        invalidateBasePresentation();
        markFrontierActivationDirty();
    }

    public void setVisibilityOverride(Pair<FrontierVisibilityData, FrontierVisibilityMask> visibilityOverride) {
        effectiveVisibilityData = new FrontierVisibilityData(visibilityData);
        effectiveVisibilityData.applyOverride(visibilityOverride.first(), visibilityOverride.second());
        invalidateBasePresentation();
        markFrontierActivationDirty();
    }

    @Override
    public boolean getVisibility(FrontierVisibility visibility) {
        return effectiveVisibilityData.get(visibility);
    }

    public boolean isVisibleOnFullscreenMap(Context.MapType mapType) {
        return isVisibleOnMap(Context.UI.Fullscreen, mapType);
    }

    public boolean isVisibleOnMap(Context.UI ui, Context.MapType mapType) {
        if (!isFrontierVisible()) {
            return false;
        }

        boolean uiVisible = switch (ui) {
            case Fullscreen -> ClientConfig.resolveVisibilityValue(ClientConfig.FULLSCREEN_VISIBILITY.get(), getVisibility(FrontierVisibility.Fullscreen));
            case Minimap -> ClientConfig.resolveVisibilityValue(ClientConfig.MINIMAP_VISIBILITY.get(), getVisibility(FrontierVisibility.Minimap));
            case Webmap -> ClientConfig.resolveVisibilityValue(ClientConfig.WEBMAP_VISIBILITY.get(), getVisibility(FrontierVisibility.Webmap));
            default -> false;
        };
        if (!uiVisible) {
            return false;
        }

        return switch (ui) {
            case Fullscreen -> switch (mapType) {
                case Day -> ClientConfig.resolveVisibilityValue(ClientConfig.FULLSCREEN_DAY_VISIBILITY.get(), getVisibility(FrontierVisibility.FullscreenDay));
                case Night -> ClientConfig.resolveVisibilityValue(ClientConfig.FULLSCREEN_NIGHT_VISIBILITY.get(), getVisibility(FrontierVisibility.FullscreenNight));
                case Underground -> ClientConfig.resolveVisibilityValue(ClientConfig.FULLSCREEN_UNDERGROUND_VISIBILITY.get(),
                        getVisibility(FrontierVisibility.FullscreenUnderground));
                case Topo -> ClientConfig.resolveVisibilityValue(ClientConfig.FULLSCREEN_TOPO_VISIBILITY.get(), getVisibility(FrontierVisibility.FullscreenTopo));
                case Biome -> ClientConfig.resolveVisibilityValue(ClientConfig.FULLSCREEN_BIOME_VISIBILITY.get(), getVisibility(FrontierVisibility.FullscreenBiome));
            };
            case Minimap -> switch (mapType) {
                case Day -> ClientConfig.resolveVisibilityValue(ClientConfig.MINIMAP_DAY_VISIBILITY.get(), getVisibility(FrontierVisibility.MinimapDay));
                case Night -> ClientConfig.resolveVisibilityValue(ClientConfig.MINIMAP_NIGHT_VISIBILITY.get(), getVisibility(FrontierVisibility.MinimapNight));
                case Underground -> ClientConfig.resolveVisibilityValue(ClientConfig.MINIMAP_UNDERGROUND_VISIBILITY.get(),
                        getVisibility(FrontierVisibility.MinimapUnderground));
                case Topo -> ClientConfig.resolveVisibilityValue(ClientConfig.MINIMAP_TOPO_VISIBILITY.get(), getVisibility(FrontierVisibility.MinimapTopo));
                case Biome -> ClientConfig.resolveVisibilityValue(ClientConfig.MINIMAP_BIOME_VISIBILITY.get(), getVisibility(FrontierVisibility.MinimapBiome));
            };
            case Webmap -> switch (mapType) {
                case Day -> ClientConfig.resolveVisibilityValue(ClientConfig.WEBMAP_DAY_VISIBILITY.get(), getVisibility(FrontierVisibility.WebmapDay));
                case Night -> ClientConfig.resolveVisibilityValue(ClientConfig.WEBMAP_NIGHT_VISIBILITY.get(), getVisibility(FrontierVisibility.WebmapNight));
                case Underground -> ClientConfig.resolveVisibilityValue(ClientConfig.WEBMAP_UNDERGROUND_VISIBILITY.get(),
                        getVisibility(FrontierVisibility.WebmapUnderground));
                case Topo -> ClientConfig.resolveVisibilityValue(ClientConfig.WEBMAP_TOPO_VISIBILITY.get(), getVisibility(FrontierVisibility.WebmapTopo));
                case Biome -> ClientConfig.resolveVisibilityValue(ClientConfig.WEBMAP_BIOME_VISIBILITY.get(), getVisibility(FrontierVisibility.WebmapBiome));
            };
            default -> false;
        };
    }

    public void setVisibilityData(FrontierVisibilityData visibilityData) {
        super.setVisibilityData(visibilityData);
        setVisibilityOverride(MapFrontiersClient.getLocalOverrides().getVisibility(id));
        hashDirty = true;
        invalidateBasePresentation();
        markFrontierActivationDirty();
    }

    @Override
    public void setColor(int color) {
        super.setColor(color);
        hashDirty = true;
        invalidateBasePresentation();
    }

    @Override
    public void setPathStyle(PathStyle pathStyle) {
        super.setPathStyle(pathStyle);
        hashDirty = true;
        invalidateFromDiscretization();
    }

    @Override
    public void setDimension(ResourceKey<Level> dimension) {
        super.setDimension(dimension);
        hashDirty = true;
        markFrontierActivationDirty();
    }

    @Override
    public void setBannerData(@Nullable BannerData bannerData) {
        super.setBannerData(bannerData);
        hashDirty = true;
        invalidateLabels();
        refreshEffectiveBannerRenderer();
    }

    @Override
    public void setBannerRotation(int rotation) {
        if (hasBanner()) {
            super.setBannerRotation(rotation);
            hashDirty = true;
            invalidateLabels();
            refreshEffectiveBannerRenderer();
        }
    }

    @Override
    public boolean hasBanner() {
        return super.hasBanner();
    }

    @Override
    public void setInheritCollectionBanner(boolean inheritCollectionBanner) {
        if (getInheritCollectionBanner() == inheritCollectionBanner) {
            return;
        }

        super.setInheritCollectionBanner(inheritCollectionBanner);
        hashDirty = true;
        invalidateLabels();
        refreshEffectiveBannerRenderer();
    }

    @Override
    public void addUserShared(SettingsUserShared userShared) {
        super.addUserShared(userShared);
        hashDirty = true;
    }

    public BlockPos getClosestVertex(BlockPos vertex, double belowDistance) {
        BlockPos closest = null;
        double closestDistance = belowDistance;

        if (frontierShape == FrontierShape.Path) {
            synchronized (points) {
                for (BlockPos point : points) {
                    double distance = point.distSqr(vertex);
                    if (distance <= closestDistance) {
                        closestDistance = distance;
                        closest = point;
                    }
                }
            }
        } else {
            for (PolygonOverlay overlay : polygonOverlays) {
                for (BlockPos v : overlay.getOuterArea().getPoints()) {
                    double distance = v.distSqr(vertex);
                    if (distance <= closestDistance) {
                        closestDistance = distance;
                        closest = v;
                    }
                }

                if (overlay.getHoles() != null) {
                    for (MapPolygon hole : overlay.getHoles()) {
                        for (BlockPos v : hole.getPoints()) {
                            double distance = v.distSqr(vertex);
                            if (distance <= closestDistance) {
                                closestDistance = distance;
                                closest = v;
                            }
                        }
                    }
                }
            }
        }

        return closest;
    }

    public int[] getBannerBounds(int x, int y, int scale) {
        int width = 22 * scale;
        int height = 40 * scale;
        float centerX = x + width / 2f;
        float centerY = y + height / 2f;

        double radians = Math.toRadians(bannerRenderer.getRotation());
        double cos = Math.abs(Math.cos(radians));
        double sin = Math.abs(Math.sin(radians));

        float rotatedWidth = (float)(width * cos + height * sin);
        float rotatedHeight = (float)(width * sin + height * cos);

        int minX = (int) Math.floor(centerX - rotatedWidth / 2f);
        int maxX = (int) Math.ceil(centerX + rotatedWidth / 2f);
        int minY = (int) Math.floor(centerY - rotatedHeight / 2f);
        int maxY = (int) Math.ceil(centerY + rotatedHeight / 2f);

        return new int[] { minX, minY, maxX, maxY };
    }

    public BannerRenderer getBannerRenderer() {
        return bannerRenderer;
    }

    public void recreateBannerRenderer() {
        refreshEffectiveBannerRenderer();
    }

    public void removeSelectedVertex() {
        if (selectedPointIndex < 0) {
            return;
        }

        super.removeVertex(selectedPointIndex);
        if (vertices.isEmpty()) {
            selectedPointIndex = -1;
        } else if (selectedPointIndex > 0) {
            --selectedPointIndex;
        } else {
            selectedPointIndex = vertices.size() - 1;
        }

        MapFrontiersClient.updateSelectedFrontierMarker(personal, getDimension(), this);

        hashDirty = true;
        invalidateFromGeometry();
        markFrontierActivationDirty();
    }

    public void removeSelectedPoint() {
        if (selectedPointIndex < 0) {
            return;
        }

        super.removePoint(selectedPointIndex);
        if (points.isEmpty()) {
            selectedPointIndex = -1;
        } else if (selectedPointIndex > 0) {
            --selectedPointIndex;
        } else {
            selectedPointIndex = 0;
        }

        MapFrontiersClient.updateSelectedFrontierMarker(personal, getDimension(), this);

        hashDirty = true;
        invalidateFromGeometry();
        markFrontierActivationDirty();
    }

    public void selectNextVertex() {
        ++selectedPointIndex;
        if (selectedPointIndex >= vertices.size()) {
            selectedPointIndex = -1;
        }
        MapFrontiersClient.updateSelectedFrontierMarker(personal, getDimension(), this);
    }

    public int getSelectedVertexIndex() {
        return frontierShape == FrontierShape.Vertex ? selectedPointIndex : -1;
    }

    public int getSelectedPointIndex() {
        return frontierShape == FrontierShape.Path ? selectedPointIndex : -1;
    }

    public int getSelectedEditablePointIndex() {
        return switch (frontierShape) {
            case Vertex, Path -> selectedPointIndex;
            case Chunk -> -1;
        };
    }

    public void clearSelectedEditablePoint() {
        selectedPointIndex = -1;
        MapFrontiersClient.updateSelectedFrontierMarker(personal, getDimension(), this);
    }

    public @Nullable BlockPos getSelectedEditablePoint() {
        if (frontierShape == FrontierShape.Path && selectedPointIndex >= 0 && selectedPointIndex < points.size()) {
            return points.get(selectedPointIndex);
        }
        if (frontierShape == FrontierShape.Vertex && selectedPointIndex >= 0 && selectedPointIndex < vertices.size()) {
            return vertices.get(selectedPointIndex);
        }

        return null;
    }

    public void moveSelectedEditablePoint(BlockPos pos, float snapDistance) {
        if (frontierShape == FrontierShape.Path) {
            moveSelectedPoint(pos, snapDistance);
        } else if (frontierShape == FrontierShape.Vertex) {
            moveSelectedVertex(pos, snapDistance);
        }
    }

    public void moveAllPathPoints(BlockPos delta) {
        super.moveAllPoints(delta);
        hashDirty = true;
        invalidateFromGeometry();
        markFrontierActivationDirty();
        MapFrontiersClient.updateSelectedFrontierMarker(personal, getDimension(), this);
    }

    public void addPathPointBeforeStart(BlockPos pos) {
        if (frontierShape != FrontierShape.Path) {
            return;
        }

        pos = snapVertex(pos, ClientConfig.SNAP_DISTANCE.get());
        super.addPoint(pos, 0);
        selectedPointIndex = 0;
        hashDirty = true;
        invalidateFromGeometry();
        markFrontierActivationDirty();
        MapFrontiersClient.updateSelectedFrontierMarker(personal, getDimension(), this);
    }

    public void addPathPointAfterEnd(BlockPos pos) {
        if (frontierShape != FrontierShape.Path) {
            return;
        }

        pos = snapVertex(pos, ClientConfig.SNAP_DISTANCE.get());
        int index = points.size();
        super.addPoint(pos, index);
        selectedPointIndex = index;
        hashDirty = true;
        invalidateFromGeometry();
        markFrontierActivationDirty();
        MapFrontiersClient.updateSelectedFrontierMarker(personal, getDimension(), this);
    }

    public void insertPathPoint(BlockPos pos) {
        if (frontierShape != FrontierShape.Path) {
            return;
        }

        pos = snapVertex(pos, ClientConfig.SNAP_DISTANCE.get());
        int insertIndex = getSmartInsertIndex(pos);
        if (insertIndex < 0) {
            addPathPointAfterEnd(pos);
            return;
        }

        super.addPoint(pos, insertIndex);
        selectedPointIndex = insertIndex;
        hashDirty = true;
        invalidateFromGeometry();
        markFrontierActivationDirty();
        MapFrontiersClient.updateSelectedFrontierMarker(personal, getDimension(), this);
    }

    public void invertPathDirection() {
        if (frontierShape != FrontierShape.Path || points.size() < 2) {
            return;
        }

        synchronized (points) {
            Collections.reverse(points);
        }
        if (selectedPointIndex >= 0 && selectedPointIndex < points.size()) {
            selectedPointIndex = points.size() - 1 - selectedPointIndex;
        }

        hashDirty = true;
        invalidateFromDiscretization();
        MapFrontiersClient.updateSelectedFrontierMarker(personal, getDimension(), this);
    }

    private int getSmartInsertIndex(BlockPos pos) {
        if (points.isEmpty()) {
            return 0;
        }

        if (points.size() == 1) {
            return 1;
        }

        Vec3 point = Vec3.atLowerCornerOf(pos);
        int y = pos.getY();
        double bestSegmentDistance = Double.POSITIVE_INFINITY;
        int bestSegmentInsertIndex = -1;

        synchronized (points) {
            for (int i = 0; i < points.size() - 1; ++i) {
                Vec3 edge1 = Vec3.atLowerCornerOf(points.get(i).atY(y));
                Vec3 edge2 = Vec3.atLowerCornerOf(points.get(i + 1).atY(y));
                Vec3 closestPoint = closestPointToEdge(point, edge1, edge2);
                if (closestPoint.equals(edge1) || closestPoint.equals(edge2)) {
                    continue;
                }

                double distance = closestPoint.distanceToSqr(point);
                if (distance < bestSegmentDistance) {
                    bestSegmentDistance = distance;
                    bestSegmentInsertIndex = i + 1;
                }
            }

            double startDistance = point.distanceToSqr(Vec3.atLowerCornerOf(points.getFirst().atY(y)));
            double endDistance = point.distanceToSqr(Vec3.atLowerCornerOf(points.getLast().atY(y)));
            if (bestSegmentInsertIndex != -1 && bestSegmentDistance < Math.min(startDistance, endDistance)) {
                return bestSegmentInsertIndex;
            }

            if (startDistance < endDistance) {
                return 0;
            }

            if (endDistance < startDistance) {
                return points.size();
            }
        }

        if (selectedPointIndex == 0) {
            return 0;
        }

        return points.size();
    }

    private void clampSelectedEditablePoint() {
        int size = switch (frontierShape) {
            case Vertex -> vertices.size();
            case Path -> points.size();
            case Chunk -> 0;
        };

        if (size == 0) {
            selectedPointIndex = -1;
        } else if (selectedPointIndex >= size) {
            selectedPointIndex = size - 1;
        }
    }

    public void setHighlighted(boolean highlighted) {
        this.highlighted = highlighted;
        invalidateHighlightVisibility();
    }

    public BlockPos getCenter() {
        if (frontierShape == FrontierShape.Path && points.size() == 1) {
            return points.getFirst();
        }

        return new BlockPos((topLeft.getX() + bottomRight.getX()) / 2, 70, (topLeft.getZ() + bottomRight.getZ()) / 2);
    }

    private BlockPos snapVertex(BlockPos vertex, float snapDistance) {
        vertex = vertex.atY(70);
        BlockPos closest = vertex;
        double closestDistance = snapDistance * snapDistance;

        for (FrontierOverlay frontier : MapFrontiersClient.getFrontiers(true, dimension)) {
            if (frontier == this) {
                continue;
            }

            BlockPos v = frontier.getClosestVertex(vertex, closestDistance);
            if (v != null) {
                double dist = v.distSqr(vertex);
                if (dist <= closestDistance) {
                    closest = v;
                    closestDistance = dist;
                }
            }
        }

        for (FrontierOverlay frontier : MapFrontiersClient.getFrontiers(false, dimension)) {
            if (frontier == this) {
                continue;
            }

            BlockPos v = frontier.getClosestVertex(vertex, closestDistance);
            if (v != null) {
                double dist = v.distSqr(vertex);
                if (dist <= closestDistance) {
                    closest = v;
                    closestDistance = dist;
                }
            }
        }

        return closest;
    }

    public void recalculateOverlays() {
        rebuildOverlayNow();
    }

    private void rebuildGeometryCache() {
        polygonRenderGeometries.clear();
        updateBounds();

        area = 0;
        perimeter = 0.f;
        polygonArea = null;

        if (frontierShape == FrontierShape.Vertex) {
            rebuildVertexGeometryCache();
        } else if (frontierShape == FrontierShape.Path) {
            rebuildPathGeometryCache();
        } else {
            rebuildChunkGeometryCache();
        }
    }

    private @Nullable PathLayoutCache ensurePathLayoutCache() {
        if (frontierShape != FrontierShape.Path) {
            pathLayoutCache = null;
            pathLayoutDirty = false;
            return null;
        }

        if (!pathLayoutDirty && pathLayoutCache != null) {
            return pathLayoutCache;
        }

        synchronized (points) {
            pathLayoutCache = buildPathLayoutCache(new ArrayList<>(points));
        }
        pathLayoutDirty = false;
        return pathLayoutCache;
    }

    private VisualConfigSnapshot ensureVisualConfigSnapshot() {
        if (!visualConfigDirty && visualConfigSnapshot != null) {
            return visualConfigSnapshot;
        }

        visualConfigSnapshot = new VisualConfigSnapshot(
                buildUiVisualConfig(Context.UI.Fullscreen),
                buildUiVisualConfig(Context.UI.Minimap),
                buildUiVisualConfig(Context.UI.Webmap));
        visualConfigDirty = false;
        return visualConfigSnapshot;
    }

    private @Nullable PolygonUiPlanCache ensurePolygonUiPlanCache() {
        if (frontierShape == FrontierShape.Path) {
            polygonUiPlanCache = null;
            polygonUiPlanDirty = false;
            return null;
        }

        if (!polygonUiPlanDirty && polygonUiPlanCache != null) {
            return polygonUiPlanCache;
        }

        VisualConfigSnapshot visualConfig = ensureVisualConfigSnapshot();
        List<PolygonUiPlanEntry> entries = new ArrayList<>();
        for (PolygonRenderGeometry geometry : polygonRenderGeometries) {
            Area overlayArea = buildOverlayArea(geometry.polygon(), geometry.holes());
            for (UiVisualConfig uiConfig : visualConfig.uiConfigs()) {
                if (!uiConfig.baseVisible()) {
                    continue;
                }

                entries.add(new PolygonUiPlanEntry(uiConfig.ui(),
                        uiConfig.activeMapTypes(),
                        uiConfig.labelVisibility(),
                        geometry,
                        overlayArea));
            }
        }

        polygonUiPlanCache = new PolygonUiPlanCache(List.copyOf(entries));
        polygonUiPlanDirty = false;
        return polygonUiPlanCache;
    }

    private UiVisualConfig buildUiVisualConfig(Context.UI ui) {
        return switch (ui) {
            case Fullscreen -> new UiVisualConfig(
                    Context.UI.Fullscreen,
                    ClientConfig.resolveVisibilityValue(ClientConfig.FULLSCREEN_VISIBILITY.get(), getVisibility(FrontierVisibility.Fullscreen)),
                    getActiveMapTypes(
                            ClientConfig.resolveVisibilityValue(ClientConfig.FULLSCREEN_DAY_VISIBILITY.get(), getVisibility(FrontierVisibility.FullscreenDay)),
                            ClientConfig.resolveVisibilityValue(ClientConfig.FULLSCREEN_NIGHT_VISIBILITY.get(), getVisibility(FrontierVisibility.FullscreenNight)),
                            ClientConfig.resolveVisibilityValue(ClientConfig.FULLSCREEN_UNDERGROUND_VISIBILITY.get(), getVisibility(FrontierVisibility.FullscreenUnderground)),
                            ClientConfig.resolveVisibilityValue(ClientConfig.FULLSCREEN_TOPO_VISIBILITY.get(), getVisibility(FrontierVisibility.FullscreenTopo)),
                            ClientConfig.resolveVisibilityValue(ClientConfig.FULLSCREEN_BIOME_VISIBILITY.get(), getVisibility(FrontierVisibility.FullscreenBiome))),
                    new LabelVisibility(
                            ClientConfig.resolveVisibilityValue(ClientConfig.FULLSCREEN_NAME_VISIBILITY.get(), getVisibility(FrontierVisibility.FullscreenName)),
                            ClientConfig.resolveVisibilityValue(ClientConfig.FULLSCREEN_COLLECTION_VISIBILITY.get(), getVisibility(FrontierVisibility.FullscreenCollection)),
                            ClientConfig.resolveVisibilityValue(ClientConfig.FULLSCREEN_OWNER_VISIBILITY.get(), getVisibility(FrontierVisibility.FullscreenOwner)),
                            ClientConfig.resolveVisibilityValue(ClientConfig.FULLSCREEN_BANNER_VISIBILITY.get(), getVisibility(FrontierVisibility.FullscreenBanner))));
            case Minimap -> new UiVisualConfig(
                    Context.UI.Minimap,
                    ClientConfig.resolveVisibilityValue(ClientConfig.MINIMAP_VISIBILITY.get(), getVisibility(FrontierVisibility.Minimap)),
                    getActiveMapTypes(
                            ClientConfig.resolveVisibilityValue(ClientConfig.MINIMAP_DAY_VISIBILITY.get(), getVisibility(FrontierVisibility.MinimapDay)),
                            ClientConfig.resolveVisibilityValue(ClientConfig.MINIMAP_NIGHT_VISIBILITY.get(), getVisibility(FrontierVisibility.MinimapNight)),
                            ClientConfig.resolveVisibilityValue(ClientConfig.MINIMAP_UNDERGROUND_VISIBILITY.get(), getVisibility(FrontierVisibility.MinimapUnderground)),
                            ClientConfig.resolveVisibilityValue(ClientConfig.MINIMAP_TOPO_VISIBILITY.get(), getVisibility(FrontierVisibility.MinimapTopo)),
                            ClientConfig.resolveVisibilityValue(ClientConfig.MINIMAP_BIOME_VISIBILITY.get(), getVisibility(FrontierVisibility.MinimapBiome))),
                    new LabelVisibility(
                            ClientConfig.resolveVisibilityValue(ClientConfig.MINIMAP_NAME_VISIBILITY.get(), getVisibility(FrontierVisibility.MinimapName)),
                            ClientConfig.resolveVisibilityValue(ClientConfig.MINIMAP_COLLECTION_VISIBILITY.get(), getVisibility(FrontierVisibility.MinimapCollection)),
                            ClientConfig.resolveVisibilityValue(ClientConfig.MINIMAP_OWNER_VISIBILITY.get(), getVisibility(FrontierVisibility.MinimapOwner)),
                            ClientConfig.resolveVisibilityValue(ClientConfig.MINIMAP_BANNER_VISIBILITY.get(), getVisibility(FrontierVisibility.MinimapBanner))));
            case Webmap -> new UiVisualConfig(
                    Context.UI.Webmap,
                    ClientConfig.resolveVisibilityValue(ClientConfig.WEBMAP_VISIBILITY.get(), getVisibility(FrontierVisibility.Webmap)),
                    getActiveMapTypes(
                            ClientConfig.resolveVisibilityValue(ClientConfig.WEBMAP_DAY_VISIBILITY.get(), getVisibility(FrontierVisibility.WebmapDay)),
                            ClientConfig.resolveVisibilityValue(ClientConfig.WEBMAP_NIGHT_VISIBILITY.get(), getVisibility(FrontierVisibility.WebmapNight)),
                            ClientConfig.resolveVisibilityValue(ClientConfig.WEBMAP_UNDERGROUND_VISIBILITY.get(), getVisibility(FrontierVisibility.WebmapUnderground)),
                            ClientConfig.resolveVisibilityValue(ClientConfig.WEBMAP_TOPO_VISIBILITY.get(), getVisibility(FrontierVisibility.WebmapTopo)),
                            ClientConfig.resolveVisibilityValue(ClientConfig.WEBMAP_BIOME_VISIBILITY.get(), getVisibility(FrontierVisibility.WebmapBiome))),
                    new LabelVisibility(
                            ClientConfig.resolveVisibilityValue(ClientConfig.WEBMAP_NAME_VISIBILITY.get(), getVisibility(FrontierVisibility.WebmapName)),
                            ClientConfig.resolveVisibilityValue(ClientConfig.WEBMAP_COLLECTION_VISIBILITY.get(), getVisibility(FrontierVisibility.WebmapCollection)),
                            ClientConfig.resolveVisibilityValue(ClientConfig.WEBMAP_OWNER_VISIBILITY.get(), getVisibility(FrontierVisibility.WebmapOwner)),
                            ClientConfig.resolveVisibilityValue(ClientConfig.WEBMAP_BANNER_VISIBILITY.get(), getVisibility(FrontierVisibility.WebmapBanner))));
            default -> new UiVisualConfig(ui, false, new Context.MapType[0], new LabelVisibility(false, false, false, false));
        };
    }

    private PathLayoutCache buildPathLayoutCache(List<BlockPos> pathPoints) {
        if (pathPoints.isEmpty()) {
            return new PathLayoutCache(List.of(), List.of(), List.of());
        }

        if (pathPoints.size() == 1) {
            ResourceLocation markerId = getPathSinglePointMarkerId();
            PathPointLayout pointLayout = new PathPointLayout(pathPoints.getFirst(), markerId, 0.f, getPathMarkerClearancePx(markerId));
            List<PathLabelAnchor> labelAnchors = List.of();
            if (pathStyle.labelAtStart || pathStyle.labelAtMiddle || pathStyle.labelAtEnd) {
                BlockPos point = pathPoints.getFirst();
                labelAnchors = List.of(new PathLabelAnchor(point.getX(), point.getZ(), 0.0, 1.0, pointLayout.markerClearancePx()));
            }
            return new PathLayoutCache(List.of(pointLayout), List.of(), labelAnchors);
        }

        List<PathSegmentLayout> segmentLayouts = new ArrayList<>(Math.max(0, pathPoints.size() - 1));
        for (int i = 0; i < pathPoints.size() - 1; ++i) {
            BlockPos from = pathPoints.get(i);
            BlockPos to = pathPoints.get(i + 1);
            float rotation = getSegmentRotation(from, to);
            double length = Math.sqrt(to.distSqr(from));
            segmentLayouts.add(new PathSegmentLayout(
                    from,
                    to,
                    rotation,
                    pathStyle.segmentMarker,
                    getPathSegmentSpacingMultiplier(pathStyle.segmentMarker),
                    length,
                    List.copyOf(getDiscreteInteriorLinePositions(from, to))));
        }

        List<PathPointLayout> pointLayouts = new ArrayList<>(pathPoints.size());
        for (int i = 0; i < pathPoints.size(); ++i) {
            ResourceLocation markerId = getPathPointMarkerId(pathPoints.size(), i);
            float rotation = i < segmentLayouts.size()
                    ? segmentLayouts.get(i).rotation()
                    : segmentLayouts.getLast().rotation();
            pointLayouts.add(new PathPointLayout(pathPoints.get(i), markerId, rotation, getPathMarkerClearancePx(markerId)));
        }

        return new PathLayoutCache(pointLayouts, segmentLayouts, buildPathLabelAnchors(pathPoints, pointLayouts, segmentLayouts));
    }

    private List<PathLabelAnchor> buildPathLabelAnchors(List<BlockPos> pathPoints,
                                                        List<PathPointLayout> pointLayouts,
                                                        List<PathSegmentLayout> segmentLayouts) {
        List<PathLabelAnchor> anchors = new ArrayList<>();
        if (pathStyle.labelAtStart) {
            anchors.add(getPathEndpointLabelAnchor(pathPoints.getFirst(), pathPoints.get(1), pointLayouts.getFirst().markerClearancePx()));
        }
        if (pathStyle.labelAtMiddle) {
            anchors.add(getPathMidpointLabelAnchor(pathPoints, segmentLayouts));
        }
        if (pathStyle.labelAtEnd) {
            anchors.add(getPathEndpointLabelAnchor(pathPoints.getLast(), pathPoints.get(pathPoints.size() - 2),
                    pointLayouts.getLast().markerClearancePx()));
        }
        return List.copyOf(anchors);
    }

    private void rebuildBaseOverlays() {
        if (frontierShape == FrontierShape.Path) {
            rebuildPathBaseOverlays();
        } else {
            rebuildPolygonBaseOverlays();
        }
    }

    private void rebuildCollectionBaseOverlays() {
        if (frontierShape == FrontierShape.Path || previewCollectionStyleEnabled) {
            clearCollectionPolygonOverlays();
            return;
        }

        rebuildPolygonCollectionOverlays();
    }

    private void rebuildLabels() {
        if (frontierShape == FrontierShape.Path) {
            rebuildPathLabels();
        } else {
            rebuildPolygonLabelsFromCurrentOverlays();
        }
    }

    private void rebuildHighlightOverlays() {
        if (frontierShape == FrontierShape.Path) {
            rebuildPathHighlightOverlays();
        } else {
            rebuildPolygonHighlightOverlays();
        }
    }

    private void rebuildPolygonBaseOverlays() {
        hidePolygonOverlays(polygonOverlays);
        polygonOverlays.clear();

        PolygonUiPlanCache polygonUiPlan = ensurePolygonUiPlanCache();
        if (polygonUiPlan == null || polygonUiPlan.entries().isEmpty()) {
            return;
        }

        ShapeProperties shapeProps = createBaseShapeProperties();
        for (PolygonUiPlanEntry entry : polygonUiPlan.entries()) {
            polygonOverlays.add(createPolygonOverlay(shapeProps, entry, resolveCollectionNormalMinZoom(entry), 0));
        }

        if (isFrontierVisible()) {
            showPolygonOverlaysQuietly(polygonOverlays);
        }
    }

    private void rebuildPolygonCollectionOverlays() {
        clearCollectionPolygonOverlays();

        PolygonUiPlanCache polygonUiPlan = ensurePolygonUiPlanCache();
        if (polygonUiPlan == null || polygonUiPlan.entries().isEmpty()) {
            return;
        }

        ShapeProperties shapeProps = createCollectionShapeProperties();
        for (PolygonUiPlanEntry entry : polygonUiPlan.entries()) {
            if (!shouldRenderCollectionView(entry.ui())) {
                continue;
            }

            int collectionMaxZoom = resolveCollectionMaxZoom(entry.ui());
            if (collectionMaxZoom <= 0) {
                continue;
            }

            collectionPolygonOverlays.add(createPolygonOverlay(shapeProps, entry, entry.minZoom(), collectionMaxZoom));
        }

        if (isFrontierVisible()) {
            showPolygonOverlaysQuietly(collectionPolygonOverlays);
        }
    }

    private void rebuildPolygonLabelsFromCurrentOverlays() {
        hideMarkerOverlays(labelOverlays);
        labelOverlays.clear();

        PolygonUiPlanCache polygonUiPlan = ensurePolygonUiPlanCache();
        if (polygonUiPlan == null || polygonUiPlan.entries().isEmpty()) {
            return;
        }

        Map<LabelPlacementKey, FrontierLabelPlacementSolver.LabelPlacement> placementCache = new HashMap<>();
        for (PolygonUiPlanEntry entry : polygonUiPlan.entries()) {
            addPolygonLabelOverlays(entry, placementCache);
        }

        if (isFrontierVisible()) {
            showMarkerOverlaysQuietly(labelOverlays);
        }
    }

    private void clearCollectionPolygonOverlays() {
        hidePolygonOverlays(collectionPolygonOverlays);
        collectionPolygonOverlays.clear();
    }

    private void rebuildPolygonHighlightOverlays() {
        hidePolygonOverlays(highlightPolygonOverlays);
        highlightPolygonOverlays.clear();

        if (!highlighted) {
            return;
        }

        ShapeProperties highlightShapeProps = createHighlightShapeProperties();
        for (PolygonRenderGeometry geometry : polygonRenderGeometries) {
            addHighlightPolygonOverlay(highlightShapeProps, geometry);
        }
        showPolygonOverlaysQuietly(highlightPolygonOverlays);
    }

    private void rebuildPathGeometryCache() {
        synchronized (points) {
            if (points.size() > 1) {
                BlockPos last = points.getFirst();
                for (int i = 1; i < points.size(); ++i) {
                    BlockPos point = points.get(i);
                    perimeter += (float) Math.sqrt(point.distSqr(last));
                    last = point;
                }
            }

            area = perimeter;
        }
    }

    private void rebuildPathBaseOverlays() {
        hideMarkerOverlays(markerOverlays);
        markerOverlays.clear();

        PathLayoutCache layoutCache = ensurePathLayoutCache();
        if (layoutCache == null || layoutCache.pointLayouts().isEmpty()) {
            return;
        }

        VisualConfigSnapshot visualConfig = ensureVisualConfigSnapshot();
        for (UiVisualConfig uiConfig : visualConfig.uiConfigs()) {
            if (uiConfig.baseVisible()) {
                createPathMarkers(layoutCache, uiConfig.ui(), uiConfig.activeMapTypes());
            }
        }

        if (isFrontierVisible()) {
            showMarkerOverlaysQuietly(markerOverlays);
        }
    }

    private void rebuildPathLabels() {
        hideMarkerOverlays(labelOverlays);
        labelOverlays.clear();

        PathLayoutCache layoutCache = ensurePathLayoutCache();
        if (layoutCache == null || layoutCache.labelAnchors().isEmpty()) {
            return;
        }

        VisualConfigSnapshot visualConfig = ensureVisualConfigSnapshot();
        for (UiVisualConfig uiConfig : visualConfig.uiConfigs()) {
            if (uiConfig.baseVisible()) {
                LabelVisibility labels = uiConfig.labelVisibility();
                createPathLabels(layoutCache, uiConfig.ui(), uiConfig.activeMapTypes(),
                        labels.nameVisible(), labels.collectionVisible(), labels.ownerVisible(), labels.bannerVisible());
            }
        }

        if (isFrontierVisible()) {
            showMarkerOverlaysQuietly(labelOverlays);
        }
    }

    private void rebuildPathHighlightOverlays() {
        hideMarkerOverlays(highlightMarkerOverlays);
        highlightMarkerOverlays.clear();

        PathLayoutCache layoutCache = ensurePathLayoutCache();
        if (layoutCache != null) {
            createPathHighlightMarkers(layoutCache);
        }

        showMarkerOverlaysQuietly(highlightMarkerOverlays);
    }

    private void refreshHighlightVisibility() {
        hidePolygonOverlays(highlightPolygonOverlays);
        hideMarkerOverlays(highlightMarkerOverlays);

        if (!highlighted) {
            return;
        }

        showPolygonOverlaysQuietly(highlightPolygonOverlays);
        showMarkerOverlaysQuietly(highlightMarkerOverlays);
    }

    private ShapeProperties createBaseShapeProperties() {
        int baseColor = previewCollectionStyleEnabled ? previewCollectionColor : color;
        float borderWidth = previewCollectionStyleEnabled ? ClientConfig.COLLECTION_BORDER_WIDTH.get() / 2.f : ClientConfig.BORDER_WIDTH.get();
        float borderOpacity = previewCollectionStyleEnabled
                ? ClientConfig.COLLECTION_BORDER_OPACITY.get().floatValue()
                : ClientConfig.BORDER_OPACITY.get().floatValue();
        float fillOpacity = previewCollectionStyleEnabled
                ? ClientConfig.COLLECTION_FILL_OPACITY.get().floatValue()
                : ClientConfig.FILL_OPACITY.get().floatValue();

        return new ShapeProperties()
                .setStrokeWidth(borderWidth)
                .setStrokeColor(baseColor)
                .setStrokeOpacity(borderOpacity)
                .setStrokePosition(ShapeProperties.StrokePosition.INSIDE)
                .setFillColor(baseColor)
                .setFillOpacity(fillOpacity);
    }

    private ShapeProperties createCollectionShapeProperties() {
        CollectionData collection = getCollection();
        int collectionColor = collection == null ? color : collection.getColor();
        return new ShapeProperties()
                .setStrokeWidth(ClientConfig.COLLECTION_BORDER_WIDTH.get() / 2.f)
                .setStrokeColor(collectionColor)
                .setStrokeOpacity(ClientConfig.COLLECTION_BORDER_OPACITY.get().floatValue())
                .setStrokePosition(ShapeProperties.StrokePosition.INSIDE)
                .setFillColor(collectionColor)
                .setFillOpacity(ClientConfig.COLLECTION_FILL_OPACITY.get().floatValue());
    }

    private void hidePolygonOverlays(List<PolygonOverlay> overlays) {
        for (PolygonOverlay polygon : overlays) {
            removePolygonOverlay(polygon);
        }
    }

    private void hideMarkerOverlays(List<MarkerOverlay> overlays) {
        for (MarkerOverlay marker : overlays) {
            removeMarkerOverlay(marker);
        }
    }

    private void showPolygonOverlaysQuietly(List<PolygonOverlay> overlays) {
        try {
            showPolygonOverlays(overlays);
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error(t.getMessage(), t);
        }
    }

    private void showMarkerOverlaysQuietly(List<MarkerOverlay> overlays) {
        try {
            showMarkerOverlays(overlays);
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error(t.getMessage(), t);
        }
    }

    private void addPolygonLabelOverlays(PolygonUiPlanEntry entry,
                                         Map<LabelPlacementKey, FrontierLabelPlacementSolver.LabelPlacement> placementCache) {
        LabelVisibility labelVisibility = entry.labelVisibility();

        addLabelOverlay(entry,
                entry.ui(),
                entry.activeMapTypes(),
                buildLabelContentMetrics(labelVisibility.nameVisible(), labelVisibility.collectionVisible(),
                        labelVisibility.ownerVisible(), labelVisibility.bannerVisible()),
                entry.overlayArea(),
                getLabelSolverPrecision(),
                placementCache,
                resolveCollectionNormalMinZoom(entry));
    }

    private static ShapeProperties createHighlightShapeProperties() {
        return new ShapeProperties()
                .setStrokeWidth(2)
                .setStrokeColor(ColorConstants.WHITE)
                .setStrokeOpacity(1)
                .setStrokePosition(ShapeProperties.StrokePosition.OUTSIDE)
                .setFillOpacity(0);
    }

    private PolygonOverlay createPolygonOverlay(ShapeProperties shapeProps, PolygonUiPlanEntry entry, int minZoom, int maxZoom) {
        PolygonRenderGeometry geometry = entry.geometry();
        PolygonOverlay overlay = new PolygonOverlay(MapFrontiers.MODID, dimension, shapeProps, geometry.polygon(), geometry.holes());
        overlay.setActiveUIs(entry.ui());
        overlay.setActiveMapTypes(entry.activeMapTypes());
        if (minZoom > 0) {
            overlay.setMinZoom(minZoom);
        }
        if (maxZoom > 0) {
            overlay.setMaxZoom(maxZoom);
        }
        return overlay;
    }

    private void addHighlightPolygonOverlay(ShapeProperties highlightShapeProps, PolygonRenderGeometry geometry) {
        PolygonOverlay overlay = new PolygonOverlay(MapFrontiers.MODID, dimension, highlightShapeProps, geometry.polygon(), geometry.holes());
        overlay.setActiveUIs(Context.UI.Fullscreen);
        overlay.setActiveMapTypes(HIGHLIGHT_MAP_TYPES);
        if (geometry.minZoom() > 0) {
            overlay.setMinZoom(geometry.minZoom());
        }
        highlightPolygonOverlays.add(overlay);
    }

    private void rebuildVertexGeometryCache() {
        synchronized (vertices) {
            if (vertices.size() > 2) {
                MapPolygon polygon = new MapPolygon(vertices);
                polygonArea = PolygonHelper.toArea(polygon);
                polygonRenderGeometries.add(new PolygonRenderGeometry(polygon, null, 0));

                BlockPos last = vertices.getLast();
                for (BlockPos vertex : vertices) {
                    area += last.getX() * vertex.getZ() - last.getZ() * vertex.getX();
                    last = vertex;
                }
                area = abs(area / 2.f);
            } else if (!vertices.isEmpty()) {
                polygonRenderGeometries.add(new PolygonRenderGeometry(createIncompleteVertexPolygon(), null, INCOMPLETE_VERTEX_FRONTIER_MIN_ZOOM));
            }

            if (vertices.size() > 1) {
                BlockPos last = vertices.getLast();
                for (BlockPos vertex : vertices) {
                    perimeter += (float) Math.sqrt(vertex.distSqr(last));
                    last = vertex;
                }
            }
        }
    }

    private Context.MapType[] getActiveMapTypes(boolean day, boolean night, boolean underground, boolean topo, boolean biome) {
        List<Context.MapType> mapTypes = new ArrayList<>();
        if (day) {
            mapTypes.add(Context.MapType.Day);
        }
        if (night) {
            mapTypes.add(Context.MapType.Night);
        }
        if (underground) {
            mapTypes.add(Context.MapType.Underground);
        }
        if (topo) {
            mapTypes.add(Context.MapType.Topo);
        }
        if (biome) {
            mapTypes.add(Context.MapType.Biome);
        }
        return mapTypes.toArray(new Context.MapType[0]);
    }

    private MapPolygon createIncompleteVertexPolygon() {
        if (vertices.size() == 1) {
            return new MapPolygon(getBlockCorners(vertices.getFirst()));
        }

        BlockPos start = vertices.get(0);
        BlockPos end = vertices.get(1);
        if (start.equals(end)) {
            return new MapPolygon(getBlockCorners(start));
        }

        Vec2 direction = new Vec2(end.getX() - start.getX(), end.getZ() - start.getZ()).normalized();
        List<BlockPos> startCorners = getBlockCorners(start);
        List<BlockPos> endCorners = getBlockCorners(end);

        startCorners.sort((a, b) -> Float.compare(getCornerProjection(a, direction), getCornerProjection(b, direction)));
        endCorners.sort((a, b) -> Float.compare(getCornerProjection(b, direction), getCornerProjection(a, direction)));

        List<BlockPos> polygonPoints = new ArrayList<>();
        addUniquePoints(polygonPoints, startCorners.subList(0, 3));
        addUniquePoints(polygonPoints, endCorners.subList(0, 3));
        sortPointsAroundCenter(polygonPoints);
        return new MapPolygon(polygonPoints);
    }

    private static void addUniquePoints(List<BlockPos> target, List<BlockPos> points) {
        for (BlockPos point : points) {
            if (!target.contains(point)) {
                target.add(point);
            }
        }
    }

    private static List<BlockPos> getBlockCorners(BlockPos pos) {
        return new ArrayList<>(List.of(
                pos,
                pos.offset(1, 0, 0),
                pos.offset(1, 0, 1),
                pos.offset(0, 0, 1)));
    }

    private static float getCornerProjection(BlockPos point, Vec2 direction) {
        return point.getX() * direction.x + point.getZ() * direction.y;
    }

    private static void sortPointsAroundCenter(List<BlockPos> points) {
        double centerX = 0.0;
        double centerZ = 0.0;
        for (BlockPos point : points) {
            centerX += point.getX();
            centerZ += point.getZ();
        }

        centerX /= points.size();
        centerZ /= points.size();
        double finalCenterX = centerX;
        double finalCenterZ = centerZ;
        points.sort((a, b) -> Double.compare(
                Math.atan2(a.getZ() - finalCenterZ, a.getX() - finalCenterX),
                Math.atan2(b.getZ() - finalCenterZ, b.getX() - finalCenterX)));
    }

    private void createPathMarkers(PathLayoutCache layoutCache, Context.UI uiArray, Context.MapType[] mapTypesArray) {
        if (layoutCache.pointLayouts().isEmpty()) {
            return;
        }

        if (layoutCache.segmentLayouts().isEmpty()) {
            PathPointLayout pointLayout = layoutCache.pointLayouts().getFirst();
            addSingleMarker(pointLayout.pos(), resolvePathMarkerImage(pointLayout.markerId(), pointLayout.rotation()), 100, uiArray, mapTypesArray);
            return;
        }

        for (int i = 0; i < layoutCache.segmentLayouts().size(); ++i) {
            PathSegmentLayout segmentLayout = layoutCache.segmentLayouts().get(i);
            addRepeatedMarkers(markerOverlays, segmentLayout, uiArray, mapTypesArray,
                    resolvePathMarkerImage(segmentLayout.segmentMarkerId(), segmentLayout.rotation()), 99);

            PathPointLayout pointLayout = layoutCache.pointLayouts().get(i);
            addSingleMarker(pointLayout.pos(), resolvePathMarkerImage(pointLayout.markerId(), pointLayout.rotation()), 100, uiArray, mapTypesArray);
        }

        PathPointLayout lastPointLayout = layoutCache.pointLayouts().getLast();
        addSingleMarker(lastPointLayout.pos(), resolvePathMarkerImage(lastPointLayout.markerId(), lastPointLayout.rotation()), 100, uiArray, mapTypesArray);
    }

    private void createPathHighlightMarkers(PathLayoutCache layoutCache) {
        if (layoutCache.pointLayouts().isEmpty()) {
            return;
        }

        if (layoutCache.segmentLayouts().isEmpty()) {
            PathPointLayout pointLayout = layoutCache.pointLayouts().getFirst();
            addSingleMarker(highlightMarkerOverlays, pointLayout.pos(), resolvePathMarkerHighlightImage(pointLayout.markerId(), pointLayout.rotation()), 101,
                    Context.UI.Fullscreen, HIGHLIGHT_MAP_TYPES);
            return;
        }

        for (int i = 0; i < layoutCache.segmentLayouts().size(); ++i) {
            PathSegmentLayout segmentLayout = layoutCache.segmentLayouts().get(i);
            addRepeatedMarkers(highlightMarkerOverlays, segmentLayout, Context.UI.Fullscreen, HIGHLIGHT_MAP_TYPES,
                    resolvePathMarkerHighlightImage(segmentLayout.segmentMarkerId(), segmentLayout.rotation()), 100);

            PathPointLayout pointLayout = layoutCache.pointLayouts().get(i);
            addSingleMarker(highlightMarkerOverlays, pointLayout.pos(), resolvePathMarkerHighlightImage(pointLayout.markerId(), pointLayout.rotation()), 101,
                    Context.UI.Fullscreen, HIGHLIGHT_MAP_TYPES);
        }

        PathPointLayout lastPointLayout = layoutCache.pointLayouts().getLast();
        addSingleMarker(highlightMarkerOverlays, lastPointLayout.pos(), resolvePathMarkerHighlightImage(lastPointLayout.markerId(), lastPointLayout.rotation()), 101,
                Context.UI.Fullscreen, HIGHLIGHT_MAP_TYPES);
    }

    private ResourceLocation getPathPointMarkerId(int pointCount, int pointIndex) {
        ResourceLocation markerId;
        if (pointIndex == 0) {
            markerId = pathStyle.startMarker;
        } else if (pointIndex == pointCount - 1) {
            markerId = pathStyle.endMarker;
        } else {
            markerId = pathStyle.innerMarker;
        }

        if (FrontierData.PathStyle.NONE.equals(markerId) && !FrontierData.PathStyle.NONE.equals(pathStyle.segmentMarker)) {
            return pathStyle.segmentMarker;
        }

        return markerId;
    }

    private void createPathLabels(PathLayoutCache layoutCache, Context.UI uiArray, Context.MapType[] mapTypesArray, boolean nameVisible, boolean collectionVisible,
                                  boolean ownerVisible, boolean bannerVisible) {
        LabelContentMetrics metrics = buildLabelContentMetrics(nameVisible, collectionVisible, ownerVisible, bannerVisible);
        if (!metrics.hasText() && !metrics.hasBanner()) {
            return;
        }

        for (PathLabelAnchor anchor : layoutCache.labelAnchors()) {
            addPathLabelOverlay(uiArray, mapTypesArray, metrics, anchor);
        }
    }

    //
    // Algorithm adapted from https://stackoverflow.com/a/63888205/2647614
    //
    private void rebuildChunkGeometryCache() {
        Multimap<ChunkPos, ChunkPos> edges = HashMultimap.create();
        synchronized (chunks) {
            for (ChunkPos chunk : chunks) {
                addNewEdge(edges, new ChunkPos(chunk.x, chunk.z), new ChunkPos(chunk.x + 1, chunk.z));
                addNewEdge(edges, new ChunkPos(chunk.x + 1, chunk.z), new ChunkPos(chunk.x + 1, chunk.z + 1));
                addNewEdge(edges, new ChunkPos(chunk.x + 1, chunk.z + 1), new ChunkPos(chunk.x, chunk.z + 1));
                addNewEdge(edges, new ChunkPos(chunk.x, chunk.z + 1), new ChunkPos(chunk.x, chunk.z));
            }
        }

        List<List<ChunkPos>> outerPolygons = new ArrayList<>();
        Multimap<ChunkPos, List<ChunkPos>> holesPolygons = HashMultimap.create();

        while (!edges.isEmpty()) {
            ChunkPos starting = Collections.min(edges.keySet(), (e1, e2) -> e1.x == e2.x ? e1.z - e2.z : e1.x - e2.x);
            List<ChunkPos> polygon = new ArrayList<>();
            ChunkPos edge = starting;
            int direction = 1;

            do {
                polygon.add(edge);
                Iterator<ChunkPos> it = edges.get(edge).iterator();
                ChunkPos edge2 = it.next();
                while (it.hasNext() && Integer.signum(direction) == Integer.signum(edge2.x - edge.x + edge.z - edge2.z)) {
                    edge2 = it.next();
                }
                edges.remove(edge, edge2);
                direction = edge2.x - edge.x + edge2.z - edge.z;
                edge = edge2;
            } while (!edge.equals(starting));

            perimeter += polygon.size() * 16;

            boolean clockwise = polygon.get(0).x != polygon.get(1).x;
            if (clockwise) {
                outerPolygons.add(polygon);
            } else {
                ChunkPos ray = polygon.getFirst();
                ChunkPos outerFound = null;
                for (int i = 0; i < 999; ++i) {
                    for (List<ChunkPos> outer : outerPolygons) {
                        ChunkPos outerStart = outer.getFirst();
                        if (outer.contains(ray)) {
                            outerFound = outerStart;
                            break;
                        }

                        for (List<ChunkPos> hole : holesPolygons.get(outerStart)) {
                            if (hole.contains(ray)) {
                                outerFound = outerStart;
                                break;
                            }
                        }

                        if (outerFound != null) {
                            break;
                        }
                    }

                    if (outerFound != null) {
                        break;
                    }

                    ray = new ChunkPos(ray.x - 1, ray.z);
                }

                if (outerFound != null) {
                    holesPolygons.put(outerFound, polygon);
                } else {
                    MapFrontiers.LOGGER.warn("Frontier {} is too large and the polygon corresponding to the hole {} could not be located", id, polygon.getFirst());
                }
            }
        }

        for (List<ChunkPos> outer : outerPolygons) {
            removeCollinear(outer);
            for (List<ChunkPos> hole : holesPolygons.get(outer.getFirst())) {
                removeCollinear(hole);
            }
        }

        for (List<ChunkPos> outer : outerPolygons) {
            MapPolygon polygon = new MapPolygon(outer.stream().map(c -> new BlockPos(c.getMinBlockX(), 70, c.getMinBlockZ())).toList());
            List<MapPolygon> polygonHoles = null;

            if (holesPolygons.containsKey(outer.getFirst())) {
                polygonHoles = new ArrayList<>();
                for (List<ChunkPos> hole : holesPolygons.get(outer.getFirst())) {
                    polygonHoles.add(new MapPolygon(hole.stream().map(c -> new BlockPos(c.getMinBlockX(), 70, c.getMinBlockZ())).toList()));
                }
            }

            polygonRenderGeometries.add(new PolygonRenderGeometry(polygon, polygonHoles, 0));
        }

        area = chunks.size() * 256;
    }

    private static void addNewEdge(Multimap<ChunkPos, ChunkPos> edges, ChunkPos from, ChunkPos to) {
        if (!edges.remove(to, from)) {
            edges.put(from, to);
        }
    }

    private static void removeCollinear(List<ChunkPos> chunks) {
        if (chunks.size() <= 4) {
            return;
        }

        ChunkPos prev = chunks.getFirst();
        for (int i = chunks.size() - 1; i > 0; --i) {
            ChunkPos next = chunks.get(i - 1);

            if (prev.x == next.x || prev.z == next.z) {
                chunks.remove(i);
            }

            if (i < chunks.size()) {
                prev = chunks.get(i);
            }
        }
    }

    private Area buildOverlayArea(MapPolygon polygon, @Nullable List<MapPolygon> holes) {
        if (frontierShape == FrontierShape.Vertex) {
            return polygonArea != null ? new Area(polygonArea) : PolygonHelper.toArea(polygon);
        }

        Area area = PolygonHelper.toArea(polygon);
        if (holes != null) {
            for (MapPolygon hole : holes) {
                area.subtract(PolygonHelper.toArea(hole));
            }
        }

        return area;
    }

    private void addLabelOverlay(PolygonUiPlanEntry entry,
                                 Context.UI ui,
                                 Context.MapType[] mapTypes,
                                 LabelContentMetrics metrics,
                                 Area overlayArea,
                                 double labelSolverPrecision,
                                 Map<LabelPlacementKey, FrontierLabelPlacementSolver.LabelPlacement> placementCache,
                                 int minZoomOverride) {
        if (!metrics.hasText() && !metrics.hasBanner()) {
            return;
        }

        TextProperties textProps = createBaseTextProperties().setOffsetY(metrics.textOffsetY());
        LabelPlacementKey placementKey = new LabelPlacementKey(entry, metrics.contentWidthPx(), metrics.contentHeightPx());
        FrontierLabelPlacementSolver.LabelPlacement placement = placementCache.computeIfAbsent(placementKey,
                ignored -> FrontierLabelPlacementSolver.solve(overlayArea,
                        metrics.contentWidthPx(),
                        metrics.contentHeightPx(),
                        labelSolverPrecision));

        if (ClientConfig.HIDE_NAMES_THAT_DONT_FIT.get()) {
            applyMinZoom(textProps, placement);
        }

        if (minZoomOverride > 0) {
            textProps.setMinZoom(Math.max(textProps.getMinZoom(), minZoomOverride));
        }

        BlockPos anchor = BlockPos.containing(placement.centerX(), OVERLAY_Y, placement.centerZ());
        MarkerOverlay labelOverlay = new MarkerOverlay(MapFrontiers.MODID, anchor, createLabelAnchorIcon(metrics));
        labelOverlay.setActiveUIs(ui);
        labelOverlay.setActiveMapTypes(mapTypes);
        labelOverlay.setDimension(dimension);
        labelOverlay.setMinZoom(textProps.getMinZoom());
        labelOverlay.setMaxZoom(textProps.getMaxZoom());
        labelOverlay.setOverlayGroupName("frontier");

        if (metrics.hasText()) {
            labelOverlay.setTextProperties(textProps).setLabel(metrics.label());
        }

        labelOverlays.add(labelOverlay);
    }

    private void addPathLabelOverlay(Context.UI uiArray, Context.MapType[] mapTypesArray, LabelContentMetrics metrics, PathLabelAnchor anchor) {
        PathLabelVisualOffset offset = getPathLabelVisualOffset(metrics, anchor);
        // MarkerOverlay applies TextProperties offsets with inverted signs; MapImage anchors use the same visual direction directly.
        TextProperties textProps = createBaseTextProperties()
                .setOffsetX(-offset.x())
                .setOffsetY(metrics.textOffsetY() - offset.y());
        MarkerOverlay labelOverlay = new MarkerOverlay(MapFrontiers.MODID,
                BlockPos.containing(anchor.x(), OVERLAY_Y, anchor.z()),
                createLabelAnchorIcon(metrics, offset.x(), offset.y()));
        labelOverlay.setActiveUIs(uiArray);
        labelOverlay.setActiveMapTypes(mapTypesArray);
        labelOverlay.setDimension(dimension);
        labelOverlay.setMaxZoom(textProps.getMaxZoom());
        labelOverlay.setMinZoom(textProps.getMinZoom());
        labelOverlay.setOverlayGroupName("frontier");

        if (metrics.hasText()) {
            labelOverlay.setTextProperties(textProps).setLabel(metrics.label());
        }

        labelOverlays.add(labelOverlay);
    }

    private LabelContentMetrics buildLabelContentMetrics(boolean nameVisible, boolean collectionVisible, boolean ownerVisible, boolean bannerVisible) {
        boolean hasBanner = bannerVisible && hasEffectiveBanner();
        String collectionName = collectionVisible ? getCollectionName() : null;
        boolean hasCollectionName = collectionName != null && !collectionName.isEmpty();
        if (!nameVisible && !hasCollectionName && !ownerVisible && !hasBanner) {
            return new LabelContentMetrics("", false, false, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }

        int lines = 0;
        int textWidthPx = 0;
        String label = "";

        if (nameVisible) {
            if (!name1.isEmpty()) {
                ++lines;
                textWidthPx = Math.max(textWidthPx, Minecraft.getInstance().font.width(name1));
                label += name1;
            }
            if (!name2.isEmpty()) {
                ++lines;
                textWidthPx = Math.max(textWidthPx, Minecraft.getInstance().font.width(name2));
                if (!label.isEmpty()) {
                    label += "\n";
                }
                label += name2;
            }
        }

        if (hasCollectionName) {
            ++lines;
            textWidthPx = Math.max(textWidthPx, Minecraft.getInstance().font.width(Component.literal(collectionName).withStyle(ChatFormatting.BOLD)));
            if (!label.isEmpty()) {
                label += "\n";
            }
            label += ChatFormatting.BOLD + collectionName + ChatFormatting.RESET;
        }

        if (ownerVisible && !owner.username.isEmpty()) {
            ++lines;
            textWidthPx = Math.max(textWidthPx, Minecraft.getInstance().font.width(owner.username));
            if (!label.isEmpty()) {
                label += "\n";
            }
            label += ChatFormatting.ITALIC + owner.username;
        }

        int textSize = getTextSize();
        int bannerSize = getBannerSize();
        textWidthPx *= textSize;
        int textHeightPx = lines * TEXT_LINE_HEIGHT_PX * textSize;
        int bannerWidthPx = hasBanner ? BANNER_BASE_WIDTH_PX * bannerSize : 0;
        int bannerHeightPx = hasBanner ? BANNER_BASE_HEIGHT_PX * bannerSize : 0;
        // Treat the banner as a square footprint for placement/min zoom so rotations do not
        // underestimate the horizontal space without having to compute the rotated bounds.
        int bannerPlacementWidthPx = hasBanner ? bannerHeightPx : 0;
        int rawContentWidthPx = Math.max(textWidthPx, bannerPlacementWidthPx);
        int rawContentHeightPx = textHeightPx + bannerHeightPx;
        int textOffsetY;
        int bannerOffsetY = 0;

        if (hasBanner) {
            int topOffset = rawContentHeightPx / 2;
            textOffsetY = topOffset - textHeightPx / 2;
            bannerOffsetY = topOffset - textHeightPx;

            if (lines > 1) {
                textOffsetY += BANNER_MULTILINE_TEXT_OFFSET_Y;
            } else if (lines == 1) {
                textOffsetY += BANNER_SINGLE_LINE_TEXT_OFFSET_Y;
            }
        } else {
            // JourneyMap centers each line again inside drawLabels(..., VAlign.Middle),
            // so multi-line MarkerOverlay labels end up shifted down by half a line unless
            // we compensate here. Single-line labels do not need this correction.
            textOffsetY = lines > 1 ? -(TEXT_LINE_HEIGHT_PX * textSize) / 2 : 0;
        }

        return new LabelContentMetrics(label,
                lines > 0,
                hasBanner,
                lines,
                textWidthPx,
                textHeightPx,
                bannerWidthPx,
                bannerHeightPx,
                rawContentWidthPx + LABEL_CONTENT_PADDING_PX,
                rawContentHeightPx + LABEL_CONTENT_PADDING_PX,
                textOffsetY,
                bannerOffsetY);
    }

    private double getLabelSolverPrecision() {
        return frontierShape == FrontierShape.Chunk ? CHUNK_LABEL_SOLVER_PRECISION : VERTEX_LABEL_SOLVER_PRECISION;
    }

    private TextProperties createBaseTextProperties() {
        TextProperties textProperties = new TextProperties()
                .setOpacity(ClientConfig.TEXT_OPACITY.get().floatValue())
                .setScale(getTextSize())
                .setBackgroundOpacity(0.f);
        switch (ClientConfig.TEXT_COLOR.get()) {
            case TextColor.FrontierColor -> textProperties.setColor(color);
            case TextColor.FrontierColorBright -> textProperties.setColor(colorMaxBrightness(color));
            case TextColor.White -> textProperties.setColor(ColorConstants.WHITE);
        }
        return textProperties;
    }

    private MapImage createLabelAnchorIcon(LabelContentMetrics metrics) {
        return createLabelAnchorIcon(metrics, 0, 0);
    }

    private MapImage createLabelAnchorIcon(LabelContentMetrics metrics, int offsetX, int offsetY) {
        if (!metrics.hasBanner()) {
            return transparentLabelMarker;
        }

        if (!bannerRenderer.hasBanner()) {
            refreshEffectiveBannerRenderer();
            if (!bannerRenderer.hasBanner()) {
                return transparentLabelMarker;
            }
        }

        MapImage bannerIcon = bannerRenderer.createJourneyMapImage(
                metrics.bannerWidthPx() / 2.0 - offsetX,
                metrics.bannerOffsetY() - offsetY,
                metrics.bannerWidthPx(),
                metrics.bannerHeightPx(),
                ClientConfig.BANNER_OPACITY.get().floatValue());
        return bannerIcon == null ? transparentLabelMarker : bannerIcon;
    }

    private int getTextSize() {
        return previewTextSize > 0 ? previewTextSize : ClientConfig.TEXT_SIZE.get();
    }

    private @Nullable CollectionData getCollection() {
        return collectionId == null ? null : MapFrontiersClient.getCollection(collectionId);
    }

    private boolean hasEffectiveBanner() {
        return getEffectiveBannerData() != null;
    }

    private @Nullable BannerData getEffectiveBannerData() {
        if (banner != null) {
            return banner;
        }

        CollectionData collection = getCollection();
        if (!getInheritCollectionBanner() || collection == null) {
            return null;
        }

        return collection.getBannerData();
    }

    private void refreshEffectiveBannerRenderer() {
        bannerRenderer.releaseTexture();
        BannerData effectiveBanner = getEffectiveBannerData();
        if (effectiveBanner != null) {
            bannerRenderer.createTexture(id, effectiveBanner);
        }
    }

    private boolean shouldRenderCollectionView(Context.UI ui) {
        if (previewCollectionStyleEnabled || frontierShape == FrontierShape.Path) {
            return false;
        }

        return resolveCollectionVisibility() && CollectionVisibilityData.isZoomEnabled(resolveCollectionMaxZoom(ui));
    }

    private int resolveCollectionMaxZoom(Context.UI ui) {
        CollectionData collection = getCollection();
        if (collection == null) {
            return CollectionVisibilityData.COLLECTION_VIEW_DISABLED_ZOOM;
        }

        Pair<CollectionVisibilityData, CollectionVisibilityMask> visibilityOverride =
                MapFrontiersClient.getCollectionLocalOverrides().getVisibility(collection.getId());
        CollectionVisibilityData resolvedVisibility = CollectionLocalOverrides.resolveVisibility(collection.getVisibilityData(), visibilityOverride);
        int zoom = switch (ui) {
            case Fullscreen -> resolvedVisibility.getFullscreenZoom();
            case Minimap -> resolvedVisibility.getMinimapZoom();
            case Webmap -> resolvedVisibility.getWebmapZoom();
        };

        return switch (ui) {
            case Fullscreen -> ClientConfig.COLLECTION_FULLSCREEN_ZOOM_FORCED.get()
                    ? ClientConfig.getNormalizedCollectionFullscreenZoom()
                    : zoom;
            case Minimap -> ClientConfig.COLLECTION_MINIMAP_ZOOM_FORCED.get()
                    ? ClientConfig.getNormalizedCollectionMinimapZoom()
                    : zoom;
            case Webmap -> ClientConfig.COLLECTION_WEBMAP_ZOOM_FORCED.get()
                    ? ClientConfig.getNormalizedCollectionWebmapZoom()
                    : zoom;
        };
    }

    private boolean resolveCollectionVisibility() {
        CollectionData collection = getCollection();
        if (collection == null) {
            return false;
        }

        Pair<CollectionVisibilityData, CollectionVisibilityMask> visibilityOverride =
                MapFrontiersClient.getCollectionLocalOverrides().getVisibility(collection.getId());
        CollectionVisibilityData resolvedVisibility = CollectionLocalOverrides.resolveVisibility(collection.getVisibilityData(), visibilityOverride);
        return ClientConfig.resolveVisibilityValue(ClientConfig.COLLECTION_VISIBILITY.get(), resolvedVisibility.isVisible());
    }

    private int resolveCollectionNormalMinZoom(PolygonUiPlanEntry entry) {
        if (!shouldRenderCollectionView(entry.ui())) {
            return entry.minZoom();
        }

        return Math.max(entry.minZoom(), CollectionVisibilityData.getZoomTransitionMinZoom(resolveCollectionMaxZoom(entry.ui())));
    }

    private @Nullable String getCollectionName() {
        CollectionData collection = getCollection();
        if (collection == null) {
            return null;
        }

        String name = collection.getName().trim();
        return name.isEmpty() ? null : name;
    }

    private int getBannerSize() {
        return previewBannerSize > 0 ? previewBannerSize : ClientConfig.BANNER_SIZE.get();
    }

    private int colorMaxBrightness(int color) {
        float[] hsv = Color.RGBtoHSB((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, null);
        return Color.HSBtoRGB(hsv[0], hsv[1], 1.f);
    }

    private void applyMinZoom(TextProperties textProperties, FrontierLabelPlacementSolver.LabelPlacement placement) {
        double polygonWidthScaled = placement.availableWidthBlocks() / 256.0;
        double polygonHeightScaled = placement.availableHeightBlocks() / 256.0;
        int zoom = 2;
        while ((placement.contentWidthPx() > polygonWidthScaled || placement.contentHeightPx() > polygonHeightScaled) && zoom < 8192) {
            zoom *= 2;
            polygonWidthScaled *= 2.0;
            polygonHeightScaled *= 2.0;
        }

        textProperties.setMinZoom(zoom);
    }

    private PathLabelAnchor getPathEndpointLabelAnchor(BlockPos endpoint, BlockPos connectedPoint, double markerClearancePx) {
        double dx = endpoint.getX() - connectedPoint.getX();
        double dz = endpoint.getZ() - connectedPoint.getZ();
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 0.0001) {
            return new PathLabelAnchor(endpoint.getX(), endpoint.getZ(), 0.0, 1.0, markerClearancePx);
        }

        return new PathLabelAnchor(endpoint.getX(), endpoint.getZ(), dx / length, dz / length, markerClearancePx);
    }

    private PathLabelAnchor getPathMidpointLabelAnchor(List<BlockPos> pathPoints, List<PathSegmentLayout> segmentLayouts) {
        if (pathPoints.isEmpty()) {
            return new PathLabelAnchor(0, 0, 0.0, 0.0, 0.0);
        }

        if (pathPoints.size() == 1) {
            BlockPos point = pathPoints.getFirst();
            return new PathLabelAnchor(point.getX(), point.getZ(), 0.0, 0.0, 0.0);
        }

        double totalLength = 0.0;
        for (PathSegmentLayout segmentLayout : segmentLayouts) {
            totalLength += segmentLayout.length();
        }

        if (totalLength < 0.0001) {
            BlockPos point = pathPoints.getFirst();
            return new PathLabelAnchor(point.getX(), point.getZ(), 0.0, 0.0, 0.0);
        }

        double halfLength = totalLength / 2.0;
        double traversed = 0.0;
        for (PathSegmentLayout segmentLayout : segmentLayouts) {
            BlockPos from = segmentLayout.from();
            BlockPos to = segmentLayout.to();
            double segmentLength = segmentLayout.length();
            if (traversed + segmentLength >= halfLength) {
                double t = (halfLength - traversed) / segmentLength;
                double x = from.getX() + (to.getX() - from.getX()) * t;
                double z = from.getZ() + (to.getZ() - from.getZ()) * t;
                return new PathLabelAnchor(x, z, 0.0, 0.0, 0.0);
            }
            traversed += segmentLength;
        }

        BlockPos point = pathPoints.getLast();
        return new PathLabelAnchor(point.getX(), point.getZ(), 0.0, 0.0, 0.0);
    }

    private static double getPathMarkerClearancePx(ResourceLocation markerId) {
        return isPathMarkerVisible(markerId) ? MarkerImageConstants.getMapDisplaySize() / 2.0 : 0.0;
    }

    private PathLabelVisualOffset getPathLabelVisualOffset(LabelContentMetrics metrics, PathLabelAnchor anchor) {
        if (anchor.screenOffsetDirectionX() == 0.0 && anchor.screenOffsetDirectionY() == 0.0) {
            return new PathLabelVisualOffset(0, 0);
        }

        double halfWidth = metrics.contentWidthPx() / 2.0;
        double halfHeight = metrics.contentHeightPx() / 2.0;
        double distanceX = anchor.screenOffsetDirectionX() == 0.0 ? Double.POSITIVE_INFINITY
                : halfWidth / Math.abs(anchor.screenOffsetDirectionX());
        double distanceY = anchor.screenOffsetDirectionY() == 0.0 ? Double.POSITIVE_INFINITY
                : halfHeight / Math.abs(anchor.screenOffsetDirectionY());
        double distance = Math.min(distanceX, distanceY) + anchor.markerClearancePx() + PATH_LABEL_OFFSET_PADDING_PX;
        int offsetX = (int) Math.round(anchor.screenOffsetDirectionX() * distance);
        int offsetY = (int) Math.round(anchor.screenOffsetDirectionY() * distance);
        return new PathLabelVisualOffset(offsetX, offsetY);
    }

    private static MapImage createTransparentLabelMarker() {
        NativeImage image = new NativeImage(1, 1, false);
        image.setPixelRGBA(0, 0, 0);
        MapImage mapImage = new MapImage(image);
        mapImage.setAnchorX(0.5);
        mapImage.setAnchorY(0.5);
        mapImage.setDisplayWidth(1);
        mapImage.setDisplayHeight(1);
        mapImage.setOpacity(0.f);
        return mapImage;
    }

    private void updateBounds() {
        if (frontierShape == FrontierShape.Vertex) {
            if (vertices.isEmpty()) {
                topLeft = new BlockPos(0, OVERLAY_Y, 0);
                bottomRight = new BlockPos(0, OVERLAY_Y, 0);
            } else {
                int minX = Integer.MAX_VALUE;
                int minZ = Integer.MAX_VALUE;
                int maxX = Integer.MIN_VALUE;
                int maxZ = Integer.MIN_VALUE;

                synchronized (vertices) {
                    for (BlockPos vertex : vertices) {
                        if (vertex.getX() < minX)
                            minX = vertex.getX();
                        if (vertex.getZ() < minZ)
                            minZ = vertex.getZ();
                        if (vertex.getX() > maxX)
                            maxX = vertex.getX();
                        if (vertex.getZ() > maxZ)
                            maxZ = vertex.getZ();
                    }
                }

                topLeft = new BlockPos(minX, OVERLAY_Y, minZ);
                bottomRight = new BlockPos(maxX, OVERLAY_Y, maxZ);
            }
        } else if (frontierShape == FrontierShape.Path) {
            if (points.isEmpty()) {
                topLeft = new BlockPos(0, OVERLAY_Y, 0);
                bottomRight = new BlockPos(0, OVERLAY_Y, 0);
            } else {
                int minX = Integer.MAX_VALUE;
                int minZ = Integer.MAX_VALUE;
                int maxX = Integer.MIN_VALUE;
                int maxZ = Integer.MIN_VALUE;

                synchronized (points) {
                    for (BlockPos point : points) {
                        if (point.getX() < minX)
                            minX = point.getX();
                        if (point.getZ() < minZ)
                            minZ = point.getZ();
                        if (point.getX() > maxX)
                            maxX = point.getX();
                        if (point.getZ() > maxZ)
                            maxZ = point.getZ();
                    }
                }

                topLeft = new BlockPos(minX, OVERLAY_Y, minZ);
                bottomRight = new BlockPos(maxX, OVERLAY_Y, maxZ);
            }
        } else {
            if (chunks.isEmpty()) {
                topLeft = new BlockPos(0, OVERLAY_Y, 0);
                bottomRight = new BlockPos(0, OVERLAY_Y, 0);
            } else {
                int minX = Integer.MAX_VALUE;
                int minZ = Integer.MAX_VALUE;
                int maxX = Integer.MIN_VALUE;
                int maxZ = Integer.MIN_VALUE;

                synchronized (chunks) {
                    for (ChunkPos chunk : chunks) {
                        if (chunk.x < minX)
                            minX = chunk.x;
                        if (chunk.z < minZ)
                            minZ = chunk.z;
                        if (chunk.x > maxX)
                            maxX = chunk.x;
                        if (chunk.z > maxZ)
                            maxZ = chunk.z;
                    }
                }

                topLeft = new BlockPos(minX * 16, OVERLAY_Y, minZ * 16);
                bottomRight = new BlockPos(maxX * 16 + 16, OVERLAY_Y, maxZ * 16 + 16);
            }
        }
    }

    private void addRepeatedMarkers(List<MarkerOverlay> overlays, PathSegmentLayout segmentLayout, Context.UI uiArray, Context.MapType[] mapTypesArray,
                                    @Nullable MapImage markerImage, int displayOrder) {
        addRepeatedMarkers(overlays, segmentLayout.repeatedMarkerPositions(), segmentLayout.length(), uiArray, mapTypesArray,
                markerImage, displayOrder, segmentLayout.segmentSpacingMultiplier());
    }

    private void addRepeatedMarkers(List<MarkerOverlay> overlays, List<BlockPos> repeatedMarkerPositions, double length, Context.UI uiArray,
                                    Context.MapType[] mapTypesArray, @Nullable MapImage markerImage, int displayOrder, double spacingMultiplier) {
        if (markerImage == null || repeatedMarkerPositions.isEmpty()) {
            return;
        }

        for (int minZoom = PATH_REPEATED_MARKER_MIN_ZOOM; minZoom <= PATH_REPEATED_MARKER_MAX_ZOOM; minZoom *= 2) {
            int maxZoom = minZoom == PATH_REPEATED_MARKER_MAX_ZOOM ? 0 : minZoom * 2 - 1;
            int markerCount = getRepeatedMarkerCount(getRepeatedMarkerTargetSpacing(minZoom) * spacingMultiplier, length,
                    repeatedMarkerPositions.size());
            double step = (repeatedMarkerPositions.size() + 1) / (double) (markerCount + 1);
            Set<Long> addedPositions = new HashSet<>();

            for (int marker = 1; marker <= markerCount; ++marker) {
                int markerIndex = Mth.clamp(Math.round((float) (marker * step)) - 1, 0, repeatedMarkerPositions.size() - 1);
                BlockPos pos = repeatedMarkerPositions.get(markerIndex);
                long positionKey = getBlockPos2DKey(pos.getX(), pos.getZ());
                if (!addedPositions.add(positionKey)) {
                    continue;
                }

                addRepeatedMarker(overlays, pos, uiArray, mapTypesArray, markerImage, displayOrder, minZoom, maxZoom);
            }
        }
    }

    private static List<BlockPos> getDiscreteInteriorLinePositions(BlockPos from, BlockPos to) {
        List<BlockPos> positions = new ArrayList<>();

        int x0 = from.getX();
        int z0 = from.getZ();
        int x1 = to.getX();
        int z1 = to.getZ();
        int dx = Math.abs(x1 - x0);
        int dz = Math.abs(z1 - z0);
        int sx = x0 < x1 ? 1 : -1;
        int sz = z0 < z1 ? 1 : -1;
        int error = dx - dz;

        int x = x0;
        int z = z0;
        while (x != x1 || z != z1) {
            int doubleError = error * 2;
            if (doubleError > -dz) {
                error -= dz;
                x += sx;
            }
            if (doubleError < dx) {
                error += dx;
                z += sz;
            }

            if (x != x1 || z != z1) {
                positions.add(new BlockPos(x, OVERLAY_Y, z));
            }
        }

        return positions;
    }

    private static int getRepeatedMarkerCount(double targetSpacing, double length, int availableMarkerPositions) {
        if (targetSpacing <= 0.0 || availableMarkerPositions <= 0) {
            return 0;
        }

        int intervalCount = (int) Math.round(length / targetSpacing);
        return Mth.clamp(intervalCount - 1, 0, availableMarkerPositions);
    }

    private void addRepeatedMarker(List<MarkerOverlay> overlays, BlockPos pos, Context.UI uiArray, Context.MapType[] mapTypesArray,
                                   MapImage markerImage, int displayOrder, int minZoom, int maxZoom) {
        MarkerOverlay dot = new MarkerOverlay(MapFrontiers.MODID, pos, markerImage);
        dot.setDimension(dimension);
        dot.setDisplayOrder(displayOrder);
        dot.setActiveUIs(uiArray);
        dot.setActiveMapTypes(mapTypesArray);
        dot.setMinZoom(minZoom);
        if (maxZoom > 0) {
            dot.setMaxZoom(maxZoom);
        }
        overlays.add(dot);
    }

    private static long getBlockPos2DKey(int x, int z) {
        return (long) x & 0xFFFFFFFFL | ((long) z & 0xFFFFFFFFL) << 32;
    }

    private static double getRepeatedMarkerTargetSpacing(int minZoom) {
        return PATH_REPEATED_MARKER_SPACING_ZOOM_REFERENCE / minZoom
                * ClientConfig.PATH_MARKER_SIZE.get()
                / PATH_REPEATED_MARKER_BASE_MARKER_SIZE;
    }

    private void addSingleMarker(BlockPos pos, @Nullable MapImage markerImage, int displayOrder, Context.UI uiArray, Context.MapType[] mapTypesArray) {
        addSingleMarker(markerOverlays, pos, markerImage, displayOrder, uiArray, mapTypesArray);
    }

    private void addSingleMarker(List<MarkerOverlay> overlays, BlockPos pos, @Nullable MapImage markerImage, int displayOrder,
                                 Context.UI uiArray, Context.MapType[] mapTypesArray) {
        if (markerImage == null) {
            return;
        }

        MarkerOverlay marker = new MarkerOverlay(MapFrontiers.MODID, pos, markerImage);
        marker.setDimension(dimension);
        marker.setDisplayOrder(displayOrder);
        marker.setActiveUIs(uiArray);
        marker.setActiveMapTypes(mapTypesArray);
        overlays.add(marker);
    }

    private ResourceLocation getPathSinglePointMarkerId() {
        if (isPathMarkerVisible(pathStyle.startMarker)) {
            return pathStyle.startMarker;
        }
        if (isPathMarkerVisible(pathStyle.endMarker)) {
            return pathStyle.endMarker;
        }
        if (isPathMarkerVisible(pathStyle.innerMarker)) {
            return pathStyle.innerMarker;
        }
        if (isPathMarkerVisible(pathStyle.segmentMarker)) {
            return pathStyle.segmentMarker;
        }

        return FrontierData.PathStyle.BIG_DOT;
    }

    private static boolean isPathMarkerVisible(ResourceLocation markerId) {
        PathMarkerCatalog.Entry entry = PathMarkerCatalog.get(markerId);
        return entry != null && entry.texture() != null;
    }

    private static double getPathSegmentSpacingMultiplier(ResourceLocation markerId) {
        PathMarkerCatalog.Entry entry = PathMarkerCatalog.get(markerId);
        return entry == null ? 1.0 : entry.segmentSpacingMultiplier();
    }

    private static double distanceToPolylineSq(BlockPos pos, List<BlockPos> polyline, boolean closed) {
        synchronized (polyline) {
            if (polyline.isEmpty()) {
                return Double.POSITIVE_INFINITY;
            }

            Vec3 point = Vec3.atLowerCornerOf(pos);
            int y = pos.getY();

            if (polyline.size() == 1) {
                return point.distanceToSqr(Vec3.atLowerCornerOf(polyline.getFirst().atY(y)));
            }

            double distance = Double.POSITIVE_INFINITY;
            int edgeCount = closed ? polyline.size() : polyline.size() - 1;
            for (int i = 0; i < edgeCount; ++i) {
                Vec3 edge1 = Vec3.atLowerCornerOf(polyline.get(i).atY(y));
                Vec3 edge2 = Vec3.atLowerCornerOf(polyline.get((i + 1) % polyline.size()).atY(y));
                distance = Math.min(distance, closestPointToEdge(point, edge1, edge2).distanceToSqr(point));
            }

            return distance;
        }
    }

    private @Nullable MapImage resolvePathMarkerImage(ResourceLocation markerId, float rotation) {
        if (FrontierData.PathStyle.NONE.equals(markerId)) {
            return null;
        }

        PathMarkerCatalog.Entry entry = PathMarkerCatalog.get(markerId);
        if (entry == null || entry.texture() == null) {
            return null;
        }

        MapImage markerImage = createMarkerImage(entry.texture(), color, ClientConfig.PATH_MARKER_OPACITY.get().floatValue());
        if (entry.directional()) {
            markerImage.setRotation(Math.round(rotation));
        }
        return markerImage;
    }

    private @Nullable MapImage resolvePathMarkerHighlightImage(ResourceLocation markerId, float rotation) {
        if (FrontierData.PathStyle.NONE.equals(markerId)) {
            return null;
        }

        ResourceLocation texture = PathMarkerCatalog.getHighlightTexture(markerId);
        if (texture == null) {
            return null;
        }

        MapImage markerImage = createMarkerImage(texture, ColorConstants.TEXTURE_TINT_NONE, 1.f);
        PathMarkerCatalog.Entry entry = PathMarkerCatalog.get(markerId);
        if (entry != null && entry.directional()) {
            markerImage.setRotation(Math.round(rotation));
        }
        return markerImage;
    }

    private static float getSegmentRotation(BlockPos from, BlockPos to) {
        return (float) -Math.toDegrees(Math.atan2(to.getZ() - from.getZ(), to.getX() - from.getX()));
    }

    private static MapImage createMarkerImage(ResourceLocation texture, int color, float opacity) {
        MapImage mapImage = new MapImage(texture, 0, 0, MarkerImageConstants.TEXTURE_SIZE, MarkerImageConstants.TEXTURE_SIZE, color, opacity);
        MarkerImageConstants.applyMapDisplaySize(mapImage);
        return mapImage;
    }

    private record LabelContentMetrics(String label,
                                       boolean hasText,
                                       boolean hasBanner,
                                       int lines,
                                       int textWidthPx,
                                       int textHeightPx,
                                       int bannerWidthPx,
                                       int bannerHeightPx,
                                       int contentWidthPx,
                                       int contentHeightPx,
                                       int textOffsetY,
                                       int bannerOffsetY) {
    }

    private record LabelPlacementKey(PolygonUiPlanEntry entry,
                                     int contentWidthPx,
                                     int contentHeightPx) {
    }

    private record VisualConfigSnapshot(UiVisualConfig fullscreen,
                                        UiVisualConfig minimap,
                                        UiVisualConfig webmap) {
        private List<UiVisualConfig> uiConfigs() {
            return List.of(fullscreen, minimap, webmap);
        }
    }

    private record UiVisualConfig(Context.UI ui,
                                  boolean baseVisible,
                                  Context.MapType[] activeMapTypes,
                                  LabelVisibility labelVisibility) {
    }

    private record LabelVisibility(boolean nameVisible,
                                   boolean collectionVisible,
                                   boolean ownerVisible,
                                   boolean bannerVisible) {
    }

    private record PolygonUiPlanCache(List<PolygonUiPlanEntry> entries) {
    }

    private record PolygonUiPlanEntry(Context.UI ui,
                                      Context.MapType[] activeMapTypes,
                                      LabelVisibility labelVisibility,
                                      PolygonRenderGeometry geometry,
                                      Area overlayArea) {
        private int minZoom() {
            return geometry.minZoom();
        }
    }

    private record PolygonRenderGeometry(MapPolygon polygon,
                                         @Nullable List<MapPolygon> holes,
                                         int minZoom) {
    }

    private record PathLayoutCache(List<PathPointLayout> pointLayouts,
                                   List<PathSegmentLayout> segmentLayouts,
                                   List<PathLabelAnchor> labelAnchors) {
    }

    private record PathPointLayout(BlockPos pos,
                                   ResourceLocation markerId,
                                   float rotation,
                                   double markerClearancePx) {
    }

    private record PathSegmentLayout(BlockPos from,
                                     BlockPos to,
                                     float rotation,
                                     ResourceLocation segmentMarkerId,
                                     double segmentSpacingMultiplier,
                                     double length,
                                     List<BlockPos> repeatedMarkerPositions) {
    }

    private record PathLabelAnchor(double x,
                                   double z,
                                   double screenOffsetDirectionX,
                                   double screenOffsetDirectionY,
                                   double markerClearancePx) {
    }

    private record PathLabelVisualOffset(int x,
                                         int y) {
    }

    public static final class CollectionGeometrySnapshot {
        private final List<CollectionGeometryIslandSnapshot> islands;

        public CollectionGeometrySnapshot(List<CollectionGeometryIslandSnapshot> islands) {
            this.islands = List.copyOf(islands);
        }

        public List<CollectionGeometryIslandSnapshot> getIslands() {
            return islands;
        }

        public boolean isEmpty() {
            return islands.isEmpty();
        }
    }

    public static final class CollectionGeometryIslandSnapshot {
        private final Area effectiveArea;
        private final int minZoom;

        public CollectionGeometryIslandSnapshot(Area effectiveArea, int minZoom) {
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
}
