package games.alejandrocoria.mapfrontiers.platform.services;

import games.alejandrocoria.mapfrontiers.client.frontier.FrontierOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;

import java.io.File;
import java.util.List;

public interface IJourneyMapHelper {
    File getJMWorldDir(Minecraft client);
    void fullscreenMapCenterOn(int x, int z);
    boolean isMinimapEnabled();
    void drawMinimapPreview(GuiGraphics graphics);
    double getMinimapWidth() throws NoSuchFieldException, IllegalAccessException;
    double getMinimapHeight() throws NoSuchFieldException, IllegalAccessException;
    double getMinimapTranslateX() throws NoSuchFieldException, IllegalAccessException;
    double getMinimapTranslateY() throws NoSuchFieldException, IllegalAccessException;
    int getMinimapMargin();
    JMPosition getMinimapPosition();
    int minimapLabelBackgroundColor();
    int minimapLabelHighlightColor();
    int minimapLabelForegroundColor();
    boolean minimapPropertiesChanged();
    List<String> getDimensionList();
    ICustomPreviewRenderer createCustomPreviewRenderer();

    enum JMPosition {
        TopRight,
        BottomRight,
        BottomLeft,
        TopLeft,
        TopCenter,
        Center,
        Custom
    }

    interface ICustomPreviewRenderer {
        void setFrontiers(List<FrontierOverlay> frontierOverlays);
        void draw(GuiGraphics graphics, MultiBufferSource.BufferSource buffers, int x, int y, int size, float scaleFactor);
    }
}
