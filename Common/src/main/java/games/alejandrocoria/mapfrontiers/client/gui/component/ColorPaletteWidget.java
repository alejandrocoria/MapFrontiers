package games.alejandrocoria.mapfrontiers.client.gui.component;

import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
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
    private static final int COLUMNS = 6;
    private static final int ROWS = 3;
    private static final int CELL_SIZE = 23;
    private static final int CELL_INSET = 1;

    private static final int[] PALETTE_COLORS = {
            0xffff0000, 0xffff8000, 0xffffff00, 0xff80ff00, 0xff00ff00, 0xff00ff80,
            0xff00ffff, 0xff0080ff, 0xff0000ff, 0xff8000ff, 0xffff00ff, 0xffff0080,
            0xff572f07, 0xff000000, 0xff404040, 0xff808080, 0xffbfbfbf, 0xffffffff};

    private static final int[] PALETTE_COLORS_INACTIVE = {
            0xff343434, 0xff595959, 0xff7e7e7e, 0xff6b6b6b, 0xff585858, 0xff5f5f5f,
            0xff666666, 0xff424242, 0xff1c1c1c, 0xff2f2f2f, 0xff424242, 0xff3b3b3b,
            0xff292929, 0xff0e0e0e, 0xff2e2e2e, 0xff4d4d4d, 0xff6d6d6d, 0xff8d8d8d};

    private int color;
    private final Consumer<Integer> onPress;

    public ColorPaletteWidget(int color, Consumer<Integer> onPress) {
        super(0, 0, COLUMNS * CELL_SIZE, ROWS * CELL_SIZE, Component.empty());
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
        double paletteX = (event.x() - getX()) / CELL_SIZE;
        double paletteY = (event.y() - getY()) / CELL_SIZE;
        if (paletteX >= 0.0 && paletteX < COLUMNS && paletteY >= 0.0 && paletteY < ROWS) {
            color = PALETTE_COLORS[(int) paletteX + (int) paletteY * COLUMNS];
            onPress.accept(color);
        }

        return true;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), ColorConstants.BLACK);
        int col = 0;
        int row = 0;
        for (int c : (active ? PALETTE_COLORS : PALETTE_COLORS_INACTIVE)) {
            if (active && c == color) {
                graphics.fill(getX() + col * CELL_SIZE, getY() + row * CELL_SIZE,
                        getX() + CELL_SIZE + col * CELL_SIZE, getY() + CELL_SIZE + row * CELL_SIZE,
                        ColorConstants.WHITE);
            }
            graphics.fill(getX() + CELL_INSET + col * CELL_SIZE, getY() + CELL_INSET + row * CELL_SIZE,
                    getX() + CELL_SIZE - CELL_INSET + col * CELL_SIZE,
                    getY() + CELL_SIZE - CELL_INSET + row * CELL_SIZE, c);
            ++col;
            if (col == COLUMNS) {
                col = 0;
                ++row;
            }
        }
    }
}
