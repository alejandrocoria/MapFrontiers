package games.alejandrocoria.mapfrontiers.client.gui.component.scroll;

import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.CheckBoxRenderHelper;
import games.alejandrocoria.mapfrontiers.common.frontier.CollectionData;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CollectionListElement extends FrontierListRowElement {
    private static final int CHEVRON_CLICK_WIDTH = 18;
    private static final int CONTENT_X = 14;
    private static final int TITLE_Y = 4;
    private static final int COUNTERS_GAP = 6;
    private static final int RIGHT_PADDING = 2;
    private static final int ACTION_GAP = 2;
    private static final int ACTION_HEIGHT = 11;
    private static final int ACTION_TEXT_Y = 2;
    private static final int CHECKBOX_Y = 2;

    private final Font font;
    private final @Nullable CollectionData collection;
    private final boolean virtualRow;
    private final boolean personal;
    private final String title;
    private final String counters;
    private final boolean collapsed;
    private final boolean checkboxVisible;
    private final boolean checkboxVisibleOnHover;
    private final int markedCount;
    private final int eligibleCount;
    private final @Nullable String actionLabel;
    private final boolean actionEnabled;
    private final int actionWidth;
    private final List<UUID> eligibleFrontierIds;
    private boolean collapseToggleRequested;
    private boolean markToggleRequested;
    private boolean actionRequested;

    public CollectionListElement(String rowId,
                                 Font font,
                                 @Nullable CollectionData collection,
                                 boolean virtualRow,
                                 boolean personal,
                                 String title,
                                 String counters,
                                 boolean collapsed,
                                 boolean checkboxVisible,
                                 boolean checkboxVisibleOnHover,
                                 int markedCount,
                                 int eligibleCount,
                                 @Nullable String actionLabel,
                                 boolean actionEnabled,
                                 int actionWidth,
                                 List<UUID> eligibleFrontierIds,
                                 int width) {
        super(rowId, width, 15);
        this.font = font;
        this.collection = collection;
        this.virtualRow = virtualRow;
        this.personal = personal;
        this.title = title;
        this.counters = counters;
        this.collapsed = collapsed;
        this.checkboxVisible = checkboxVisible;
        this.checkboxVisibleOnHover = checkboxVisibleOnHover;
        this.markedCount = markedCount;
        this.eligibleCount = eligibleCount;
        this.actionLabel = actionLabel;
        this.actionEnabled = actionEnabled;
        this.actionWidth = actionWidth;
        this.eligibleFrontierIds = List.copyOf(eligibleFrontierIds);
    }

    public @Nullable CollectionData getCollection() {
        return collection;
    }

    public boolean isVirtualRow() {
        return virtualRow;
    }

    public boolean isPersonal() {
        return personal;
    }

    public List<UUID> getEligibleFrontierIds() {
        return eligibleFrontierIds;
    }

    public boolean consumeCollapseToggleRequested() {
        boolean requested = collapseToggleRequested;
        collapseToggleRequested = false;
        return requested;
    }

    public boolean consumeMarkToggleRequested() {
        boolean requested = markToggleRequested;
        markToggleRequested = false;
        return requested;
    }

    public boolean consumeActionRequested() {
        boolean requested = actionRequested;
        actionRequested = false;
        return requested;
    }

    public boolean isActionEnabled() {
        return actionEnabled;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks,
                                            boolean selected, boolean focused) {
        int textColor = selected ? ColorConstants.TEXT_HIGHLIGHT : ColorConstants.TEXT;
        if (selected) {
            graphics.fill(x, y, x + width, y + height, ColorConstants.SCROLL_ELEMENT_SELECTED);
        } else if (isHovered) {
            graphics.fill(x, y, x + width, y + height, ColorConstants.SCROLL_ELEMENT_HOVERED);
        }

        graphics.text(font, collapsed ? ">" : "v", x + 2, y + TITLE_Y, textColor);
        int titleColor = virtualRow ? ColorConstants.TEXT_HIGHLIGHT : textColor;
        graphics.text(font, title, x + CONTENT_X, y + TITLE_Y, titleColor);

        int countersX = x + CONTENT_X + font.width(title) + COUNTERS_GAP;
        int rightZoneStart = getRightZoneStart();
        graphics.text(font, ellipsize(counters, rightZoneStart - countersX), countersX, y + TITLE_Y, ColorConstants.TEXT_DIMENSION);

        renderMarkedCount(graphics);
        renderActionButton(graphics, mouseX, mouseY);
        renderCheckBox(graphics, mouseX, mouseY);
    }

    private void renderMarkedCount(GuiGraphicsExtractor graphics) {
        if (markedCount <= 0) {
            return;
        }

        String markedText = "[" + markedCount + "]";
        int countX = getActionLeft() - ACTION_GAP - font.width(markedText);
        graphics.text(font, markedText, countX, y + TITLE_Y, ColorConstants.TEXT_HIGHLIGHT);
    }

    private void renderActionButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (actionLabel == null) {
            return;
        }

        int left = getActionLeft();
        int top = y + 2;
        boolean hovered = isActionHovered(mouseX, mouseY);
        int borderColor = actionEnabled ? (hovered ? ColorConstants.SIMPLE_BUTTON_BORDER_FOCUSED : ColorConstants.SIMPLE_BUTTON_BORDER)
                : ColorConstants.SIMPLE_BUTTON_BORDER_DISABLED;
        int textColor = actionEnabled ? (hovered ? ColorConstants.SIMPLE_BUTTON_TEXT_HIGHLIGHT : ColorConstants.SIMPLE_BUTTON_TEXT)
                : ColorConstants.SIMPLE_BUTTON_TEXT_INACTIVE;

        graphics.fill(left, top, left + actionWidth, top + ACTION_HEIGHT, borderColor);
        graphics.fill(left + 1, top + 1, left + actionWidth - 1, top + ACTION_HEIGHT - 1, ColorConstants.SIMPLE_BUTTON_BG);
        int textX = left + (actionWidth - font.width(actionLabel)) / 2;
        graphics.text(font, actionLabel, textX, top + ACTION_TEXT_Y, textColor);
    }

    private void renderCheckBox(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!shouldRenderCheckBox()) {
            return;
        }

        CheckBoxRenderHelper.render(graphics, getCheckBoxX(), y + CHECKBOX_Y, isCheckBoxHovered(mouseX, mouseY), getCheckBoxState());
    }

    private boolean shouldRenderCheckBox() {
        return checkboxVisible || checkboxVisibleOnHover && isHovered;
    }

    private CheckBoxRenderHelper.State getCheckBoxState() {
        if (markedCount <= 0) {
            return CheckBoxRenderHelper.State.UNCHECKED;
        }
        if (eligibleCount > 0 && markedCount >= eligibleCount) {
            return CheckBoxRenderHelper.State.CHECKED;
        }
        return CheckBoxRenderHelper.State.PARTIAL;
    }

    private boolean isActionHovered(int mouseX, int mouseY) {
        int left = getActionLeft();
        return isHovered && actionLabel != null && mouseX >= left && mouseY >= y + 2
                && mouseX < left + actionWidth && mouseY < y + 2 + ACTION_HEIGHT;
    }

    private boolean isCheckBoxHovered(int mouseX, int mouseY) {
        return CheckBoxRenderHelper.contains(getCheckBoxX(), y + CHECKBOX_Y, mouseX, mouseY);
    }

    private int getCheckBoxX() {
        return x + width - RIGHT_PADDING - CheckBoxRenderHelper.SIZE;
    }

    private int getActionLeft() {
        return getCheckBoxX() - ACTION_GAP - actionWidth;
    }

    private int getRightZoneStart() {
        int countWidth = markedCount <= 0 ? 0 : font.width("[" + markedCount + "]");
        return getActionLeft() - ACTION_GAP - countWidth;
    }

    private String ellipsize(String text, int maxWidth) {
        if (maxWidth <= 0) {
            return "";
        }
        if (font.width(text) <= maxWidth) {
            return text;
        }

        int ellipsisWidth = font.width("...");
        if (ellipsisWidth >= maxWidth) {
            return font.plainSubstrByWidth("...", maxWidth);
        }

        return font.plainSubstrByWidth(text, maxWidth - ellipsisWidth) + "...";
    }

    @Override
    protected ScrollBox.ScrollElement.Action mousePressed(MouseButtonEvent event, boolean doubleClick) {
        if (!visible || !isHovered) {
            return ScrollBox.ScrollElement.Action.None;
        }

        if (shouldRenderCheckBox() && CheckBoxRenderHelper.contains(getCheckBoxX(), y + CHECKBOX_Y, event.x(), event.y())) {
            markToggleRequested = true;
            return ScrollBox.ScrollElement.Action.Handled;
        }

        if (actionLabel != null && event.x() >= getActionLeft() && event.x() < getActionLeft() + actionWidth
                && event.y() >= y + 2 && event.y() < y + 2 + ACTION_HEIGHT) {
            actionRequested = true;
            return ScrollBox.ScrollElement.Action.Handled;
        }

        if (event.x() >= x && event.x() < x + CHEVRON_CLICK_WIDTH) {
            collapseToggleRequested = true;
            return ScrollBox.ScrollElement.Action.Handled;
        }

        return ScrollBox.ScrollElement.Action.Clicked;
    }
}
