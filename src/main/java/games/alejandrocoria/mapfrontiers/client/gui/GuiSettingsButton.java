package games.alejandrocoria.mapfrontiers.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class GuiSettingsButton extends Button {
    private final Font font;
    private GuiSimpleLabel label;
    private int textColor = GuiColors.SETTINGS_BUTTON_TEXT;
    private int textColorHighlight = GuiColors.SETTINGS_BUTTON_TEXT_HIGHLIGHT;

    public GuiSettingsButton(Font font, int x, int y, int width, Component text, Button.OnPress pressedAction) {
        super(x, y, width, 16, text, pressedAction, Button.DEFAULT_NARRATION);
        this.font = font;
        this.label = new GuiSimpleLabel(font, x + width / 2, y + 5, GuiSimpleLabel.Align.Center, text, textColor);
    }

    @Override
    public void setMessage(Component text) {
        this.label = new GuiSimpleLabel(font, getX() + width / 2, getY() + 5, GuiSimpleLabel.Align.Center, text, textColor);
    }

    @Override
    public void renderWidget(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        if (isHovered) {
            label.setColor(textColorHighlight);
        } else {
            label.setColor(textColor);
        }

        RenderSystem.setShaderColor(1.f, 1.f, 1.f, 1.f);

        hLine(matrixStack, getX(), getX() + width, getY(), GuiColors.SETTINGS_BUTTON_BORDER);
        hLine(matrixStack, getX(), getX() + width, getY() + 16, GuiColors.SETTINGS_BUTTON_BORDER);
        vLine(matrixStack, getX(), getY(), getY() + 16, GuiColors.SETTINGS_BUTTON_BORDER);
        vLine(matrixStack, getX() + width, getY(), getY() + 16, GuiColors.SETTINGS_BUTTON_BORDER);

        label.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    public void setTextColors(int color, int highlight) {
        textColor = color;
        textColorHighlight = highlight;
    }
}
