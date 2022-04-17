package games.alejandrocoria.mapfrontiers.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.gui.GuiColors;
import games.alejandrocoria.mapfrontiers.client.gui.GuiFrontierBook;
import games.alejandrocoria.mapfrontiers.common.ConfigData;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import games.alejandrocoria.mapfrontiers.common.util.BlockPosHelper;
import journeymap.client.api.IClientAPI;
import journeymap.client.api.display.MarkerOverlay;
import journeymap.client.api.display.PolygonOverlay;
import journeymap.client.api.model.MapImage;
import journeymap.client.api.model.MapPolygon;
import journeymap.client.api.model.ShapeProperties;
import journeymap.client.api.model.TextProperties;
import journeymap.client.api.util.PolygonHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.lang.Math.abs;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
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
    private int vertexSelected;

    private boolean highlighted = false;

    private final IClientAPI jmAPI;
    private PolygonOverlay polygonOverlay;
    private Area polygonArea;
    private final List<MarkerOverlay> markerOverlays = new ArrayList<>();
    private String displayId;
    private BannerDisplayData bannerDisplay;

    private int hash;
    private boolean dirty = true;

    public FrontierOverlay(FrontierData data, @Nullable IClientAPI jmAPI) {
        super(data);
        this.jmAPI = jmAPI;
        displayId = "frontier_" + id.toString();
        vertexSelected = vertices.size() - 1;
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
            dirty = true;
        }
    }

    public int getHash() {
        if (dirty) {
            dirty = false;

            int prime = 31;
            hash = 1;
            hash = prime * hash + id.hashCode();
            hash = prime * hash + (visible ? 1231 : 1237);
            hash = prime * hash + color;
            hash = prime * hash + ((dimension == null) ? 0 : dimension.hashCode());
            hash = prime * hash + ((name1 == null) ? 0 : name1.hashCode());
            hash = prime * hash + ((name2 == null) ? 0 : name2.hashCode());
            hash = prime * hash + (nameVisible ? 1231 : 1237);
            hash = prime * hash + ((vertices == null) ? 0 : vertices.hashCode());
            hash = prime * hash + ((banner == null) ? 0 : banner.hashCode());
            hash = prime * hash + ((usersShared == null) ? 0 : usersShared.hashCode());
        }

        return hash;
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            if (ConfigData.alwaysShowUnfinishedFrontiers || ClientProxy.hasBookItemInHand()
                    || Minecraft.getInstance().screen instanceof GuiFrontierBook) {
                markerVertex.setOpacity(1.f);
                markerDot.setOpacity(1.f);
            } else {
                markerVertex.setOpacity(0.f);
                markerDot.setOpacity(0.f);
            }
        }
    }

    public void updateOverlay() {
        dirty = true;

        if (jmAPI == null) {
            return;
        }

        removeOverlay();
        recalculateOverlays();

        if (visible) {
            try {
                if (polygonOverlay != null) {
                    jmAPI.show(polygonOverlay);
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
        if (polygonOverlay != null) {
            jmAPI.remove(polygonOverlay);
        }

        for (MarkerOverlay marker : markerOverlays) {
            jmAPI.remove(marker);
        }
    }

    public boolean pointIsInside(BlockPos pos, double maxDistanceToOpen) {
        if (visible) {
            if (vertices.size() > 2) {
                return polygonArea != null && polygonArea.contains(pos.getX() + 0.5, pos.getZ() + 0.5);
            } else if (maxDistanceToOpen > 0.0) {
                for (int i = 0; i < vertices.size(); ++i) {
                    Vec3 point = Vec3.atLowerCornerOf(pos);
                    Vec3 edge1 = Vec3.atLowerCornerOf(BlockPosHelper.atY(vertices.get(i),pos.getY()));
                    Vec3 edge2 = Vec3.atLowerCornerOf(BlockPosHelper.atY(vertices.get((i + 1) % vertices.size()),pos.getY()));
                    double distance = closestPointToEdge(point, edge1, edge2).distanceToSqr(point);
                    if (distance <= maxDistanceToOpen * maxDistanceToOpen) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public void selectClosestVertex(BlockPos pos, double limit) {
        double distance = limit * limit;
        int closest = -1;

        if (!vertices.isEmpty()) {
            for (int i = 0; i < vertices.size(); ++i) {
                BlockPos vertex = vertices.get(i);
                double dist = vertex.distSqr(BlockPosHelper.atY(pos,vertex.getY()));
                if (dist <= distance) {
                    distance = dist;
                    closest = i;
                }
            }
        }

        vertexSelected = closest;
        ClientProxy.getFrontiersOverlayManager(personal).updateSelectedMarker(getDimension(), this);
    }

    public void selectClosestEdge(BlockPos pos) {
        double distance = Double.MAX_VALUE;
        int closest = -1;
        double angleSimilarity = -1.0;

        if (vertices.size() == 1) {
            closest = 0;
        } else if (vertices.size() > 1) {
            for (int i = 0; i < vertices.size(); ++i) {
                Vec3 point = Vec3.atLowerCornerOf(pos);
                Vec3 edge1 = Vec3.atLowerCornerOf(BlockPosHelper.atY(vertices.get(i),pos.getY()));
                Vec3 edge2 = Vec3.atLowerCornerOf(BlockPosHelper.atY(vertices.get((i + 1) % vertices.size()),pos.getY()));
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
                        Vec2 edgeDirection = new Vec2((float)edge.x, (float)edge.z).normalized();
                        Vec3 toPos;

                        if (closestPoint.equals(edge1)) {
                            toPos = point.subtract(edge1);
                        } else {
                            edgeDirection = edgeDirection.negated();
                            toPos = point.subtract(edge2);
                        }

                        Vec2 toPosDirection = new Vec2((float)toPos.x, (float)toPos.z).normalized();
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
        updateOverlay();
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
        updateOverlay();
    }

    @Override
    public void removeVertex(int index) {
        super.removeVertex(index);
        updateOverlay();
    }

    public void moveSelectedVertex(BlockPos pos, float snapDistance) {
        if (vertexSelected < 0 || vertexSelected >= vertices.size()) {
            return;
        }

        if (snapDistance != 0) {
            pos = ClientProxy.snapVertex(pos, snapDistance, dimension, this);
        }

        super.moveVertex(pos, vertexSelected);
        updateOverlay();
        ClientProxy.getFrontiersOverlayManager(personal).updateSelectedMarker(getDimension(), this);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);

        if (!visible) {
            vertexSelected = -1;
        }

        updateOverlay();
    }

    @Override
    public void setName1(String name) {
        super.setName1(name);
        updateOverlay();
    }

    @Override
    public void setName2(String name) {
        super.setName2(name);
        updateOverlay();
    }

    @Override
    public void setNameVisible(boolean nameVisible) {
        super.setNameVisible(nameVisible);
        updateOverlay();
    }

    @Override
    public void setColor(int color) {
        super.setColor(color);
        updateOverlay();
    }

    @Override
    public void setDimension(ResourceKey<Level> dimension) {
        super.setDimension(dimension);
        dirty = true;
    }

    @Override
    public void setBanner(@Nullable ItemStack itemBanner) {
        super.setBanner(itemBanner);
        updateOverlay();

        if (itemBanner == null) {
            bannerDisplay = null;
        } else {
            bannerDisplay = new BannerDisplayData(banner);
        }
    }

    @Override
    public void addUserShared(SettingsUserShared userShared) {
        super.addUserShared(userShared);
        dirty = true;
    }

    @Override
    public void removeUserShared(int index) {
        super.removeUserShared(index);
        dirty = true;
    }

    @Override
    public void setUsersShared(List<SettingsUserShared> usersShared) {
        super.setUsersShared(usersShared);
        dirty = true;
    }

    public void renderBanner(Minecraft mc, PoseStack matrixStack, int x, int y, int scale) {
        if (bannerDisplay == null) {
            return;
        }

        for (int i = 0; i < bannerDisplay.patternList.size(); ++i) {
            BannerPattern pattern = bannerDisplay.patternList.get(i);
            TextureAtlasSprite sprite = mc.getTextureAtlas(Sheets.BANNER_SHEET).apply(pattern.location(true));
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

        updateOverlay();
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
        updateOverlay();
    }

    public boolean getHighlighted() {
        return highlighted;
    }

    private void recalculateOverlays() {
        polygonOverlay = null;
        markerOverlays.clear();

        updateBounds();

        area = 0;

        if (vertices.size() > 2) {
            ShapeProperties shapeProps = new ShapeProperties().setStrokeWidth(highlighted ? 3 : 0).setStrokeColor(GuiColors.WHITE)
                    .setFillColor(color).setFillOpacity((float) ConfigData.polygonsOpacity);

            MapPolygon polygon = new MapPolygon(vertices);
            polygonOverlay = new PolygonOverlay(MapFrontiers.MODID, displayId, dimension, shapeProps, polygon);
            polygonArea = PolygonHelper.toArea(polygonOverlay.getOuterArea());

            ConfigData.NameVisibility nameVisibility = ConfigData.nameVisibility;
            if (nameVisibility == ConfigData.NameVisibility.Show
                    || (nameVisibility == ConfigData.NameVisibility.Manual && nameVisible)) {
                TextProperties textProps = new TextProperties().setColor(color).setScale(2.f).setBackgroundOpacity(0.f);
                textProps = setMinSizeTextPropierties(textProps);
                if (!name1.isEmpty() && !name2.isEmpty()) {
                    textProps.setOffsetY(9);
                }
                polygonOverlay.setTextProperties(textProps).setOverlayGroupName("frontier").setLabel(name1 + "\n" + name2);
            }

            BlockPos last = vertices.get(vertices.size() - 1);
            for (BlockPos vertex : vertices) {
                area += abs(vertex.getZ() + last.getZ()) / 2.f * (vertex.getX() - last.getX());
                last = vertex;
            }
            area = abs(area);
        } else {
            for (int i = 0; i < vertices.size(); ++i) {
                String markerId = displayId + "_" + i;
                MarkerOverlay marker = new MarkerOverlay(MapFrontiers.MODID, markerId, vertices.get(i), markerVertex);
                marker.setDimension(dimension);
                marker.setDisplayOrder(100);
                markerOverlays.add(marker);
                if (i == 0 && vertices.size() == 2) {
                    addMarkerDots(markerId, vertices.get(0), vertices.get(1));
                }
            }
        }

        updatePerimeter();
    }

    private void updateBounds() {
        if (vertices.isEmpty()) {
            topLeft = new BlockPos(0, 70, 0);
            bottomRight = new BlockPos(0, 70, 0);
        } else {
            int minX = Integer.MAX_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxZ = Integer.MIN_VALUE;

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

            topLeft = new BlockPos(minX, 70, minZ);
            bottomRight = new BlockPos(maxX, 70, maxZ);
        }
    }

    private void updatePerimeter() {
        perimeter = 0.f;

        if (vertices.size() > 1) {
            BlockPos last = vertices.get(vertices.size() - 1);
            for (BlockPos vertex : vertices) {
                perimeter += Math.sqrt(vertex.distSqr(last));
                last = vertex;
            }
        }
    }

    //
    // Functions adapted from https://en.wikipedia.org/wiki/Bresenham%27s_line_algorithm
    //
    private void addMarkerDots(String markerId, BlockPos from, BlockPos to) {
        if (abs(to.getZ() - from.getZ()) < abs(to.getX() - from.getX())) {
            if (from.getX() > to.getX()) {
                addLineMarkerDots(markerId, to.getX(), to.getZ(), from.getX(), from.getZ());
            } else{
                addLineMarkerDots(markerId, from.getX(), from.getZ(), to.getX(), to.getZ());
            }
        } else {
            if (from.getZ() > to.getZ()) {
                addLineMarkerDots(markerId, to.getX(), to.getZ(), from.getX(), from.getZ());
            } else{
                addLineMarkerDots(markerId, from.getX(), from.getZ(), to.getX(), to.getZ());
            }
        }
    }

    private void addLineMarkerDots(String markerId, int x0, int z0, int x1, int z1) {
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

    private TextProperties setMinSizeTextPropierties(TextProperties textProperties) {
        int width = bottomRight.getX() - topLeft.getX();
        int name1Width = Minecraft.getInstance().font.width(name1) * 2;
        int name2Width = Minecraft.getInstance().font.width(name2) * 2;
        int nameWidth = Math.max(name1Width, name2Width) + 6;

        int zoom = 0;
        while (nameWidth > width && zoom < 5) {
            ++zoom;
            width *= 2;
        }

        return textProperties.setMinZoom(zoom);
    }

    @OnlyIn(Dist.CLIENT)
    public static class BannerDisplayData {
        public final List<BannerPattern> patternList;
        public final List<DyeColor> colorList;
        public String patternResourceLocation;

        public BannerDisplayData(FrontierData.BannerData bannerData) {
            patternList = new ArrayList<>();
            colorList = new ArrayList<>();
            patternList.add(BannerPattern.BASE);
            colorList.add(bannerData.baseColor);
            patternResourceLocation = "b" + bannerData.baseColor.getId();

            if (bannerData.patterns != null) {
                for (int i = 0; i < bannerData.patterns.size(); ++i) {
                    CompoundTag nbttagcompound = bannerData.patterns.getCompound(i);
                    BannerPattern bannerpattern = BannerPattern.byHash(nbttagcompound.getString("Pattern"));

                    if (bannerpattern != null) {
                        patternList.add(bannerpattern);
                        int j = nbttagcompound.getInt("Color");
                        colorList.add(DyeColor.byId(j));
                        patternResourceLocation += bannerpattern.getHashname() + j;
                    }
                }
            }
        }
    }
}
