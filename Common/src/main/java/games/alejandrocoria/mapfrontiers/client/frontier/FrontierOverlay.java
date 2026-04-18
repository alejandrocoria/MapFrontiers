package games.alejandrocoria.mapfrontiers.client.frontier;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.MapFrontiersClient;
import games.alejandrocoria.mapfrontiers.client.config.ClientConfig;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.mixin.CubeInvoker;
import games.alejandrocoria.mapfrontiers.client.mixin.GuiGraphicsAccessor;
import games.alejandrocoria.mapfrontiers.client.mixin.SpriteContentsInvoker;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierChange;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierData;
import games.alejandrocoria.mapfrontiers.common.frontier.FrontierSharingChange;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.data.AtlasIds;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
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
import java.util.Optional;
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
    protected VisibilityData effectiveVisibilityData;

    private boolean highlighted = false;

    private final IClientAPI jmAPI;
    private final List<PolygonOverlay> polygonOverlays = new ArrayList<>();
    private final List<PolygonOverlay> highlightPolygonOverlays = new ArrayList<>();
    private Area polygonArea;
    private final List<MarkerOverlay> markerOverlays = new ArrayList<>();
    private final List<MarkerOverlay> highlightMarkerOverlays = new ArrayList<>();
    private final List<MarkerOverlay> labelOverlays = new ArrayList<>();
    private final BannerRenderer bannerRenderer = new BannerRenderer();
    private int previewTextSize = -1;
    private int previewBannerSize = -1;

    private int hash;
    private boolean hashDirty = true;

    private boolean needUpdateOverlay = true;

    public FrontierOverlay(FrontierData data, @Nullable IClientAPI jmAPI) {
        super(data);
        this.jmAPI = jmAPI;
        setVisibilityOverride(MapFrontiersClient.getLocalOverrides().getVisibility(id));
        if (banner != null) {
            bannerRenderer.createTexture(id, banner);
        }
        updateOverlay();
    }

    @Override
    public void updateFromData(FrontierData other) {
        super.updateFromData(other);
        setVisibilityOverride(MapFrontiersClient.getLocalOverrides().getVisibility(id));

        clampSelectedEditablePoint();

        if (banner == null) {
            bannerRenderer.releaseTexture();
        } else {
            bannerRenderer.createTexture(id, banner);
        }

        updateOverlay();
        hashDirty = true;
        markFrontierActivationDirty();
    }

    public void applyChange(FrontierChange change) {
        super.applyChange(change);
        setVisibilityOverride(MapFrontiersClient.getLocalOverrides().getVisibility(id));

        clampSelectedEditablePoint();

        if (change.hasBannerChange()) {
            if (banner == null) {
                bannerRenderer.releaseTexture();
            } else {
                bannerRenderer.createTexture(id, banner);
            }
        }

        if (change.hasNameChange() || change.hasShapeChange() || change.hasColorChange() || change.hasVisibilityChange()
                || change.hasPathStyleChange() || change.hasBannerChange()) {
            updateOverlay();
        }
        if (change.hasShapeChange() || change.hasVisibilityChange()) {
            markFrontierActivationDirty();
        }
    }

    public void applySharingChange(FrontierSharingChange sharingChange) {
        super.applySharingChange(sharingChange);
        hashDirty = true;
    }

    public int getHash() {
        if (hashDirty) {
            hashDirty = false;
            hash = Objects.hash(id, color, dimension, name1, name2, visibilityData, vertices, chunks, points, mode, pathStyle, banner, usersShared,
                    copiedFrom, sourcePluginId);
        }

        return hash;
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

    public void setPreviewLabelSizes(int textSize, int bannerSize) {
        previewTextSize = Math.max(1, textSize);
        previewBannerSize = Math.max(1, bannerSize);
    }

    public void updateOverlayIfNeeded() {
        if (needUpdateOverlay) {
            needUpdateOverlay = false;
            updateOverlay();
        }
    }

    public void updateOverlay() {
        hashDirty = true;

        if (jmAPI == null) {
            return;
        }

        removeOverlay();
        recalculateOverlays();

        try {
            if (isFrontierVisible()) {
                showPolygonOverlays(polygonOverlays);
                showMarkerOverlays(markerOverlays);
                showMarkerOverlays(labelOverlays);
            }

            if (highlighted) {
                showPolygonOverlays(highlightPolygonOverlays);
                showMarkerOverlays(highlightMarkerOverlays);
            }
        } catch (Throwable t) {
            MapFrontiers.LOGGER.error(t.getMessage(), t);
        }
    }

    private void markFrontierActivationDirty() {
        if (jmAPI != null) {
            MapFrontiersClient.markFrontierActivationDirty();
        }
    }

    private boolean isFrontierVisible() {
        return ClientConfig.getVisibilityValue(ClientConfig.FRONTIER_VISIBILITY.get(), getVisibility(VisibilityData.Visibility.Frontier));
    }

    private void showPolygonOverlays(List<PolygonOverlay> overlays) throws Exception {
        for (PolygonOverlay polygon : overlays) {
            jmAPI.show(polygon);
        }
    }

    private void showMarkerOverlays(List<MarkerOverlay> overlays) throws Exception {
        for (MarkerOverlay marker : overlays) {
            jmAPI.show(marker);
        }
    }

    private void removeOverlay() {
        for (PolygonOverlay polygon : polygonOverlays) {
            jmAPI.remove(polygon);
        }

        for (PolygonOverlay polygon : highlightPolygonOverlays) {
            jmAPI.remove(polygon);
        }

        for (MarkerOverlay marker : markerOverlays) {
            jmAPI.remove(marker);
        }

        for (MarkerOverlay marker : highlightMarkerOverlays) {
            jmAPI.remove(marker);
        }

        for (MarkerOverlay label : labelOverlays) {
            jmAPI.remove(label);
        }
    }

    public void deleted() {
        removeOverlay();
        bannerRenderer.releaseTexture();
    }

    public boolean pointIsInside(BlockPos pos, double maxDistanceToOpen) {
        if (mode == Mode.Vertex) {
            if (vertices.size() > 2) {
                return polygonArea != null && polygonArea.contains(pos.getX() + 0.5, pos.getZ() + 0.5);
            } else if (maxDistanceToOpen > 0.0) {
                return distanceToPolylineSq(pos, vertices, true) <= maxDistanceToOpen * maxDistanceToOpen;
            }
        } else if (mode == Mode.Path) {
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
        if (mode != Mode.Vertex) {
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
        if (mode != Mode.Vertex) {
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
        if (mode != Mode.Path) {
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
        needUpdateOverlay = true;
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
        needUpdateOverlay = true;
        markFrontierActivationDirty();
    }

    @Override
    public void removeVertex(int index) {
        super.removeVertex(index);
        hashDirty = true;
        needUpdateOverlay = true;
        markFrontierActivationDirty();
    }

    @Override
    public void moveAllVertices(BlockPos delta) {
        super.moveAllVertices(delta);
        hashDirty = true;
        needUpdateOverlay = true;
        markFrontierActivationDirty();
        MapFrontiersClient.updateSelectedFrontierMarker(personal, getDimension(), this);
    }

    @Override
    public boolean toggleChunk(ChunkPos chunk) {
        boolean added = super.toggleChunk(chunk);
        hashDirty = true;
        needUpdateOverlay = true;
        markFrontierActivationDirty();
        return added;
    }

    @Override
    public boolean addChunk(ChunkPos chunk) {
        if (super.addChunk(chunk)) {
            hashDirty = true;
            needUpdateOverlay = true;
            markFrontierActivationDirty();
            return true;
        }

        return false;
    }

    @Override
    public boolean removeChunk(ChunkPos chunk) {
        if (super.removeChunk(chunk)) {
            hashDirty = true;
            needUpdateOverlay = true;
            markFrontierActivationDirty();
            return true;
        }

        return false;
    }

    @Override
    public void moveAllChunks(ChunkPos delta) {
        super.moveAllChunks(delta);
        hashDirty = true;
        needUpdateOverlay = true;
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
        needUpdateOverlay = true;
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
        needUpdateOverlay = true;
        markFrontierActivationDirty();
        MapFrontiersClient.updateSelectedFrontierMarker(personal, getDimension(), this);
    }

    @Override
    public void setName1(String name) {
        super.setName1(name);
        hashDirty = true;
        needUpdateOverlay = true;
    }

    @Override
    public void setName2(String name) {
        super.setName2(name);
        hashDirty = true;
        needUpdateOverlay = true;
    }

    @Override
    public void setVisibility(VisibilityData.Visibility visibility, boolean enable) {
        super.setVisibility(visibility, enable);
        setVisibilityOverride(MapFrontiersClient.getLocalOverrides().getVisibility(id));
        hashDirty = true;
        needUpdateOverlay = true;
        markFrontierActivationDirty();
    }

    @Override
    public void toggleVisibility(VisibilityData.Visibility visibility) {
        super.toggleVisibility(visibility);
        setVisibilityOverride(MapFrontiersClient.getLocalOverrides().getVisibility(id));
        hashDirty = true;
        needUpdateOverlay = true;
        markFrontierActivationDirty();
    }

    public void setVisibilityOverride(Pair<VisibilityData, VisibilityData> visibilityOverride) {
        effectiveVisibilityData = new VisibilityData(visibilityData);
        for (VisibilityData.Visibility visibility : VisibilityData.Visibility.values()) {
            if (visibilityOverride.second().getValue(visibility)) {
                effectiveVisibilityData.setValue(visibility, visibilityOverride.first().getValue(visibility));
            }
        }
        needUpdateOverlay = true;
        markFrontierActivationDirty();
    }

    @Override
    public boolean getVisibility(VisibilityData.Visibility visibility) {
        return effectiveVisibilityData.getValue(visibility);
    }

    public boolean isVisibleOnFullscreenMap(Context.MapType mapType) {
        if (!isFrontierVisible()) {
            return false;
        }

        if (!ClientConfig.getVisibilityValue(ClientConfig.FULLSCREEN_VISIBILITY.get(), getVisibility(VisibilityData.Visibility.Fullscreen))) {
            return false;
        }

        return switch (mapType) {
            case Day -> ClientConfig.getVisibilityValue(ClientConfig.FULLSCREEN_DAY_VISIBILITY.get(), getVisibility(VisibilityData.Visibility.FullscreenDay));
            case Night -> ClientConfig.getVisibilityValue(ClientConfig.FULLSCREEN_NIGHT_VISIBILITY.get(), getVisibility(VisibilityData.Visibility.FullscreenNight));
            case Underground -> ClientConfig.getVisibilityValue(ClientConfig.FULLSCREEN_UNDERGROUND_VISIBILITY.get(),
                    getVisibility(VisibilityData.Visibility.FullscreenUnderground));
            case Topo -> ClientConfig.getVisibilityValue(ClientConfig.FULLSCREEN_TOPO_VISIBILITY.get(), getVisibility(VisibilityData.Visibility.FullscreenTopo));
            case Biome -> ClientConfig.getVisibilityValue(ClientConfig.FULLSCREEN_BIOME_VISIBILITY.get(), getVisibility(VisibilityData.Visibility.FullscreenBiome));
        };
    }

    public void setVisibilityData(VisibilityData visibilityData) {
        super.setVisibilityData(visibilityData);
        setVisibilityOverride(MapFrontiersClient.getLocalOverrides().getVisibility(id));
        hashDirty = true;
        needUpdateOverlay = true;
        markFrontierActivationDirty();
    }

    @Override
    public void setColor(int color) {
        super.setColor(color);
        hashDirty = true;
        needUpdateOverlay = true;
    }

    @Override
    public void setPathStyle(PathStyle pathStyle) {
        super.setPathStyle(pathStyle);
        hashDirty = true;
        needUpdateOverlay = true;
    }

    @Override
    public void setDimension(ResourceKey<Level> dimension) {
        super.setDimension(dimension);
        hashDirty = true;
        markFrontierActivationDirty();
    }

    @Override
    public void setBanner(@Nullable ItemStack itemBanner) {
        super.setBanner(itemBanner);
        hashDirty = true;
        needUpdateOverlay = true;

        if (itemBanner == null) {
            bannerRenderer.releaseTexture();
        } else {
            bannerRenderer.createTexture(id, banner);
        }
    }

    @Override
    public void setBanner(DyeColor base, BannerPatternLayers bannerPatterns) {
        super.setBanner(base, bannerPatterns);
        bannerRenderer.createTexture(id, banner);
        hashDirty = true;
        needUpdateOverlay = true;
    }

    @Override
    public void setBannerData(@Nullable BannerData bannerData) {
        super.setBannerData(bannerData);
        hashDirty = true;
        needUpdateOverlay = true;

        if (bannerData == null) {
            bannerRenderer.releaseTexture();
        } else {
            bannerRenderer.createTexture(id, banner);
        }
    }

    @Override
    public void setBannerRotation(int rotation) {
        if (hasBanner()) {
            super.setBannerRotation(rotation);
            bannerRenderer.setRotation(rotation);
            needUpdateOverlay = true;
            hashDirty = true;
        }
    }

    @Override
    public void addUserShared(SettingsUserShared userShared) {
        super.addUserShared(userShared);
        hashDirty = true;
    }

    public BlockPos getClosestVertex(BlockPos vertex, double belowDistance) {
        BlockPos closest = null;
        double closestDistance = belowDistance;

        if (mode == Mode.Path) {
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

        double radians = Math.toRadians(banner.rotation);
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
        bannerRenderer.releaseTexture();
        if (banner != null) {
            bannerRenderer.createTexture(id, banner);
        }
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
        needUpdateOverlay = true;
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
        needUpdateOverlay = true;
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
        return mode == Mode.Vertex ? selectedPointIndex : -1;
    }

    public int getSelectedPointIndex() {
        return mode == Mode.Path ? selectedPointIndex : -1;
    }

    public int getSelectedEditablePointIndex() {
        return switch (mode) {
            case Vertex, Path -> selectedPointIndex;
            case Chunk -> -1;
        };
    }

    public void clearSelectedEditablePoint() {
        selectedPointIndex = -1;
        MapFrontiersClient.updateSelectedFrontierMarker(personal, getDimension(), this);
    }

    public @Nullable BlockPos getSelectedEditablePoint() {
        if (mode == Mode.Path && selectedPointIndex >= 0 && selectedPointIndex < points.size()) {
            return points.get(selectedPointIndex);
        }
        if (mode == Mode.Vertex && selectedPointIndex >= 0 && selectedPointIndex < vertices.size()) {
            return vertices.get(selectedPointIndex);
        }

        return null;
    }

    public void moveSelectedEditablePoint(BlockPos pos, float snapDistance) {
        if (mode == Mode.Path) {
            moveSelectedPoint(pos, snapDistance);
        } else if (mode == Mode.Vertex) {
            moveSelectedVertex(pos, snapDistance);
        }
    }

    public void moveAllPathPoints(BlockPos delta) {
        super.moveAllPoints(delta);
        hashDirty = true;
        needUpdateOverlay = true;
        markFrontierActivationDirty();
        MapFrontiersClient.updateSelectedFrontierMarker(personal, getDimension(), this);
    }

    public void addPathPointBeforeStart(BlockPos pos) {
        if (mode != Mode.Path) {
            return;
        }

        pos = snapVertex(pos, ClientConfig.SNAP_DISTANCE.get());
        super.addPoint(pos, 0);
        selectedPointIndex = 0;
        hashDirty = true;
        needUpdateOverlay = true;
        markFrontierActivationDirty();
        MapFrontiersClient.updateSelectedFrontierMarker(personal, getDimension(), this);
    }

    public void addPathPointAfterEnd(BlockPos pos) {
        if (mode != Mode.Path) {
            return;
        }

        pos = snapVertex(pos, ClientConfig.SNAP_DISTANCE.get());
        int index = points.size();
        super.addPoint(pos, index);
        selectedPointIndex = index;
        hashDirty = true;
        needUpdateOverlay = true;
        markFrontierActivationDirty();
        MapFrontiersClient.updateSelectedFrontierMarker(personal, getDimension(), this);
    }

    public void insertPathPoint(BlockPos pos) {
        if (mode != Mode.Path) {
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
        needUpdateOverlay = true;
        markFrontierActivationDirty();
        MapFrontiersClient.updateSelectedFrontierMarker(personal, getDimension(), this);
    }

    public void invertPathDirection() {
        if (mode != Mode.Path || points.size() < 2) {
            return;
        }

        synchronized (points) {
            Collections.reverse(points);
        }
        if (selectedPointIndex >= 0 && selectedPointIndex < points.size()) {
            selectedPointIndex = points.size() - 1 - selectedPointIndex;
        }

        hashDirty = true;
        needUpdateOverlay = true;
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
        int size = switch (mode) {
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
        needUpdateOverlay = true;
    }

    public BlockPos getCenter() {
        if (mode == Mode.Path && points.size() == 1) {
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
        polygonOverlays.clear();
        highlightPolygonOverlays.clear();
        markerOverlays.clear();
        highlightMarkerOverlays.clear();
        labelOverlays.clear();

        updateBounds();

        area = 0;
        perimeter = 0.f;
        polygonArea = null;

        ShapeProperties shapeProps = new ShapeProperties()
                .setStrokeWidth(ClientConfig.BORDER_WIDTH.get())
                .setStrokeColor(color)
                .setStrokeOpacity(ClientConfig.BORDER_OPACITY.get().floatValue())
                .setStrokePosition(ShapeProperties.StrokePosition.INSIDE)
                .setFillColor(color)
                .setFillOpacity(ClientConfig.POLYGONS_OPACITY.get().floatValue());

        if (mode == Mode.Vertex) {
            recalculateVertices(shapeProps, highlighted ? createHighlightShapeProperties() : null);
        } else if (mode == Mode.Path) {
            recalculatePath();
        } else {
            recalculateChunks(shapeProps, highlighted ? createHighlightShapeProperties() : null);
        }
    }

    private static ShapeProperties createHighlightShapeProperties() {
        return new ShapeProperties()
                .setStrokeWidth(2)
                .setStrokeColor(0xFFFFFF)
                .setStrokeOpacity(1)
                .setStrokePosition(ShapeProperties.StrokePosition.OUTSIDE)
                .setFillOpacity(0);
    }

    private void addPolygonOverlays(ShapeProperties shapeProps, PolygonRenderGeometry geometry, boolean addLabels) {
        boolean fullscreenV = ClientConfig.getVisibilityValue(ClientConfig.FULLSCREEN_VISIBILITY.get(), getVisibility(VisibilityData.Visibility.Fullscreen));
        boolean fullscreenDayV = ClientConfig.getVisibilityValue(ClientConfig.FULLSCREEN_DAY_VISIBILITY.get(), getVisibility(VisibilityData.Visibility.FullscreenDay));
        boolean fullscreenNightV = ClientConfig.getVisibilityValue(ClientConfig.FULLSCREEN_NIGHT_VISIBILITY.get(), getVisibility(VisibilityData.Visibility.FullscreenNight));
        boolean fullscreenUndergroundV = ClientConfig.getVisibilityValue(ClientConfig.FULLSCREEN_UNDERGROUND_VISIBILITY.get(), getVisibility(VisibilityData.Visibility.FullscreenUnderground));
        boolean fullscreenTopoV = ClientConfig.getVisibilityValue(ClientConfig.FULLSCREEN_TOPO_VISIBILITY.get(), getVisibility(VisibilityData.Visibility.FullscreenTopo));
        boolean fullscreenBiomeV = ClientConfig.getVisibilityValue(ClientConfig.FULLSCREEN_BIOME_VISIBILITY.get(), getVisibility(VisibilityData.Visibility.FullscreenBiome));
        boolean minimapV = ClientConfig.getVisibilityValue(ClientConfig.MINIMAP_VISIBILITY.get(), getVisibility(VisibilityData.Visibility.Minimap));
        boolean minimapDayV = ClientConfig.getVisibilityValue(ClientConfig.MINIMAP_DAY_VISIBILITY.get(), getVisibility(VisibilityData.Visibility.MinimapDay));
        boolean minimapNightV = ClientConfig.getVisibilityValue(ClientConfig.MINIMAP_NIGHT_VISIBILITY.get(), getVisibility(VisibilityData.Visibility.MinimapNight));
        boolean minimapUndergroundV = ClientConfig.getVisibilityValue(ClientConfig.MINIMAP_UNDERGROUND_VISIBILITY.get(), getVisibility(VisibilityData.Visibility.MinimapUnderground));
        boolean minimapTopoV = ClientConfig.getVisibilityValue(ClientConfig.MINIMAP_TOPO_VISIBILITY.get(), getVisibility(VisibilityData.Visibility.MinimapTopo));
        boolean minimapBiomeV = ClientConfig.getVisibilityValue(ClientConfig.MINIMAP_BIOME_VISIBILITY.get(), getVisibility(VisibilityData.Visibility.MinimapBiome));
        boolean webmapV = ClientConfig.getVisibilityValue(ClientConfig.WEBMAP_VISIBILITY.get(), getVisibility(VisibilityData.Visibility.Webmap));
        boolean webmapDayV = ClientConfig.getVisibilityValue(ClientConfig.WEBMAP_DAY_VISIBILITY.get(), getVisibility(VisibilityData.Visibility.WebmapDay));
        boolean webmapNightV = ClientConfig.getVisibilityValue(ClientConfig.WEBMAP_NIGHT_VISIBILITY.get(), getVisibility(VisibilityData.Visibility.WebmapNight));
        boolean webmapUndergroundV = ClientConfig.getVisibilityValue(ClientConfig.WEBMAP_UNDERGROUND_VISIBILITY.get(), getVisibility(VisibilityData.Visibility.WebmapUnderground));
        boolean webmapTopoV = ClientConfig.getVisibilityValue(ClientConfig.WEBMAP_TOPO_VISIBILITY.get(), getVisibility(VisibilityData.Visibility.WebmapTopo));
        boolean webmapBiomeV = ClientConfig.getVisibilityValue(ClientConfig.WEBMAP_BIOME_VISIBILITY.get(), getVisibility(VisibilityData.Visibility.WebmapBiome));
        Area overlayArea = addLabels ? buildOverlayArea(geometry.polygon(), geometry.holes()) : null;
        double labelSolverPrecision = getLabelSolverPrecision();
        Map<LabelPlacementKey, FrontierLabelPlacementSolver.LabelPlacement> placementCache = new HashMap<>();

        if (fullscreenV) {
            PolygonOverlay overlay = createPolygonOverlay(shapeProps, geometry, Context.UI.Fullscreen,
                    getActiveMapTypes(fullscreenDayV, fullscreenNightV, fullscreenUndergroundV, fullscreenTopoV, fullscreenBiomeV));
            if (addLabels) {
                boolean fullscreenNameV = ClientConfig.getVisibilityValue(ClientConfig.FULLSCREEN_NAME_VISIBILITY.get(),
                        getVisibility(VisibilityData.Visibility.FullscreenName));
                boolean fullscreenOwnerV = ClientConfig.getVisibilityValue(ClientConfig.FULLSCREEN_OWNER_VISIBILITY.get(),
                        getVisibility(VisibilityData.Visibility.FullscreenOwner));
                boolean fullscreenBannerV = ClientConfig.getVisibilityValue(ClientConfig.FULLSCREEN_BANNER_VISIBILITY.get(),
                        getVisibility(VisibilityData.Visibility.FullscreenBanner));
                addLabelOverlay(overlay,
                        buildLabelContentMetrics(fullscreenNameV, fullscreenOwnerV, fullscreenBannerV),
                        overlayArea,
                        labelSolverPrecision,
                        placementCache);
            }
            polygonOverlays.add(overlay);
        }
        if (minimapV) {
            PolygonOverlay overlay = createPolygonOverlay(shapeProps, geometry, Context.UI.Minimap,
                    getActiveMapTypes(minimapDayV, minimapNightV, minimapUndergroundV, minimapTopoV, minimapBiomeV));
            if (addLabels) {
                boolean minimapNameV = ClientConfig.getVisibilityValue(ClientConfig.MINIMAP_NAME_VISIBILITY.get(),
                        getVisibility(VisibilityData.Visibility.MinimapName));
                boolean minimapOwnerV = ClientConfig.getVisibilityValue(ClientConfig.MINIMAP_OWNER_VISIBILITY.get(),
                        getVisibility(VisibilityData.Visibility.MinimapOwner));
                boolean minimapBannerV = ClientConfig.getVisibilityValue(ClientConfig.MINIMAP_BANNER_VISIBILITY.get(),
                        getVisibility(VisibilityData.Visibility.MinimapBanner));
                addLabelOverlay(overlay,
                        buildLabelContentMetrics(minimapNameV, minimapOwnerV, minimapBannerV),
                        overlayArea,
                        labelSolverPrecision,
                        placementCache);
            }
            polygonOverlays.add(overlay);
        }
        if (webmapV) {
            PolygonOverlay overlay = createPolygonOverlay(shapeProps, geometry, Context.UI.Webmap,
                    getActiveMapTypes(webmapDayV, webmapNightV, webmapUndergroundV, webmapTopoV, webmapBiomeV));
            if (addLabels) {
                boolean webmapNameV = ClientConfig.getVisibilityValue(ClientConfig.WEBMAP_NAME_VISIBILITY.get(),
                        getVisibility(VisibilityData.Visibility.WebmapName));
                boolean webmapOwnerV = ClientConfig.getVisibilityValue(ClientConfig.WEBMAP_OWNER_VISIBILITY.get(),
                        getVisibility(VisibilityData.Visibility.WebmapOwner));
                boolean webmapBannerV = ClientConfig.getVisibilityValue(ClientConfig.WEBMAP_BANNER_VISIBILITY.get(),
                        getVisibility(VisibilityData.Visibility.WebmapBanner));
                addLabelOverlay(overlay,
                        buildLabelContentMetrics(webmapNameV, webmapOwnerV, webmapBannerV),
                        overlayArea,
                        labelSolverPrecision,
                        placementCache);
            }
            polygonOverlays.add(overlay);
        }
    }

    private PolygonOverlay createPolygonOverlay(ShapeProperties shapeProps, PolygonRenderGeometry geometry,
                                                Context.UI ui, Context.MapType[] mapTypes) {
        PolygonOverlay overlay = new PolygonOverlay(MapFrontiers.MODID, dimension, shapeProps, geometry.polygon(), geometry.holes());
        overlay.setActiveUIs(ui);
        overlay.setActiveMapTypes(mapTypes);
        if (geometry.minZoom() > 0) {
            overlay.setMinZoom(geometry.minZoom());
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

    private void addPolygonGeometryOverlays(ShapeProperties shapeProps, @Nullable ShapeProperties highlightShapeProps,
                                            PolygonRenderGeometry geometry, boolean addLabels) {
        addPolygonOverlays(shapeProps, geometry, addLabels);
        if (highlightShapeProps != null) {
            addHighlightPolygonOverlay(highlightShapeProps, geometry);
        }
    }

    private void recalculateVertices(ShapeProperties shapeProps, @Nullable ShapeProperties highlightShapeProps) {
        synchronized (vertices) {
            if (vertices.size() > 2) {
                MapPolygon polygon = new MapPolygon(vertices);
                polygonArea = PolygonHelper.toArea(polygon);
                addPolygonGeometryOverlays(shapeProps, highlightShapeProps, new PolygonRenderGeometry(polygon, null, 0), true);

                BlockPos last = vertices.getLast();
                for (BlockPos vertex : vertices) {
                    area += last.getX() * vertex.getZ() - last.getZ() * vertex.getX();
                    last = vertex;
                }
                area = abs(area / 2.f);
            } else if (!vertices.isEmpty()) {
                addPolygonGeometryOverlays(shapeProps, highlightShapeProps,
                        new PolygonRenderGeometry(createIncompleteVertexPolygon(), null, INCOMPLETE_VERTEX_FRONTIER_MIN_ZOOM), false);
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

    private void recalculatePath() {
        synchronized (points) {
            boolean fullscreenV = ClientConfig.getVisibilityValue(ClientConfig.FULLSCREEN_VISIBILITY.get(), getVisibility(VisibilityData.Visibility.Fullscreen));
            boolean fullscreenNameV = ClientConfig.getVisibilityValue(ClientConfig.FULLSCREEN_NAME_VISIBILITY.get(), getVisibility(VisibilityData.Visibility.FullscreenName));
            boolean fullscreenOwnerV = ClientConfig.getVisibilityValue(ClientConfig.FULLSCREEN_OWNER_VISIBILITY.get(), getVisibility(VisibilityData.Visibility.FullscreenOwner));
            boolean fullscreenBannerV = ClientConfig.getVisibilityValue(ClientConfig.FULLSCREEN_BANNER_VISIBILITY.get(), getVisibility(VisibilityData.Visibility.FullscreenBanner));
            boolean fullscreenDayV = ClientConfig.getVisibilityValue(ClientConfig.FULLSCREEN_DAY_VISIBILITY.get(), getVisibility(VisibilityData.Visibility.FullscreenDay));
            boolean fullscreenNightV = ClientConfig.getVisibilityValue(ClientConfig.FULLSCREEN_NIGHT_VISIBILITY.get(), getVisibility(VisibilityData.Visibility.FullscreenNight));
            boolean fullscreenUndergroundV = ClientConfig.getVisibilityValue(ClientConfig.FULLSCREEN_UNDERGROUND_VISIBILITY.get(), getVisibility(VisibilityData.Visibility.FullscreenUnderground));
            boolean fullscreenTopoV = ClientConfig.getVisibilityValue(ClientConfig.FULLSCREEN_TOPO_VISIBILITY.get(), getVisibility(VisibilityData.Visibility.FullscreenTopo));
            boolean fullscreenBiomeV = ClientConfig.getVisibilityValue(ClientConfig.FULLSCREEN_BIOME_VISIBILITY.get(), getVisibility(VisibilityData.Visibility.FullscreenBiome));
            boolean minimapV = ClientConfig.getVisibilityValue(ClientConfig.MINIMAP_VISIBILITY.get(), getVisibility(VisibilityData.Visibility.Minimap));
            boolean minimapNameV = ClientConfig.getVisibilityValue(ClientConfig.MINIMAP_NAME_VISIBILITY.get(), getVisibility(VisibilityData.Visibility.MinimapName));
            boolean minimapOwnerV = ClientConfig.getVisibilityValue(ClientConfig.MINIMAP_OWNER_VISIBILITY.get(), getVisibility(VisibilityData.Visibility.MinimapOwner));
            boolean minimapBannerV = ClientConfig.getVisibilityValue(ClientConfig.MINIMAP_BANNER_VISIBILITY.get(), getVisibility(VisibilityData.Visibility.MinimapBanner));
            boolean minimapDayV = ClientConfig.getVisibilityValue(ClientConfig.MINIMAP_DAY_VISIBILITY.get(), getVisibility(VisibilityData.Visibility.MinimapDay));
            boolean minimapNightV = ClientConfig.getVisibilityValue(ClientConfig.MINIMAP_NIGHT_VISIBILITY.get(), getVisibility(VisibilityData.Visibility.MinimapNight));
            boolean minimapUndergroundV = ClientConfig.getVisibilityValue(ClientConfig.MINIMAP_UNDERGROUND_VISIBILITY.get(), getVisibility(VisibilityData.Visibility.MinimapUnderground));
            boolean minimapTopoV = ClientConfig.getVisibilityValue(ClientConfig.MINIMAP_TOPO_VISIBILITY.get(), getVisibility(VisibilityData.Visibility.MinimapTopo));
            boolean minimapBiomeV = ClientConfig.getVisibilityValue(ClientConfig.MINIMAP_BIOME_VISIBILITY.get(), getVisibility(VisibilityData.Visibility.MinimapBiome));
            boolean webmapV = ClientConfig.getVisibilityValue(ClientConfig.WEBMAP_VISIBILITY.get(), getVisibility(VisibilityData.Visibility.Webmap));
            boolean webmapNameV = ClientConfig.getVisibilityValue(ClientConfig.WEBMAP_NAME_VISIBILITY.get(), getVisibility(VisibilityData.Visibility.WebmapName));
            boolean webmapOwnerV = ClientConfig.getVisibilityValue(ClientConfig.WEBMAP_OWNER_VISIBILITY.get(), getVisibility(VisibilityData.Visibility.WebmapOwner));
            boolean webmapBannerV = ClientConfig.getVisibilityValue(ClientConfig.WEBMAP_BANNER_VISIBILITY.get(), getVisibility(VisibilityData.Visibility.WebmapBanner));
            boolean webmapDayV = ClientConfig.getVisibilityValue(ClientConfig.WEBMAP_DAY_VISIBILITY.get(), getVisibility(VisibilityData.Visibility.WebmapDay));
            boolean webmapNightV = ClientConfig.getVisibilityValue(ClientConfig.WEBMAP_NIGHT_VISIBILITY.get(), getVisibility(VisibilityData.Visibility.WebmapNight));
            boolean webmapUndergroundV = ClientConfig.getVisibilityValue(ClientConfig.WEBMAP_UNDERGROUND_VISIBILITY.get(), getVisibility(VisibilityData.Visibility.WebmapUnderground));
            boolean webmapTopoV = ClientConfig.getVisibilityValue(ClientConfig.WEBMAP_TOPO_VISIBILITY.get(), getVisibility(VisibilityData.Visibility.WebmapTopo));
            boolean webmapBiomeV = ClientConfig.getVisibilityValue(ClientConfig.WEBMAP_BIOME_VISIBILITY.get(), getVisibility(VisibilityData.Visibility.WebmapBiome));

            if (fullscreenV) {
                Context.MapType[] mapTypes = getActiveMapTypes(fullscreenDayV, fullscreenNightV, fullscreenUndergroundV, fullscreenTopoV, fullscreenBiomeV);
                createPathMarkers(Context.UI.Fullscreen, mapTypes);
                createPathLabels(Context.UI.Fullscreen, mapTypes, fullscreenNameV, fullscreenOwnerV, fullscreenBannerV);
            }
            if (minimapV) {
                Context.MapType[] mapTypes = getActiveMapTypes(minimapDayV, minimapNightV, minimapUndergroundV, minimapTopoV, minimapBiomeV);
                createPathMarkers(Context.UI.Minimap, mapTypes);
                createPathLabels(Context.UI.Minimap, mapTypes, minimapNameV, minimapOwnerV, minimapBannerV);
            }
            if (webmapV) {
                Context.MapType[] mapTypes = getActiveMapTypes(webmapDayV, webmapNightV, webmapUndergroundV, webmapTopoV, webmapBiomeV);
                createPathMarkers(Context.UI.Webmap, mapTypes);
                createPathLabels(Context.UI.Webmap, mapTypes, webmapNameV, webmapOwnerV, webmapBannerV);
            }

            if (highlighted) {
                createPathHighlightMarkers();
            }

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

    private void createPathMarkers(Context.UI uiArray, Context.MapType[] mapTypesArray) {
        if (points.isEmpty()) {
            return;
        }

        if (points.size() == 1) {
            Identifier markerId = getPathSinglePointMarkerId();
            addSingleMarker(points.getFirst(), resolvePathMarkerImage(markerId, 0.f), 100, uiArray, mapTypesArray);
            return;
        }

        for (int i = 0; i < points.size(); ++i) {
            BlockPos point = points.get(i);

            if (i < points.size() - 1) {
                BlockPos nextPoint = points.get(i + 1);
                float rotation = getSegmentRotation(point, nextPoint);
                MapImage segmentMarker = resolvePathMarkerImage(pathStyle.segmentMarker, rotation);
                double segmentSpacingMultiplier = getPathSegmentSpacingMultiplier(pathStyle.segmentMarker);
                addRepeatedMarkers(point, nextPoint, uiArray, mapTypesArray, segmentMarker, 99, segmentSpacingMultiplier);
            }

            Identifier markerId = getPathPointMarkerId(i);
            float rotation = getPathPointMarkerRotation(i);

            addSingleMarker(point, resolvePathMarkerImage(markerId, rotation), 100, uiArray, mapTypesArray);
        }
    }

    private void createPathHighlightMarkers() {
        if (points.isEmpty()) {
            return;
        }

        if (points.size() == 1) {
            Identifier markerId = getPathSinglePointMarkerId();
            addSingleMarker(highlightMarkerOverlays, points.getFirst(), resolvePathMarkerHighlightImage(markerId, 0.f), 101,
                    Context.UI.Fullscreen, HIGHLIGHT_MAP_TYPES);
            return;
        }

        for (int i = 0; i < points.size(); ++i) {
            BlockPos point = points.get(i);

            if (i < points.size() - 1) {
                BlockPos nextPoint = points.get(i + 1);
                float rotation = getSegmentRotation(point, nextPoint);
                MapImage segmentHighlight = resolvePathMarkerHighlightImage(pathStyle.segmentMarker, rotation);
                double segmentSpacingMultiplier = getPathSegmentSpacingMultiplier(pathStyle.segmentMarker);
                addRepeatedMarkers(highlightMarkerOverlays, point, nextPoint, Context.UI.Fullscreen, HIGHLIGHT_MAP_TYPES,
                        segmentHighlight, 100, segmentSpacingMultiplier);
            }

            Identifier markerId = getPathPointMarkerId(i);
            float rotation = getPathPointMarkerRotation(i);
            addSingleMarker(highlightMarkerOverlays, point, resolvePathMarkerHighlightImage(markerId, rotation), 101,
                    Context.UI.Fullscreen, HIGHLIGHT_MAP_TYPES);
        }
    }

    private Identifier getPathPointMarkerId(int pointIndex) {
        Identifier markerId;
        if (pointIndex == 0) {
            markerId = pathStyle.startMarker;
        } else if (pointIndex == points.size() - 1) {
            markerId = pathStyle.endMarker;
        } else {
            markerId = pathStyle.innerMarker;
        }

        if (FrontierData.PathStyle.NONE.equals(markerId) && !FrontierData.PathStyle.NONE.equals(pathStyle.segmentMarker)) {
            return pathStyle.segmentMarker;
        }

        return markerId;
    }

    private float getPathPointMarkerRotation(int pointIndex) {
        if (pointIndex < points.size() - 1) {
            return getSegmentRotation(points.get(pointIndex), points.get(pointIndex + 1));
        }

        return getSegmentRotation(points.get(pointIndex - 1), points.get(pointIndex));
    }

    private void createPathLabels(Context.UI uiArray, Context.MapType[] mapTypesArray, boolean nameVisible, boolean ownerVisible, boolean bannerVisible) {
        LabelContentMetrics metrics = buildLabelContentMetrics(nameVisible, ownerVisible, bannerVisible);
        if (!metrics.hasText() && !metrics.hasBanner()) {
            return;
        }

        for (PathLabelAnchor anchor : getPathLabelAnchors()) {
            addPathLabelOverlay(uiArray, mapTypesArray, metrics, anchor);
        }
    }

    //
    // Algorithm adapted from https://stackoverflow.com/a/63888205/2647614
    //
    private void recalculateChunks(ShapeProperties shapeProps, @Nullable ShapeProperties highlightShapeProps) {
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
                    MapFrontiers.LOGGER.warn(String.format("Frontier %1$s is too large and the polygon corresponding to the hole %2$s could not be located", id, polygon.getFirst()));
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

            addPolygonGeometryOverlays(shapeProps, highlightShapeProps, new PolygonRenderGeometry(polygon, polygonHoles, 0), true);
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
        if (mode == Mode.Vertex) {
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

    private void addLabelOverlay(PolygonOverlay polygonOverlay,
                                 LabelContentMetrics metrics,
                                 Area overlayArea,
                                 double labelSolverPrecision,
                                 Map<LabelPlacementKey, FrontierLabelPlacementSolver.LabelPlacement> placementCache) {
        if (!metrics.hasText() && !metrics.hasBanner()) {
            return;
        }

        TextProperties textProps = createBaseTextProperties().setOffsetY(metrics.textOffsetY());
        LabelPlacementKey placementKey = new LabelPlacementKey(metrics.contentWidthPx(), metrics.contentHeightPx());
        FrontierLabelPlacementSolver.LabelPlacement placement = placementCache.computeIfAbsent(placementKey,
                ignored -> FrontierLabelPlacementSolver.solve(overlayArea,
                        metrics.contentWidthPx(),
                        metrics.contentHeightPx(),
                        labelSolverPrecision));

        if (ClientConfig.HIDE_NAMES_THAT_DONT_FIT.get()) {
            applyMinZoom(textProps, placement);
        }

        BlockPos anchor = BlockPos.containing(placement.centerX(), OVERLAY_Y, placement.centerZ());
        MarkerOverlay labelOverlay = new MarkerOverlay(MapFrontiers.MODID, anchor, createLabelAnchorIcon(metrics));
        labelOverlay.setActiveUIs(polygonOverlay.getActiveUIs().toArray(new Context.UI[0]));
        labelOverlay.setActiveMapTypes(polygonOverlay.getActiveMapTypes().toArray(new Context.MapType[0]));
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

    private LabelContentMetrics buildLabelContentMetrics(boolean nameVisible, boolean ownerVisible, boolean bannerVisible) {
        boolean hasBanner = bannerVisible && bannerRenderer.hasBanner();
        if (!nameVisible && !ownerVisible && !hasBanner) {
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
        return mode == Mode.Chunk ? CHUNK_LABEL_SOLVER_PRECISION : VERTEX_LABEL_SOLVER_PRECISION;
    }

    private TextProperties createBaseTextProperties() {
        TextProperties textProperties = new TextProperties()
                .setOpacity(ClientConfig.TEXT_OPACITY.get().floatValue())
                .setScale(getTextSize())
                .setBackgroundOpacity(0.f);
        switch (ClientConfig.TEXT_COLOR.get()) {
            case ClientConfig.TextColor.FrontierColor -> textProperties.setColor(color);
            case ClientConfig.TextColor.FrontierColorBright -> textProperties.setColor(colorMaxBrightness(color));
            case ClientConfig.TextColor.White -> textProperties.setColor(ColorConstants.WHITE);
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

        MapImage bannerIcon = new MapImage(bannerRenderer.getImage());
        bannerIcon.setBlur(false);
        bannerIcon.setAnchorX(metrics.bannerWidthPx() / 2.0 - offsetX);
        bannerIcon.setAnchorY(metrics.bannerOffsetY() - offsetY);
        bannerIcon.setDisplayWidth(metrics.bannerWidthPx());
        bannerIcon.setDisplayHeight(metrics.bannerHeightPx());
        bannerIcon.setOpacity(ClientConfig.BANNER_OPACITY.get().floatValue());
        bannerIcon.setRotation(-bannerRenderer.getRotation());
        return bannerIcon;
    }

    private int getTextSize() {
        return previewTextSize > 0 ? previewTextSize : ClientConfig.TEXT_SIZE.get();
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

    private List<PathLabelAnchor> getPathLabelAnchors() {
        List<PathLabelAnchor> anchors = new ArrayList<>();

        if (points.isEmpty()) {
            return anchors;
        }

        if (points.size() == 1) {
            if (pathStyle.labelAtStart || pathStyle.labelAtEnd || pathStyle.labelAtMiddle) {
                BlockPos point = points.getFirst();
                anchors.add(new PathLabelAnchor(point.getX(), point.getZ(), 0.0, 1.0,
                        getPathMarkerClearancePx(getPathSinglePointMarkerId())));
            }
            return anchors;
        }

        if (pathStyle.labelAtStart) {
            anchors.add(getPathEndpointLabelAnchor(points.getFirst(), points.get(1), getPathMarkerClearancePx(getPathPointMarkerId(0))));
        }
        if (pathStyle.labelAtMiddle) {
            anchors.add(getPathMidpointLabelAnchor());
        }
        if (pathStyle.labelAtEnd) {
            int lastPointIndex = points.size() - 1;
            anchors.add(getPathEndpointLabelAnchor(points.getLast(), points.get(points.size() - 2),
                    getPathMarkerClearancePx(getPathPointMarkerId(lastPointIndex))));
        }

        return anchors;
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

    private PathLabelAnchor getPathMidpointLabelAnchor() {
        if (points.isEmpty()) {
            return new PathLabelAnchor(0, 0, 0.0, 0.0, 0.0);
        }

        if (points.size() == 1) {
            BlockPos point = points.getFirst();
            return new PathLabelAnchor(point.getX(), point.getZ(), 0.0, 0.0, 0.0);
        }

        double totalLength = 0.0;
        for (int i = 1; i < points.size(); ++i) {
            totalLength += Math.sqrt(points.get(i).distSqr(points.get(i - 1)));
        }

        if (totalLength < 0.0001) {
            BlockPos point = points.getFirst();
            return new PathLabelAnchor(point.getX(), point.getZ(), 0.0, 0.0, 0.0);
        }

        double halfLength = totalLength / 2.0;
        double traversed = 0.0;
        for (int i = 1; i < points.size(); ++i) {
            BlockPos from = points.get(i - 1);
            BlockPos to = points.get(i);
            double segmentLength = Math.sqrt(to.distSqr(from));
            if (traversed + segmentLength >= halfLength) {
                double t = (halfLength - traversed) / segmentLength;
                double x = from.getX() + (to.getX() - from.getX()) * t;
                double z = from.getZ() + (to.getZ() - from.getZ()) * t;
                return new PathLabelAnchor(x, z, 0.0, 0.0, 0.0);
            }
            traversed += segmentLength;
        }

        BlockPos point = points.getLast();
        return new PathLabelAnchor(point.getX(), point.getZ(), 0.0, 0.0, 0.0);
    }

    private static double getPathMarkerClearancePx(Identifier markerId) {
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
        image.setPixel(0, 0, 0);
        MapImage mapImage = new MapImage(image);
        mapImage.setAnchorX(0.5);
        mapImage.setAnchorY(0.5);
        mapImage.setDisplayWidth(1);
        mapImage.setDisplayHeight(1);
        mapImage.setOpacity(0.f);
        return mapImage;
    }

    private void updateBounds() {
        if (mode == Mode.Vertex) {
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
        } else if (mode == Mode.Path) {
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

    private void addRepeatedMarkers(BlockPos from, BlockPos to, Context.UI uiArray, Context.MapType[] mapTypesArray,
                                    @Nullable MapImage markerImage, int displayOrder, double spacingMultiplier) {
        addRepeatedMarkers(markerOverlays, from, to, uiArray, mapTypesArray, markerImage, displayOrder, spacingMultiplier);
    }

    private void addRepeatedMarkers(List<MarkerOverlay> overlays, BlockPos from, BlockPos to, Context.UI uiArray, Context.MapType[] mapTypesArray,
                                    @Nullable MapImage markerImage, int displayOrder, double spacingMultiplier) {
        if (markerImage == null) {
            return;
        }

        int dx = to.getX() - from.getX();
        int dz = to.getZ() - from.getZ();
        double length = Math.hypot(dx, dz);
        if (length <= 1.0) {
            return;
        }

        List<BlockPos> repeatedMarkerPositions = getDiscreteInteriorLinePositions(from, to);
        if (repeatedMarkerPositions.isEmpty()) {
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
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
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

    private Identifier getPathSinglePointMarkerId() {
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

    private static boolean isPathMarkerVisible(Identifier markerId) {
        PathMarkerCatalog.Entry entry = PathMarkerCatalog.get(markerId);
        return entry != null && entry.texture() != null;
    }

    private static double getPathSegmentSpacingMultiplier(Identifier markerId) {
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

    private @Nullable MapImage resolvePathMarkerImage(Identifier markerId, float rotation) {
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

    private @Nullable MapImage resolvePathMarkerHighlightImage(Identifier markerId, float rotation) {
        if (FrontierData.PathStyle.NONE.equals(markerId)) {
            return null;
        }

        Identifier texture = PathMarkerCatalog.getHighlightTexture(markerId);
        if (texture == null) {
            return null;
        }

        MapImage markerImage = createMarkerImage(texture, ColorConstants.WHITE, 1.f);
        PathMarkerCatalog.Entry entry = PathMarkerCatalog.get(markerId);
        if (entry != null && entry.directional()) {
            markerImage.setRotation(Math.round(rotation));
        }
        return markerImage;
    }

    private static float getSegmentRotation(BlockPos from, BlockPos to) {
        return (float) -Math.toDegrees(Math.atan2(to.getZ() - from.getZ(), to.getX() - from.getX()));
    }

    private static MapImage createMarkerImage(Identifier texture, int color, float opacity) {
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

    private record LabelPlacementKey(int contentWidthPx,
                                     int contentHeightPx) {
    }

    private record PolygonRenderGeometry(MapPolygon polygon,
                                         @Nullable List<MapPolygon> holes,
                                         int minZoom) {
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

    public static class BannerRenderer {
        private Identifier textureLocation;
        private NativeImage bannerImage;
        private int rotation;

        private void createTexture(UUID id, BannerData bannerData) {
            releaseTexture();

            rotation = bannerData.rotation;

            Minecraft mc = Minecraft.getInstance();
            ClientLevel level = mc.level;
            if (level == null) {
                return;
            }

            ListTag patterns = FrontierData.BannerData.normalizePatterns(bannerData.patterns);
            BannerPatternLayers patternLayers = BannerPatternLayers.EMPTY;
            if (patterns != null) {
                Optional<BannerPatternLayers> bannerPatterns = BannerPatternLayers.CODEC.parse(level.registryAccess().createSerializationContext(NbtOps.INSTANCE), patterns).result();
                if (bannerPatterns.isPresent()) {
                    patternLayers = bannerPatterns.get();
                } else {
                    MapFrontiers.LOGGER.error("Error creating banner pattern layers");
                    return;
                }
            }

            ModelPart bannerModelPart = mc.getEntityModels().bakeLayer(ModelLayers.STANDING_BANNER_FLAG).getChild("flag");
            float[] flagUV = {0, 0, 0, 0};
            bannerModelPart.visit(new PoseStack(), (pose, path, i, cube) -> {
                for (ModelPart.Polygon polygon : ((CubeInvoker) cube).mapfrontiers$getPolygon()) {
                    if (polygon.normal().z() < 0) {
                        flagUV[0] = polygon.vertices()[0].u();
                        flagUV[1] = polygon.vertices()[0].v();
                        flagUV[2] = polygon.vertices()[2].u();
                        flagUV[3] = polygon.vertices()[2].v();
                    }
                }
            });

            if (flagUV[0] == flagUV[2] || flagUV[1] == flagUV[3]) {
                MapFrontiers.LOGGER.error("Error creating banner pattern layers");
                return;
            }

            TextureAtlasSprite base = mc.getAtlasManager().get(Sheets.BANNER_BASE);
            SpriteContents baseSprite = base.contents();
            int width = (int) (abs(flagUV[0] - flagUV[2]) * baseSprite.width());
            int height = (int) (abs(flagUV[1] - flagUV[3]) * baseSprite.height());
            NativeImage tempBannerImage = new NativeImage(width, height, false);

            generateBannerLayer(tempBannerImage, flagUV, baseSprite, bannerData.baseColor);

            for (int i = 0; i < patternLayers.layers().size(); ++i) {
                BannerPatternLayers.Layer layer = patternLayers.layers().get(i);
                Identifier patternTextureLocation = layer.pattern().value().assetId().withPrefix("entity/banner/");
                TextureAtlasSprite sprite = mc.getAtlasManager().getAtlasOrThrow(AtlasIds.BANNER_PATTERNS).getSprite(patternTextureLocation);

                generateBannerLayer(tempBannerImage, flagUV, sprite.contents(), layer.color());
            }

            bannerImage = tempBannerImage.mappedCopy(ARGB::opaque);

            textureLocation = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, id.toString());
            DynamicTexture texture = new DynamicTexture(() -> textureLocation.toString(), bannerImage);
            mc.getTextureManager().register(textureLocation, texture);
        }

        private static void generateBannerLayer(NativeImage bannerImage, float[] flagUV, SpriteContents sprite, DyeColor dye) {
            NativeImage spriteImage = ((SpriteContentsInvoker) sprite).mapfrontiers$getOriginalImage();
            for (int y = 0; y < bannerImage.getHeight(); ++y) {
                for (int x = 0; x < bannerImage.getWidth(); ++x) {
                    int u = (int) (Mth.lerp((x + 0.5f) / bannerImage.getWidth(), flagUV[2], flagUV[0]) * sprite.width());
                    int v = (int) (Mth.lerp((y + 0.5f) / bannerImage.getHeight(), flagUV[1], flagUV[3]) * sprite.height());
                    int color = ARGB.multiply(spriteImage.getPixel(u, v), dye.getTextureDiffuseColor());
                    blendPixel(bannerImage, x, y, color);
                }
            }
        }

        private static void blendPixel(NativeImage image, int x, int y, int color) {
            int i = image.getPixel(x, y);
            float f = (float) ARGB.alpha(color) / 255.0F;
            float f1 = (float) ARGB.red(color) / 255.0F;
            float f2 = (float) ARGB.green(color) / 255.0F;
            float f3 = (float) ARGB.blue(color) / 255.0F;
            float f4 = (float) ARGB.alpha(i) / 255.0F;
            float f5 = (float) ARGB.red(i) / 255.0F;
            float f6 = (float) ARGB.green(i) / 255.0F;
            float f7 = (float) ARGB.blue(i) / 255.0F;
            float f8 = 1.0F - f;
            float f9 = f * f + f4 * f8;
            float f10 = f1 * f + f5 * f8;
            float f11 = f2 * f + f6 * f8;
            float f12 = f3 * f + f7 * f8;
            if (f9 > 1.0F)
            {
                f9 = 1.0F;
            }

            if (f10 > 1.0F)
            {
                f10 = 1.0F;
            }

            if (f11 > 1.0F)
            {
                f11 = 1.0F;
            }

            if (f12 > 1.0F)
            {
                f12 = 1.0F;
            }

            int j = (int) (f9 * 255.0F);
            int k = (int) (f10 * 255.0F);
            int l = (int) (f11 * 255.0F);
            int i1 = (int) (f12 * 255.0F);
            image.setPixel(x, y, ARGB.color(j, k, l, i1));
        }

        public void renderBanner(GuiGraphics graphics, int centerX, int y, int scale) {
            if (textureLocation == null) {
                return;
            }

            int width = 20 * scale;
            int height = 40 * scale;

            int x = centerX - width / 2;
            float centerY = y + height / 2f;

            graphics.pose().pushMatrix();
            graphics.pose().translate(centerX, centerY);
            graphics.pose().rotate((float) Math.toRadians(rotation));
            graphics.pose().translate(-centerX, -centerY);

            ((GuiGraphicsAccessor) graphics).innerBlitInvoker(RenderPipelines.GUI_TEXTURED, textureLocation, x, x + width, y, y + height, 0, 1, 0, 1, 0xFFFFFFFF);

            graphics.pose().popMatrix();
        }

        public boolean hasBanner() {
            return textureLocation != null;
        }

        public NativeImage getImage() {
            return bannerImage;
        }

        private void releaseTexture() {
            if (textureLocation != null) {
                Minecraft.getInstance().getTextureManager().release(textureLocation);
                textureLocation = null;
            }

            if (bannerImage != null) {
                bannerImage.close();
                bannerImage = null;
            }
        }

        public void setRotation(int rotation) {
            this.rotation = rotation;
        }

        public int getRotation() {
            return rotation;
        }
    }
}
