package games.alejandrocoria.mapfrontiers.client.gui.component.scroll;

import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.frontier.CollectionScope;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.StringWidget;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.CheckBoxButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.IconButton;
import games.alejandrocoria.mapfrontiers.client.gui.util.SourcePluginUiHelper;
import games.alejandrocoria.mapfrontiers.client.gui.util.TextEllipsizeHelper;
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
    private static final int TITLE_X = 15;
    private static final int TITLE_Y = 6;
    private static final int COUNTERS_GAP = 4;
    private static final int COUNTERS_RIGHT_GAP = 2;
    private static final int TITLE_HOVER_X = 14;
    private static final int SOURCE_PLUGIN_GAP = 2;
    private static final int TITLE_BG_TOP_OFFSET = -2;
    private static final int TITLE_BG_BOTTOM_OFFSET = 8;
    private static final int TITLE_BG_FADE_WIDTH = 6;
    private static final int LEFT_PADDING = 4;
    private static final int RIGHT_PADDING = 4;
    private static final int ACTION_GAP = 2;
    private static final int ACTION_BUTTON_WIDTH = 11;
    private static final int SELECTION_RAIL_GAP = 2;
    private static final int RAIL_CONTENT_Y = 4;
    private static final int MARKED_BADGE_HEIGHT = 11;
    private static final int MARKED_BADGE_TEXT_LEFT = 3;
    private static final int MARKED_BADGE_WIDTH_EXTRA = 5;
    private static final int MARKED_BADGE_GAP = 4;
    private static final int RAIL_HOVER_COLOR = 0xA0202020;
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
    private final @Nullable StringWidget sourcePluginWidget;
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
        sourcePluginWidget = createSourcePluginWidget();

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
        collapseToggleButton.setX(this.x + LEFT_PADDING);
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
        if (sourcePluginWidget != null) {
            sourcePluginWidget.setX(getSourcePluginX());
        }
    }

    @Override
    protected void setY(int y) {
        super.setY(y);
        collapseToggleButton.setY(this.y + RAIL_CONTENT_Y);
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
        if (sourcePluginWidget != null) {
            sourcePluginWidget.setY(this.y + 4);
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
        renderTexts(graphics, mouseX, mouseY, partialTicks);
        renderMarkedCount(graphics);
        renderActionButtons(graphics, mouseX, mouseY, partialTicks);
        renderCheckBox(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    protected void drawFocusOutline(GuiGraphicsExtractor graphics) {
        int left = x + 2;
        int top = y + 2;
        graphics.outline(left, top, width - 4, height - 2, ColorConstants.WHITE);
    }

    private void renderTexts(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        int titleColor = ColorConstants.TEXT;
        if (virtualRow) {
            titleColor = ColorConstants.VIRTUAL_COLLECTION;
        }

        int titleX = getTitleX();
        int rightZoneStart = getRightZoneStart();
        int countersX = rightZoneStart - font.width(counters);
        int titleMaxWidth = countersX - titleX - COUNTERS_GAP;
        String visibleTitle = ellipsize(title, titleMaxWidth);

        boolean titleTruncated = !visibleTitle.equals(title);
        boolean showExpandedTitle = titleTruncated && isTitleAreaHovered(mouseX, mouseY, titleX, visibleTitle);

        renderSourcePlugin(graphics, mouseX, mouseY, partialTicks);
        graphics.text(font, counters, countersX, y + TITLE_Y, ColorConstants.TEXT_DIMENSION);
        drawTitle(graphics, titleX, titleColor, visibleTitle, showExpandedTitle);
    }

    private void renderSourcePlugin(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        if (sourcePluginWidget != null) {
            sourcePluginWidget.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        }
    }

    private void renderMarkedCount(GuiGraphicsExtractor graphics) {
        if (markedCount <= 0) {
            return;
        }

        String markedText = Integer.toString(markedCount);
        int badgeWidth = getMarkedBadgeWidth();
        int badgeRight = getRailTextRight();
        int badgeLeft = badgeRight - badgeWidth;
        int badgeTop = y + RAIL_CONTENT_Y;

        graphics.outline(badgeLeft, badgeTop, badgeWidth, MARKED_BADGE_HEIGHT, ColorConstants.WHITE);
        graphics.text(font, markedText, badgeLeft + MARKED_BADGE_TEXT_LEFT, y + TITLE_Y, ColorConstants.TEXT_HIGHLIGHT);
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

        return getRailTextRight() - getMarkedBadgeWidth() - MARKED_BADGE_GAP;
    }

    private int getTitleX() {
        return x + TITLE_X + getSourcePluginOffset();
    }

    private int getSourcePluginOffset() {
        if (sourcePluginWidget == null) {
            return 0;
        }

        return sourcePluginWidget.getWidth() + SOURCE_PLUGIN_GAP;
    }

    private int getSourcePluginX() {
        return x + TITLE_X;
    }

    private int getRailTextRight() {
        return getSelectionRightBound() - COUNTERS_RIGHT_GAP;
    }

    private int getMarkedBadgeWidth() {
        if (markedCount <= 0) {
            return 0;
        }

        return font.width(Integer.toString(markedCount)) + MARKED_BADGE_WIDTH_EXTRA;
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

        return TextEllipsizeHelper.ellipsizeByWidth(font, text, maxWidth);
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

    private @Nullable StringWidget createSourcePluginWidget() {
        if (collection == null) {
            return null;
        }

        Tooltip tooltip = SourcePluginUiHelper.createTooltip(collection.getSourcePluginId());
        if (tooltip == null) {
            return null;
        }

        StringWidget widget = new StringWidget(SourcePluginUiHelper.createSymbolComponent(), font);
        widget.setColor(ColorConstants.TEXT_SOURCE_PLUGIN);
        widget.setTooltip(tooltip);
        return widget;
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
