package games.alejandrocoria.mapfrontiers.client.gui;

import net.minecraft.ChatFormatting;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public final class ColorConstants {
    // Base
    public static final int WHITE = 0xFFFFFFFF;
    public static final int BASE_TEXT_NORMAL = 0xFFC0C0C0;
    public static final int BASE_TEXT_HIGHLIGHT = 0xFFFFFFFF;
    public static final int BASE_TEXT_DISABLED = 0xFF444444;
    public static final int BASE_BORDER_NORMAL = 0xFF777777;
    public static final int BASE_BORDER_FOCUSED = 0xFFFFFFFF;
    public static final int BASE_BORDER_DISABLED = 0xFF444444;
    public static final int BASE_FRAME_BG = 0xC7101010;
    public static final int BASE_INPUT_BG = 0xFF000000;


    // Generic UI
    public static final int TEXT = BASE_TEXT_NORMAL;
    public static final int TEXT_HIGHLIGHT = BASE_TEXT_HIGHLIGHT;
    public static final int TEXT_MEDIUM = 0xFFAAAAAA;
    public static final int TEXT_DIMENSION = 0xFF999999;
    public static final int TEXT_PENDING = 0xFF00DD00;
    public static final int TEXT_ERROR_NORMAL = 0xFFDD1111;
    public static final int TEXT_ERROR_HIGHLIGHT = 0xFFFF4444;
    public static final int SCREEN_FRAME_BG = BASE_FRAME_BG;
    public static final int SCREEN_POPUP_OVERLAY_BG = 0xBF000000;
    public static final int POSITION_SEPARATOR_TEXT = BASE_BORDER_NORMAL;
    public static final int STRING_WIDGET_TEXT_DEFAULT = BASE_TEXT_HIGHLIGHT;

    public static final ChatFormatting WARNING = ChatFormatting.YELLOW;


    // Texture tint
    public static final int TEXTURE_TINT_NONE = WHITE;


    // Buttons
    public static final int SIMPLE_BUTTON_BORDER_NORMAL = BASE_BORDER_NORMAL;
    public static final int SIMPLE_BUTTON_BORDER_FOCUSED = BASE_BORDER_FOCUSED;
    public static final int SIMPLE_BUTTON_BORDER_DISABLED = BASE_BORDER_DISABLED;
    public static final int SIMPLE_BUTTON_BG = BASE_FRAME_BG;
    public static final int SIMPLE_BUTTON_TEXT_NORMAL = BASE_TEXT_NORMAL;
    public static final int SIMPLE_BUTTON_TEXT_HIGHLIGHT = BASE_TEXT_HIGHLIGHT;
    public static final int SIMPLE_BUTTON_TEXT_DISABLED = BASE_TEXT_DISABLED;
    public static final int SIMPLE_BUTTON_TEXT_CONFIRM_NORMAL = 0xFF8BEB8B;
    public static final int SIMPLE_BUTTON_TEXT_CONFIRM_HIGHLIGHT = 0xFF24FF24;
    public static final int SIMPLE_BUTTON_TEXT_DELETE_NORMAL = 0xFFFF7777;
    public static final int SIMPLE_BUTTON_TEXT_DELETE_HIGHLIGHT = 0xFFFF0000;

    public static final int ICON_COLOR_NORMAL = BASE_TEXT_HIGHLIGHT;
    public static final int ICON_COLOR_DISABLED = 0xFF7F7F7F;

    public static final int LINK_NORMAL = WHITE;
    public static final int LINK_HIGHLIGHT = 0xFFFFFF00;


    // Controls
    public static final int CHECKBOX_BG = BASE_INPUT_BG;
    public static final int CHECKBOX_BORDER_NORMAL = BASE_BORDER_NORMAL;
    public static final int CHECKBOX_BORDER_FOCUSED = BASE_BORDER_FOCUSED;
    public static final int CHECKBOX_BORDER_DISABLED = BASE_BORDER_DISABLED;
    public static final int CHECKBOX_CHECK_NORMAL = BASE_TEXT_NORMAL;
    public static final int CHECKBOX_CHECK_DISABLED = BASE_TEXT_DISABLED;

    public static final int OPTION_BG = BASE_INPUT_BG;
    public static final int OPTION_BORDER_NORMAL = BASE_BORDER_NORMAL;
    public static final int OPTION_BORDER_FOCUSED = BASE_BORDER_FOCUSED;
    public static final int OPTION_BORDER_DISABLED = BASE_BORDER_DISABLED;
    public static final int OPTION_TEXT_NORMAL = BASE_TEXT_NORMAL;
    public static final int OPTION_TEXT_HIGHLIGHT = BASE_TEXT_HIGHLIGHT;
    public static final int OPTION_TEXT_DISABLED = BASE_TEXT_DISABLED;

    public static final int TAB_BORDER_NORMAL = BASE_BORDER_NORMAL;
    public static final int TAB_BORDER_FOCUSED = BASE_BORDER_FOCUSED;
    public static final int TAB_TEXT_NORMAL = BASE_TEXT_NORMAL;
    public static final int TAB_TEXT_HIGHLIGHT = BASE_TEXT_HIGHLIGHT;
    public static final int TAB_TEXT_DISABLED = BASE_TEXT_DISABLED;

    public static final int SLIDER_BORDER_NORMAL = BASE_BORDER_NORMAL;
    public static final int SLIDER_BORDER_FOCUSED = BASE_BORDER_FOCUSED;
    public static final int SLIDER_BORDER_DISABLED = BASE_BORDER_DISABLED;
    public static final int SLIDER_HANDLER_NORMAL = 0xFFAAAAAA;
    public static final int SLIDER_HANDLER_FOCUSED = BASE_TEXT_HIGHLIGHT;
    public static final int SLIDER_HANDLER_DISABLED = BASE_TEXT_DISABLED;
    public static final int SLIDER_TEXT_NORMAL = BASE_TEXT_NORMAL;
    public static final int SLIDER_TEXT_HIGHLIGHT = BASE_TEXT_HIGHLIGHT;
    public static final int SLIDER_TEXT_DISABLED = BASE_TEXT_DISABLED;


    // Scroll and lists
    public static final int SCROLLBAR_NORMAL = BASE_BORDER_NORMAL;
    public static final int SCROLLBAR_HOVERED = 0xFFAAAAAA;
    public static final int SCROLLBAR_GRABBED = 0xFF666666;
    public static final int SCROLLBAR_BG = 0x1AFFFFFF;
    public static final int SCROLLBOX_FOCUS_OUTLINE = BASE_BORDER_FOCUSED;
    public static final int SCROLL_ELEMENT_HOVERED = 0xA0303030;
    public static final int SCROLL_ELEMENT_SELECTED = 0xFF303030;
    public static final int TERRITORY_LIST_RAIL_HOVER_BG = 0xA0202020;

    public static final int SECTION_HEADER_TEXT = 0xFF666666;
    public static final int RADIO_LIST_TEXT = BASE_TEXT_NORMAL;
    public static final int SORT_TOOLBAR_TEXT_NORMAL = BASE_TEXT_NORMAL;
    public static final int SORT_TOOLBAR_TEXT_HIGHLIGHT = BASE_TEXT_HIGHLIGHT;

    public static final int GROUP_ACTION_TEXT = BASE_TEXT_HIGHLIGHT;
    public static final int USER_SHARED_TEXT = BASE_TEXT_HIGHLIGHT;

    public static final int FRONTIER_LIST_ROW_OUTLINE = BASE_BORDER_FOCUSED;
    public static final int COLLECTION_LIST_ROW_OUTLINE = BASE_BORDER_FOCUSED;

    public static final int MARKED_BADGE_OUTLINE = BASE_BORDER_FOCUSED;
    public static final int MARKED_BADGE_TEXT = BASE_TEXT_HIGHLIGHT;

    public static final int SHAPE_BADGE_OUTLINE_NORMAL = BASE_BORDER_NORMAL;
    public static final int SHAPE_BADGE_OUTLINE_SELECTED = BASE_BORDER_FOCUSED;


    // Text boxes
    public static final int TEXTBOX_TEXT = 0xFFE0E0E0;
    public static final int TEXTBOX_POPUP_BG = BASE_INPUT_BG;
    public static final int TEXTBOX_POPUP_BORDER = 0xFFA0A0A0;
    public static final int TEXTBOX_POPUP_TEXT = BASE_TEXT_HIGHLIGHT;


    // Selectors and previews
    public static final int COLOR_PALETTE_BORDER = 0xFF404040;
    public static final int COLOR_PALETTE_FOCUS_OUTLINE = BASE_BORDER_FOCUSED;
    public static final int COLOR_PICKER_VALUE_DISABLED_TINT = 0xFF8D8D8D;

    public static final int PATH_MARKER_SELECTOR_BG = 0xFF444444;
    public static final int PATH_MARKER_SELECTOR_BORDER_NORMAL = BASE_BORDER_NORMAL;
    public static final int PATH_MARKER_SELECTOR_BORDER_HOVERED = BASE_BORDER_NORMAL;
    public static final int PATH_MARKER_SELECTOR_BORDER_SELECTED = BASE_BORDER_FOCUSED;
    public static final int PATH_MARKER_SELECTOR_FOCUS_OUTLINE = BASE_BORDER_FOCUSED;
    public static final int PATH_MARKER_UNSELECTED = 0xFFCCCCCC;
    public static final int PATH_MARKER_SELECTED = WHITE;

    public static final int SHAPE_PRESET_SELECTOR_LABEL = BASE_TEXT_HIGHLIGHT;
    public static final int SHAPE_PRESET_SELECTOR_SELECTION_BORDER = BASE_BORDER_FOCUSED;
    public static final int SHAPE_PRESET_SELECTOR_FOCUS_OUTLINE = BASE_BORDER_FOCUSED;

    public static final int PREVIEW_PANEL_BORDER = BASE_BORDER_NORMAL;


    // Screen-specific
    public static final int SCREEN_TITLE_TEXT = BASE_TEXT_HIGHLIGHT;
    public static final int DIALOG_TITLE_TEXT = BASE_TEXT_HIGHLIGHT;
    public static final int FRONTIER_INFO_TEXT = BASE_TEXT_HIGHLIGHT;
    public static final int COLLECTION_INFO_TEXT = BASE_TEXT_HIGHLIGHT;
    public static final int NEW_FRONTIER_DETAILS_TEXT = BASE_TEXT_HIGHLIGHT;


    // Domain-specific
    public static final int HUD_ANCHOR_DARK = 0xFF222222;
    public static final int HUD_ANCHOR_LIGHT = 0xFFDDDDDD;

    public static final int LABEL_R = 0xFFE84949;
    public static final int LABEL_G = 0xFF52F152;
    public static final int LABEL_B = 0xFF4343E2;

    public static final int PING_BAR = WHITE;
    public static final int VIRTUAL_COLLECTION_COLOR = 0xFF666666;


    private ColorConstants() {
    }
}
