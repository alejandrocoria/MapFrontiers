package games.alejandrocoria.mapfrontiers.client.gui.component.scroll;

import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.frontier.CollectionScope;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.CheckBoxRenderHelper;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.IconButton;
import games.alejandrocoria.mapfrontiers.common.frontier.CollectionData;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CollectionListElement extends FrontierListRowElement {
    private static final Identifier NAME_FADE_TEXTURE = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/frontier_list/name_fade.png");
    private static final int CONTENT_X = 14;
    private static final int TITLE_Y = 6;
    private static final int COUNTERS_GAP = 4;
    private static final int COUNTERS_RIGHT_GAP = 4;
    private static final int TITLE_HOVER_X = 14;
    private static final int TITLE_BG_TOP_OFFSET = -2;
    private static final int TITLE_BG_BOTTOM_OFFSET = 8;
    private static final int TITLE_BG_FADE_WIDTH = 6;
    private static final int RIGHT_PADDING = 4;
    private static final int ACTION_GAP = 2;
    private static final int ACTION_Y = 3;
    private static final int CHECKBOX_Y = 4;
    private static final String ELLIPSIS = "...";

    private final Font font;
    private final @Nullable CollectionData collection;
    private final boolean virtualRow;
    private final CollectionScope scope;
    private final String title;
    private final String counters;
    private final int color;
    private final boolean checkboxVisible;
    private final boolean checkboxVisibleOnHover;
    private final int markedCount;
    private final int eligibleCount;
    private final @Nullable IconButton actionButton;
    private final boolean actionEnabled;
    private final boolean actionVisibleWhenDisabled;
    private final List<UUID> eligibleFrontierIds;
    private final IconButton collapseToggleButton;
    private boolean collapseToggleRequested;
    private boolean markToggleRequested;
    private boolean actionRequested;

    public CollectionListElement(String rowId,
                                 Font font,
                                 @Nullable CollectionData collection,
                                 boolean virtualRow,
                                 CollectionScope scope,
                                 String title,
                                 String counters,
                                 int color,
                                 boolean collapsed,
                                 boolean checkboxVisible,
                                 boolean checkboxVisibleOnHover,
                                 int markedCount,
                                 int eligibleCount,
                                 @Nullable IconButton.Type actionType,
                                 boolean actionEnabled,
                                 @Nullable Tooltip actionTooltip,
                                 List<UUID> eligibleFrontierIds,
                                 int width) {
        super(rowId, width, 17);
        this.font = font;
        this.collection = collection;
        this.virtualRow = virtualRow;
        this.scope = scope;
        this.title = title;
        this.counters = counters;
        this.color = color;
        this.checkboxVisible = checkboxVisible;
        this.checkboxVisibleOnHover = checkboxVisibleOnHover;
        this.markedCount = markedCount;
        this.eligibleCount = eligibleCount;
        this.actionEnabled = actionEnabled;
        this.actionVisibleWhenDisabled = actionType == IconButton.Type.MoveHere;
        this.eligibleFrontierIds = List.copyOf(eligibleFrontierIds);

        collapseToggleButton = new IconButton(collapsed ? IconButton.Type.Collapsed : IconButton.Type.Expanded, (button) -> {});
        actionButton = actionType == null ? null : new IconButton(actionType, (button) -> {});
        if (actionButton != null) {
            actionButton.setTooltip(actionTooltip);
            actionButton.active = actionEnabled;
        }
    }

    @Override
    protected void setX(int x) {
        super.setX(x);
        collapseToggleButton.setX(this.x + 4);
        if (actionButton != null) {
            actionButton.setX(getActionLeft());
        }
    }

    @Override
    protected void setY(int y) {
        super.setY(y);
        collapseToggleButton.setY(this.y + 5);
        if (actionButton != null) {
            actionButton.setY(this.y + ACTION_Y);
        }
    }

    public @Nullable CollectionData getCollection() {
        return collection;
    }

    public boolean isVirtualRow() {
        return virtualRow;
    }

    public boolean isPersonal() {
        return scope != CollectionScope.GLOBAL_PERSISTENT;
    }

    public CollectionScope getScope() {
        return scope;
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
        if (selected) {
            graphics.fill(x, y, x + width, y + height, ColorConstants.SCROLL_ELEMENT_SELECTED);
        } else if (isHovered) {
            graphics.fill(x, y, x + width, y + height, ColorConstants.SCROLL_ELEMENT_HOVERED);
        }

        graphics.fill(x, y, x + width, y + 2, color);
        graphics.fill(x, y + 2, x + 2, y + height, color);
        graphics.fill(x + width - 2, y + 2, x + width, y + height, color);

        collapseToggleButton.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        renderTexts(graphics, mouseX, mouseY, selected);
        renderMarkedCount(graphics);
        renderActionButton(graphics, mouseX, mouseY, partialTicks);
        renderCheckBox(graphics, mouseX, mouseY);
    }

    @Override
    protected void drawFocusOutline(GuiGraphicsExtractor graphics) {
        int left = x + 2;
        int right = x + width - 3;
        int top = y + 2;
        int bottom = y + height - 1;
        graphics.horizontalLine(left, right, top, ColorConstants.WHITE);
        graphics.horizontalLine(left, right, bottom, ColorConstants.WHITE);
        graphics.verticalLine(left, top, bottom, ColorConstants.WHITE);
        graphics.verticalLine(right, top, bottom, ColorConstants.WHITE);
    }

    private void renderTexts(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean selected) {
        int titleColor = ColorConstants.TEXT;
        if (selected) {
            titleColor = ColorConstants.TEXT_HIGHLIGHT;
        } else if (virtualRow) {
            titleColor = ColorConstants.VIRTUAL_COLLECTION;
        }

        int rightZoneStart = getRightZoneStart();
        int countersX = rightZoneStart - font.width(counters);
        int titleX = x + CONTENT_X;
        int titleMaxWidth = countersX - titleX - COUNTERS_GAP;
        String visibleTitle = ellipsize(title, titleMaxWidth);

        boolean titleTruncated = !visibleTitle.equals(title);
        boolean showExpandedTitle = titleTruncated && isTitleAreaHovered(mouseX, mouseY, titleX, visibleTitle);

        graphics.text(font, counters, countersX, y + TITLE_Y, ColorConstants.TEXT_DIMENSION);
        drawTitle(graphics, titleX, titleColor, visibleTitle, showExpandedTitle);
    }

    private void renderMarkedCount(GuiGraphicsExtractor graphics) {
        if (markedCount <= 0) {
            return;
        }

        String markedText = "[" + markedCount + "]";
        int countX = getActionLeft() - ACTION_GAP - font.width(markedText);
        graphics.text(font, markedText, countX, y + TITLE_Y, ColorConstants.TEXT_HIGHLIGHT);
    }

    private void renderActionButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        if (actionButton == null) {
            return;
        }

        if (!actionEnabled && !actionVisibleWhenDisabled) {
            return;
        }

        actionButton.extractRenderState(graphics, mouseX, mouseY, partialTicks);
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
        return isHovered && actionButton != null && actionEnabled && actionButton.isMouseOver(mouseX, mouseY);
    }

    private boolean isCheckBoxHovered(int mouseX, int mouseY) {
        return CheckBoxRenderHelper.contains(getCheckBoxX(), y + CHECKBOX_Y, mouseX, mouseY);
    }

    private int getCheckBoxX() {
        return x + width - RIGHT_PADDING - CheckBoxRenderHelper.SIZE;
    }

    private int getActionLeft() {
        if (actionButton == null) {
            return getCheckBoxX();
        }
        return getCheckBoxX() - ACTION_GAP - actionButton.getWidth();
    }

    private int getRightZoneStart() {
        int countWidth = markedCount <= 0 ? 0 : font.width("[" + markedCount + "]");
        return getActionLeft() - ACTION_GAP - countWidth - COUNTERS_RIGHT_GAP;
    }

    private String ellipsize(String text, int maxWidth) {
        if (maxWidth <= 0) {
            return "";
        }
        if (font.width(text) <= maxWidth) {
            return text;
        }

        int ellipsisWidth = font.width(ELLIPSIS);
        if (ellipsisWidth >= maxWidth) {
            return font.plainSubstrByWidth(ELLIPSIS, maxWidth);
        }

        return font.plainSubstrByWidth(text, maxWidth - ellipsisWidth) + ELLIPSIS;
    }

    private void drawTitle(GuiGraphicsExtractor graphics, int titleX, int titleColor, String visibleTitle, boolean showExpandedTitle) {
        String renderedTitle = showExpandedTitle ? title : visibleTitle;
        if (showExpandedTitle) {
            int textWidth = font.width(title);
            int bgLeft = titleX - 1;
            int bgOpaqueRight = bgLeft + textWidth + 2;
            graphics.fill(bgLeft, y + TITLE_Y + TITLE_BG_TOP_OFFSET, bgOpaqueRight,
                    y + TITLE_Y + TITLE_BG_BOTTOM_OFFSET, ColorConstants.SCROLL_ELEMENT_SELECTED);
            graphics.blit(RenderPipelines.GUI_TEXTURED, NAME_FADE_TEXTURE, bgOpaqueRight, y + TITLE_Y + TITLE_BG_TOP_OFFSET, 0, 0,
                    TITLE_BG_FADE_WIDTH, TITLE_BG_BOTTOM_OFFSET - TITLE_BG_TOP_OFFSET, TITLE_BG_FADE_WIDTH,
                    TITLE_BG_BOTTOM_OFFSET - TITLE_BG_TOP_OFFSET, ColorConstants.SCROLL_ELEMENT_SELECTED);
        }

        graphics.text(font, renderedTitle, titleX, y + TITLE_Y, titleColor);
    }

    private boolean isTitleAreaHovered(int mouseX, int mouseY, int titleX, String visibleTitle) {
        int titleWidth = font.width(visibleTitle);
        int hoverLeft = Math.max(x + TITLE_HOVER_X, titleX);
        int hoverRight = titleX + titleWidth;
        return mouseX >= hoverLeft && mouseY >= y && mouseX < hoverRight && mouseY < y + height;
    }

    @Override
    protected ScrollBox.ScrollElement.Action mousePressed(MouseButtonEvent event, boolean doubleClick) {
        if (!visible || !isHovered) {
            return ScrollBox.ScrollElement.Action.None;
        }

        if (collapseToggleButton.isMouseOver(event.x(), event.y())) {
            collapseToggleRequested = true;
            return ScrollBox.ScrollElement.Action.Handled;
        }

        if (shouldRenderCheckBox() && CheckBoxRenderHelper.contains(getCheckBoxX(), y + CHECKBOX_Y, event.x(), event.y())) {
            markToggleRequested = true;
            return ScrollBox.ScrollElement.Action.Handled;
        }

        if (isActionHovered((int) event.x(), (int) event.y())) {
            actionRequested = true;
            return ScrollBox.ScrollElement.Action.Handled;
        }

        return ScrollBox.ScrollElement.Action.Clicked;
    }
}
