package games.alejandrocoria.mapfrontiers.client;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.gui.GuiColors;
import games.alejandrocoria.mapfrontiers.common.ConfigData;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import games.alejandrocoria.mapfrontiers.common.util.BlockPosHelper;
import journeymap.client.api.IClientAPI;
import journeymap.client.api.display.Context;
import journeymap.client.api.display.MarkerOverlay;
import journeymap.client.api.display.PolygonOverlay;
import journeymap.client.api.model.MapImage;
import journeymap.client.api.model.MapPolygon;
import journeymap.client.api.model.ShapeProperties;
import journeymap.client.api.model.TextProperties;
import journeymap.client.api.util.PolygonHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatterns;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import static java.lang.Math.abs;

@ParametersAreNonnullByDefault
@Environment(EnvType.CLIENT)
public class FrontierOverlay extends FrontierData {
    private static final MapImage markerVertex = new MapImage(new ResourceLocation(MapFrontiers.MODID + ":textures/gui/marker.png"), 0,
            0, 12, 12, GuiColors.WHITE, 1.f);
    private static final MapImage markerDot = new MapImage(new ResourceLocation(MapFrontiers.MODID + ":textures/gui/marker.png"), 12, 0,
            8, 8, GuiColors.WHITE, 1.f);

    static {
        markerVertex.setAnchorX(markerVertex.getDisplayWidth() / 2.0).setAnchorY(markerVertex.getDisplayHeight() / 2.0);
        markerVertex.setRotation(0);
        markerDot.setAnchorX(markerDot.getDisplayWidth() / 2.0).setAnchorY(markerDot.getDisplayHeight() / 2.0);
        markerDot.setRotation(0);
    }

    public BlockPos topLeft;
    public BlockPos bottomRight;
    public float perimeter = 0.f;
    public float area = 0.f;
    private int vertexSelected = -1;

    private boolean highlighted = false;

    private final IClientAPI jmAPI;
    private final List<PolygonOverlay> polygonOverlays = new ArrayList<>();
    private Area polygonArea;
    private final List<MarkerOverlay> markerOverlays = new ArrayList<>();
    private String displayId;
    private BannerDisplayData bannerDisplay;

    private int hash;
    private boolean dirtyhash = true;

    private boolean needUpdateOverlay = true;

    public FrontierOverlay(FrontierData data, @Nullable IClientAPI jmAPI) {
        super(data);
        this.jmAPI = jmAPI;
        displayId = "frontier_" + id.toString();
        updateOverlay();

        if (banner != null) {
            bannerDisplay = new BannerDisplayData(banner);
        }
    }

