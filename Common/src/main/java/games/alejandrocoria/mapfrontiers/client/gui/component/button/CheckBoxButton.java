package games.alejandrocoria.mapfrontiers.client.gui.component.button;

import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class CheckBoxButton extends ButtonBase {
    private static final int SIZE = 11;
    private static final int BORDER_INSET = 1;
    private static final int CHECK_INSET = 2;

    private boolean checked;

    public CheckBoxButton(boolean initialValue, OnPress pressedAction) {
        super(0, 0, SIZE, SIZE, Component.empty(), (b) -> pressedAction.onPress((CheckBoxButton) b), Button.DEFAULT_NARRATION);
        checked = initialValue;
    }

    public void toggle() {
        checked = !checked;
    }

    public boolean isChecked() {
        return checked;
    }

    @Override
    public void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (!visible || !active) {
            return;
        }

        graphics.fill(getX(), getY(), getX() + width, getY() + height, isHoveredOrKeyboardFocused() ? ColorConstants.CHECKBOX_BORDER_FOCUSED : ColorConstants.CHECKBOX_BORDER);
        graphics.fill(getX() + BORDER_INSET, getY() + BORDER_INSET, getX() + width - BORDER_INSET,
                getY() + height - BORDER_INSET, ColorConstants.CHECKBOX_BG);
        if (checked) {
            graphics.fill(getX() + CHECK_INSET, getY() + CHECK_INSET, getX() + width - CHECK_INSET,
                    getY() + height - CHECK_INSET, ColorConstants.CHECKBOX_CHECK);
        }
    }

    @Override
    public void onPress(InputWithModifiers modifiers) {
        toggle();
        super.onPress(modifiers);
    }


    public interface OnPress {
        void onPress(CheckBoxButton button);
    }
}
