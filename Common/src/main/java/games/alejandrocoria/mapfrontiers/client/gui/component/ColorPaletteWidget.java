package games.alejandrocoria.mapfrontiers.client.gui.component;

import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
public class ColorPaletteWidget extends AbstractWidgetNoNarration {
    private static final int COLUMNS = 7;
    private static final int ROWS = 4;
    private static final int CELL_SIZE = 20;
    private static final int CELL_GAP = 1;
    private static final int CELL_INSET = 1;
    private static final int CELL_PITCH = CELL_SIZE + CELL_GAP;
    private static final int WIDTH = COLUMNS * CELL_SIZE + (COLUMNS - 1) * CELL_GAP;
    private static final int HEIGHT = ROWS * CELL_SIZE + (ROWS - 1) * CELL_GAP;
    private static final int BORDER_COLOR = 0xFF404040;
    private static final int SELECTED_COLOR = 0xFFFFFFFF;
    private static final int FOCUS_OUTLINE_INSET = 1;

    private static final int[] PALETTE_COLORS = {
            0xFFFF0000, 0xFFFF8000, 0xFFFFFF00, 0xFF80FF00, 0xFF00FF00, 0xFF00FF80, 0xFFFFFFFF,
            0xFFD29292, 0xFFE4C5A5, 0xFFF7F7B8, 0xFFCEEEAE, 0xFFA5E4A5, 0xFFA8E8C8, 0xFFBFBFBF,
            0xFF00FFFF, 0xFF0080FF, 0xFF0000FF, 0xFF8000FF, 0xFFFF00FF, 0xFFFF0080, 0xFF808080,
            0xFFACEBEB, 0xFF99B9D9, 0xFF8686C6, 0xFFB090CF, 0xFFD999D9, 0xFFD596B6, 0xFF404040};

    private static final int[] PALETTE_COLORS_INACTIVE = {
            0xFF343434, 0xFF595959, 0xFF7E7E7E, 0xFF6B6B6B, 0xFF585858, 0xFFA4A4A4, 0xFFFFFFFF,
            0xFFA5A5A5, 0xFFCBCBCB, 0xFFF0F0F0, 0xFFDDDDDD, 0xFFCACACA, 0xFFD1D1D1, 0xFFBFBFBF,
            0xFF666666, 0xFF424242, 0xFF1C1C1C, 0xFF2F2F2F, 0xFF424242, 0xFF5B5B5B, 0xFF808080,
            0xFFD8D8D8, 0xFFB3B3B3, 0xFF8D8D8D, 0xFFA1A1A1, 0xFFB3B3B3, 0xFFACACAC, 0xFF404040};

    private int color;
    private int focusedIndex;
    private final Consumer<Integer> onPress;

    public ColorPaletteWidget(int color, Consumer<Integer> onPress) {
        super(0, 0, WIDTH, HEIGHT, Component.empty());
        this.color = color;
        focusedIndex = Math.max(0, findColorIndex(color));
        this.onPress = onPress;
    }

    public void setColor(int color) {
        this.color = color;
        int colorIndex = findColorIndex(color);
        if (colorIndex != -1) {
            focusedIndex = colorIndex;
        }
    }

