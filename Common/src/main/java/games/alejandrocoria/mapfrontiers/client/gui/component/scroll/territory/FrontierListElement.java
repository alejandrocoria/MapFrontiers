package games.alejandrocoria.mapfrontiers.client.gui.component.scroll.territory;

import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import games.alejandrocoria.mapfrontiers.MapFrontiers;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.PluginSourceBadge;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.CheckBoxButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.IconButton;
import games.alejandrocoria.mapfrontiers.client.gui.component.scroll.ScrollBox;
import games.alejandrocoria.mapfrontiers.client.gui.util.GuiGraphicsHelper;
import games.alejandrocoria.mapfrontiers.client.gui.util.SourcePluginUiHelper;
import games.alejandrocoria.mapfrontiers.client.gui.util.TextEllipsizeHelper;
import games.alejandrocoria.mapfrontiers.client.territory.frontier.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.util.SettingsUserFormatter;
import games.alejandrocoria.mapfrontiers.common.territory.frontier.FrontierShape;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class FrontierListElement extends TerritoryListRowElement implements ScrollBox.KeyedFocusNavigation {
    private static final ResourceLocation NAME_FADE_TEXTURE = ResourceLocation.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/territory_list/name_fade.png");
    private static final ResourceLocation CHUNK_FILL_TEXTURE = frontierListTexture("chunk_fill.png");
    private static final ResourceLocation CHUNK_OUTLINE_TEXTURE = frontierListTexture("chunk_outline.png");
    private static final ResourceLocation PATH_FILL_TEXTURE = frontierListTexture("path_fill.png");
    private static final ResourceLocation PATH_OUTLINE_TEXTURE = frontierListTexture("path_outline.png");
    private static final ResourceLocation VERTEX_FILL_TEXTURE = frontierListTexture("vertex_fill.png");
    private static final ResourceLocation VERTEX_OUTLINE_TEXTURE = frontierListTexture("vertex_outline.png");
    private static final int LEFT_PADDING = 4;
    private static final int RIGHT_PADDING = 4;
    private static final int SELECTION_RAIL_GAP = 2;
    private static final int ACTION_GAP = 2;
    private static final int ACTION_BUTTON_WIDTH = 11;
    private static final int RAIL_CONTENT_Y = 7;
    private static final int NAME_HOVER_X = 24;
    private static final int NAME_X = 26;
    private static final int SOURCE_PLUGIN_GAP = 2;
    private static final int METADATA_X = 254;
    private static final int NAME_METADATA_SPACING = 2;
    private static final int SHAPE_BADGE_X = 2;
    private static final int SHAPE_BADGE_ICON_Y = 1;
    private static final int SHAPE_BADGE_COUNT_Y = 16;
    private static final int SHAPE_BADGE_ICON_WIDTH = 21;
    private static final int SHAPE_BADGE_ICON_HEIGHT = 14;
    private static final int NAME_LINE_1_Y = 4;
    private static final int NAME_LINE_2_Y = 14;
    private static final int NAME_LINE_BG_TOP_OFFSET = -1;
    private static final int NAME_LINE_BG_BOTTOM_OFFSET = 9;
    private static final int NAME_LINE_BG_FADE_WIDTH = 6;
    private static final Tooltip DELETE_TOOLTIP = Tooltip.create(Component.translatable("mapfrontiers.delete"));
    private final Font font;
    private final FrontierOverlay frontier;
    private final String name1;
    private final String name2;
    private final String owner;
    private final String dimension;
    private final int count;
    private final int collectionColor;
    private final boolean checkboxVisible;
    private final boolean checkboxVisibleOnHover;
    private final CheckBoxButton checkBoxButton;
    private final FocusTarget mainFocusTarget;
    private final List<GuiEventListener> children;
    private final @Nullable PluginSourceBadge sourcePluginWidget;
    private final @Nullable IconButton visibilityButton;
    private final @Nullable IconButton deleteButton;
    private boolean markToggleRequested;
    private boolean visibilityRequested;
    private boolean deleteRequested;

    public FrontierListElement(Font font, FrontierOverlay frontier, int width, int collectionColor,
                               boolean checkboxVisible, boolean checkboxVisibleOnHover, boolean checked,
                               boolean visibilityEnabled, boolean deleteEnabled) {
        super(frontier.getId().toString(), width, 25);
        this.font = font;
        this.frontier = frontier;
        this.collectionColor = collectionColor;
        this.checkboxVisible = checkboxVisible;
        this.checkboxVisibleOnHover = checkboxVisibleOnHover;
        mainFocusTarget = new FocusTarget(this::getMainFocusRectangle);
        checkBoxButton = new CheckBoxButton(checked, button -> requestMarkToggle());
        sourcePluginWidget = createSourcePluginWidget();

        visibilityButton = visibilityEnabled ? new IconButton(getVisibilityButtonType(), button -> requestVisibility()) : null;
        if (visibilityButton != null) {
            visibilityButton.active = true;
            visibilityButton.setTooltip(getVisibilityTooltip());
        }
        deleteButton = deleteEnabled ? new IconButton(IconButton.Type.Remove, button -> requestDelete()) : null;
        if (deleteButton != null) {
            deleteButton.active = true;
            deleteButton.setTooltip(DELETE_TOOLTIP);
        }
        children = buildChildren();

        if (frontier.isNamed()) {
            name1 = frontier.getName1();
            name2 = frontier.getName2();
        } else {
            name1 = I18n.get("mapfrontiers.unnamed_1", ChatFormatting.ITALIC);
            name2 = I18n.get("mapfrontiers.unnamed_2", ChatFormatting.ITALIC);
        }

        owner = I18n.get("mapfrontiers.owner", SettingsUserFormatter.getDisplayName(frontier.getOwner()));
        dimension = I18n.get("mapfrontiers.dimension", frontier.getDimension().location().toString());

        if (frontier.getShape() == FrontierShape.Vertex) {
            count = frontier.getVertexCount();
        } else if (frontier.getShape() == FrontierShape.Path) {
            count = frontier.getPointCount();
        } else {
            count = frontier.getChunkCount();
        }
    }

    public FrontierOverlay getFrontier() {
        return frontier;
    }

    public boolean consumeMarkToggleRequested() {
        boolean requested = markToggleRequested;
        markToggleRequested = false;
        return requested;
    }

    public boolean consumeVisibilityRequested() {
        boolean requested = visibilityRequested;
        visibilityRequested = false;
        return requested;
    }

    public boolean consumeDeleteRequested() {
        boolean requested = deleteRequested;
        deleteRequested = false;
        return requested;
    }

    @Override
    protected void setX(int x) {
        super.setX(x);
        if (visibilityButton != null) {
            visibilityButton.setX(getVisibilityLeft());
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
        if (visibilityButton != null) {
            visibilityButton.setY(this.y + RAIL_CONTENT_Y);
        }
        if (deleteButton != null) {
            deleteButton.setY(this.y + RAIL_CONTENT_Y);
        }
        checkBoxButton.setY(this.y + RAIL_CONTENT_Y);
        if (sourcePluginWidget != null) {
            sourcePluginWidget.setY(this.y + 1);
        }
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, boolean selected, boolean focused) {
        int selectionRightBound = getSelectionRightBound();
        if (isHovered) {
            graphics.fill(x, y, selectionRightBound, y + height, ColorConstants.SCROLL_ELEMENT_HOVERED);
            graphics.fill(selectionRightBound, y, x + width, y + height, ColorConstants.TERRITORY_LIST_RAIL_HOVER_BG);
        }

        int rowContentX = x + LEFT_PADDING;
        int nameX = getNameX();
        int maxNameWidth = METADATA_X - nameX - NAME_METADATA_SPACING;
        String visibleName1 = ellipsize(name1, maxNameWidth);
        String visibleName2 = ellipsize(name2, maxNameWidth);
        boolean name1Truncated = !visibleName1.equals(name1);
        boolean name2Truncated = !visibleName2.equals(name2);
        boolean showExpandedNames = isNameAreaHovered(mouseX, mouseY, rowContentX, nameX) && (name1Truncated || name2Truncated);

        graphics.fill(x, y, x + 2, y + height, collectionColor);
        graphics.fill(x + width - 2, y, x + width, y + height, collectionColor);

        graphics.drawString(font, owner, rowContentX + METADATA_X, y + 4, ColorConstants.TEXT);
        graphics.drawString(font, dimension, rowContentX + METADATA_X, y + 14, ColorConstants.TEXT_DIMENSION);
        renderSourcePlugin(graphics, mouseX, mouseY, partialTicks);

        int nameColor = selected ? ColorConstants.TEXT_HIGHLIGHT : ColorConstants.TEXT;
        drawNameLine(graphics, name1, visibleName1, name1Truncated, showExpandedNames, NAME_LINE_1_Y,
                nameColor, rowContentX, nameX);
        drawNameLine(graphics, name2, visibleName2, name2Truncated, showExpandedNames, NAME_LINE_2_Y,
                nameColor, rowContentX, nameX);

        drawShapeBadge(graphics, selected, rowContentX);
        renderActionButtons(graphics, mouseX, mouseY, partialTicks, focused);
        renderCheckBox(graphics, mouseX, mouseY, partialTicks, focused);
    }

    @Override
    protected void drawFocusOutline(GuiGraphics graphics) {
        if (getFocused() != mainFocusTarget) {
            return;
        }

        int left = x + 2;
        int top = y;
        graphics.renderOutline(left, top, width - 4, height, ColorConstants.FRONTIER_LIST_ROW_OUTLINE);
    }

    private void renderCheckBox(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, boolean focused) {
        if (!shouldRenderCheckBox(focused)) {
            return;
        }

        checkBoxButton.visible = true;
        checkBoxButton.active = true;
        checkBoxButton.render(graphics, mouseX, mouseY, partialTicks);
    }

    private void renderActionButtons(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, boolean focused) {
        if (visibilityButton != null && (focused || isHovered || !frontier.getVisibilityData().getFrontier())) {
            visibilityButton.setType(getVisibilityButtonType());
            visibilityButton.setTooltip(getVisibilityTooltip());
            visibilityButton.render(graphics, mouseX, mouseY, partialTicks);
        }
        if (deleteButton != null && (focused || isHovered)) {
            deleteButton.render(graphics, mouseX, mouseY, partialTicks);
        }
    }

    private void renderSourcePlugin(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (sourcePluginWidget != null) {
            sourcePluginWidget.render(graphics, mouseX, mouseY, partialTicks);
        }
    }

    private boolean shouldRenderCheckBox(boolean focused) {
        return checkboxVisible || checkboxVisibleOnHover && (isHovered || focused);
    }

    private void drawShapeBadge(GuiGraphics graphics, boolean selected, int rowContentX) {
        ResourceLocation fillTexture;
        ResourceLocation outlineTexture;
        switch (frontier.getShape()) {
            case Vertex -> {
                fillTexture = VERTEX_FILL_TEXTURE;
                outlineTexture = VERTEX_OUTLINE_TEXTURE;
            }
            case Path -> {
                fillTexture = PATH_FILL_TEXTURE;
                outlineTexture = PATH_OUTLINE_TEXTURE;
            }
            case Chunk -> {
                fillTexture = CHUNK_FILL_TEXTURE;
                outlineTexture = CHUNK_OUTLINE_TEXTURE;
            }
            default -> throw new IllegalStateException("Unexpected frontier shape: " + frontier.getShape());
        }

        int iconX = rowContentX + SHAPE_BADGE_X;
        int iconY = y + SHAPE_BADGE_ICON_Y;
        GuiGraphicsHelper.blitTinted(graphics, fillTexture, iconX, iconY, 0, 0, SHAPE_BADGE_ICON_WIDTH,
                SHAPE_BADGE_ICON_HEIGHT, SHAPE_BADGE_ICON_WIDTH, SHAPE_BADGE_ICON_HEIGHT, frontier.getColor() | 0xFF000000);
        int outlineColor = selected ? ColorConstants.SHAPE_BADGE_OUTLINE_SELECTED : ColorConstants.SHAPE_BADGE_OUTLINE_NORMAL;
        GuiGraphicsHelper.blitTinted(graphics, outlineTexture, iconX, iconY, 0, 0, SHAPE_BADGE_ICON_WIDTH,
                SHAPE_BADGE_ICON_HEIGHT, SHAPE_BADGE_ICON_WIDTH, SHAPE_BADGE_ICON_HEIGHT, outlineColor);

        String countText = Integer.toString(count);
        int countX = iconX + (SHAPE_BADGE_ICON_WIDTH - font.width(countText)) / 2;
        graphics.drawString(font, countText, countX, y + SHAPE_BADGE_COUNT_Y, ColorConstants.TEXT_DIMENSION);
    }

    private void drawNameLine(GuiGraphics graphics,
                              String fullName,
                              String visibleName,
                              boolean truncated,
                              boolean showExpandedNames,
                              int lineY,
                              int textColor,
                              int rowContentX,
                              int nameX) {
        String renderedName = showExpandedNames && truncated ? fullName : visibleName;

        if (showExpandedNames && truncated) {
            int textWidth = font.width(fullName);
            int bgLeft = rowContentX + nameX - 1;
            int bgOpaqueRight = bgLeft + textWidth + 2;
            graphics.fill(bgLeft, y + lineY + NAME_LINE_BG_TOP_OFFSET, bgOpaqueRight, y + lineY + NAME_LINE_BG_BOTTOM_OFFSET, ColorConstants.SCROLL_ELEMENT_SELECTED);
            GuiGraphicsHelper.blitTinted(graphics, NAME_FADE_TEXTURE, bgOpaqueRight, y + lineY + NAME_LINE_BG_TOP_OFFSET, 0, 0,
                    NAME_LINE_BG_FADE_WIDTH, NAME_LINE_BG_BOTTOM_OFFSET - NAME_LINE_BG_TOP_OFFSET, NAME_LINE_BG_FADE_WIDTH,
                    NAME_LINE_BG_BOTTOM_OFFSET - NAME_LINE_BG_TOP_OFFSET, ColorConstants.SCROLL_ELEMENT_SELECTED);
        }

        graphics.drawString(font, renderedName, rowContentX + nameX, y + lineY, textColor);
    }

    private String ellipsize(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }

        return TextEllipsizeHelper.ellipsizeByWidth(font, text, maxWidth);
    }

    private boolean isNameAreaHovered(int mouseX, int mouseY, int rowContentX, int nameX) {
        int hoverLeft = Math.max(rowContentX + NAME_HOVER_X, rowContentX + nameX - 1);
        return mouseX >= hoverLeft && mouseY >= y && mouseX < rowContentX + METADATA_X && mouseY < y + height;
    }

    private int getNameX() {
        return NAME_X + getSourcePluginOffset();
    }

    private int getSourcePluginOffset() {
        if (sourcePluginWidget == null) {
            return 0;
        }

        return sourcePluginWidget.getWidth() + SOURCE_PLUGIN_GAP;
    }

    private int getSourcePluginX() {
        return x + LEFT_PADDING + NAME_X;
    }

    private int getCheckBoxX() {
        return x + width - RIGHT_PADDING - CheckBoxButton.SIZE;
    }

    private int getSelectionRightBound() {
        return Math.max(x, getRailLeft() - SELECTION_RAIL_GAP);
    }

    private int getRailLeft() {
        return getVisibilitySlotLeft();
    }

    private int getDeleteLeft() {
        return getDeleteSlotLeft();
    }

    private int getVisibilityLeft() {
        return getVisibilitySlotLeft();
    }

    private int getDeleteSlotLeft() {
        return getCheckBoxX() - ACTION_GAP - ACTION_BUTTON_WIDTH;
    }

    private int getVisibilitySlotLeft() {
        return getDeleteSlotLeft() - ACTION_GAP - ACTION_BUTTON_WIDTH;
    }

    private boolean isCheckBoxHovered(int mouseX, int mouseY) {
        return shouldRenderCheckBox(false) && checkBoxButton.isMouseOver(mouseX, mouseY);
    }

    private static ResourceLocation frontierListTexture(String fileName) {
        return ResourceLocation.fromNamespaceAndPath(MapFrontiers.MODID, "textures/gui/territory_list/" + fileName);
    }

    @Override
    public ScrollBox.ScrollElement.Action mousePressed(double mouseX, double mouseY, int button) {
        if (visible && isHovered) {
            if (isCheckBoxHovered((int) mouseX, (int) mouseY)) {
                requestMarkToggle();
                return ScrollBox.ScrollElement.Action.Handled;
            }
            if (deleteButton != null && deleteButton.isMouseOver(mouseX, mouseY)) {
                requestDelete();
                return ScrollBox.ScrollElement.Action.Handled;
            }
            if (visibilityButton != null && visibilityButton.isMouseOver(mouseX, mouseY)) {
                requestVisibility();
                return ScrollBox.ScrollElement.Action.Handled;
            }
            if (mouseX >= getSelectionRightBound()) {
                return ScrollBox.ScrollElement.Action.None;
            }
            return ScrollBox.ScrollElement.Action.Handled;
        }

        return ScrollBox.ScrollElement.Action.None;
    }

    @Override
    public List<GuiEventListener> children() {
        return children;
    }

    @Override
    public @Nullable Object getFocusedNavigationKey() {
        return resolveNavigationKey(getFocused());
    }

    @Override
    public Object getDefaultNavigationKey() {
        return TerritoryListFocusKey.MAIN;
    }

    @Override
    public @Nullable Object getEdgeNavigationKey(boolean forward) {
        return forward ? TerritoryListFocusKey.MAIN : getLastNavigationKey();
    }

    @Override
    public @Nullable ComponentPath getFocusPathForKey(Object key) {
        if (!(key instanceof TerritoryListFocusKey focusKey)) {
            return null;
        }

        return switch (focusKey) {
            case SOURCE_PLUGIN -> focusPathForListener(sourcePluginWidget);
            case MAIN -> focusPathForListener(mainFocusTarget);
            case ADD_ACTION -> null;
            case VISIBILITY_ACTION -> focusPathForListener(visibilityButton);
            case CONTEXT_ACTION -> focusPathForListener(deleteButton);
            case MARK -> checkboxVisible || checkboxVisibleOnHover ? focusPathForListener(checkBoxButton) : null;
            case COLLAPSE -> null;
        };
    }

    @Override
    public boolean isPrimaryActionFocused() {
        return getFocused() == mainFocusTarget;
    }

    @Override
    public @Nullable ComponentPath focusNavigationKey(FocusNavigationEvent navigationEvent, Object key) {
        return getFocusPathForKey(key);
    }

    @Override
    public @Nullable ComponentPath focusRelativeNavigationKey(FocusNavigationEvent navigationEvent, int delta) {
        TerritoryListFocusKey[] order = {
                TerritoryListFocusKey.SOURCE_PLUGIN,
                TerritoryListFocusKey.MAIN,
                TerritoryListFocusKey.VISIBILITY_ACTION,
                TerritoryListFocusKey.CONTEXT_ACTION,
                TerritoryListFocusKey.MARK
        };

        TerritoryListFocusKey currentKey = (TerritoryListFocusKey) getFocusedNavigationKey();
        int currentIndex = indexOf(order, currentKey);
        for (int i = currentIndex + delta; i >= 0 && i < order.length; i += delta) {
            ComponentPath path = focusNavigationKey(navigationEvent, order[i]);
            if (path != null) {
                return path;
            }
        }

        return null;
    }

    private void requestMarkToggle() {
        markToggleRequested = true;
    }

    private void requestVisibility() {
        visibilityRequested = true;
    }

    private void requestDelete() {
        deleteRequested = true;
    }

    private List<GuiEventListener> buildChildren() {
        List<GuiEventListener> children = new ArrayList<>(5);
        if (sourcePluginWidget != null) {
            children.add(sourcePluginWidget);
        }
        children.add(mainFocusTarget);
        if (visibilityButton != null) {
            children.add(visibilityButton);
        }
        if (deleteButton != null) {
            children.add(deleteButton);
        }
        if (checkboxVisible || checkboxVisibleOnHover) {
            children.add(checkBoxButton);
        }
        return List.copyOf(children);
    }

    private IconButton.Type getVisibilityButtonType() {
        return frontier.getVisibilityData().getFrontier() ? IconButton.Type.Hide : IconButton.Type.Show;
    }

    private Tooltip getVisibilityTooltip() {
        return Tooltip.create(Component.translatable(
                frontier.getVisibilityData().getFrontier() ? "mapfrontiers.hide.tooltip" : "mapfrontiers.show.tooltip"));
    }

    private @Nullable ComponentPath focusPathForListener(@Nullable GuiEventListener listener) {
        if (listener == null) {
            return null;
        }

        return ComponentPath.path(this, ComponentPath.leaf(listener));
    }

    private TerritoryListFocusKey getLastNavigationKey() {
        if (checkboxVisible || checkboxVisibleOnHover) {
            return TerritoryListFocusKey.MARK;
        }
        if (deleteButton != null) {
            return TerritoryListFocusKey.CONTEXT_ACTION;
        }
        if (visibilityButton != null) {
            return TerritoryListFocusKey.VISIBILITY_ACTION;
        }
        if (sourcePluginWidget != null) {
            return TerritoryListFocusKey.SOURCE_PLUGIN;
        }
        return TerritoryListFocusKey.MAIN;
    }

    private TerritoryListFocusKey resolveNavigationKey(@Nullable GuiEventListener listener) {
        if (listener == sourcePluginWidget) {
            return TerritoryListFocusKey.SOURCE_PLUGIN;
        }
        if (listener == mainFocusTarget) {
            return TerritoryListFocusKey.MAIN;
        }
        if (listener == visibilityButton) {
            return TerritoryListFocusKey.VISIBILITY_ACTION;
        }
        if (listener == deleteButton) {
            return TerritoryListFocusKey.CONTEXT_ACTION;
        }
        if (listener == checkBoxButton) {
            return TerritoryListFocusKey.MARK;
        }
        return TerritoryListFocusKey.MAIN;
    }

    private @Nullable PluginSourceBadge createSourcePluginWidget() {
        if (!SourcePluginUiHelper.hasSourcePlugin(frontier.getSourcePluginId())) {
            return null;
        }

        return new PluginSourceBadge(font, frontier.getSourcePluginId(), false, height - 1);
    }

    private ScreenRectangle getMainFocusRectangle() {
        return new ScreenRectangle(x + 2, y, Math.max(1, getSelectionRightBound() - (x + 2)), height);
    }

    private static int indexOf(TerritoryListFocusKey[] order, TerritoryListFocusKey key) {
        for (int i = 0; i < order.length; ++i) {
            if (order[i] == key) {
                return i;
            }
        }
        return -1;
    }
}
