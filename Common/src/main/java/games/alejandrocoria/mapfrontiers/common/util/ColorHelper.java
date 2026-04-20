package games.alejandrocoria.mapfrontiers.common.util;

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

    private ColorHelper() {
    }
}
