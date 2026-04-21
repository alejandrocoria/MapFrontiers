package games.alejandrocoria.mapfrontiers.client.gui.screen.dialog;

import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.LayoutConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.button.SimpleButton;
import games.alejandrocoria.mapfrontiers.client.gui.screen.AutoScaledScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public abstract class PanelDialog extends AutoScaledScreen {
    private static final Component CANCEL_LABEL = Component.translatable("gui.cancel");

    public PanelDialog(int minWidth, int minHeight) {
        super(Component.empty(), minWidth, minHeight, BottomButtonsMode.Integrated);
    }

    @Override
    protected void renderScaledBackgroundScreen(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        drawCenteredBoxBackground(graphics);
    }

    protected SimpleButton addConfirmButton(Component label, SimpleButton.OnPress onPress) {
        SimpleButton confirmButton = addBottomButton(new SimpleButton(font, LayoutConstants.PANEL_BUTTON_WIDTH, label, onPress));
        confirmButton.setTextColors(ColorConstants.SIMPLE_BUTTON_TEXT_CONFIRM, ColorConstants.SIMPLE_BUTTON_TEXT_CONFIRM_HIGHLIGHT);
        return confirmButton;
    }

    protected SimpleButton addCancelButton() {
        return addBottomButton(new SimpleButton(font, LayoutConstants.PANEL_BUTTON_WIDTH, CANCEL_LABEL, (b) -> onClose()));
    }
}
