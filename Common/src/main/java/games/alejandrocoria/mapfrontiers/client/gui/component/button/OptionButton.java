package games.alejandrocoria.mapfrontiers.client.gui.component.button;

import com.mojang.blaze3d.vertex.PoseStack;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
public class OptionButton extends Button {
    protected final Font font;
    private final List<Component> options;
    private int selected = 0;
    private int color = ColorConstants.TEXT;
    private int highlightedColor = ColorConstants.TEXT_HIGHLIGHT;

    public OptionButton(Font font, int x, int y, int width, OnPress pressedAction) {
        super(x, y, width, 12, Component.empty(), pressedAction, Button.DEFAULT_NARRATION);
        this.font = font;
        options = new ArrayList<>();
    }

    public void addOption(Component text) {
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

    public void setColor(int color, int highlightedColor) {
        this.color = color;
        this.highlightedColor = highlightedColor;
    }

    public int getColor() {
        return color;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (visible && isHovered) {
            if (delta > 0) {
                ++selected;
                if (selected >= options.size()) {
                    selected = 0;
                }
            } else {
                --selected;
                if (selected < 0) {
                    selected = options.size() - 1;
                }
            }

            playDownSound(Minecraft.getInstance().getSoundManager());
            onPress();
            return true;
        }

        return false;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        int c = color;
        if (!active) {
            c = ColorConstants.TEXT_DARK;
        } else if (isHovered) {
            c = highlightedColor;
        }

        graphics.fill(getX() - 1, getY() - 1, getX() + width + 1, getY() + height + 1, ColorConstants.OPTION_BORDER);
        graphics.fill(getX(), getY(), getX() + width, getY() + height, ColorConstants.OPTION_BG);

        graphics.drawString(font, options.get(selected), getX() + 4, getY() + 2, c);
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
