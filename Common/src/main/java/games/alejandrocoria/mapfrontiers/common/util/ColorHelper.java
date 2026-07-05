package games.alejandrocoria.mapfrontiers.common.util;

import net.minecraft.util.Mth;

import javax.annotation.ParametersAreNonnullByDefault;
import java.awt.Color;
import java.util.Random;

@ParametersAreNonnullByDefault
public final class ColorHelper {
    private static final Random random = new Random();

    public static int getRandomColor() {
        final float hue = random.nextFloat();
        final float saturation = 1.f - (float) Math.pow(random.nextFloat(), 4.0);
        final float luminance = 1.f - (float) Math.pow(random.nextFloat(), 6.0) * 0.5f;
        return Color.getHSBColor(hue, saturation, luminance).getRGB();
    }

    public static int ensureMinBrightness(int color, float minBrightness) {
        minBrightness = Mth.clamp(minBrightness, 0, 1);
        float[] hsv = Color.RGBtoHSB((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, null);
        float brightness = Math.max(hsv[2], minBrightness);
        int newColor = Color.getHSBColor(hsv[0], hsv[1], brightness).getRGB();
        return (newColor & 0x00FFFFFF) | (color & 0xFF000000);
    }

    private ColorHelper() {
    }
}
