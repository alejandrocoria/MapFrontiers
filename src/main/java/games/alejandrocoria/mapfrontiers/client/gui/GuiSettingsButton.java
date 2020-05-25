package games.alejandrocoria.mapfrontiers.client.gui;

import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@ParametersAreNonnullByDefault
@SideOnly(Side.CLIENT)
public class GuiSettingsButton extends GuiButton {
    private GuiSimpleLabel label;

    public GuiSettingsButton(int componentId, FontRenderer fontRenderer, int x, int y, int width, String label) {
        super(componentId, x, y, width, 16, "");
        this.label = new GuiSimpleLabel(fontRenderer, x + width / 2, y + 5, GuiSimpleLabel.Align.Center, label,
                GuiColors.SETTINGS_BUTTON_TEXT);
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (visible) {
            hovered = (mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height);

            if (hovered) {
                label.setColor(GuiColors.SETTINGS_BUTTON_TEXT_HIGHLIGHT);
            } else {
                label.setColor(GuiColors.SETTINGS_BUTTON_TEXT);
            }

            drawHorizontalLine(x, x + width, y, GuiColors.SETTINGS_BUTTON_BORDER);
            drawHorizontalLine(x, x + width, y + 16, GuiColors.SETTINGS_BUTTON_BORDER);
            drawVerticalLine(x, y, y + 16, GuiColors.SETTINGS_BUTTON_BORDER);
            drawVerticalLine(x + width, y, y + 16, GuiColors.SETTINGS_BUTTON_BORDER);

            label.drawLabel(mc, mouseX, mouseY);
        } else {
            hovered = false;
        }
    }
}