package games.alejandrocoria.mapfrontiers.client.gui.component;

import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
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
    private final Consumer<Integer> onPress;

    public ColorPaletteWidget(int color, Consumer<Integer> onPress) {
        super(0, 0, WIDTH, HEIGHT, Component.empty());
        this.color = color;
        this.onPress = onPress;
    }

    public void setColor(int color) {
        this.color = color;
    }

    @Nullable
    public ComponentPath nextFocusPath(FocusNavigationEvent navigationEvent) {
        return null;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int localX = (int) Math.floor(event.x() - getX());
        int localY = (int) Math.floor(event.y() - getY());

        if (localX < 0 || localX >= WIDTH || localY < 0 || localY >= HEIGHT) {
            return false;
        }

        int col = localX / CELL_PITCH;
        int row = localY / CELL_PITCH;

        if (localX % CELL_PITCH >= CELL_SIZE || localY % CELL_PITCH >= CELL_SIZE) {
            return false;
        }

        color = PALETTE_COLORS[col + row * COLUMNS];
        onPress.accept(color);
        return true;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
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
    }
}
