package games.alejandrocoria.mapfrontiers.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@Environment(EnvType.CLIENT)
public class GuiSettingsButton extends Button {
    private final Font font;
    private GuiSimpleLabel label;
    private int textColor = GuiColors.SETTINGS_BUTTON_TEXT;
    private int textColorHighlight = GuiColors.SETTINGS_BUTTON_TEXT_HIGHLIGHT;

    public GuiSettingsButton(Font font, int x, int y, int width, Component text, Button.OnPress pressedAction) {
        super(x, y, width, 16, text, pressedAction);
        this.font = font;
        this.label = new GuiSimpleLabel(font, x + width / 2, y + 5, GuiSimpleLabel.Align.Center, text, textColor);
    }

    @Override
    public void setMessage(Component text) {
        this.label = new GuiSimpleLabel(font, x + width / 2, y + 5, GuiSimpleLabel.Align.Center, text, textColor);
    }

    @Override
    public void renderButton(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        if (isHovered) {
            label.setColor(textColorHighlight);
        } else {
            label.setColor(textColor);
        }

        hLine(matrixStack, x, x + width, y, GuiColors.SETTINGS_BUTTON_BORDER);
        hLine(matrixStack, x, x + width, y + 16, GuiColors.SETTINGS_BUTTON_BORDER);
        vLine(matrixStack, x, y, y + 16, GuiColors.SETTINGS_BUTTON_BORDER);
        vLine(matrixStack, x + width, y, y + 16, GuiColors.SETTINGS_BUTTON_BORDER);

        label.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    public void setTextColors(int color, int highlight) {
        textColor = color;
        textColorHighlight = highlight;
    }
}
