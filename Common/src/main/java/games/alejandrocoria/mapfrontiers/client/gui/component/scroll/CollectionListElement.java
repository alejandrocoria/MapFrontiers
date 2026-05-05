package games.alejandrocoria.mapfrontiers.client.gui.component.scroll;

import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.frontier.CollectionScope;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.CheckBoxButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.IconButton;
import games.alejandrocoria.mapfrontiers.common.frontier.CollectionData;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CollectionListElement extends FrontierListRowElement {
    public enum ActionState {
        NONE,
        CREATE_FRONTIER,
        MOVE_HERE_ENABLED,
        MOVE_HERE_DISABLED
    }

    private static final Identifier NAME_FADE_TEXTURE = Identifier.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/frontier_list/name_fade.png");
    private static final int CONTENT_X = 14;
    private static final int TITLE_Y = 6;
    private static final int COUNTERS_GAP = 4;
    private static final int COUNTERS_RIGHT_GAP = 1;
    private static final int TITLE_HOVER_X = 14;
    private static final int TITLE_BG_TOP_OFFSET = -2;
    private static final int TITLE_BG_BOTTOM_OFFSET = 8;
    private static final int TITLE_BG_FADE_WIDTH = 6;
    private static final int RIGHT_PADDING = 4;
    private static final int ACTION_GAP = 2;
    private static final int ACTION_BUTTON_WIDTH = 11;
    private static final int SELECTION_RAIL_GAP = 2;
    private static final int RAIL_CONTENT_Y = 4;
    private static final String ELLIPSIS = "...";
    private static final int RAIL_HOVER_COLOR = 0xA0202020;
    private static final int RAIL_SELECTED_COLOR = 0xFF202020;
    private static final Tooltip MOVE_HERE_TOOLTIP = Tooltip.create(Component.translatable("mapfrontiers.tooltip.move_here"));
    private static final Tooltip DELETE_TOOLTIP = Tooltip.create(Component.translatable("mapfrontiers.delete"));

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
    private final @Nullable IconButton createButton;
    private final @Nullable IconButton moveHereButton;
    private final @Nullable IconButton deleteButton;
    private final CheckBoxButton checkBoxButton;
    private final List<UUID> eligibleFrontierIds;
    private final IconButton collapseToggleButton;
    private boolean collapseToggleRequested;
    private boolean markToggleRequested;
    private boolean createRequested;
    private boolean moveHereRequested;
    private boolean deleteRequested;

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
                                 ActionState actionState,
                                 boolean deleteEnabled,
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
        this.eligibleFrontierIds = List.copyOf(eligibleFrontierIds);

        collapseToggleButton = new IconButton(collapsed ? IconButton.Type.Collapsed : IconButton.Type.Expanded, (button) -> {});
        createButton = actionState == ActionState.CREATE_FRONTIER ? new IconButton(IconButton.Type.Add, (button) -> {}) : null;
        if (createButton != null) {
            createButton.active = true;
            createButton.setTooltip(getCreateTooltip(scope, virtualRow));
        }
        moveHereButton = actionState == ActionState.MOVE_HERE_ENABLED || actionState == ActionState.MOVE_HERE_DISABLED
                ? new IconButton(IconButton.Type.MoveHere, (button) -> {})
                : null;
        if (moveHereButton != null) {
            moveHereButton.active = actionState == ActionState.MOVE_HERE_ENABLED;
            moveHereButton.setTooltip(MOVE_HERE_TOOLTIP);
        }
        deleteButton = deleteEnabled ? new IconButton(IconButton.Type.Remove, (button) -> {}) : null;
        if (deleteButton != null) {
            deleteButton.active = true;
            deleteButton.setTooltip(DELETE_TOOLTIP);
        }
        checkBoxButton = new CheckBoxButton(false, button -> {});
        if (markedCount <= 0) {
            checkBoxButton.setChecked(false);
        } else if (eligibleCount > 0 && markedCount >= eligibleCount) {
            checkBoxButton.setChecked(true);
        } else {
            checkBoxButton.setPartial();
        }
    }

    @Override
    protected void setX(int x) {
        super.setX(x);
        collapseToggleButton.setX(this.x + 4);
        if (createButton != null) {
            createButton.setX(getCreateLeft());
        }
        if (moveHereButton != null) {
            moveHereButton.setX(getMoveHereLeft());
        }
        if (deleteButton != null) {
            deleteButton.setX(getDeleteLeft());
        }
        checkBoxButton.setX(getCheckBoxX());
    }

    @Override
    protected void setY(int y) {
        super.setY(y);
        collapseToggleButton.setY(this.y + 5);
        if (createButton != null) {
            createButton.setY(this.y + RAIL_CONTENT_Y);
        }
        if (moveHereButton != null) {
            moveHereButton.setY(this.y + RAIL_CONTENT_Y);
        }
        if (deleteButton != null) {
            deleteButton.setY(this.y + RAIL_CONTENT_Y);
        }
        checkBoxButton.setY(this.y + RAIL_CONTENT_Y);
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

    public boolean consumeCreateRequested() {
        boolean requested = createRequested;
        createRequested = false;
        return requested;
    }

    public boolean consumeMoveHereRequested() {
        boolean requested = moveHereRequested;
        moveHereRequested = false;
        return requested;
    }

    public boolean consumeDeleteRequested() {
        boolean requested = deleteRequested;
        deleteRequested = false;
        return requested;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks,
                                            boolean selected, boolean focused) {
        int selectionRightBound = getSelectionRightBound();
        if (isHovered) {
            graphics.fill(x, y, selectionRightBound, y + height, ColorConstants.SCROLL_ELEMENT_HOVERED);
            graphics.fill(selectionRightBound, y, x + width, y + height, RAIL_HOVER_COLOR);
        }

        graphics.fill(x, y, x + width, y + 2, color);
        graphics.fill(x, y + 2, x + 2, y + height, color);
        graphics.fill(x + width - 2, y + 2, x + width, y + height, color);

        collapseToggleButton.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        renderTexts(graphics, mouseX, mouseY);
        renderMarkedCount(graphics);
        renderActionButtons(graphics, mouseX, mouseY, partialTicks);
        renderCheckBox(graphics, mouseX, mouseY, partialTicks);
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

    private void renderTexts(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int titleColor = ColorConstants.TEXT;
        if (virtualRow) {
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
        int countX = getRailTextRight() - font.width(markedText);
        graphics.text(font, markedText, countX, y + TITLE_Y, ColorConstants.TEXT_HIGHLIGHT);
    }

    private void renderActionButtons(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        if (createButton != null) {
            createButton.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        }
        if (moveHereButton != null) {
            moveHereButton.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        }
        if (deleteButton != null && isHovered) {
            deleteButton.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        }
    }

    private void renderCheckBox(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        if (!shouldRenderCheckBox()) {
            return;
        }

        checkBoxButton.visible = true;
        checkBoxButton.active = true;
        checkBoxButton.extractRenderState(graphics, mouseX, mouseY, partialTicks);
    }

    private boolean shouldRenderCheckBox() {
        return checkboxVisible || checkboxVisibleOnHover && isHovered;
    }

    private boolean isCreateHovered(int mouseX, int mouseY) {
        return isHovered && createButton != null && createButton.isMouseOver(mouseX, mouseY);
    }

    private boolean isMoveHereHovered(int mouseX, int mouseY) {
        return isHovered && moveHereButton != null && moveHereButton.isMouseOver(mouseX, mouseY);
    }

    private boolean isDeleteHovered(int mouseX, int mouseY) {
        return isHovered && deleteButton != null && deleteButton.isMouseOver(mouseX, mouseY);
    }

    private boolean isCheckBoxHovered(int mouseX, int mouseY) {
        return shouldRenderCheckBox() && checkBoxButton.isMouseOver(mouseX, mouseY);
    }

    private int getCheckBoxX() {
        return x + width - RIGHT_PADDING - CheckBoxButton.SIZE;
    }

    private int getSelectionRightBound() {
        return Math.max(x, getRailLeft() - SELECTION_RAIL_GAP);
    }

    private int getRailLeft() {
        return getPrimarySlotLeft();
    }

    private int getDeleteLeft() {
        return getDeleteSlotLeft();
    }

    private int getMoveHereLeft() {
        return getDeleteSlotLeft();
    }

    private int getCreateLeft() {
        return getPrimarySlotLeft();
    }

    private int getDeleteSlotLeft() {
        return getCheckBoxX() - ACTION_GAP - ACTION_BUTTON_WIDTH;
    }

    private int getPrimarySlotLeft() {
        return getDeleteSlotLeft() - ACTION_GAP - ACTION_BUTTON_WIDTH;
    }

    private int getRightZoneStart() {
        if (markedCount <= 0) {
            return getRailTextRight();
        }

        int markedTextWidth = font.width("[" + markedCount + "]");
        return getRailTextRight() - markedTextWidth - ACTION_GAP;
    }

    private int getRailTextRight() {
        return getSelectionRightBound() - COUNTERS_RIGHT_GAP;
    }

    private static Tooltip getCreateTooltip(CollectionScope scope, boolean virtualRow) {
        String key;
        if (virtualRow) {
            key = switch (scope) {
                case PERSONAL_PERSISTENT -> "mapfrontiers.tooltip.create_frontier_personal";
                case PERSONAL_SESSION -> "mapfrontiers.tooltip.create_frontier_temporary";
                case GLOBAL_PERSISTENT -> "mapfrontiers.tooltip.create_frontier_global";
            };
        } else {
            key = switch (scope) {
                case PERSONAL_PERSISTENT -> "mapfrontiers.tooltip.create_frontier_personal_in_collection";
                case PERSONAL_SESSION -> "mapfrontiers.tooltip.create_frontier_temporary_in_collection";
                case GLOBAL_PERSISTENT -> "mapfrontiers.tooltip.create_frontier_global_in_collection";
            };
        }

        return Tooltip.create(Component.translatable(key));
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

        if (isCheckBoxHovered((int) event.x(), (int) event.y())) {
            markToggleRequested = true;
            return ScrollBox.ScrollElement.Action.Handled;
        }

        if (isDeleteHovered((int) event.x(), (int) event.y())) {
            deleteRequested = true;
            return ScrollBox.ScrollElement.Action.Handled;
        }

        if (isMoveHereHovered((int) event.x(), (int) event.y())) {
            moveHereRequested = true;
            return ScrollBox.ScrollElement.Action.Handled;
        }

        if (isCreateHovered((int) event.x(), (int) event.y())) {
            createRequested = true;
            return ScrollBox.ScrollElement.Action.Handled;
        }

        if (event.x() >= getSelectionRightBound()) {
            return ScrollBox.ScrollElement.Action.None;
        }

        return ScrollBox.ScrollElement.Action.Handled;
    }
}
