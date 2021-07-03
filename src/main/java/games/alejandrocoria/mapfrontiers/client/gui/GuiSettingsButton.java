package games.alejandrocoria.mapfrontiers.client.gui;

import javax.annotation.ParametersAreNonnullByDefault;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class GuiSettingsButton extends Button {
    private final GuiSimpleLabel label;

    public GuiSettingsButton(FontRenderer font, int x, int y, int width, ITextComponent text,
            Button.IPressable pressedAction) {
        super(x, y, width, 16, text, pressedAction);
        this.label = new GuiSimpleLabel(font, x + width / 2, y + 5, GuiSimpleLabel.Align.Center, text,
                GuiColors.SETTINGS_BUTTON_TEXT);
    }

    @Override
    public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        if (isHovered) {
            label.setColor(GuiColors.SETTINGS_BUTTON_TEXT_HIGHLIGHT);
        } else {
            label.setColor(GuiColors.SETTINGS_BUTTON_TEXT);
        }

        hLine(matrixStack, x, x + width, y, GuiColors.SETTINGS_BUTTON_BORDER);
        hLine(matrixStack, x, x + width, y + 16, GuiColors.SETTINGS_BUTTON_BORDER);
        vLine(matrixStack, x, y, y + 16, GuiColors.SETTINGS_BUTTON_BORDER);
        vLine(matrixStack, x + width, y, y + 16, GuiColors.SETTINGS_BUTTON_BORDER);

        label.render(matrixStack, mouseX, mouseY, partialTicks);
    }
}
