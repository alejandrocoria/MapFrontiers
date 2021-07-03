package games.alejandrocoria.mapfrontiers.client.gui;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class GuiOptionButton extends Button {
    protected final FontRenderer font;
    private final List<String> options;
    private int selected = 0;
    private int color = GuiColors.SETTINGS_TEXT;
    private int highlightedColor = GuiColors.SETTINGS_TEXT_HIGHLIGHT;

    public GuiOptionButton(FontRenderer font, int x, int y, int width, Button.IPressable pressedAction) {
        super(x, y, width, 12, StringTextComponent.EMPTY, pressedAction);
        this.font = font;
        options = new ArrayList<>();
    }

    public void addOption(String text) {
        options.add(text);
    }

    public void setSelected(int selected) {
        if (selected < 0) {
            selected = 0;
        } else if (selected >= options.size()) {
            selected = options.size() - 1;
        }

        this.selected = selected;
    }

    public int getSelected() {
        return selected;
    }

    public void setColor(int color) {
        this.color = color;
        highlightedColor = color;
    }

    public void setColor(int color, int highlightedColor) {
        this.color = color;
        this.highlightedColor = highlightedColor;
    }

    public int getColor() {
        return color;
    }

    @Override
    public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        int c = color;
        if (isHovered) {
            c = highlightedColor;
        }

        fill(matrixStack, x - 1, y - 1, x + width + 1, y + height + 1, GuiColors.SETTINGS_OPTION_BORDER);
        fill(matrixStack, x, y, x + width, y + height, GuiColors.SETTINGS_OPTION_BG);

        font.draw(matrixStack, options.get(selected), x + 4, y + 2, c);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        ++selected;
        if (selected >= options.size()) {
            selected = 0;
        }

        onPress();
    }
}
