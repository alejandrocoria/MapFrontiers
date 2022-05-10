package games.alejandrocoria.mapfrontiers.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class GuiSettingsButton extends Button {
    private final FontRenderer font;
    private GuiSimpleLabel label;
    private int textColor = GuiColors.SETTINGS_BUTTON_TEXT;
    private int textColorHighlight = GuiColors.SETTINGS_BUTTON_TEXT_HIGHLIGHT;

    public GuiSettingsButton(FontRenderer font, int x, int y, int width, TextComponent text, Button.IPressable pressedAction) {
        super(x, y, width, 16, text, pressedAction);
        this.font = font;
        this.label = new GuiSimpleLabel(font, x + width / 2, y + 5, GuiSimpleLabel.Align.Center, text, textColor);
    }

    @Override
    public void setMessage(ITextComponent text) {
        this.label = new GuiSimpleLabel(font, x + width / 2, y + 5, GuiSimpleLabel.Align.Center, text, textColor);
    }

    @Override
    public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
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
