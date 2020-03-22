package games.alejandrocoria.mapfrontiers.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.gui.GuiFrontierBook;
import games.alejandrocoria.mapfrontiers.client.util.Earcut;
import games.alejandrocoria.mapfrontiers.common.ConfigData;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import journeymap.client.api.IClientAPI;
import journeymap.client.api.display.MarkerOverlay;
import journeymap.client.api.display.PolygonOverlay;
import journeymap.client.api.model.MapImage;
import journeymap.client.api.model.MapPolygon;
import journeymap.client.api.model.ShapeProperties;
import journeymap.client.api.model.TextProperties;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

@ParametersAreNonnullByDefault
public class FrontierOverlay extends FrontierData {
    private static MapImage markerVertex = new MapImage(new ResourceLocation(MapFrontiers.MODID + ":textures/gui/marker.png"), 0,
            0, 12, 12, 0xffffff, 1.f);
    private static MapImage markerDot = new MapImage(new ResourceLocation(MapFrontiers.MODID + ":textures/gui/marker.png"), 12, 0,
            8, 8, 0xffffff, 1.f);
    private static MapImage markerDotExtra = new MapImage(new ResourceLocation(MapFrontiers.MODID + ":textures/gui/marker.png"),
            12, 0, 8, 8, 0xffffff, 0.4f);
    private static MapImage markerDotSelected = new MapImage(
            new ResourceLocation(MapFrontiers.MODID + ":textures/gui/marker.png"), 20, 0, 10, 10, 0xffffff, 1.f);

    private static float targetDotSelectedOpacity = 0.3f;

    static {
        markerVertex.setAnchorX(markerVertex.getDisplayWidth() / 2.0).setAnchorY(markerVertex.getDisplayHeight() / 2.0);
        markerVertex.setRotation(0);
        markerDot.setAnchorX(markerDot.getDisplayWidth() / 2.0).setAnchorY(markerDot.getDisplayHeight() / 2.0);
        markerDot.setRotation(0);
        markerDotExtra.setAnchorX(markerDotExtra.getDisplayWidth() / 2.0).setAnchorY(markerDotExtra.getDisplayHeight() / 2.0);
        markerDotExtra.setRotation(0);
        markerDotSelected.setAnchorX(markerDotSelected.getDisplayWidth() / 2.0)
                .setAnchorY(markerDotSelected.getDisplayHeight() / 2.0);
        markerDotSelected.setRotation(0);
    }

    public BlockPos topLeft;
    public BlockPos bottomRight;
    public float perimeter = 0.f;
    public float area = 0.f;
    public int vertexSelected = -1;
    public boolean selected = false;

    private final IClientAPI jmAPI;
    private List<PolygonOverlay> polygonOverlays = new ArrayList<PolygonOverlay>();
    private List<MarkerOverlay> markerOverlays = new ArrayList<MarkerOverlay>();
    private final String displayId = "frontier_" + String.valueOf(id++);

    public FrontierOverlay(IClientAPI jmAPI) {
        this.jmAPI = jmAPI;
    }

    @SubscribeEvent
    static public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            EntityPlayerSP player = Minecraft.getMinecraft().player;
            if (player == null) {
                return;
            }

            ItemStack itemMainhand = player.getHeldItemMainhand();
            ItemStack itemOffhand = player.getHeldItemOffhand();

            boolean hasTheBook = false;

            if (itemMainhand != null && itemMainhand.getItem() == MapFrontiers.frontierBook) {
                if (itemMainhand.hasTagCompound()) {
                    NBTTagCompound nbt = itemMainhand.getTagCompound();
                    if (nbt.hasKey("Dimension") && nbt.getInteger("Dimension") == player.dimension) {
                        hasTheBook = true;
                    }
                }
            }

            if (!hasTheBook && itemOffhand != null && itemOffhand.getItem() == MapFrontiers.frontierBook) {
                if (itemOffhand.hasTagCompound()) {
                    NBTTagCompound nbt = itemOffhand.getTagCompound();
                    if (nbt.hasKey("Dimension") && nbt.getInteger("Dimension") == player.dimension) {
                        hasTheBook = true;
                    }
                }
            }

