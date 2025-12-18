package games.alejandrocoria.mapfrontiers.client.gui.component.button;

import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class LinkButton extends ButtonBase {
    private final Font font;

    public LinkButton(Font font, Component text, OnPress pressedAction) {
        super(0, 0, font.width(text.getString()) + 8, 12, text, pressedAction, Button.DEFAULT_NARRATION);
        this.font = font;
    }

    @Override
    public void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        graphics.drawString(font, getMessage(), getX(), getY() + 2, isHoveredOrKeyboardFocused() ? ColorConstants.LINK_HIGHLIGHT : ColorConstants.LINK);
    }
}
