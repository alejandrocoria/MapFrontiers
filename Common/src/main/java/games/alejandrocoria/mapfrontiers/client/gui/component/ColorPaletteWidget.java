package games.alejandrocoria.mapfrontiers.client.gui.component;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class ColorPaletteWidget extends AbstractWidgetNoNarration {
    private static final int[] palette = {
            0xffff0000, 0xffff8000, 0xffffff00, 0xff80ff00, 0xff00ff00, 0xff00ff80,
            0xff00ffff, 0xff0080ff, 0xff0000ff, 0xff8000ff, 0xffff00ff, 0xffff0080,
            0xff572f07, 0xff000000, 0xff404040, 0xff808080, 0xffbfbfbf, 0xffffffff};

    private static final int[] paletteInactive = {
            0xff343434, 0xff595959, 0xff7e7e7e, 0xff6b6b6b, 0xff585858, 0xff5f5f5f,
            0xff666666, 0xff424242, 0xff1c1c1c, 0xff2f2f2f, 0xff424242, 0xff3b3b3b,
            0xff292929, 0xff0e0e0e, 0xff2e2e2e, 0xff4d4d4d, 0xff6d6d6d, 0xff8d8d8d};

    private int color;
    private final Consumer<Integer> onPress;

    public ColorPaletteWidget(int color, Consumer<Integer> onPress) {
        super(0, 0, 139, 70, Component.empty());
        this.color = color;
        this.onPress = onPress;
    }

    public void setColor(int color) {
        this.color = color;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        double paletteX = (mouseX - getX()) / 23.0;
        double paletteY = (mouseY - getY()) / 23.0;
        if (paletteX >= 0.0 && paletteX < 6.0 && paletteY >= 0.0 && paletteY < 3.0) {
            color = palette[(int) paletteX + (int) paletteY * 6];
            onPress.accept(color);
        }
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0xff000000);
        int col = 0;
        int row = 0;
        for (int c : (active ? palette : paletteInactive)) {
            if (active && c == color) {
                graphics.fill(getX() + col * 23, getY() + row * 23, getX() + 23 + col * 23, getY() + 23 + row * 23, 0xffffffff);
            }
            graphics.fill(getX() + 1 + col * 23, getY() + 1 + row * 23, getX() + 22 + col * 23, getY() + 22 + row * 23, c);
            ++col;
            if (col == 6) {
                col = 0;
                ++row;
            }
        }
    }
}
