package games.alejandrocoria.mapfrontiers.client.util;

import net.minecraft.client.gui.screen.Screen;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class ScreenHelper {
    public static float getScaleFactorThatFit(Screen screen, int minWidth, int minHeight) {
        int windowScale = (int) screen.getMinecraft().getWindow().getGuiScale();

        if (windowScale == 1 || (minWidth <= screen.width && minHeight <= screen.height)) {
            return 1.f;
        }

        int baseWidth = screen.width * windowScale;
        int baseHeight = screen.height * windowScale;

        int maxScale = windowScale;
        while (maxScale > 1 && (minWidth > baseWidth / maxScale || minHeight > baseHeight / maxScale)) {
            --maxScale;
        }

        return (float) windowScale / maxScale;
    }

    private ScreenHelper() {

    }
}
