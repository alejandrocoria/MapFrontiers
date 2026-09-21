package games.alejandrocoria.mapfrontiers.client.util;

import com.mojang.blaze3d.platform.InputConstants;
import games.alejandrocoria.mapfrontiers.common.config.ConfigEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public final class ScreenHelper {
    public static float getScaleFactorThatFit(Minecraft minecraft, Screen screen, int minWidth, int minHeight) {
        int windowScale = (int) minecraft.getWindow().getGuiScale();

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

    public static boolean hasControlDown() {
        return InputConstants.isKeyDown(InputConstants.KEY_LCONTROL) || InputConstants.isKeyDown(InputConstants.KEY_RCONTROL);
    }

    public static boolean hasShiftDown() {
        return InputConstants.isKeyDown(InputConstants.KEY_LSHIFT) || InputConstants.isKeyDown(InputConstants.KEY_RSHIFT);
    }

    public static int getPaddedMaxTextWidth(Font font, int minWidth, int padding, Component... labels) {
        int width = minWidth;
        for (Component label : labels) {
            width = Math.max(width, font.width(label) + padding);
        }

        return width;
    }

    @Nullable
    public static Tooltip tooltip(ConfigEntry<?, ?> entry) {
        Component component = entry.tooltipComponent();
        return component == null ? null : Tooltip.create(component);
    }

    private ScreenHelper() {
    }
}
