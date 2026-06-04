package games.alejandrocoria.mapfrontiers.client.gui.component.button;

import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class CheckBoxButton extends ButtonBase {
    public static final int SIZE = 11;
    private static final int BORDER_INSET = 1;
    private static final int CHECK_INSET = 2;
    private static final int PARTIAL_WIDTH = 5;
    private static final int PARTIAL_HEIGHT = 1;

    private enum State {
        UNCHECKED,
        CHECKED,
        PARTIAL
    }

    private State state;

    public CheckBoxButton(boolean initialValue, OnPress pressedAction) {
        super(0, 0, SIZE, SIZE, Component.empty(),
                (b) -> pressedAction.onPress((CheckBoxButton) b), Button.DEFAULT_NARRATION);
        state = initialValue ? State.CHECKED : State.UNCHECKED;
    }

    public void toggle() {
        if (state == State.PARTIAL) {
            state = State.UNCHECKED;
            return;
        }

        state = state == State.CHECKED ? State.UNCHECKED : State.CHECKED;
    }

    public boolean isChecked() {
        return state == State.CHECKED;
    }

    public void setChecked(boolean checked) {
        state = checked ? State.CHECKED : State.UNCHECKED;
    }

    public void setPartial() {
        state = State.PARTIAL;
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        if (!visible) {
            return;
        }

        int borderColor = !active ? ColorConstants.CHECKBOX_BORDER_DISABLED
                : isHoveredOrKeyboardFocused() ? ColorConstants.CHECKBOX_BORDER_FOCUSED : ColorConstants.CHECKBOX_BORDER;
        int backgroundColor = active ? ColorConstants.CHECKBOX_BG : ColorConstants.CHECKBOX_BG_DISABLED;
        int checkColor = active ? ColorConstants.CHECKBOX_CHECK : ColorConstants.CHECKBOX_CHECK_DISABLED;

        graphics.fill(getX(), getY(), getX() + SIZE, getY() + SIZE,
                borderColor);
        graphics.fill(getX() + BORDER_INSET, getY() + BORDER_INSET, getX() + SIZE - BORDER_INSET, getY() + SIZE - BORDER_INSET, backgroundColor);

        if (state == State.CHECKED) {
            graphics.fill(getX() + CHECK_INSET, getY() + CHECK_INSET, getX() + SIZE - CHECK_INSET, getY() + SIZE - CHECK_INSET,
                    checkColor);
        } else if (state == State.PARTIAL) {
            int partialX = getX() + (SIZE - PARTIAL_WIDTH) / 2;
            int partialY = getY() + (SIZE - PARTIAL_HEIGHT) / 2;
            graphics.fill(partialX, partialY, partialX + PARTIAL_WIDTH, partialY + PARTIAL_HEIGHT, checkColor);
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