    @Override
    public void updateFromData(FrontierData other) {
        super.updateFromData(other);

        if (vertexSelected >= vertices.size()) {
            vertexSelected = vertices.size() - 1;
        }

        if (other.hasChange(Change.Name) || other.hasChange(Change.Vertices) || other.hasChange(Change.Other)) {
            updateOverlay();
        }

        if (other.hasChange(Change.Banner)) {
            if (banner == null) {
                bannerDisplay = null;
            } else {
                bannerDisplay = new BannerDisplayData(banner);
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
            hash = prime * hash + (visible ? 1231 : 1237);
            hash = prime * hash + (fullscreenVisible ? 1231 : 1237);
            hash = prime * hash + (fullscreenNameVisible ? 1231 : 1237);
            hash = prime * hash + (fullscreenOwnerVisible ? 1231 : 1237);
            hash = prime * hash + (minimapVisible ? 1231 : 1237);
            hash = prime * hash + (minimapNameVisible ? 1231 : 1237);
            hash = prime * hash + (minimapOwnerVisible ? 1231 : 1237);
            hash = prime * hash + (announceInChat ? 1231 : 1237);
            hash = prime * hash + (announceInTitle ? 1231 : 1237);
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

        if (visible) {
            try {
                for (PolygonOverlay polygon : polygonOverlays) {
                    jmAPI.show(polygon);
                }

                for (MarkerOverlay marker : markerOverlays) {
                    jmAPI.show(marker);
                }
            } catch (Throwable t) {
                MapFrontiers.LOGGER.error(t.getMessage(), t);
            }
        }
    }

    public void removeOverlay() {
        for (PolygonOverlay polygon : polygonOverlays) {
            jmAPI.remove(polygon);
        }

        for (MarkerOverlay marker : markerOverlays) {
            jmAPI.remove(marker);
        }
    }

    public boolean pointIsInside(BlockPos pos, double maxDistanceToOpen) {
        if (mode == Mode.Vertex) {
            if (vertices.size() > 2) {
                return polygonArea != null && polygonArea.contains(pos.getX() + 0.5, pos.getZ() + 0.5);
            } else if (maxDistanceToOpen > 0.0) {
                synchronized (vertices) {
                    for (int i = 0; i < vertices.size(); ++i) {
                        Vec3 point = Vec3.atLowerCornerOf(pos);
                        Vec3 edge1 = Vec3.atLowerCornerOf(BlockPosHelper.atY(vertices.get(i), pos.getY()));
                        Vec3 edge2 = Vec3.atLowerCornerOf(BlockPosHelper.atY(vertices.get((i + 1) % vertices.size()), pos.getY()));
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
                    double dist = vertex.distSqr(BlockPosHelper.atY(pos, vertex.getY()));
                    if (dist <= distance) {
                        distance = dist;
                        closest = i;
                    }
                }
            }
        }

        vertexSelected = closest;
        ClientProxy.getFrontiersOverlayManager(personal).updateSelectedMarker(getDimension(), this);
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
                    Vec3 edge1 = Vec3.atLowerCornerOf(BlockPosHelper.atY(vertices.get(i), pos.getY()));
                    Vec3 edge2 = Vec3.atLowerCornerOf(BlockPosHelper.atY(vertices.get((i + 1) % vertices.size()), pos.getY()));
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
        ClientProxy.getFrontiersOverlayManager(personal).updateSelectedMarker(getDimension(), this);
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

    @Override
    public void setId(UUID id) {
        super.setId(id);
        displayId = "frontier_" + id;
        needUpdateOverlay = true;
    }

    @Override
    public void addVertex(BlockPos pos) {
        addVertex(pos, vertexSelected + 1, ConfigData.snapDistance);
        selectNextVertex();
    }

    public void addVertex(BlockPos pos, int index, int snapDistance) {
        if (snapDistance != 0) {
            pos = ClientProxy.snapVertex(pos, snapDistance, dimension, this);
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

    public void moveSelectedVertex(BlockPos pos, float snapDistance) {
        if (vertexSelected < 0 || vertexSelected >= vertices.size()) {
            return;
        }

        if (snapDistance != 0) {
            pos = ClientProxy.snapVertex(pos, snapDistance, dimension, this);
        }

        super.moveVertex(pos, vertexSelected);
        needUpdateOverlay = true;
        ClientProxy.getFrontiersOverlayManager(personal).updateSelectedMarker(getDimension(), this);
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
    public void setVisible(boolean visible) {
        super.setVisible(visible);

        if (!visible) {
            vertexSelected = -1;
        }

        needUpdateOverlay = true;
    }

    @Override
    public void setFullscreenVisible(boolean visible) {
        super.setFullscreenVisible(visible);
        needUpdateOverlay = true;
    }

    @Override
    public void setFullscreenNameVisible(boolean nameVisible) {
        super.setFullscreenNameVisible(nameVisible);
        needUpdateOverlay = true;
    }

    @Override
    public void setFullscreenOwnerVisible(boolean ownerVisible) {
        super.setFullscreenOwnerVisible(ownerVisible);
        needUpdateOverlay = true;
    }

    @Override
    public void setMinimapVisible(boolean visible) {
        super.setMinimapVisible(visible);
        needUpdateOverlay = true;
    }

    @Override
    public void setMinimapNameVisible(boolean nameVisible) {
        super.setMinimapNameVisible(nameVisible);
        needUpdateOverlay = true;
    }

    @Override
    public void setMinimapOwnerVisible(boolean ownerVisible) {
        super.setMinimapOwnerVisible(ownerVisible);
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
            bannerDisplay = null;
        } else {
            bannerDisplay = new BannerDisplayData(banner);
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

    public void renderBanner(Minecraft mc, PoseStack matrixStack, int x, int y, int scale) {
        if (bannerDisplay == null) {
            return;
        }

        for (int i = 0; i < bannerDisplay.patternList.size(); ++i) {
            BannerPattern pattern = bannerDisplay.patternList.get(i);
            TextureAtlasSprite sprite = mc.getTextureAtlas(Sheets.BANNER_SHEET).apply(BannerPattern.location(BuiltInRegistries.BANNER_PATTERN.getResourceKey(pattern).get(), true));
            RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
            RenderSystem.setShaderTexture(0, Sheets.BANNER_SHEET);
            RenderSystem.setShaderColor(1.f, 1.f, 1.f, 1.f);

            RenderSystem.enableBlend();

            Tesselator tessellator = Tesselator.getInstance();
            BufferBuilder buf = tessellator.getBuilder();
            float[] colors = bannerDisplay.colorList.get(i).getTextureDiffuseColors();
            int width = 22 * scale;
            int height = 40 * scale;
            float zLevel = 0.f;
            float u1 = sprite.getU0();
            float u2 = sprite.getU0() + 22.f / 512.f;
            float v1 = sprite.getV0() + 1.f / 512.f;
            float v2 = sprite.getV0() + 41.f / 512.f;
            buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
            Matrix4f matrix = matrixStack.last().pose();
            buf.vertex(matrix, x, y + height, zLevel).color(colors[0], colors[1], colors[2], 1.f).uv(u1, v2).endVertex();
            buf.vertex(matrix, x + width, y + height, zLevel).color(colors[0], colors[1], colors[2], 1.f).uv(u2, v2).endVertex();
            buf.vertex(matrix, x + width, y, zLevel).color(colors[0], colors[1], colors[2], 1.f).uv(u2, v1).endVertex();
            buf.vertex(matrix, x, y, zLevel).color(colors[0], colors[1], colors[2], 1.f).uv(u1, v1).endVertex();
            tessellator.end();

            RenderSystem.disableBlend();
        }
    }

    public void removeSelectedVertex() {
        if (vertexSelected < 0) {
            return;
        }

        super.removeVertex(vertexSelected);
        if (vertices.size() == 0) {
            vertexSelected = -1;
        } else if (vertexSelected > 0) {
            --vertexSelected;
        } else {
            vertexSelected = vertices.size() - 1;
        }

        ClientProxy.getFrontiersOverlayManager(personal).updateSelectedMarker(getDimension(), this);

        needUpdateOverlay = true;
    }

    public void selectNextVertex() {
        ++vertexSelected;
        if (vertexSelected >= vertices.size()) {
            vertexSelected = -1;
        }
        ClientProxy.getFrontiersOverlayManager(personal).updateSelectedMarker(getDimension(), this);
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

    public boolean getHighlighted() {
        return highlighted;
    }

    public BlockPos getCenter() {
        return new BlockPos((topLeft.getX() + bottomRight.getX()) / 2, 70, (topLeft.getZ() + bottomRight.getZ()) / 2);
    }

    private void recalculateOverlays() {
        polygonOverlays.clear();
        markerOverlays.clear();

        updateBounds();

        area = 0;
        perimeter = 0.f;
        polygonArea = null;

        ShapeProperties shapeProps = new ShapeProperties().setStrokeWidth(highlighted ? 3 : 0).setStrokeColor(GuiColors.WHITE)
                .setFillColor(color).setFillOpacity((float) ConfigData.polygonsOpacity);

        if (mode == Mode.Vertex) {
            recalculateVertices(shapeProps);
        } else {
            recalculateChunks(shapeProps);
        }
    }

    private void addPolygonOverlays(String id, ShapeProperties shapeProps, MapPolygon polygon, @Nullable List<MapPolygon> polygonHoles) {
        PolygonOverlay polygonOverlay = null;
        PolygonOverlay polygonOverlayFullscreen = null;
        PolygonOverlay polygonOverlayMinimap = null;

        boolean fullscreenV = ConfigData.getVisibilityValue(ConfigData.fullscreenVisibility, fullscreenVisible);
        boolean fullscreenNameV = ConfigData.getVisibilityValue(ConfigData.fullscreenNameVisibility, fullscreenNameVisible);
        boolean fullscreenOwnerV = ConfigData.getVisibilityValue(ConfigData.fullscreenOwnerVisibility, fullscreenOwnerVisible);
        boolean minimapV = ConfigData.getVisibilityValue(ConfigData.minimapVisibility, minimapVisible);
        boolean minimapNameV = ConfigData.getVisibilityValue(ConfigData.minimapNameVisibility, minimapNameVisible);
        boolean minimapOwnerV = ConfigData.getVisibilityValue(ConfigData.minimapNameVisibility, minimapOwnerVisible);

        if (fullscreenV && minimapV && (fullscreenNameV == minimapNameV) && (fullscreenOwnerV == minimapOwnerV)) {
            polygonOverlay = new PolygonOverlay(MapFrontiers.MODID, id, dimension, shapeProps, polygon, polygonHoles);
            polygonOverlay.setActiveUIs(EnumSet.of(Context.UI.Any));
        } else {
            if (fullscreenV) {
                polygonOverlayFullscreen = new PolygonOverlay(MapFrontiers.MODID, id + "_fullscreen", dimension, shapeProps, polygon, polygonHoles);
                polygonOverlayFullscreen.setActiveUIs(EnumSet.of(Context.UI.Fullscreen));
            }
            if (minimapV) {
                polygonOverlayMinimap = new PolygonOverlay(MapFrontiers.MODID, id + "_minimap", dimension, shapeProps, polygon, polygonHoles);
                polygonOverlayMinimap.setActiveUIs(EnumSet.of(Context.UI.Minimap, Context.UI.Webmap));
            }
        }

        if (polygonOverlay != null) {
            addNameAndOwner(polygonOverlay, fullscreenNameV, fullscreenOwnerV);
            polygonOverlays.add(polygonOverlay);
        } else {
            if (polygonOverlayFullscreen != null) {
                addNameAndOwner(polygonOverlayFullscreen, fullscreenNameV, fullscreenOwnerV);
                polygonOverlays.add(polygonOverlayFullscreen);
            }
            if (polygonOverlayMinimap != null) {
                addNameAndOwner(polygonOverlayMinimap, minimapNameV, minimapOwnerV);
                polygonOverlays.add(polygonOverlayMinimap);
            }
        }
    }

    private void recalculateVertices(ShapeProperties shapeProps) {
        synchronized (vertices) {
            if (vertices.size() > 2) {
                MapPolygon polygon = new MapPolygon(vertices);
                addPolygonOverlays(displayId, shapeProps, polygon, null);
                polygonArea = PolygonHelper.toArea(polygon);

                BlockPos last = vertices.get(vertices.size() - 1);
                for (BlockPos vertex : vertices) {
                    area += abs(vertex.getZ() + last.getZ()) / 2.f * (vertex.getX() - last.getX());
                    last = vertex;
                }
                area = abs(area);
            } else {
                boolean fullscreenV = ConfigData.getVisibilityValue(ConfigData.fullscreenVisibility, fullscreenVisible);
                boolean minimapV = ConfigData.getVisibilityValue(ConfigData.minimapVisibility, minimapVisible);
                if (fullscreenV || minimapV) {
                    EnumSet<Context.UI> ui;
                    if (fullscreenV && minimapV) {
                        ui = EnumSet.of(Context.UI.Any);
                    } else if (fullscreenV) {
                        ui = EnumSet.of(Context.UI.Fullscreen);
                    } else {
                        ui = EnumSet.of(Context.UI.Minimap, Context.UI.Webmap);
                    }
                    for (int i = 0; i < vertices.size(); ++i) {
                        String markerId = displayId + "_" + i;
                        MarkerOverlay marker = new MarkerOverlay(MapFrontiers.MODID, markerId, vertices.get(i), markerVertex);
                        marker.setDimension(dimension);
                        marker.setDisplayOrder(100);
                        marker.setActiveUIs(ui);
                        markerOverlays.add(marker);
                        if (i == 0 && vertices.size() == 2) {
                            addMarkerDots(markerId, vertices.get(0), vertices.get(1), ui);
                        }
                    }
                }
            }

            if (vertices.size() > 1) {
                BlockPos last = vertices.get(vertices.size() - 1);
                for (BlockPos vertex : vertices) {
                    perimeter += Math.sqrt(vertex.distSqr(last));
                    last = vertex;
                }
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
                ChunkPos ray = polygon.get(0);
                ChunkPos outerFound = null;
                for (int i = 0; i < 999; ++i) {
                    for (List<ChunkPos> outer : outerPolygons) {
                        ChunkPos outerStart = outer.get(0);
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
                    MapFrontiers.LOGGER.warn(String.format("Frontier %1$s is too large and the polygon corresponding to the hole %2$s could not be located", id, polygon.get(0)));
                }
            }
        }

        for (List<ChunkPos> outer : outerPolygons) {
            removeCollinear(outer);
            for (List<ChunkPos> hole : holesPolygons.get(outer.get(0))) {
                removeCollinear(hole);
            }
        }

        for (List<ChunkPos> outer : outerPolygons) {
            MapPolygon polygon = new MapPolygon(Lists.transform(outer, (c) -> BlockPosHelper.toBlockPos(c, 70)));
            List<MapPolygon> polygonHoles = null;

            if (holesPolygons.containsKey(outer.get(0))) {
                polygonHoles = new ArrayList<>();
                for (List<ChunkPos> hole : holesPolygons.get(outer.get(0))) {
                    polygonHoles.add(new MapPolygon(Lists.transform(hole, (c) -> BlockPosHelper.toBlockPos(c, 70))));
                }
            }

            addPolygonOverlays(displayId + "_" + outer.get(0), shapeProps, polygon, polygonHoles);
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

        ChunkPos prev = chunks.get(0);
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

    private void addNameAndOwner(PolygonOverlay polygonOverlay, boolean nameVisible, boolean ownerVisible) {
        if (!nameVisible && !ownerVisible) {
            return;
        }

        TextProperties textProps = new TextProperties().setColor(color).setScale(2.f).setBackgroundOpacity(0.f);
        if (ConfigData.hideNamesThatDontFit) {
            if (mode == Mode.Vertex) {
                textProps = setMinSizeTextPropierties(textProps, bottomRight.getX() - topLeft.getX(), nameVisible, ownerVisible);
            } else {
                int minX = Integer.MAX_VALUE;
                int maxX = Integer.MIN_VALUE;

                for (BlockPos vertex : polygonOverlay.getOuterArea().getPoints()) {
                    if (vertex.getX() < minX)
                        minX = vertex.getX();
                    if (vertex.getX() > maxX)
                        maxX = vertex.getX();
                }
                textProps = setMinSizeTextPropierties(textProps, maxX - minX, nameVisible, ownerVisible);
            }
        }

        int lines = 0;
        String label = "";

        if (nameVisible) {
            if (!name1.isEmpty()) {
                ++lines;
                label += name1 + "\n";
            }
            if (!name2.isEmpty()) {
                ++lines;
                label += name2 + "\n";
            }
        }

        if (ownerVisible && !owner.username.isEmpty()) {
            ++lines;
            label += ChatFormatting.ITALIC + owner.username + "\n";
        }

        if (lines > 0) {
            if (lines > 1) {
                textProps.setOffsetY(10);
            }
            polygonOverlay.setTextProperties(textProps).setOverlayGroupName("frontier").setLabel(label);
        }
    }

    private TextProperties setMinSizeTextPropierties(TextProperties textProperties, int polygonWidth, boolean nameVisible, boolean ownerVisible) {
        int name1Width = nameVisible ? Minecraft.getInstance().font.width(name1) * 2 : 0;
        int name2Width = nameVisible ? Minecraft.getInstance().font.width(name2) * 2 : 0;
        int onwerWidth = ownerVisible ? Minecraft.getInstance().font.width(owner.username) * 2 : 0;
        int labelWidth = Math.max(onwerWidth, Math.max(name1Width, name2Width)) + 6;

        int zoom = 0;
        while (labelWidth > polygonWidth && zoom < 8) {
            ++zoom;
            polygonWidth *= 2;
        }

        return textProperties.setMinZoom(zoom);
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
    private void addMarkerDots(String markerId, BlockPos from, BlockPos to, EnumSet<Context.UI> ui) {
        if (abs(to.getZ() - from.getZ()) < abs(to.getX() - from.getX())) {
            if (from.getX() > to.getX()) {
                addLineMarkerDots(markerId, to.getX(), to.getZ(), from.getX(), from.getZ(), ui);
            } else{
                addLineMarkerDots(markerId, from.getX(), from.getZ(), to.getX(), to.getZ(), ui);
            }
        } else {
            if (from.getZ() > to.getZ()) {
                addLineMarkerDots(markerId, to.getX(), to.getZ(), from.getX(), from.getZ(), ui);
            } else{
                addLineMarkerDots(markerId, from.getX(), from.getZ(), to.getX(), to.getZ(), ui);
            }
        }
    }

    private void addLineMarkerDots(String markerId, int x0, int z0, int x1, int z1, EnumSet<Context.UI> ui) {
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
            MarkerOverlay dot = new MarkerOverlay(MapFrontiers.MODID, markerId + "_" + i, pos, markerDot);
            dot.setDimension(dimension);
            dot.setDisplayOrder(99);
            int minZoom = 0;
            if (i % 2 == 0) {
                minZoom = 3;
            } else if (i % 4 == 1) {
                minZoom = 2;
            } else if (i % 8 == 3) {
                minZoom = 1;
            }
            dot.setMinZoom(minZoom);
            markerOverlays.add(dot);

            ++i;
        }
    }

    @Environment(EnvType.CLIENT)
    public static class BannerDisplayData {
        public final List<BannerPattern> patternList;
        public final List<DyeColor> colorList;
        public String patternResourceLocation;

        public BannerDisplayData(FrontierData.BannerData bannerData) {
            patternList = new ArrayList<>();
            colorList = new ArrayList<>();
            patternList.add(BuiltInRegistries.BANNER_PATTERN.get(BannerPatterns.BASE));
            colorList.add(bannerData.baseColor);
            patternResourceLocation = "b" + bannerData.baseColor.getId();

            if (bannerData.patterns != null) {
                for (int i = 0; i < bannerData.patterns.size(); ++i) {
                    CompoundTag nbttagcompound = bannerData.patterns.getCompound(i);
                    Holder<BannerPattern> bannerpattern = BannerPattern.byHash(nbttagcompound.getString("Pattern"));

                    if (bannerpattern != null) {
                        patternList.add(bannerpattern.value());
                        int j = nbttagcompound.getInt("Color");
                        colorList.add(DyeColor.byId(j));
                        patternResourceLocation += bannerpattern.value().getHashname() + j;
                    }
                }
            }
        }
    }
}
