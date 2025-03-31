package games.alejandrocoria.mapfrontiers.client;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.mixin.CubeInvoker;
import games.alejandrocoria.mapfrontiers.client.mixin.SpriteContentsInvoker;
import games.alejandrocoria.mapfrontiers.common.Config;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
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
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static java.lang.Math.abs;

@ParametersAreNonnullByDefault
public class FrontierOverlay extends FrontierData {
    private static final MapImage markerVertex = new MapImage(ResourceLocation.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/marker.png"), 0,
            0, 12, 12, ColorConstants.WHITE, 1.f);
    private static final MapImage markerDot = new MapImage(ResourceLocation.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/marker.png"), 12, 0,
            8, 8, ColorConstants.WHITE, 1.f);

    static {
        markerVertex.setAnchorX(markerVertex.getDisplayWidth() / 2.0).setAnchorY(markerVertex.getDisplayHeight() / 2.0);
        markerDot.setAnchorX(markerDot.getDisplayWidth() / 2.0).setAnchorY(markerDot.getDisplayHeight() / 2.0);
    }

    public BlockPos topLeft;
    public BlockPos bottomRight;
    public float perimeter = 0.f;
    public float area = 0.f;
    private int vertexSelected = -1;
    protected VisibilityData effectiveVisibilityData;

    private boolean highlighted = false;

    private final IClientAPI jmAPI;
    private final List<PolygonOverlay> polygonOverlays = new ArrayList<>();
    private Area polygonArea;
    private final List<MarkerOverlay> markerOverlays = new ArrayList<>();
    private final List<MarkerOverlay> bannerOverlays = new ArrayList<>();
    private final BannerRenderer bannerRenderer = new BannerRenderer();

    private int hash;
    private boolean dirtyhash = true;

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

        if (vertexSelected >= vertices.size()) {
            vertexSelected = vertices.size() - 1;
        }

        if (other.hasChange(Change.Name) || other.hasChange(Change.Vertices) || other.hasChange(Change.Color) || other.hasChange(Change.Visibility)) {
            updateOverlay();
        }

