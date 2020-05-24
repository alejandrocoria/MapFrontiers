package games.alejandrocoria.mapfrontiers.client.util;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.client.gui.FontRenderer;

@ParametersAreNonnullByDefault
public class StringHelper {
    public static int getMaxWidth(FontRenderer fontRenderer, List<String> strings) {
        return getMaxWidth(fontRenderer, strings.toArray(new String[0]));
    }

    public static int getMaxWidth(FontRenderer fontRenderer, String... strings) {
        int maxWidth = 0;

        for (String s : strings) {
            int width = fontRenderer.getStringWidth(s);
            if (width > maxWidth) {
                maxWidth = width;
            }
        }

        return maxWidth;
    }

    private StringHelper() {

    }
}
