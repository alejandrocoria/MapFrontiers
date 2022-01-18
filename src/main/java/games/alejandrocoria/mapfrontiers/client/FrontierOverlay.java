package games.alejandrocoria.mapfrontiers.client;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.gui.GuiColors;
import games.alejandrocoria.mapfrontiers.client.gui.GuiFrontierBook;
import games.alejandrocoria.mapfrontiers.common.ConfigData;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUserShared;
import journeymap.client.api.IClientAPI;
import journeymap.client.api.display.MarkerOverlay;
import journeymap.client.api.display.PolygonOverlay;
import journeymap.client.api.model.MapImage;
import journeymap.client.api.model.MapPolygon;
import journeymap.client.api.model.ShapeProperties;
import journeymap.client.api.model.TextProperties;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import com.mojang.math.Matrix4f;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import static java.lang.Math.abs;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class FrontierOverlay extends FrontierData {
    private static final MapImage markerVertex = new MapImage(new ResourceLocation(MapFrontiers.MODID + ":textures/gui/marker.png"), 0,
            0, 12, 12, GuiColors.WHITE, 1.f);
    private static final MapImage markerDot = new MapImage(new ResourceLocation(MapFrontiers.MODID + ":textures/gui/marker.png"), 12, 0,
            8, 8, GuiColors.WHITE, 1.f);
    private static final MapImage markerDotExtra = new MapImage(new ResourceLocation(MapFrontiers.MODID + ":textures/gui/marker.png"),
            12, 0, 8, 8, GuiColors.WHITE, 0.4f);

    static {
        markerVertex.setAnchorX(markerVertex.getDisplayWidth() / 2.0).setAnchorY(markerVertex.getDisplayHeight() / 2.0);
        markerVertex.setRotation(0);
        markerDot.setAnchorX(markerDot.getDisplayWidth() / 2.0).setAnchorY(markerDot.getDisplayHeight() / 2.0);
        markerDot.setRotation(0);
        markerDotExtra.setAnchorX(markerDotExtra.getDisplayWidth() / 2.0).setAnchorY(markerDotExtra.getDisplayHeight() / 2.0);
        markerDotExtra.setRotation(0);
    }

    public BlockPos topLeft;
    public BlockPos bottomRight;
    public float perimeter = 0.f;
    public float area = 0.f;
    private int vertexSelected;

    private final IClientAPI jmAPI;
    private final List<PolygonOverlay> polygonOverlays = new ArrayList<>();
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
            hash = prime * hash + (closed ? 1231 : 1237);
            hash = prime * hash + color;
            hash = prime * hash + ((dimension == null) ? 0 : dimension.hashCode());
            hash = prime * hash + mapSlice;
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
                markerDotExtra.setOpacity(0.4f);
            } else {
                markerVertex.setOpacity(0.f);
                markerDot.setOpacity(0.f);
                markerDotExtra.setOpacity(0.f);
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

    public void removeOverlay() {
        for (PolygonOverlay polygon : polygonOverlays) {
            jmAPI.remove(polygon);
        }

        for (MarkerOverlay marker : markerOverlays) {
            jmAPI.remove(marker);
        }
    }

    public boolean pointIsInside(BlockPos point) {
        if (closed && vertices.size() > 2) {
            for (PolygonOverlay polygon : polygonOverlays) {
                MapPolygon map = polygon.getOuterArea();
                List<BlockPos> points = map.getPoints();

                if (points.get(0) == points.get(1)) {
                    continue;
                }

                /////////////////////////////////////////////////////////////////
                // Copyright (c) Justas (https://stackoverflow.com/users/407108)
                // Licensed under cc by-sa 4.0
                // https://creativecommons.org/licenses/by-sa/4.0/
                //
                // Adapted from https://stackoverflow.com/a/34689268
                // by Alejandro Coria - 29/04/2020
                /////////////////////////////////////////////////////////////////

                int pos = 0;
                int neg = 0;

                boolean inside = true;

                for (int i = 0; i < points.size(); ++i) {
                    if (points.get(i).equals(point)) {
                        break;
                    }

                    int x1 = points.get(i).getX();
                    int z1 = points.get(i).getZ();

                    int i2 = (i + 1) % points.size();

                    int x2 = points.get(i2).getX();
                    int z2 = points.get(i2).getZ();

                    int x = point.getX();
                    int z = point.getZ();

                    int d = (x - x1) * (z2 - z1) - (z - z1) * (x2 - x1);

                    if (d > 0) {
                        pos++;
                    } else if (d < 0) {
                        neg++;
                    }

                    if (pos > 0 && neg > 0) {
                        inside = false;
                        break;
                    }
                }

                if (inside) {
                    return true;
                }

                /////////////////////////////////////////////////////////////////
            }
        }

        return false;
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

    @Override
    public void setClosed(boolean closed) {
        super.setClosed(closed);
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
    public void setMapSlice(int mapSlice) {
        super.setMapSlice(mapSlice);
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

        if (vertices.size() < 3) {
            super.setClosed(false);
        } else {
            dirty = true;
        }

        updateOverlay();
    }

    public void selectNextVertex() {
        ++vertexSelected;
        if (vertexSelected >= vertices.size()) {
            vertexSelected = -1;
        }
        ClientProxy.getFrontiersOverlayManager(personal).updateSelectedMarker(getDimension(), this);
    }

    public void selectPreviousVertex() {
        --vertexSelected;
        if (vertexSelected < -1) {
            vertexSelected = vertices.size() - 1;
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

    private void recalculateOverlays() {
        polygonOverlays.clear();
        markerOverlays.clear();

        updateBounds();

        area = 0;

        if (closed && vertices.size() > 2) {
            ShapeProperties shapeProps = new ShapeProperties().setStrokeWidth(0).setFillColor(color)
                    .setFillOpacity((float) ConfigData.polygonsOpacity);

            MapPolygon polygon = new MapPolygon(vertices);
            PolygonOverlay polygonOverlay = new PolygonOverlay(MapFrontiers.MODID, displayId, dimension, shapeProps, polygon);

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

            polygonOverlays.add(polygonOverlay);

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
                addMarkerDots(markerId, vertices.get(i), vertices.get((i + 1) % vertices.size()), i == vertices.size() - 1);
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

    private void addMarkerDots(String markerId, BlockPos from, BlockPos to, boolean extra) {
        BlockPos toFrom = to.subtract(from);
        Vec3 vector = new Vec3(toFrom.getX(), toFrom.getY(), toFrom.getZ());
        double lenght = vector.length();
        int count = (int) (lenght / 8.0);
        double distance = lenght / count;
        vector = vector.normalize().scale(distance);

        for (int i = 1; i < count; ++i) {
            BlockPos pos = from.offset(new BlockPos(vector.scale(i)));
            MarkerOverlay dot = new MarkerOverlay(MapFrontiers.MODID, markerId + "_" + (i - 1), pos,
                    extra ? markerDotExtra : markerDot);
            dot.setDimension(dimension);
            dot.setDisplayOrder(99);
            markerOverlays.add(dot);
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
