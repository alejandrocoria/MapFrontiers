package games.alejandrocoria.mapfrontiers.platform;

import games.alejandrocoria.mapfrontiers.common.util.ReflectionHelper;
import games.alejandrocoria.mapfrontiers.platform.services.IJourneyMapHelper;
import journeymap.client.data.WorldData;
import journeymap.client.io.FileHandler;
import journeymap.client.io.ThemeLoader;
import journeymap.client.properties.MiniMapProperties;
import journeymap.client.ui.UIManager;
import journeymap.client.ui.minimap.DisplayVars;
import journeymap.client.ui.minimap.MiniMap;
import journeymap.client.ui.minimap.Position;
import journeymap.client.ui.minimap.Shape;
import journeymap.client.ui.theme.Theme;
import journeymap.common.waypoint.WaypointStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

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
        if (UIManager.INSTANCE.getMiniMap().getCurrentMinimapProperties().shape.get() == Shape.Circle) {
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
    public int getMinimapFontScale() {
        return UIManager.INSTANCE.getMiniMap().getCurrentMinimapProperties().fontScale.get().intValue();
    }

    @Override
    public int minimapLabelBackgroundColor() {
        if (UIManager.INSTANCE.getMiniMap().getCurrentMinimapProperties().shape.get() == Shape.Circle) {
            return colorSpecToInt(ThemeLoader.getCurrentTheme().minimap.circle.labelTop.background);
        } else {
            return colorSpecToInt(ThemeLoader.getCurrentTheme().minimap.square.labelTop.background);
        }
    }

    @Override
    public int minimapLabelHighlightColor() {
        if (UIManager.INSTANCE.getMiniMap().getCurrentMinimapProperties().shape.get() == Shape.Circle) {
            return colorSpecToInt(ThemeLoader.getCurrentTheme().minimap.circle.labelTop.highlight);
        } else {
            return colorSpecToInt(ThemeLoader.getCurrentTheme().minimap.square.labelTop.highlight);
        }
    }

    @Override
    public int minimapLabelForegroundColor() {
        if (UIManager.INSTANCE.getMiniMap().getCurrentMinimapProperties().shape.get() == Shape.Circle) {
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
                || minimapInfo1.equals(minimapProperties.info1Label.get()) || minimapInfo2.equals(minimapProperties.info2Label.get())
                || minimapInfo3.equals(minimapProperties.info3Label.get()) || minimapInfo4.equals(minimapProperties.info4Label.get())
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
}
