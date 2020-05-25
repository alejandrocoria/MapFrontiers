package games.alejandrocoria.mapfrontiers.client.gui;

import journeymap.client.ui.theme.Theme;
import net.minecraft.util.text.TextFormatting;

public final class GuiColors {
    public static final int WHITE = 0xffffffff;
    public static final int BLACK = 0xff000000;

    public static final int COLOR_INDICATOR_BORDER = 0xff000000;
    public static final int BOOKMARK_TEXT = 0xffffffff;
    public static final int BOOKTAG_TEXT = 0xffeeeeee;
    public static final int LABEL_TEXT_DEFAULT = 0xff000000;
    public static final int INDEX_TEXT = 0xff777777;
    public static final int INDEX_TEXT_HIGHLIGHT = 0xff000000;
    public static final int INDEX_SEPARATOR = 0x1a999999;

    public static final int HUD_FRAME_DEFAULT = 0xff000000;
    public static final int HUD_TEXT_NAME_DEFAULT = 0xffffffff;
    public static final int HUD_TEXT_OWNER_DEFAULT = 0xffdfdfdf;

    public static final int SETTINGS_BG = 0xc7101010;
    public static final int SETTINGS_TEXT = 0xffdddddd;
    public static final int SETTINGS_TEXT_HIGHLIGHT = 0xffffffff;
    public static final int SETTINGS_TEXT_ERROR = 0xffdd1111;
    public static final int SETTINGS_TEXT_ERROR_HIGHLIGHT = 0xffff4444;
    public static final int SETTINGS_TEXT_MEDIUM = 0xaaaaaa;
    public static final int SETTINGS_TEXT_DARK = 0xff777777;
    public static final int SETTINGS_BUTTON_BORDER = 0xff777777;
    public static final int SETTINGS_BUTTON_TEXT = 0xff777777;
    public static final int SETTINGS_BUTTON_TEXT_HIGHLIGHT = 0xffffffff;
    public static final int SETTINGS_TAB_BORDER = 0xff777777;
    public static final int SETTINGS_TAB_TEXT = 0xff777777;
    public static final int SETTINGS_TAB_TEXT_HIGHLIGHT = 0xffffffff;
    public static final int SETTINGS_TEXT_BOX_BORDER = 0xff444444;
    public static final int SETTINGS_TEXT_BOX_BG = 0xff000000;
    public static final int SETTINGS_TEXT_BOX_TEXT = 0xff000000;
    public static final int SETTINGS_TEXT_BOX_TEXT_HIGHLIGHT = 0xff222222;
    public static final int SETTINGS_TEXT_BOX_TEXT_DEFAULT = 0xffbbbbbb;
    public static final int SETTINGS_TEXT_BOX_EXTRA_BORDER = 0xffa0a0a0;
    public static final int SETTINGS_TEXT_BOX_EXTRA_BG = 0xff000000;
    public static final int SETTINGS_ELEMENT_HOVERED = 0xff222222;
    public static final int SETTINGS_CHECKBOX_BORDER = 0xff444444;
    public static final int SETTINGS_CHECKBOX_BG = 0xff000000;
    public static final int SETTINGS_CHECKBOX_CHECK = 0xff666666;
    public static final int SETTINGS_OPTION_BORDER = 0xff444444;
    public static final int SETTINGS_OPTION_BG = 0xff000000;
    public static final int SETTINGS_SCROLLBAR = 0xff777777;
    public static final int SETTINGS_SCROLLBAR_HOVERED = 0xffaaaaaa;
    public static final int SETTINGS_SCROLLBAR_GRABBED = 0xff666666;
    public static final int SETTINGS_SCROLLBAR_BG = 0x1affffff;
    public static final int SETTINGS_ANCHOR_LIGHT = 0xffdddddd;
    public static final int SETTINGS_ANCHOR_DARK = 0xff222222;
    public static final int SETTINGS_PING_BAR = 0xffffffff;

    public static final TextFormatting CHAT_PLAYER = TextFormatting.YELLOW;
    public static final TextFormatting WARNING = TextFormatting.YELLOW;
    public static final TextFormatting DEFAULT_CONFIG = TextFormatting.AQUA;

    public static int colorSpecToInt(Theme.ColorSpec colorSpec) {
        int color = colorSpec.getColor();
        color |= Math.round(colorSpec.alpha * 255) << 24;

        return color;
    }

    private GuiColors() {

    }
}
