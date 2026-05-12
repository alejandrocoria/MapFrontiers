package games.alejandrocoria.mapfrontiers.client.gui.util;

import net.minecraft.client.gui.Font;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public final class TextEllipsizeHelper {
    public static final String ELLIPSIS = "...";

    public static String ellipsizeByWidth(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }

        int ellipsisWidth = font.width(ELLIPSIS);
        if (ellipsisWidth >= maxWidth) {
            return font.plainSubstrByWidth(ELLIPSIS, maxWidth);
        }

        return font.plainSubstrByWidth(text, maxWidth - ellipsisWidth) + ELLIPSIS;
    }

    private TextEllipsizeHelper() {
    }
}
