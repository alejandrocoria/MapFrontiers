package games.alejandrocoria.mapfrontiers.platform.services;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.util.List;

public interface IJourneyMapHelper {
    File getJMWorldDir(Minecraft client);
    void fullscreenMapCenterOn(int x, int z);
    boolean isMinimapEnabled();
    void drawMinimapPreview(PoseStack matrixStack);
    int getMinimapWidth() throws NoSuchFieldException, IllegalAccessException;
    int getMinimapHeight() throws NoSuchFieldException, IllegalAccessException;
    int getMinimapTranslateX() throws NoSuchFieldException, IllegalAccessException;
    int getMinimapTranslateY() throws NoSuchFieldException, IllegalAccessException;
    int getMinimapMargin();
    JMPosition getMinimapPosition();
    int getMinimapFontScale();
    int minimapLabelBackgroundColor();
    int minimapLabelHighlightColor();
    int minimapLabelForegroundColor();
    boolean minimapPropertiesChanged();
    List<String> getDimensionList();

    enum JMPosition {
        TopRight,
        BottomRight,
        BottomLeft,
        TopLeft,
        TopCenter,
        Center,
        Custom;
    }
}
