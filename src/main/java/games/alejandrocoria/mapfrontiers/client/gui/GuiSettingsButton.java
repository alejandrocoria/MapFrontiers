package games.alejandrocoria.mapfrontiers.client.gui;

import javax.annotation.ParametersAreNonnullByDefault;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class GuiSettingsButton extends Button {
    private final Font font;
    private GuiSimpleLabel label;

    public GuiSettingsButton(Font font, int x, int y, int width, Component text, Button.OnPress pressedAction) {
        super(x, y, width, 16, text, pressedAction);
        this.font = font;
        this.label = new GuiSimpleLabel(font, x + width / 2, y + 5, GuiSimpleLabel.Align.Center, text,
                GuiColors.SETTINGS_BUTTON_TEXT);
    }

    @Override
    public void setMessage(Component text) {
        this.label = new GuiSimpleLabel(font, x + width / 2, y + 5, GuiSimpleLabel.Align.Center, text,
                GuiColors.SETTINGS_BUTTON_TEXT);
    }

    @Override
    public void renderButton(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
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
