package games.alejandrocoria.mapfrontiers.client.gui.component.button;

import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class CheckBoxButton extends ButtonBase {
    private boolean checked;

    public CheckBoxButton(boolean initialValue, OnPress pressedAction) {
        super(0, 0, 12, 12, Component.empty(), (b) -> pressedAction.onPress((CheckBoxButton) b), Button.DEFAULT_NARRATION);
        checked = initialValue;
    }

    public void toggle() {
        checked = !checked;
    }

    public boolean isChecked() {
        return checked;
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        if (!visible || !active) {
            return;
        }

        graphics.fill(getX(), getY(), getX() + 12, getY() + 12, isHoveredOrKeyboardFocused() ? ColorConstants.CHECKBOX_BORDER_FOCUSED : ColorConstants.CHECKBOX_BORDER);
        graphics.fill(getX() + 1, getY() + 1, getX() + 11, getY() + 11, ColorConstants.CHECKBOX_BG);
        if (checked) {
            graphics.fill(getX() + 2, getY() + 2, getX() + 10, getY() + 10, ColorConstants.CHECKBOX_CHECK);
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
