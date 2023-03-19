package games.alejandrocoria.mapfrontiers.common.util;

import javax.annotation.ParametersAreNonnullByDefault;
import java.awt.Color;
import java.util.Random;

@ParametersAreNonnullByDefault
public class ColorHelper {
    private static final Random rand = new Random();

    public static int getRandomColor() {

        final float hue = rand.nextFloat();
        final float saturation = 1.f - (float) Math.pow(rand.nextFloat(), 6.0);
        final float luminance = 1.f - (float) Math.pow(rand.nextFloat(), 6.0);
        return Color.getHSBColor(hue, saturation, luminance).getRGB();
    }

    private ColorHelper() {

    }
}
