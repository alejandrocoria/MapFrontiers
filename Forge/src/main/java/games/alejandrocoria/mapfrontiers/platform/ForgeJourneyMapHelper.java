package games.alejandrocoria.mapfrontiers.platform;

import games.alejandrocoria.mapfrontiers.client.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.common.util.ReflectionHelper;
import games.alejandrocoria.mapfrontiers.platform.services.IJourneyMapHelper;
import journeymap.api.v2.client.display.Context;
import journeymap.api.v2.client.display.MarkerOverlay;
import journeymap.api.v2.client.display.PolygonOverlay;
import journeymap.client.data.WorldData;
import journeymap.client.io.FileHandler;
import journeymap.client.io.ThemeLoader;
import journeymap.client.model.MapState;
import journeymap.client.model.MapType;
import journeymap.client.properties.MiniMapProperties;
import journeymap.client.render.JMRenderTypes;
import journeymap.client.render.draw.DrawMarkerStep;
import journeymap.client.render.draw.DrawPolygonStep;
import journeymap.client.render.draw.DrawStep;
import journeymap.client.render.draw.DrawUtil;
import journeymap.client.render.map.MapRenderer;
import journeymap.client.ui.UIManager;
import journeymap.client.ui.minimap.DisplayVars;
import journeymap.client.ui.minimap.MiniMap;
import journeymap.client.ui.minimap.Position;
import journeymap.client.ui.minimap.Shape;
import journeymap.client.ui.theme.Theme;
import journeymap.common.waypoint.WaypointStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ForgeJourneyMapHelper implements IJourneyMapHelper {
    @Override
    public File getJMWorldDir(Minecraft client) {
        return FileHandler.getJMWorldDir(client);
    }

    @Override
    public void fullscreenMapCenterOn(int x, int z) {
        UIManager.INSTANCE.openFullscreenMap().centerOn(x, z);
    }

    @Override
    public boolean isMinimapEnabled() {
        return UIManager.INSTANCE.isMiniMapEnabled();
    }

    @Override
    public void drawMinimapPreview(GuiGraphics graphics) {
        UIManager.INSTANCE.getMiniMap().drawMap(graphics, true);
    }

    @Override
    public int getMinimapWidth() throws NoSuchFieldException, IllegalAccessException {
        return ReflectionHelper.getPrivateField(getDisplayVars(), "minimapWidth");
    }

    @Override
    public int getMinimapHeight() throws NoSuchFieldException, IllegalAccessException {
        return ReflectionHelper.getPrivateField(getDisplayVars(), "minimapHeight");
    }

    @Override
    public int getMinimapTranslateX() throws NoSuchFieldException, IllegalAccessException {
        return ReflectionHelper.getPrivateField(getDisplayVars(), "translateX");
    }

    @Override
    public int getMinimapTranslateY() throws NoSuchFieldException, IllegalAccessException {
        return ReflectionHelper.getPrivateField(getDisplayVars(), "translateY");
    }

    @Override
    public int getMinimapMargin() {
        Theme.Minimap.MinimapSpec minimapSpec;
        if (UIManager.INSTANCE.getMiniMap().getCurrentMinimapProperties().shape.get() == Shape.Circle && ThemeLoader.getCurrentTheme().minimap.circle != null) {
            minimapSpec = ThemeLoader.getCurrentTheme().minimap.circle;
        } else {
            minimapSpec = ThemeLoader.getCurrentTheme().minimap.square;
        }
        return minimapSpec.margin;
    }

    @Override
    public JMPosition getMinimapPosition() {
        return switch (UIManager.INSTANCE.getMiniMap().getCurrentMinimapProperties().position.get()) {
            case TopRight -> JMPosition.TopRight;
            case BottomRight -> JMPosition.BottomRight;
            case BottomLeft -> JMPosition.BottomLeft;
            case TopLeft -> JMPosition.TopLeft;
            case TopCenter -> JMPosition.TopCenter;
            case Center -> JMPosition.Center;
            default -> JMPosition.Custom;
        };
    }

    @Override
    public int minimapLabelBackgroundColor() {
        if (UIManager.INSTANCE.getMiniMap().getCurrentMinimapProperties().shape.get() == Shape.Circle && ThemeLoader.getCurrentTheme().minimap.circle != null) {
            return colorSpecToInt(ThemeLoader.getCurrentTheme().minimap.circle.labelTop.background);
        } else {
            return colorSpecToInt(ThemeLoader.getCurrentTheme().minimap.square.labelTop.background);
        }
    }

    @Override
    public int minimapLabelHighlightColor() {
        if (UIManager.INSTANCE.getMiniMap().getCurrentMinimapProperties().shape.get() == Shape.Circle && ThemeLoader.getCurrentTheme().minimap.circle != null) {
            return colorSpecToInt(ThemeLoader.getCurrentTheme().minimap.circle.labelTop.highlight);
        } else {
            return colorSpecToInt(ThemeLoader.getCurrentTheme().minimap.square.labelTop.highlight);
        }
    }

    @Override
    public int minimapLabelForegroundColor() {
        if (UIManager.INSTANCE.getMiniMap().getCurrentMinimapProperties().shape.get() == Shape.Circle && ThemeLoader.getCurrentTheme().minimap.circle != null) {
            return colorSpecToInt(ThemeLoader.getCurrentTheme().minimap.circle.labelTop.foreground);
        } else {
            return colorSpecToInt(ThemeLoader.getCurrentTheme().minimap.square.labelTop.foreground);
        }
    }

    @Override
    public boolean minimapPropertiesChanged() {
        if (!minimapPropertiesInitialized) {
            setAllMinimapProperties();
            return true;
        }

        MiniMapProperties minimapProperties = UIManager.INSTANCE.getMiniMap().getCurrentMinimapProperties();
        if (minimapEnabled != minimapProperties.enabled.get() || minimapSize != minimapProperties.sizePercent.get()
                || minimapShape != minimapProperties.shape.get() || minimapPosition != minimapProperties.position.get()
                || !minimapInfo1.equals(minimapProperties.info1Label.get()) || !minimapInfo2.equals(minimapProperties.info2Label.get())
                || !minimapInfo3.equals(minimapProperties.info3Label.get()) || !minimapInfo4.equals(minimapProperties.info4Label.get())
                || minimapFontScale != minimapProperties.fontScale.get().intValue()
                || minimapCompassFontScale != minimapProperties.compassFontScale.get().intValue()) {
            setAllMinimapProperties();
            return true;
        }

        return false;
    }

    @Override
    public List<String> getDimensionList() {
        List<String> list = new ArrayList<>();
        List<WorldData.DimensionProvider> dimensionProviders = WorldData.getDimensionProviders(WaypointStore.getInstance().getLoadedDimensions());
        for (WorldData.DimensionProvider dimension : dimensionProviders) {
            list.add(dimension.getDimensionId());
        }

        return list;
    }

    @Override
    public ICustomPreviewRenderer createCustomPreviewRenderer() {
        return new CustomPreviewRenderer();
    }

    private static int colorSpecToInt(Theme.ColorSpec colorSpec) {
        int color = colorSpec.getColor();
        color |= Math.round(colorSpec.alpha * 255) << 24;

        return color;
    }

    private static DisplayVars getDisplayVars() throws NoSuchFieldException, IllegalAccessException {
        MiniMap minimap = UIManager.INSTANCE.getMiniMap();
        return ReflectionHelper.getPrivateField(minimap, "dv");
    }

    private static void setAllMinimapProperties() {
        MiniMapProperties minimapProperties = UIManager.INSTANCE.getMiniMap().getCurrentMinimapProperties();
        minimapEnabled = minimapProperties.enabled.get();
        minimapSize = minimapProperties.sizePercent.get();
        minimapShape = minimapProperties.shape.get();
        minimapPosition = minimapProperties.position.get();
        minimapInfo1 = minimapProperties.info1Label.get();
        minimapInfo2 = minimapProperties.info2Label.get();
        minimapInfo3 = minimapProperties.info3Label.get();
        minimapInfo4 = minimapProperties.info4Label.get();
        minimapFontScale = minimapProperties.fontScale.get().intValue();
        minimapCompassFontScale = minimapProperties.compassFontScale.get().intValue();
        minimapPropertiesInitialized = true;
    }

    private static boolean minimapPropertiesInitialized = false;
    private static boolean minimapEnabled;
    private static int minimapSize;
    private static Shape minimapShape;
    private static Position minimapPosition;
    private static String minimapInfo1;
    private static String minimapInfo2;
    private static String minimapInfo3;
    private static String minimapInfo4;
    private static int minimapFontScale;
    private static int minimapCompassFontScale;


    private static class CustomPreviewRenderer implements ICustomPreviewRenderer {
        private final MapRenderer mapRenderer;
        private final List<DrawStep> drawSteps = new ArrayList<>();

        public CustomPreviewRenderer() {
            mapRenderer = new MapRenderer(Context.UI.Fullscreen);
            mapRenderer.setZoom(512);
            mapRenderer.setViewPortBounds(null);
            MapState mapState = new MapState();
            mapState.setMapType(MapType.day(ResourceKey.create(Registries.DIMENSION, ResourceLocation.withDefaultNamespace("overworld"))));
            mapRenderer.setContext(mapState);
            mapRenderer.center(mapState.getWorldDir(), mapState.getMapType(), 0, 0, 512);
        }

        @Override
        public void setFrontiers(List<FrontierOverlay> frontierOverlays) {
            drawSteps.clear();

            for (FrontierOverlay frontierOverlay : frontierOverlays) {
                for (PolygonOverlay polygon : frontierOverlay.getPolygonOverlays()) {
                    drawSteps.add(new DrawPolygonStep(polygon));
                }
                for (MarkerOverlay banner : frontierOverlay.getBannerOverlays()) {
                    drawSteps.add(new DrawMarkerStep(banner));
                }
            }
        }

        @Override
        public void draw(GuiGraphics graphics, int x, int y, int size, float scaleFactor) {
            if (drawSteps.isEmpty()) {
                return;
            }

            int width = Minecraft.getInstance().getWindow().getScreenWidth();
            int height = Minecraft.getInstance().getWindow().getScreenHeight();
            double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
            DrawUtil.sizeDisplay(width, height);

            graphics.pose().pushPose();
            graphics.pose().translate(-width * scaleFactor / 2 + x * guiScale, -height * scaleFactor / 2 + y * guiScale, 0);
            graphics.pose().scale(scaleFactor, scaleFactor, scaleFactor);

            mapRenderer.setViewPortBounds(new Rectangle2D.Double(0, 0, width * scaleFactor, height * scaleFactor));
            graphics.fill(JMRenderTypes.MINIMAP_RECTANGLE_MASK_RENDER_TYPE, width / 2 + 1, height / 2 + 1, width / 2 + size - 1, height / 2 + size - 1, 0, 0xFFFFFFFF);

            for(DrawStep.Pass pass : DrawStep.Pass.values()) {
                int zLevel = 0;

                for (DrawStep drawStep : drawSteps) {
                    ++zLevel;
                    graphics.pose().pushPose();
                    graphics.pose().translate(0, 0, zLevel);
                    drawStep.draw(graphics, graphics.bufferSource(), pass, 0, 0, mapRenderer, 1, 0);
                    graphics.pose().popPose();
                }
            }

            graphics.pose().popPose();

            DrawUtil.sizeDisplay(width / guiScale, height / guiScale);
        }
    }
}
