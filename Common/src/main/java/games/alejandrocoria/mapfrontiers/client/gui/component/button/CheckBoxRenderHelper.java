package games.alejandrocoria.mapfrontiers.client.gui.component.button;

import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public final class CheckBoxRenderHelper {
    public static final int SIZE = 11;
    private static final int BORDER_INSET = 1;
    private static final int CHECK_INSET = 2;
    private static final int PARTIAL_WIDTH = 5;
    private static final int PARTIAL_HEIGHT = 1;

    private CheckBoxRenderHelper() {
    }

    public static void render(GuiGraphicsExtractor graphics, int x, int y, boolean focused, State state) {
        graphics.fill(x, y, x + SIZE, y + SIZE,
                focused ? ColorConstants.CHECKBOX_BORDER_FOCUSED : ColorConstants.CHECKBOX_BORDER);
        graphics.fill(x + BORDER_INSET, y + BORDER_INSET, x + SIZE - BORDER_INSET, y + SIZE - BORDER_INSET, ColorConstants.CHECKBOX_BG);

        if (state == State.CHECKED) {
            graphics.fill(x + CHECK_INSET, y + CHECK_INSET, x + SIZE - CHECK_INSET, y + SIZE - CHECK_INSET,
                    ColorConstants.CHECKBOX_CHECK);
        } else if (state == State.PARTIAL) {
            int partialX = x + (SIZE - PARTIAL_WIDTH) / 2;
            int partialY = y + (SIZE - PARTIAL_HEIGHT) / 2;
            graphics.fill(partialX, partialY, partialX + PARTIAL_WIDTH, partialY + PARTIAL_HEIGHT, ColorConstants.CHECKBOX_CHECK);
        }
    }

    public static boolean contains(int x, int y, double mouseX, double mouseY) {
        return mouseX >= x && mouseY >= y && mouseX < x + SIZE && mouseY < y + SIZE;
    }

    public enum State {
        UNCHECKED,
        PARTIAL,
        CHECKED
    }
}