        if (other.hasChange(Change.Banner)) {
            if (banner == null) {
                bannerRenderer.releaseTexture();
            } else {
                bannerRenderer.createTexture(id, banner);
            }
            dirtyhash = true;
        }
    }

    public int getHash() {
        if (dirtyhash) {
            dirtyhash = false;

            int prime = 31;
            hash = 1;
            hash = prime * hash + id.hashCode();
            hash = prime * hash + color;
            hash = prime * hash + ((dimension == null) ? 0 : dimension.hashCode());
            hash = prime * hash + ((name1 == null) ? 0 : name1.hashCode());
            hash = prime * hash + ((name2 == null) ? 0 : name2.hashCode());
            hash = prime * hash + visibilityData.getHash();
            hash = prime * hash + ((vertices == null) ? 0 : vertices.hashCode());
            hash = prime * hash + ((chunks == null) ? 0 : chunks.hashCode());
            hash = prime * hash + mode.ordinal();
            hash = prime * hash + ((banner == null) ? 0 : banner.hashCode());
            hash = prime * hash + ((usersShared == null) ? 0 : usersShared.hashCode());
        }

        return hash;
    }

    public void updateOverlayIfNeeded() {
        if (needUpdateOverlay) {
            needUpdateOverlay = false;
            updateOverlay();
        }
    }

    public void updateOverlay() {
        dirtyhash = true;

        if (jmAPI == null) {
            return;
        }

        removeOverlay();
        recalculateOverlays();

        if (Config.getVisibilityValue(Config.frontierVisibility, getVisibility(VisibilityData.Visibility.Frontier))) {
            try {
                for (PolygonOverlay polygon : polygonOverlays) {
                    jmAPI.show(polygon);
                }

                for (MarkerOverlay marker : markerOverlays) {
                    jmAPI.show(marker);
                }

                for (MarkerOverlay banner : bannerOverlays) {
                    jmAPI.show(banner);
                }
            } catch (Throwable t) {
                MapFrontiers.LOGGER.error(t.getMessage(), t);
            }
        }
    }

    private void removeOverlay() {
        for (PolygonOverlay polygon : polygonOverlays) {
            jmAPI.remove(polygon);
        }

        for (MarkerOverlay marker : markerOverlays) {
            jmAPI.remove(marker);
        }

        for (MarkerOverlay banner : bannerOverlays) {
            jmAPI.remove(banner);
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
                synchronized (vertices) {
                    for (int i = 0; i < vertices.size(); ++i) {
                        Vec3 point = Vec3.atLowerCornerOf(pos);
                        int y1 = pos.getY();
                        Vec3 edge1 = Vec3.atLowerCornerOf(vertices.get(i).atY(y1));
                        int y = pos.getY();
                        Vec3 edge2 = Vec3.atLowerCornerOf(vertices.get((i + 1) % vertices.size()).atY(y));
                        double distance = closestPointToEdge(point, edge1, edge2).distanceToSqr(point);
                        if (distance <= maxDistanceToOpen * maxDistanceToOpen) {
                            return true;
                        }
                    }
                }
            }
        } else if (pos.getX() >= topLeft.getX() && pos.getX() <= bottomRight.getX() && pos.getZ() >= topLeft.getZ() && pos.getZ() <= bottomRight.getZ()) {
            return chunks.contains(new ChunkPos(pos));
        }

        return false;
    }

    public void selectClosestVertex(BlockPos pos, double limit) {
        if (mode != Mode.Vertex) {
            vertexSelected = -1;
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

        vertexSelected = closest;
        MapFrontiersClient.getFrontiersOverlayManager(personal).updateSelectedMarker(getDimension(), this);
    }

    public void selectClosestEdge(BlockPos pos) {
        if (mode != Mode.Vertex) {
            vertexSelected = -1;
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

        vertexSelected = closest;
        MapFrontiersClient.getFrontiersOverlayManager(personal).updateSelectedMarker(getDimension(), this);
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
        needUpdateOverlay = true;
    }

    @Override
    public void addVertex(BlockPos pos) {
        addVertex(pos, vertexSelected + 1, Config.snapDistance);
        selectNextVertex();
    }

    public void addVertex(BlockPos pos, int index, int snapDistance) {
        if (snapDistance != 0) {
            pos = snapVertex(pos, snapDistance);
        }

        super.addVertex(pos, index);
        needUpdateOverlay = true;
    }

    @Override
    public void removeVertex(int index) {
        super.removeVertex(index);
        needUpdateOverlay = true;
    }

    @Override
    public void moveAllVertices(BlockPos delta) {
        super.moveAllVertices(delta);
        needUpdateOverlay = true;
    }

    @Override
    public boolean toggleChunk(ChunkPos chunk) {
        boolean added = super.toggleChunk(chunk);
        needUpdateOverlay = true;
        return added;
    }

    @Override
    public boolean addChunk(ChunkPos chunk) {
        if (super.addChunk(chunk)) {
            needUpdateOverlay = true;
            return true;
        }

        return false;
    }

    @Override
    public boolean removeChunk(ChunkPos chunk) {
        if (super.removeChunk(chunk)) {
            needUpdateOverlay = true;
            return true;
        }

        return false;
    }

    @Override
    public void moveAllChunks(ChunkPos delta) {
        super.moveAllChunks(delta);
        needUpdateOverlay = true;
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
        if (vertexSelected < 0 || vertexSelected >= vertices.size()) {
            return;
        }

        if (snapDistance != 0) {
            pos = snapVertex(pos, snapDistance);
        }

        super.moveVertex(pos, vertexSelected);
        needUpdateOverlay = true;
        MapFrontiersClient.getFrontiersOverlayManager(personal).updateSelectedMarker(getDimension(), this);
    }

    @Override
    public void setName1(String name) {
        super.setName1(name);
        needUpdateOverlay = true;
    }

    @Override
    public void setName2(String name) {
        super.setName2(name);
        needUpdateOverlay = true;
    }

    @Override
    public void setVisibility(VisibilityData.Visibility visibility, boolean enable) {
        super.setVisibility(visibility, enable);
        setVisibilityOverride(MapFrontiersClient.getLocalOverrides().getVisibility(id));
        needUpdateOverlay = true;
    }

    @Override
    public void toggleVisibility(VisibilityData.Visibility visibility) {
        super.toggleVisibility(visibility);
        setVisibilityOverride(MapFrontiersClient.getLocalOverrides().getVisibility(id));
        needUpdateOverlay = true;
    }

    public void setVisibilityOverride(Pair<VisibilityData, VisibilityData> visibilityOverride) {
        effectiveVisibilityData = new VisibilityData(visibilityData);
        for (VisibilityData.Visibility visibility : VisibilityData.Visibility.values()) {
            if (visibilityOverride.second().getValue(visibility)) {
                effectiveVisibilityData.setValue(visibility, visibilityOverride.first().getValue(visibility));
            }
        }
        needUpdateOverlay = true;
    }

    @Override
    public boolean getVisibility(VisibilityData.Visibility visibility) {
        return effectiveVisibilityData.getValue(visibility);
    }

    public void setVisibilityData(VisibilityData visibilityData) {
        super.setVisibilityData(visibilityData);
        setVisibilityOverride(MapFrontiersClient.getLocalOverrides().getVisibility(id));
        needUpdateOverlay = true;
    }

    @Override
    public void setColor(int color) {
        super.setColor(color);
        needUpdateOverlay = true;
    }

    @Override
    public void setDimension(ResourceKey<Level> dimension) {
        super.setDimension(dimension);
        dirtyhash = true;
    }

    @Override
    public void setBanner(@Nullable ItemStack itemBanner) {
        super.setBanner(itemBanner);
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
        needUpdateOverlay = true;
    }

    @Override
    public void setBannerData(@Nullable BannerData bannerData) {
        super.setBannerData(bannerData);
        needUpdateOverlay = true;

        if (bannerData == null) {
            bannerRenderer.releaseTexture();
        } else {
            bannerRenderer.createTexture(id, banner);
        }
    }

    @Override
    public void addUserShared(SettingsUserShared userShared) {
        super.addUserShared(userShared);
        dirtyhash = true;
    }

    @Override
    public void removeUserShared(int index) {
        super.removeUserShared(index);
        dirtyhash = true;
    }

    @Override
    public void setUsersShared(List<SettingsUserShared> usersShared) {
        super.setUsersShared(usersShared);
        dirtyhash = true;
    }

    public BlockPos getClosestVertex(BlockPos vertex, double belowDistance) {
        BlockPos closest = null;
        double closestDistance = belowDistance;

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

        return closest;
    }

    public BannerRenderer getBannerRenderer() {
        return bannerRenderer;
    }

    public void removeSelectedVertex() {
        if (vertexSelected < 0) {
            return;
        }

        super.removeVertex(vertexSelected);
        if (vertices.isEmpty()) {
            vertexSelected = -1;
        } else if (vertexSelected > 0) {
            --vertexSelected;
        } else {
            vertexSelected = vertices.size() - 1;
        }

        MapFrontiersClient.getFrontiersOverlayManager(personal).updateSelectedMarker(getDimension(), this);

        needUpdateOverlay = true;
    }

    public void selectNextVertex() {
        ++vertexSelected;
        if (vertexSelected >= vertices.size()) {
            vertexSelected = -1;
        }
        MapFrontiersClient.getFrontiersOverlayManager(personal).updateSelectedMarker(getDimension(), this);
    }

    public int getSelectedVertexIndex() {
        return vertexSelected;
    }

    public BlockPos getSelectedVertex() {
        if (vertexSelected >= 0 && vertexSelected < vertices.size()) {
            return vertices.get(vertexSelected);
        }

        return null;
    }

    public void setHighlighted(boolean highlighted) {
        this.highlighted = highlighted;
        needUpdateOverlay = true;
    }

    public BlockPos getCenter() {
        return new BlockPos((topLeft.getX() + bottomRight.getX()) / 2, 70, (topLeft.getZ() + bottomRight.getZ()) / 2);
    }

    private BlockPos snapVertex(BlockPos vertex, float snapDistance) {
        BlockPos closest = vertex.atY(70);
        double closestDistance = snapDistance * snapDistance;

        for (FrontierOverlay frontier : MapFrontiersClient.getFrontiersOverlayManager(true).getAllFrontiers(dimension)) {
            if (frontier == this) {
                continue;
            }

            BlockPos v = frontier.getClosestVertex(closest, closestDistance);
            if (v != null) {
                closest = v;
                closestDistance = v.distSqr(vertex);
            }
        }

        for (FrontierOverlay frontier : MapFrontiersClient.getFrontiersOverlayManager(false).getAllFrontiers(dimension)) {
            if (frontier == this) {
                continue;
            }

            BlockPos v = frontier.getClosestVertex(closest, closestDistance);
            if (v != null) {
                closest = v;
                closestDistance = v.distSqr(vertex);
            }
        }

        return closest;
    }

    private void recalculateOverlays() {
        polygonOverlays.clear();
        markerOverlays.clear();
        bannerOverlays.clear();

        updateBounds();

        area = 0;
        perimeter = 0.f;
        polygonArea = null;

        ShapeProperties shapeProps = new ShapeProperties().setStrokeWidth(highlighted ? 3 : 0).setStrokeColor(ColorConstants.WHITE)
                .setFillColor(color).setFillOpacity((float) Config.polygonsOpacity);

        if (mode == Mode.Vertex) {
            recalculateVertices(shapeProps);
        } else {
            recalculateChunks(shapeProps);
        }
    }

    private void addPolygonOverlays(ShapeProperties shapeProps, MapPolygon polygon, @Nullable List<MapPolygon> polygonHoles) {
        boolean fullscreenV = Config.getVisibilityValue(Config.fullscreenVisibility, getVisibility(VisibilityData.Visibility.Fullscreen));
        boolean fullscreenNameV = Config.getVisibilityValue(Config.fullscreenNameVisibility, getVisibility(VisibilityData.Visibility.FullscreenName));
        boolean fullscreenOwnerV = Config.getVisibilityValue(Config.fullscreenOwnerVisibility, getVisibility(VisibilityData.Visibility.FullscreenOwner));
        boolean fullscreenBannerV = Config.getVisibilityValue(Config.fullscreenBannerVisibility, getVisibility(VisibilityData.Visibility.FullscreenBanner));
        boolean fullscreenDayV = Config.getVisibilityValue(Config.fullscreenDayVisibility, getVisibility(VisibilityData.Visibility.FullscreenDay));
        boolean fullscreenNightV = Config.getVisibilityValue(Config.fullscreenNightVisibility, getVisibility(VisibilityData.Visibility.FullscreenNight));
        boolean fullscreenUndergroundV = Config.getVisibilityValue(Config.fullscreenUndergroundVisibility, getVisibility(VisibilityData.Visibility.FullscreenUnderground));
        boolean fullscreenTopoV = Config.getVisibilityValue(Config.fullscreenTopoVisibility, getVisibility(VisibilityData.Visibility.FullscreenTopo));
        boolean fullscreenBiomeV = Config.getVisibilityValue(Config.fullscreenBiomeVisibility, getVisibility(VisibilityData.Visibility.FullscreenBiome));
        boolean minimapV = Config.getVisibilityValue(Config.minimapVisibility, getVisibility(VisibilityData.Visibility.Minimap));
        boolean minimapNameV = Config.getVisibilityValue(Config.minimapNameVisibility, getVisibility(VisibilityData.Visibility.MinimapName));
        boolean minimapOwnerV = Config.getVisibilityValue(Config.minimapOwnerVisibility, getVisibility(VisibilityData.Visibility.MinimapOwner));
        boolean minimapBannerV = Config.getVisibilityValue(Config.minimapBannerVisibility, getVisibility(VisibilityData.Visibility.MinimapBanner));
        boolean minimapDayV = Config.getVisibilityValue(Config.minimapDayVisibility, getVisibility(VisibilityData.Visibility.MinimapDay));
        boolean minimapNightV = Config.getVisibilityValue(Config.minimapNightVisibility, getVisibility(VisibilityData.Visibility.MinimapNight));
        boolean minimapUndergroundV = Config.getVisibilityValue(Config.minimapUndergroundVisibility, getVisibility(VisibilityData.Visibility.MinimapUnderground));
        boolean minimapTopoV = Config.getVisibilityValue(Config.minimapTopoVisibility, getVisibility(VisibilityData.Visibility.MinimapTopo));
        boolean minimapBiomeV = Config.getVisibilityValue(Config.minimapBiomeVisibility, getVisibility(VisibilityData.Visibility.MinimapBiome));
        boolean webmapV = Config.getVisibilityValue(Config.webmapVisibility, getVisibility(VisibilityData.Visibility.Webmap));
        boolean webmapNameV = Config.getVisibilityValue(Config.webmapNameVisibility, getVisibility(VisibilityData.Visibility.WebmapName));
        boolean webmapOwnerV = Config.getVisibilityValue(Config.webmapOwnerVisibility, getVisibility(VisibilityData.Visibility.WebmapOwner));
        boolean webmapBannerV = Config.getVisibilityValue(Config.webmapBannerVisibility, getVisibility(VisibilityData.Visibility.WebmapBanner));
        boolean webmapDayV = Config.getVisibilityValue(Config.webmapDayVisibility, getVisibility(VisibilityData.Visibility.WebmapDay));
        boolean webmapNightV = Config.getVisibilityValue(Config.webmapNightVisibility, getVisibility(VisibilityData.Visibility.WebmapNight));
        boolean webmapUndergroundV = Config.getVisibilityValue(Config.webmapUndergroundVisibility, getVisibility(VisibilityData.Visibility.WebmapUnderground));
        boolean webmapTopoV = Config.getVisibilityValue(Config.webmapTopoVisibility, getVisibility(VisibilityData.Visibility.WebmapTopo));
        boolean webmapBiomeV = Config.getVisibilityValue(Config.webmapBiomeVisibility, getVisibility(VisibilityData.Visibility.WebmapBiome));


        BlockPos firstpoint = polygon.getPoints().getFirst();
        Rectangle2D.Double polygonBound = new Rectangle2D.Double(firstpoint.getX(), firstpoint.getZ(), 1, 1);
        for (BlockPos point : polygon.getPoints()) {
            polygonBound.add(point.getX(), point.getZ());
        }

        if (fullscreenV) {
            PolygonOverlay overlay = new PolygonOverlay(MapFrontiers.MODID, dimension, shapeProps, polygon, polygonHoles);
            overlay.setActiveUIs(Context.UI.Fullscreen);
            overlay.setActiveMapTypes(getActiveMapTypes(fullscreenDayV, fullscreenNightV, fullscreenUndergroundV, fullscreenTopoV, fullscreenBiomeV));
            addNameOwnerAndBanner(overlay, polygonBound, fullscreenNameV, fullscreenOwnerV, fullscreenBannerV);
            polygonOverlays.add(overlay);
        }
        if (minimapV) {
            PolygonOverlay overlay = new PolygonOverlay(MapFrontiers.MODID, dimension, shapeProps, polygon, polygonHoles);
            overlay.setActiveUIs(Context.UI.Minimap);
            overlay.setActiveMapTypes(getActiveMapTypes(minimapDayV, minimapNightV, minimapUndergroundV, minimapTopoV, minimapBiomeV));
            addNameOwnerAndBanner(overlay, polygonBound, minimapNameV, minimapOwnerV, minimapBannerV);
            polygonOverlays.add(overlay);
        }
        if (webmapV) {
            PolygonOverlay overlay = new PolygonOverlay(MapFrontiers.MODID, dimension, shapeProps, polygon, polygonHoles);
            overlay.setActiveUIs(Context.UI.Webmap);
            overlay.setActiveMapTypes(getActiveMapTypes(webmapDayV, webmapNightV, webmapUndergroundV, webmapTopoV, webmapBiomeV));
            addNameOwnerAndBanner(overlay, polygonBound, webmapNameV, webmapOwnerV, webmapBannerV);
            polygonOverlays.add(overlay);
        }
    }

    private void recalculateVertices(ShapeProperties shapeProps) {
        synchronized (vertices) {
            if (vertices.size() > 2) {
                MapPolygon polygon = new MapPolygon(vertices);
                addPolygonOverlays(shapeProps, polygon, null);
                polygonArea = PolygonHelper.toArea(polygon);

                BlockPos last = vertices.getLast();
                for (BlockPos vertex : vertices) {
                    area += abs(vertex.getZ() + last.getZ()) / 2.f * (vertex.getX() - last.getX());
                    last = vertex;
                }
                area = abs(area);
            } else {
                boolean fullscreenV = Config.getVisibilityValue(Config.fullscreenVisibility, getVisibility(VisibilityData.Visibility.Fullscreen));
                boolean fullscreenDayV = Config.getVisibilityValue(Config.fullscreenDayVisibility, getVisibility(VisibilityData.Visibility.FullscreenDay));
                boolean fullscreenNightV = Config.getVisibilityValue(Config.fullscreenNightVisibility, getVisibility(VisibilityData.Visibility.FullscreenNight));
                boolean fullscreenUndergroundV = Config.getVisibilityValue(Config.fullscreenUndergroundVisibility, getVisibility(VisibilityData.Visibility.FullscreenUnderground));
                boolean fullscreenTopoV = Config.getVisibilityValue(Config.fullscreenTopoVisibility, getVisibility(VisibilityData.Visibility.FullscreenTopo));
                boolean fullscreenBiomeV = Config.getVisibilityValue(Config.fullscreenBiomeVisibility, getVisibility(VisibilityData.Visibility.FullscreenBiome));
                boolean minimapV = Config.getVisibilityValue(Config.minimapVisibility, getVisibility(VisibilityData.Visibility.Minimap));
                boolean minimapDayV = Config.getVisibilityValue(Config.minimapDayVisibility, getVisibility(VisibilityData.Visibility.MinimapDay));
                boolean minimapNightV = Config.getVisibilityValue(Config.minimapNightVisibility, getVisibility(VisibilityData.Visibility.MinimapNight));
                boolean minimapUndergroundV = Config.getVisibilityValue(Config.minimapUndergroundVisibility, getVisibility(VisibilityData.Visibility.MinimapUnderground));
                boolean minimapTopoV = Config.getVisibilityValue(Config.minimapTopoVisibility, getVisibility(VisibilityData.Visibility.MinimapTopo));
                boolean minimapBiomeV = Config.getVisibilityValue(Config.minimapBiomeVisibility, getVisibility(VisibilityData.Visibility.MinimapBiome));
                boolean webmapV = Config.getVisibilityValue(Config.webmapVisibility, getVisibility(VisibilityData.Visibility.Webmap));
                boolean webmapDayV = Config.getVisibilityValue(Config.webmapDayVisibility, getVisibility(VisibilityData.Visibility.WebmapDay));
                boolean webmapNightV = Config.getVisibilityValue(Config.webmapNightVisibility, getVisibility(VisibilityData.Visibility.WebmapNight));
                boolean webmapUndergroundV = Config.getVisibilityValue(Config.webmapUndergroundVisibility, getVisibility(VisibilityData.Visibility.WebmapUnderground));
                boolean webmapTopoV = Config.getVisibilityValue(Config.webmapTopoVisibility, getVisibility(VisibilityData.Visibility.WebmapTopo));
                boolean webmapBiomeV = Config.getVisibilityValue(Config.webmapBiomeVisibility, getVisibility(VisibilityData.Visibility.WebmapBiome));

                if (fullscreenV) {
                    createMarkersFromVertices(Context.UI.Fullscreen,
                            getActiveMapTypes(fullscreenDayV, fullscreenNightV, fullscreenUndergroundV, fullscreenTopoV, fullscreenBiomeV)
                    );
                }
                if (minimapV){
                    createMarkersFromVertices(Context.UI.Minimap,
                            getActiveMapTypes(minimapDayV, minimapNightV, minimapUndergroundV, minimapTopoV, minimapBiomeV)
                    );
                }
                if (webmapV){
                    createMarkersFromVertices(Context.UI.Webmap,
                            getActiveMapTypes(webmapDayV, webmapNightV, webmapUndergroundV, webmapTopoV, webmapBiomeV)
                    );
                }
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

    private void createMarkersFromVertices(Context.UI uiArray, Context.MapType[] mapTypesArray) {
        for (int i = 0; i < vertices.size(); ++i) {
            MarkerOverlay marker = new MarkerOverlay(MapFrontiers.MODID, vertices.get(i), markerVertex);
            marker.setDimension(dimension);
            marker.setDisplayOrder(100);
            marker.setActiveUIs(uiArray);
            marker.setActiveMapTypes(mapTypesArray);
            markerOverlays.add(marker);
            if (i == 0 && vertices.size() == 2) {
                addMarkerDots(vertices.get(0), vertices.get(1), uiArray, mapTypesArray);
            }
        }
    }

    //
    // Algorithm adapted from https://stackoverflow.com/a/63888205/2647614
    //
    private void recalculateChunks(ShapeProperties shapeProps) {
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

            addPolygonOverlays(shapeProps, polygon, polygonHoles);
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

    private void addNameOwnerAndBanner(PolygonOverlay polygonOverlay, Rectangle2D.Double polygonBound, boolean nameVisible, boolean ownerVisible, boolean bannerVisible) {
        bannerVisible = bannerVisible && bannerRenderer.hasBanner();
        if (!nameVisible && !ownerVisible && !bannerVisible) {
            return;
        }

        TextProperties textProps = new TextProperties().setColor(color).setScale(2.f).setBackgroundOpacity(0.f);

        int lines = 0;
        int totalWidth = 0;
        String label = "";

        if (nameVisible) {
            if (!name1.isEmpty()) {
                ++lines;
                totalWidth = Math.max(totalWidth, Minecraft.getInstance().font.width(name1));
                label += name1;
            }
            if (!name2.isEmpty()) {
                ++lines;
                totalWidth = Math.max(totalWidth, Minecraft.getInstance().font.width(name2));
                if (!label.isEmpty()) {
                    label += "\n";
                }
                label += name2;
            }
        }

        if (ownerVisible && !owner.username.isEmpty()) {
            ++lines;
            totalWidth = Math.max(totalWidth, Minecraft.getInstance().font.width(owner.username));
            if (!label.isEmpty()) {
                label += "\n";
            }
            label += ChatFormatting.ITALIC + owner.username;
        }

        int totalHeight = lines * 18;
        if (bannerVisible) {
            totalHeight += 40;
        }

        int topOffset = totalHeight / 2;
        int textOffset = topOffset - lines * 9;
        int bannerOffset = topOffset - lines * 18;
        if (lines > 1) {
            if (bannerVisible) {
                textOffset -= 6;
            } else {
                textOffset += 12;
            }
        } else if (lines == 1) {
            if (bannerVisible) {
                textOffset += 5;
            } else {
                textOffset += 3;
            }
        }
        textProps.setOffsetY(textOffset);

        if (Config.hideNamesThatDontFit) {
            totalWidth *= 2;
            if (bannerVisible) {
                totalWidth = Math.max(totalWidth, 20);
            }
            setMinSizeTextProperties(textProps, polygonBound, totalWidth + 6, totalHeight + 6);
        }

        if (bannerVisible) {
            MapImage bannerIcon = new MapImage(bannerRenderer.getImage(), 0, 0, 20, 40, ColorConstants.WHITE, 1.f);
            bannerIcon.setBlur(false);
            bannerIcon.setAnchorX(10);
            bannerIcon.setAnchorY(bannerOffset);
            bannerIcon.setDisplayWidth(bannerRenderer.getImage().getWidth());
            bannerIcon.setDisplayHeight(bannerRenderer.getImage().getHeight());
            BlockPos polygonCenter = BlockPos.containing(polygonBound.getCenterX(), 70, polygonBound.getCenterY());

            MarkerOverlay bannerOverlay = new MarkerOverlay(MapFrontiers.MODID, polygonCenter, bannerIcon);
            bannerOverlay.setActiveUIs(polygonOverlay.getActiveUIs().toArray(new Context.UI[0]));
            bannerOverlay.setActiveMapTypes(polygonOverlay.getActiveMapTypes().toArray(new Context.MapType[0]));
            bannerOverlay.setDimension(dimension);
            bannerOverlay.setMinZoom(textProps.getMinZoom());
            bannerOverlay.setMaxZoom(textProps.getMaxZoom());
            bannerOverlays.add(bannerOverlay);

            if (lines > 0) {
                bannerOverlay.setTextProperties(textProps).setOverlayGroupName("frontier").setLabel(label);
            }
        } else {
            if (lines > 0) {
                polygonOverlay.setTextProperties(textProps).setOverlayGroupName("frontier").setLabel(label);
            }
        }
    }

    private void setMinSizeTextProperties(TextProperties textProperties, Rectangle2D.Double polygonBound, int width, int height) {
        double polygonWidthScaled = polygonBound.getWidth() / 256.0;
        double polygonHeightScaled = polygonBound.getHeight() / 256.0;
        int zoom = 2;
        while ((width > polygonWidthScaled || height > polygonHeightScaled) && zoom < 8192) {
            zoom *= 2;
            polygonWidthScaled *= 2.0;
            polygonHeightScaled *= 2.0;
        }

        textProperties.setMinZoom(zoom);
    }

    private void updateBounds() {
        if (mode == Mode.Vertex) {
            if (vertices.isEmpty()) {
                topLeft = new BlockPos(0, 70, 0);
                bottomRight = new BlockPos(0, 70, 0);
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

                topLeft = new BlockPos(minX, 70, minZ);
                bottomRight = new BlockPos(maxX, 70, maxZ);
            }
        } else {
            if (chunks.isEmpty()) {
                topLeft = new BlockPos(0, 70, 0);
                bottomRight = new BlockPos(0, 70, 0);
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

                topLeft = new BlockPos(minX * 16, 70, minZ * 16);
                bottomRight = new BlockPos(maxX * 16 + 16, 70, maxZ * 16 + 16);
            }
        }
    }

    //
    // Functions adapted from https://en.wikipedia.org/wiki/Bresenham%27s_line_algorithm
    //
    private void addMarkerDots(BlockPos from, BlockPos to, Context.UI uiArray, Context.MapType[] mapTypesArray) {
        if (abs(to.getZ() - from.getZ()) < abs(to.getX() - from.getX())) {
            if (from.getX() > to.getX()) {
                addLineMarkerDots(to.getX(), to.getZ(), from.getX(), from.getZ(), uiArray, mapTypesArray);
            } else{
                addLineMarkerDots(from.getX(), from.getZ(), to.getX(), to.getZ(), uiArray, mapTypesArray);
            }
        } else {
            if (from.getZ() > to.getZ()) {
                addLineMarkerDots(to.getX(), to.getZ(), from.getX(), from.getZ(), uiArray, mapTypesArray);
            } else{
                addLineMarkerDots(from.getX(), from.getZ(), to.getX(), to.getZ(), uiArray, mapTypesArray);
            }
        }
    }

    private void addLineMarkerDots(int x0, int z0, int x1, int z1, Context.UI uiArray, Context.MapType[] mapTypesArray) {
        int dx = abs(x1 - x0);
        int sx = x0 < x1 ? 1 : -1;
        int dz = -abs(z1 - z0);
        int sz = z0 < z1 ? 1 : -1;
        int err = dx + dz;
        int i = 0;
        while (true) {
            if (x0 == x1 && z0 == z1) {
                break;
            }
            int e2 = 2 * err;
            if (e2 >= dz) {
                if (x0 == x1) {
                    break;
                }
                err += dz;
                x0 += sx;
            }
            if (e2 <= dx) {
                if (z0 == z1) {
                    break;
                }
                err += dx;
                z0 += sz;
            }

            BlockPos pos = new BlockPos(x0, 70, z0);
            MarkerOverlay dot = new MarkerOverlay(MapFrontiers.MODID, pos, markerDot);
            dot.setDimension(dimension);
            dot.setDisplayOrder(99);
            dot.setActiveUIs(uiArray);
            dot.setActiveMapTypes(mapTypesArray);
            int minZoom = 2;
            if (i % 2 == 0) {
                minZoom = 16384;
            } else if (i % 4 == 1) {
                minZoom = 8192;
            } else if (i % 8 == 3) {
                minZoom = 4096;
            }
            dot.setMinZoom(minZoom);
            markerOverlays.add(dot);

            ++i;
        }
    }

    public static class BannerRenderer {
        private ResourceLocation textureLocation;
        private NativeImage bannerImage;

        private void createTexture(UUID id, BannerData bannerData) {
            releaseTexture();

            Minecraft mc = Minecraft.getInstance();
            ClientLevel level = mc.level;
            if (level == null) {
                return;
            }

            BannerPatternLayers patternLayers;
            Optional<BannerPatternLayers> bannerPatterns = BannerPatternLayers.CODEC.parse(level.registryAccess().createSerializationContext(NbtOps.INSTANCE), bannerData.patterns).result();
            if (bannerPatterns.isPresent()) {
                patternLayers = bannerPatterns.get();
            } else {
                MapFrontiers.LOGGER.error("Error creating banner pattern layers");
                return;
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

            TextureAtlasSprite base = Sheets.BANNER_BASE.sprite();
            SpriteContents baseSprite = base.contents();
            int width = (int) (abs(flagUV[0] - flagUV[2]) * baseSprite.width());
            int height = (int) (abs(flagUV[1] - flagUV[3]) * baseSprite.height());
            bannerImage = new NativeImage(width, height, false);

            generateBannerLayer(bannerImage, flagUV, baseSprite, bannerData.baseColor);

            for (int i = 0; i < patternLayers.layers().size(); ++i) {
                BannerPatternLayers.Layer layer = patternLayers.layers().get(i);
                ResourceLocation patternTextureLocation = layer.pattern().value().assetId().withPrefix("entity/banner/");
                TextureAtlasSprite sprite = mc.getTextureAtlas(Sheets.BANNER_SHEET).apply(patternTextureLocation);

                generateBannerLayer(bannerImage, flagUV, sprite.contents(), layer.color());
            }

            bannerImage.applyToAllPixels(ARGB::opaque);

            textureLocation = ResourceLocation.fromNamespaceAndPath(MapFrontiers.MODID, id.toString());
            DynamicTexture texture = new DynamicTexture(bannerImage);
            texture.setFilter(false, false);
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

        public void renderBanner(GuiGraphics graphics, int x, int y, int scale) {
            if (textureLocation == null) {
                return;
            }

            graphics.flush();

            RenderSystem.setShader(CoreShaders.POSITION_TEX_COLOR);
            RenderSystem.setShaderTexture(0, textureLocation);
            RenderSystem.setShaderColor(1.f, 1.f, 1.f, 1.f);

            RenderSystem.enableBlend();

            Tesselator tesselator = Tesselator.getInstance();
            BufferBuilder buf = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            int color = 0xFFFFFFFF;
            int width = 20 * scale;
            int height = 40 * scale;
            float zLevel = 0.f;
            x -= width / 2;
            Matrix4f matrix = graphics.pose().last().pose();
            buf.addVertex(matrix, x, y + height, zLevel).setUv(0, 1).setColor(color);
            buf.addVertex(matrix, x + width, y + height, zLevel).setUv(1, 1).setColor(color);
            buf.addVertex(matrix, x + width, y, zLevel).setUv(1, 0).setColor(color);
            buf.addVertex(matrix, x, y, zLevel).setUv(0, 0).setColor(color);
            BufferUploader.drawWithShader(buf.buildOrThrow());
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
    }
}
