package games.alejandrocoria.mapfrontiers.client.util;

import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.client.gui.FontRenderer;

@ParametersAreNonnullByDefault
public class StringHelper {
    public static int getMaxWidth(FontRenderer font, String... strings) {
        int maxWidth = 0;

        for (String s : strings) {
            int width = font.width(s);
            if (width > maxWidth) {
                maxWidth = width;
            }
        }

        return maxWidth;
    }

    private StringHelper() {

    }
}
