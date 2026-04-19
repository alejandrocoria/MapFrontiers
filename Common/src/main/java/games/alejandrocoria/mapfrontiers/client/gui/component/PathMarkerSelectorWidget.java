package games.alejandrocoria.mapfrontiers.client.gui.component;

import games.alejandrocoria.mapfrontiers.client.frontier.MarkerImageConstants;
import games.alejandrocoria.mapfrontiers.client.frontier.PathMarkerCatalog;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenAxis;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
public class PathMarkerSelectorWidget extends AbstractWidgetNoNarration {
    private static final int CELL_SIZE = 18;
    private static final int CELL_SPACING = 2;
    private static final int SELECTED_MARKER_COLOR = 0xFFFFFFFF;
    private static final int UNSELECTED_MARKER_COLOR = 0xFFCCCCCC;

    private Consumer<Identifier> onPress;
    private Identifier selectedId;
    private int focusedIndex;

    public PathMarkerSelectorWidget(Identifier selectedId, Consumer<Identifier> onPress) {
        super(0, 0, getSelectorWidth(), CELL_SIZE, Component.empty());
        this.selectedId = selectedId;
        this.onPress = onPress;
        this.focusedIndex = getSelectedIndexOrFallback();
    }

    public void setOnPress(Consumer<Identifier> onPress) {
        this.onPress = onPress;
    }

    public void setSelectedId(Identifier selectedId) {
        this.selectedId = selectedId;
        if (!isFocused()) {
            focusedIndex = getSelectedIndexOrFallback();
        }
    }

    @Nullable
    public ComponentPath nextFocusPath(FocusNavigationEvent navigationEvent) {
        if (!visible || !active) {
            return null;
        }

        if (!isFocused()) {
            focusedIndex = getSelectedIndexOrFallback();
            return ComponentPath.leaf(this);
        }

        if (navigationEvent instanceof FocusNavigationEvent.TabNavigation) {
            return null;
        }

        if (navigationEvent instanceof FocusNavigationEvent.ArrowNavigation arrowNavigation) {
            if (arrowNavigation.direction().getAxis() != ScreenAxis.HORIZONTAL) {
                return null;
            }

            int nextIndex = getNextFocusedIndex(arrowNavigation.direction());
            if (nextIndex == -1) {
                return null;
            }

            focusedIndex = nextIndex;
            return ComponentPath.leaf(this);
        }

        return ComponentPath.leaf(this);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!isFocused() || !visible || !active || !event.isSelection()) {
            return false;
        }

        selectFocusedIndex();
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int hoveredIndex = getHoveredIndex(event.x(), event.y());
        if (hoveredIndex < 0 || hoveredIndex >= PathMarkerCatalog.BUILT_INS.size()) {
            return false;
        }

        selectedId = PathMarkerCatalog.BUILT_INS.get(hoveredIndex).id();
        focusedIndex = hoveredIndex;
        onPress.accept(selectedId);
        return true;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        int hoveredIndex = getHoveredIndex(mouseX, mouseY);

        int x = getX();
        for (int i = 0; i < PathMarkerCatalog.BUILT_INS.size(); ++i) {
            PathMarkerCatalog.Entry entry = PathMarkerCatalog.BUILT_INS.get(i);
            boolean selected = entry.id().equals(selectedId);
            boolean hovered = hoveredIndex >= 0 && PathMarkerCatalog.BUILT_INS.get(hoveredIndex) == entry;

            int borderColor = selected ? ColorConstants.WHITE : hovered ? ColorConstants.OPTION_BORDER : ColorConstants.CHECKBOX_BORDER;
            graphics.fill(x, getY(), x + CELL_SIZE, getY() + CELL_SIZE, borderColor);
            graphics.fill(x + 1, getY() + 1, x + CELL_SIZE - 1, getY() + CELL_SIZE - 1, ColorConstants.PATH_MARKER_SELECTOR_BG);

            if (entry.texture() != null) {
                Services.JOURNEYMAP.prepareMapTexture(entry.texture());
                int markerX = x + (CELL_SIZE - MarkerImageConstants.SELECTOR_DISPLAY_SIZE) / 2;
                int markerY = getY() + (CELL_SIZE - MarkerImageConstants.SELECTOR_DISPLAY_SIZE) / 2;
                graphics.blit(RenderPipelines.GUI_TEXTURED, entry.texture(), markerX, markerY, 0, 0,
                        MarkerImageConstants.SELECTOR_DISPLAY_SIZE, MarkerImageConstants.SELECTOR_DISPLAY_SIZE,
                        MarkerImageConstants.TEXTURE_SIZE, MarkerImageConstants.TEXTURE_SIZE,
                        MarkerImageConstants.TEXTURE_SIZE, MarkerImageConstants.TEXTURE_SIZE,
                        selected ? SELECTED_MARKER_COLOR : UNSELECTED_MARKER_COLOR);
            }

            if (isKeyboardFocused() && focusedIndex == i) {
                graphics.hLine(x - 1, x + CELL_SIZE, getY() - 1, ColorConstants.WHITE);
                graphics.hLine(x - 1, x + CELL_SIZE, getY() + CELL_SIZE, ColorConstants.WHITE);
                graphics.vLine(x - 1, getY() - 1, getY() + CELL_SIZE, ColorConstants.WHITE);
                graphics.vLine(x + CELL_SIZE, getY() - 1, getY() + CELL_SIZE, ColorConstants.WHITE);
            }

            x += CELL_SIZE + CELL_SPACING;
        }
    }

    private int getHoveredIndex(double mouseX, double mouseY) {
        if (!active || mouseX < getX() || mouseX >= getX() + getWidth() || mouseY < getY() || mouseY >= getY() + getHeight()) {
            return -1;
        }

        double localX = mouseX - getX();
        int step = CELL_SIZE + CELL_SPACING;
        int index = (int) (localX / step);
        double cellOffset = localX - index * step;
        if (index < 0 || index >= PathMarkerCatalog.BUILT_INS.size() || cellOffset >= CELL_SIZE) {
            return -1;
        }

        return index;
    }

    private static int getSelectorWidth() {
        return PathMarkerCatalog.BUILT_INS.size() * CELL_SIZE + (PathMarkerCatalog.BUILT_INS.size() - 1) * CELL_SPACING;
    }

    private int getSelectedIndexOrFallback() {
        int index = getSelectedIndex();
        return index == -1 ? 0 : index;
    }

    private int getSelectedIndex() {
        for (int i = 0; i < PathMarkerCatalog.BUILT_INS.size(); ++i) {
            if (PathMarkerCatalog.BUILT_INS.get(i).id().equals(selectedId)) {
                return i;
            }
        }

        return -1;
    }

    private int getNextFocusedIndex(ScreenDirection direction) {
        int nextIndex = switch (direction) {
            case LEFT -> focusedIndex - 1;
            case RIGHT -> focusedIndex + 1;
            case UP, DOWN -> -1;
        };

        return nextIndex >= 0 && nextIndex < PathMarkerCatalog.BUILT_INS.size() ? nextIndex : -1;
    }

    private void selectFocusedIndex() {
        if (focusedIndex < 0 || focusedIndex >= PathMarkerCatalog.BUILT_INS.size()) {
            return;
        }

        selectedId = PathMarkerCatalog.BUILT_INS.get(focusedIndex).id();
        onPress.accept(selectedId);
    }

    private boolean isKeyboardFocused() {
        return isFocused() && Minecraft.getInstance().getLastInputType().isKeyboard();
    }
}
