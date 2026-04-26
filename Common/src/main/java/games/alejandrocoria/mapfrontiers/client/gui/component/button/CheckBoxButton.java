package games.alejandrocoria.mapfrontiers.client.gui.component.button;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class CheckBoxButton extends ButtonBase {
    private boolean checked;

    public CheckBoxButton(boolean initialValue, OnPress pressedAction) {
        super(0, 0, CheckBoxRenderHelper.SIZE, CheckBoxRenderHelper.SIZE, Component.empty(),
                (b) -> pressedAction.onPress((CheckBoxButton) b), Button.DEFAULT_NARRATION);
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

        CheckBoxRenderHelper.render(graphics, getX(), getY(), isHoveredOrKeyboardFocused(),
                checked ? CheckBoxRenderHelper.State.CHECKED : CheckBoxRenderHelper.State.UNCHECKED);
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
