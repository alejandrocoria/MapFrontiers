package games.alejandrocoria.mapfrontiers.client.gui.component.button;

import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
public class OptionButton extends ButtonBase {
    private static final int DEFAULT_HEIGHT = 13;
    public static final OnPress DO_NOTHING = (b) -> {};

    protected final Font font;
    private final List<Component> options;
    private int selected = 0;
    private int color = ColorConstants.TEXT;
    private int highlightedColor = ColorConstants.TEXT_HIGHLIGHT;

    public OptionButton(Font font, int width, OnPress pressedAction) {
        super(0, 0, width, DEFAULT_HEIGHT, Component.empty(), (b) -> pressedAction.onPress((OptionButton) b), Button.DEFAULT_NARRATION);
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
    public boolean mouseScrolled(double mouseX, double mouseY, double hDelta, double vDelta) {
        if (visible && isHovered) {
            if (vDelta > 0) {
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
            this.onPress.onPress(this);
            return true;
        }

        return false;
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        int c = color;
        if (!active) {
            c = ColorConstants.TEXT_DARK;
        } else if (isHoveredOrKeyboardFocused()) {
            c = highlightedColor;
        }

        int borderColor = isKeyboardFocused() ? ColorConstants.OPTION_BORDER_FOCUSED : active ? ColorConstants.OPTION_BORDER : ColorConstants.OPTION_BORDER_DISABLED;
        graphics.fill(getX(), getY(), getX() + width, getY() + height, borderColor);
        graphics.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, ColorConstants.OPTION_BG);

        graphics.text(font, options.get(selected), getX() + 4, getY() + 3, c);
    }

    @Override
    public void onPress(InputWithModifiers modifiers) {
        ++selected;
        if (selected >= options.size()) {
            selected = 0;
        }

        super.onPress(modifiers);
    }


    public interface OnPress {
        void onPress(OptionButton button);
    }
}
