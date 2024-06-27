package games.alejandrocoria.mapfrontiers.client.gui.component.button;

import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.SimpleLabel;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class LinkButton extends Button {
    private final SimpleLabel label;

    public LinkButton(Font font, int x, int y, Component text, OnPress pressedAction) {
        super(x, y, font.width(text.getString()) + 8, 16, text, pressedAction, Button.DEFAULT_NARRATION);
        setX(getX() - width / 2);
        this.label = new SimpleLabel(font, x, y + 5, SimpleLabel.Align.Center, text, ColorConstants.SIMPLE_BUTTON_TEXT);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (isHovered) {
            label.setColor(ColorConstants.LINK_HIGHLIGHT);
        } else {
            label.setColor(ColorConstants.LINK);
        }

        label.render(graphics, mouseX, mouseY, partialTicks);
    }
}
