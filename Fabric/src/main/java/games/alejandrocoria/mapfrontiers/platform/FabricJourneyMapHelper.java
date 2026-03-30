package games.alejandrocoria.mapfrontiers.platform;

import games.alejandrocoria.mapfrontiers.client.frontier.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.util.ReflectionHelper;
import games.alejandrocoria.mapfrontiers.platform.services.IJourneyMapHelper;
import journeymap.api.v2.client.display.Context;
import journeymap.api.v2.client.display.MarkerOverlay;
import journeymap.api.v2.client.display.PolygonOverlay;
import journeymap.client.data.WorldData;
import journeymap.client.io.FileHandler;
import journeymap.client.io.ThemeLoader;
import journeymap.client.model.map.MapState;
import journeymap.client.model.map.MapType;
import journeymap.client.properties.MiniMapProperties;
import journeymap.client.render.GuiRenderToTexture;
import journeymap.client.render.draw.DrawMarkerStep;
import journeymap.client.render.draw.DrawPolygonStep;
import journeymap.client.render.draw.DrawStep;
import journeymap.client.render.map.MapRenderer;
import journeymap.client.ui.UIManager;
import journeymap.client.ui.minimap.DisplayVars;
import journeymap.client.ui.minimap.MiniMap;
import journeymap.client.ui.minimap.Position;
import journeymap.client.ui.minimap.Shape;
import journeymap.client.ui.theme.Theme;
import journeymap.common.waypoint.WaypointStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FabricJourneyMapHelper implements IJourneyMapHelper {
    @Override
    public File getJMWorldDir(Minecraft client) {
        return FileHandler.getJMWorldDir(client);
    }

    @Override
    public void fullscreenMapCenterOn(int x, int z) {
        UIManager.INSTANCE.getOrOpenFullscreenMap().centerOn(x, z);
    }

    @Override
    public boolean isMinimapEnabled() {
        return UIManager.INSTANCE.isMiniMapEnabled();
    }

    @Override
    public void drawMinimapPreview(GuiGraphicsExtractor graphics) {
        UIManager.INSTANCE.getMiniMap().drawMap(graphics, true);
    }

    @Override
    public double getMinimapWidth() throws NoSuchFieldException, IllegalAccessException {
        return ReflectionHelper.getPrivateField(getDisplayVars(), "minimapWidth");
    }

    @Override
    public double getMinimapHeight() throws NoSuchFieldException, IllegalAccessException {
        return ReflectionHelper.getPrivateField(getDisplayVars(), "minimapHeight");
    }

    @Override
    public double getMinimapTranslateX() throws NoSuchFieldException, IllegalAccessException {
        return ReflectionHelper.getPrivateField(getDisplayVars(), "translateX");
    }

    @Override
    public double getMinimapTranslateY() throws NoSuchFieldException, IllegalAccessException {
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
        private final GuiRenderToTexture polygonSurface;
        private final List<DrawPolygonStep> polygonDrawSteps = new ArrayList<>();
        private final List<DrawStep> overlayDrawSteps = new ArrayList<>();

        public CustomPreviewRenderer() {
            mapRenderer = new MapRenderer(Context.UI.Fullscreen);
            polygonSurface = new GuiRenderToTexture("MapFrontiers JourneyMap Polygon Preview");
            mapRenderer.setZoom(512);
            mapRenderer.setViewPortBounds(null);
            MapState mapState = new MapState();
            mapState.setMapType(MapType.day(ResourceKey.create(Registries.DIMENSION, Identifier.withDefaultNamespace("overworld"))));
            mapRenderer.setContext(mapState);
            mapRenderer.center(mapState.getWorldDir(), mapState.getMapType(), 0, 0, 512);
        }

        @Override
        public void setFrontiers(List<FrontierOverlay> frontierOverlays) {
            polygonDrawSteps.clear();
            overlayDrawSteps.clear();

            for (FrontierOverlay frontierOverlay : frontierOverlays) {
                for (PolygonOverlay polygon : frontierOverlay.getPolygonOverlays()) {
                    polygonDrawSteps.add(new DrawPolygonStep(polygon));
                }
                for (MarkerOverlay banner : frontierOverlay.getBannerOverlays()) {
                    overlayDrawSteps.add(new DrawMarkerStep(banner));
                }
            }
        }

        @Override
        public void draw(GuiGraphicsExtractor graphics, MultiBufferSource.BufferSource buffers, int x, int y, int size, float scaleFactor) {
            if (polygonDrawSteps.isEmpty() && overlayDrawSteps.isEmpty()) {
                return;
            }

            int width = Minecraft.getInstance().getWindow().getScreenWidth();
            int height = Minecraft.getInstance().getWindow().getScreenHeight();
            double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
            int previewSize = Math.max(1, Math.round(size * scaleFactor / (float) guiScale));

            graphics.enableScissor(x, y, x + previewSize, y + previewSize);
            graphics.pose().pushMatrix();
            graphics.pose().translate((float) (-width / guiScale / 2 * scaleFactor) + x, (float) (-height / guiScale / 2 * scaleFactor) + y);
            graphics.pose().scale((float) (1 / guiScale) * scaleFactor, (float) (1 / guiScale) * scaleFactor);

            mapRenderer.setViewPortBounds(new Rectangle2D.Double(0, 0, width * scaleFactor, height * scaleFactor));

            try {
                polygonSurface.render(graphics, context -> {
                    var pose = context.pose();
                    pose.pushMatrix();
                    pose.translate((float) (-width / 2 * scaleFactor + x * guiScale), (float) (-height / 2 * scaleFactor + y * guiScale));
                    pose.scale(scaleFactor);
                    try {
                        for (DrawPolygonStep drawPolygonStep : polygonDrawSteps) {
                            drawPolygonStep.drawGeometry(graphics, pose, context.buffers(), 0, 0, mapRenderer, 1, 0);
                        }
                    } finally {
                        pose.popMatrix();
                    }
                });

                for (DrawPolygonStep drawPolygonStep : polygonDrawSteps) {
                    drawPolygonStep.drawTextLayer(graphics, 0, 0, mapRenderer, 1, 0);
                }

                for (DrawStep drawStep : overlayDrawSteps) {
                    drawStep.draw(graphics, 0, 0, mapRenderer, 1, 0);
                }
                buffers.endBatch();
            } finally {
                graphics.pose().popMatrix();
                graphics.disableScissor();
            }
        }
    }
}
