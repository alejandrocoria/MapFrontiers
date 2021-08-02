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
public class GuiBookButton extends Button {
    private final GuiSimpleLabel label;
    private final boolean renderBorder;
    private final boolean lightColors;

    public GuiBookButton(Font font, int x, int y, int width, Component text, boolean renderBorder,
            boolean lightColors, Button.OnPress pressedAction) {
        super(x, y, width, 14, text, pressedAction);
        this.label = new GuiSimpleLabel(font, x + width / 2, y + 4, GuiSimpleLabel.Align.Center, text,
                GuiColors.SETTINGS_BUTTON_TEXT);
        this.renderBorder = renderBorder;
        this.lightColors = lightColors;
    }

    @Override
    public void renderButton(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        if (isHovered) {
            label.setColor(lightColors ? GuiColors.SETTINGS_BUTTON_TEXT_HIGHLIGHT : GuiColors.SETTINGS_BUTTON_TEXT_DARK);
        } else {
            label.setColor(lightColors ? GuiColors.SETTINGS_BUTTON_TEXT_LIGHT : GuiColors.SETTINGS_BUTTON_TEXT);
        }

        if (renderBorder) {
            int color = lightColors ? GuiColors.SETTINGS_BUTTON_BORDER_LIGHT : GuiColors.SETTINGS_BUTTON_BORDER;
            hLine(matrixStack, x, x + width, y, color);
            hLine(matrixStack, x, x + width, y + 14, color);
            vLine(matrixStack, x, y, y + 14, color);
            vLine(matrixStack, x + width, y, y + 14, color);
        }

        label.x = x + width / 2;
        label.y = y + 4;

        label.render(matrixStack, mouseX, mouseY, partialTicks);
    }
}
