package games.alejandrocoria.mapfrontiers.platform.services;

import games.alejandrocoria.mapfrontiers.client.territory.collection.CollectionOverlay;
import games.alejandrocoria.mapfrontiers.client.territory.frontier.FrontierOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;

import java.io.File;
import java.util.List;

public interface IJourneyMapHelper {
    File getJMWorldDir(Minecraft client);
    void fullscreenMapCenterOn(int x, int z);
    boolean isMinimapEnabled();
    void drawMinimapPreview(GuiGraphics graphics);
    int getMinimapWidth() throws NoSuchFieldException, IllegalAccessException;
    int getMinimapHeight() throws NoSuchFieldException, IllegalAccessException;
    int getMinimapTranslateX() throws NoSuchFieldException, IllegalAccessException;
    int getMinimapTranslateY() throws NoSuchFieldException, IllegalAccessException;
    int getMinimapMargin();
    JMPosition getMinimapPosition();
    int minimapLabelBackgroundColor();
    int minimapLabelHighlightColor();
    int minimapLabelForegroundColor();
    boolean minimapPropertiesChanged();
    List<String> getDimensionList();
    void prepareMapTexture(ResourceLocation texture);
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
        /**
         * Receives territories whose overlays were already recalculated by the preview widget.
         */
        void setTerritories(List<FrontierOverlay> frontierOverlays, List<CollectionOverlay> collectionOverlays);

        /**
         * Draws a JourneyMap-based preview. The size is expressed in map pixels; scaleFactor compensates GUI scaling.
         */
        void draw(GuiGraphics graphics, MultiBufferSource.BufferSource buffers, int x, int y, int size, float scaleFactor);
    }
}
