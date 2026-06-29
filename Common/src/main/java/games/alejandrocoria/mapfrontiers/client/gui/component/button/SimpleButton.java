package games.alejandrocoria.mapfrontiers.client.gui.component.button;

import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.StringWidget;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class SimpleButton extends ButtonBase {
    private static final int DEFAULT_HEIGHT = 15;
    private static final int LABEL_Y_OFFSET = -5;

    private final StringWidget label;
    private int textColor = ColorConstants.SIMPLE_BUTTON_TEXT_NORMAL;
    private int textColorHighlight = ColorConstants.SIMPLE_BUTTON_TEXT_HIGHLIGHT;
    private int textColorInactive = ColorConstants.SIMPLE_BUTTON_TEXT_DISABLED;

    public SimpleButton(Font font, int width, Component text, OnPress pressedAction) {
        super(0, 0, width, DEFAULT_HEIGHT, text, (b) -> pressedAction.onPress((SimpleButton) b), Button.DEFAULT_NARRATION);
        this.label = new StringWidget(text, font, StringWidget.Align.Center);
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        this.label.setX(x + width / 2);
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        this.label.setY(y + height / 2 + LABEL_Y_OFFSET);
    }

    @Override
    public void setMessage(Component text) {
        this.label.setMessage(text);
        setX(getX());
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (!active) {
            label.setColor(textColorInactive);
        } else if (isHoveredOrKeyboardFocused()) {
            label.setColor(textColorHighlight);
        } else {
            label.setColor(textColor);
        }

        int borderColor = isKeyboardFocused() ? ColorConstants.SIMPLE_BUTTON_BORDER_FOCUSED : active ? ColorConstants.SIMPLE_BUTTON_BORDER_NORMAL : ColorConstants.SIMPLE_BUTTON_BORDER_DISABLED;
        graphics.renderOutline(getX(), getY(), width, height, borderColor);
        graphics.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, ColorConstants.SIMPLE_BUTTON_BG);

        label.render(graphics, mouseX, mouseY, partialTicks);
    }

    public void setTextColors(int color, int highlight) {
        textColor = color;
        textColorHighlight = highlight;
    }


    public interface OnPress {
        void onPress(SimpleButton button);
    }
}