            if (hasTheBook) {
                float opacity = markerDotSelected.getOpacity();
                if (opacity < targetDotSelectedOpacity) {
                    opacity += event.renderTickTime * 0.5f;
                    if (opacity >= targetDotSelectedOpacity) {
                        opacity = targetDotSelectedOpacity;
                        targetDotSelectedOpacity = 0.f;
                    }
                } else {
                    opacity -= event.renderTickTime * 0.07f;
                    if (opacity <= targetDotSelectedOpacity) {
                        opacity = targetDotSelectedOpacity;
                        targetDotSelectedOpacity = 1.f;
                    }
                }
                markerDotSelected.setOpacity(opacity);
            } else {
                markerDotSelected.setOpacity(0.f);
                targetDotSelectedOpacity = 1.f;
            }

            if (ConfigData.alwaysShowUnfinishedFrontiers || hasTheBook
                    || Minecraft.getMinecraft().currentScreen instanceof GuiFrontierBook) {
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

    @Override
    public void addVertex(BlockPos pos, int index) {
        super.addVertex(pos, index);
        updateOverlay();
    }

    @Override
    public void addVertex(BlockPos pos) {
        super.addVertex(pos);
        updateOverlay();
    }

    @Override
    public void removeVertex(int index) {
        super.removeVertex(index);
        updateOverlay();
    }

    @Override
    public void setClosed(boolean closed) {
        this.closed = closed;
        updateOverlay();
    }

    @Override
    public void setName1(String name) {
        name1 = name;
        updateOverlay();
    }

    @Override
    public void setName2(String name) {
        name2 = name;
        updateOverlay();
    }

    @Override
    public void setNameVisible(boolean nameVisible) {
        this.nameVisible = nameVisible;
        updateOverlay();
    }

    @Override
    public void setColor(int color) {
        this.color = color;
        updateOverlay();
    }

    @Override
    public void setDimension(int dimension) {
        this.dimension = dimension;
        updateOverlay();
    }

    public void removeSelectedVertex() {
        if (vertexSelected < 0) {
            return;
        }

        super.removeVertex(vertexSelected);
        if (vertexSelected > 0) {
            --vertexSelected;
        } else {
            vertexSelected = vertices.size() - 1;
        }

        if (vertices.size() < 3) {
            closed = false;
        }

        updateOverlay();
    }

    public void selectNextVertex() {
        ++vertexSelected;
        if (vertexSelected >= vertices.size()) {
            vertexSelected = -1;
        }
        updateOverlay();
    }

    public void selectPreviousVertex() {
        --vertexSelected;
        if (vertexSelected < -1) {
            vertexSelected = vertices.size() - 1;
        }
        updateOverlay();
    }

    private void recalculateOverlays() {
        polygonOverlays.clear();
        markerOverlays.clear();

        updateBounds();

        area = 0;

        if (closed && vertices.size() > 2) {
            ShapeProperties shapeProps = new ShapeProperties().setStrokeWidth(0).setFillColor(color)
                    .setFillOpacity((float) ConfigData.polygonsOpacity);

            double[] vertexArray = new double[vertices.size() * 2];
            int p = 0;
            for (BlockPos pos : vertices) {
                vertexArray[p++] = pos.getX();
                vertexArray[p++] = pos.getZ();
            }
            List<Integer> triangles = Earcut.earcut(vertexArray, null, 2);

            for (int i = 0; i < triangles.size(); i += 3) {
                BlockPos p1 = vertices.get(triangles.get(i + 2));
                BlockPos p2 = vertices.get(triangles.get(i + 1));
                BlockPos p3 = vertices.get(triangles.get(i));
                MapPolygon polygon = new MapPolygon(Arrays.asList(p1, p2, p3));
                PolygonOverlay polygonOverlay = new PolygonOverlay(MapFrontiers.MODID, displayId + "_" + String.valueOf(i),
                        dimension, shapeProps, polygon);
                polygonOverlays.add(polygonOverlay);

                area += Math.abs(p1.getX() * (p2.getZ() - p3.getZ()) + p2.getX() * (p3.getZ() - p1.getZ())
                        + p3.getX() * (p1.getZ() - p2.getZ())) / 2.f;
            }

            ConfigData.NameVisibility nameVisibility = ConfigData.nameVisibility;
            if (nameVisibility == ConfigData.NameVisibility.Show
                    || (nameVisibility == ConfigData.NameVisibility.Manual && nameVisible)) {
                ShapeProperties shapePropsTransparent = new ShapeProperties().setStrokeWidth(0).setFillOpacity(0.f);
                BlockPos center = new BlockPos((topLeft.getX() + bottomRight.getX()) / 2, 70,
                        (topLeft.getZ() + bottomRight.getZ()) / 2);
                MapPolygon polygon = new MapPolygon(Arrays.asList(center, center, center));
                PolygonOverlay polygonOverlay = new PolygonOverlay(MapFrontiers.MODID, displayId + "_name", dimension,
                        shapePropsTransparent, polygon);
                TextProperties textProps = new TextProperties().setColor(color).setScale(2.f).setBackgroundOpacity(0.f);
                textProps = setMinSizeTextPropierties(textProps);
                if (!name1.isEmpty() && !name2.isEmpty()) {
                    textProps.setOffsetY(9);
                }
                polygonOverlay.setTextProperties(textProps).setOverlayGroupName("frontier").setLabel(name1 + "\n" + name2);
                polygonOverlays.add(polygonOverlay);
            }
        } else {
            for (int i = 0; i < vertices.size(); ++i) {
                String markerId = displayId + "_" + String.valueOf(i);
                MarkerOverlay marker = new MarkerOverlay(MapFrontiers.MODID, markerId, vertices.get(i), markerVertex);
                marker.setDimension(dimension);
                marker.setDisplayOrder(100);
                markerOverlays.add(marker);
                addMarkerDots(markerId, vertices.get(i), vertices.get((i + 1) % vertices.size()), i == vertices.size() - 1);
            }
        }

        if (selected && vertexSelected >= 0) {
            BlockPos pos = vertices.get(vertexSelected);
            MarkerOverlay dot = new MarkerOverlay(MapFrontiers.MODID, displayId + "_selected", pos, markerDotSelected);
            dot.setDimension(dimension);
            dot.setDisplayOrder(101);
            markerOverlays.add(dot);
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

            for (int i = 0; i < vertices.size(); ++i) {
                if (vertices.get(i).getX() < minX)
                    minX = vertices.get(i).getX();
                if (vertices.get(i).getZ() < minZ)
                    minZ = vertices.get(i).getZ();
                if (vertices.get(i).getX() > maxX)
                    maxX = vertices.get(i).getX();
                if (vertices.get(i).getZ() > maxZ)
                    maxZ = vertices.get(i).getZ();
            }

            topLeft = new BlockPos(minX, 70, minZ);
            bottomRight = new BlockPos(maxX, 70, maxZ);
        }
    }

    private void updatePerimeter() {
        perimeter = 0.f;

        if (vertices.size() > 1) {
            BlockPos last = vertices.get(vertices.size() - 1);
            for (int i = 0; i < vertices.size(); ++i) {
                perimeter += Math.sqrt(vertices.get(i).distanceSq(last));
                last = vertices.get(i);
            }
        }
    }

    private void addMarkerDots(String markerId, BlockPos from, BlockPos to, boolean extra) {
        Vec3d vector = new Vec3d(to.subtract(from));
        double lenght = vector.lengthVector();
        int count = (int) (lenght / 8.0);
        double distance = lenght / count;
        vector = vector.normalize().scale(distance);

        for (int i = 1; i < count; ++i) {
            BlockPos pos = from.add(new BlockPos(vector.scale(i)));
            MarkerOverlay dot = new MarkerOverlay(MapFrontiers.MODID, markerId + "_" + String.valueOf(i - 1), pos,
                    extra ? markerDotExtra : markerDot);
            dot.setDimension(dimension);
            dot.setDisplayOrder(99);
            markerOverlays.add(dot);
        }
    }

    private TextProperties setMinSizeTextPropierties(TextProperties textProperties) {
        int width = bottomRight.getX() - topLeft.getX();
        int name1Width = Minecraft.getMinecraft().fontRenderer.getStringWidth(name1) * 2;
        int name2Width = Minecraft.getMinecraft().fontRenderer.getStringWidth(name2) * 2;
        int nameWidth = Math.max(name1Width, name2Width) + 6;

        int zoom = 0;
        while (nameWidth > width && zoom < 5) {
            ++zoom;
            width *= 2;
        }

        return textProperties.setMinZoom(zoom);
    }
}