    @Nullable
    @Override
    public ComponentPath nextFocusPath(FocusNavigationEvent navigationEvent) {
        if (!visible || !active) {
            return null;
        }

        if (!isFocused()) {
            int colorIndex = findColorIndex(color);
            if (colorIndex != -1) {
                focusedIndex = colorIndex;
            } else {
                focusedIndex = getEntryIndexForInitialFocus(navigationEvent);
            }
            return ComponentPath.leaf(this);
        }

        if (navigationEvent instanceof FocusNavigationEvent.TabNavigation) {
            return null;
        }

        if (navigationEvent instanceof FocusNavigationEvent.ArrowNavigation arrowNavigation) {
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

        color = PALETTE_COLORS[focusedIndex];
        onPress.accept(color);
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int clickedIndex = getCellIndex(event.x(), event.y());
        if (clickedIndex == -1) {
            return false;
        }

        focusedIndex = clickedIndex;
        color = PALETTE_COLORS[clickedIndex];
        onPress.accept(color);
        return true;
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        int col = 0;
        int row = 0;
        for (int c : (active ? PALETTE_COLORS : PALETTE_COLORS_INACTIVE)) {
            int x = getX() + col * CELL_PITCH;
            int y = getY() + row * CELL_PITCH;
            if (active && c == color) {
                graphics.fill(x, y, x + CELL_SIZE, y + CELL_SIZE, SELECTED_COLOR);
            } else {
                graphics.fill(x, y, x + CELL_SIZE, y + CELL_SIZE, BORDER_COLOR);
            }
            graphics.fill(x + CELL_INSET, y + CELL_INSET, x + CELL_SIZE - CELL_INSET,
                    y + CELL_SIZE - CELL_INSET, c);
            ++col;
            if (col == COLUMNS) {
                col = 0;
                ++row;
            }
        }

        if (isKeyboardFocused()) {
            int x = getX() + (focusedIndex % COLUMNS) * CELL_PITCH;
            int y = getY() + (focusedIndex / COLUMNS) * CELL_PITCH;
            graphics.outline(x - FOCUS_OUTLINE_INSET, y - FOCUS_OUTLINE_INSET,
                    CELL_SIZE + 2 * FOCUS_OUTLINE_INSET, CELL_SIZE + 2 * FOCUS_OUTLINE_INSET, ColorConstants.WHITE);
        }
    }

    private int getCellIndex(double mouseX, double mouseY) {
        int localX = (int) Math.floor(mouseX - getX());
        int localY = (int) Math.floor(mouseY - getY());

        if (localX < 0 || localX >= WIDTH || localY < 0 || localY >= HEIGHT) {
            return -1;
        }

        int col = localX / CELL_PITCH;
        int row = localY / CELL_PITCH;

        if (localX % CELL_PITCH >= CELL_SIZE || localY % CELL_PITCH >= CELL_SIZE) {
            return -1;
        }

        return col + row * COLUMNS;
    }

    private int findColorIndex(int color) {
        for (int i = 0; i < PALETTE_COLORS.length; ++i) {
            if (PALETTE_COLORS[i] == color) {
                return i;
            }
        }

        return -1;
    }

    private int getEntryIndexForInitialFocus(FocusNavigationEvent navigationEvent) {
        if (navigationEvent instanceof FocusNavigationEvent.TabNavigation tabNavigation) {
            return tabNavigation.forward() ? 0 : PALETTE_COLORS.length - 1;
        }

        ScreenDirection direction = navigationEvent.getVerticalDirectionForInitialFocus();
        if (navigationEvent instanceof FocusNavigationEvent.ArrowNavigation arrowNavigation) {
            direction = arrowNavigation.direction();
        }

        return projectFocusedIndexToEntryEdge(direction);
    }

    private int getNextFocusedIndex(ScreenDirection direction) {
        int nextIndex = switch (direction) {
            case LEFT -> focusedIndex % COLUMNS == 0 ? -1 : focusedIndex - 1;
            case RIGHT -> focusedIndex % COLUMNS == COLUMNS - 1 ? -1 : focusedIndex + 1;
            case UP -> focusedIndex - COLUMNS;
            case DOWN -> focusedIndex + COLUMNS;
        };

        return nextIndex >= 0 && nextIndex < PALETTE_COLORS.length ? nextIndex : -1;
    }

    private int projectFocusedIndexToEntryEdge(ScreenDirection direction) {
        int row = focusedIndex / COLUMNS;
        int col = focusedIndex % COLUMNS;

        return switch (direction) {
            case UP -> (ROWS - 1) * COLUMNS + col;
            case DOWN -> col;
            case LEFT -> row * COLUMNS + (COLUMNS - 1);
            case RIGHT -> row * COLUMNS;
        };
    }

    private boolean isKeyboardFocused() {
        return isFocused() && Minecraft.getInstance().getLastInputType().isKeyboard();
    }
}
