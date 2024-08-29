package games.alejandrocoria.mapfrontiers.client.gui.component.button;

import com.mojang.blaze3d.systems.RenderSystem;
import games.alejandrocoria.mapfrontiers.client.gui.ColorConstants;
import games.alejandrocoria.mapfrontiers.client.gui.component.SimpleLabel;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class SimpleButton extends Button {
    private final Font font;
    private SimpleLabel label;
    private int textColor = ColorConstants.SIMPLE_BUTTON_TEXT;
    private int textColorHighlight = ColorConstants.SIMPLE_BUTTON_TEXT_HIGHLIGHT;

    public SimpleButton(Font font, int x, int y, int width, Component text, OnPress pressedAction) {
        super(x, y, width, 16, text, pressedAction, Button.DEFAULT_NARRATION);
        this.font = font;
        this.label = new SimpleLabel(font, x + width / 2, y + 5, SimpleLabel.Align.Center, text, textColor);
    }

    @Override
    public void setMessage(Component text) {
        this.label = new SimpleLabel(font, getX() + width / 2, getY() + 5, SimpleLabel.Align.Center, text, textColor);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (isHovered) {
            label.setColor(textColorHighlight);
        } else {
            label.setColor(textColor);
        }

        RenderSystem.setShaderColor(1.f, 1.f, 1.f, 1.f);

        graphics.hLine(getX(), getX() + width, getY(), ColorConstants.SIMPLE_BUTTON_BORDER);
        graphics.hLine(getX(), getX() + width, getY() + 16, ColorConstants.SIMPLE_BUTTON_BORDER);
        graphics.vLine(getX(), getY(), getY() + 16, ColorConstants.SIMPLE_BUTTON_BORDER);
        graphics.vLine(getX() + width, getY(), getY() + 16, ColorConstants.SIMPLE_BUTTON_BORDER);

        label.render(graphics, mouseX, mouseY, partialTicks);
    }

    public void setTextColors(int color, int highlight) {
        textColor = color;
        textColorHighlight = highlight;
    }
}
