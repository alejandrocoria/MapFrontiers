package games.alejandrocoria.mapfrontiers.client.gui;

import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@ParametersAreNonnullByDefault
@SideOnly(Side.CLIENT)
public class IndexEntryButton extends GuiButton {
    private final int page;
    private final String label1;
    private final String label2;
    private final int color;
    private final boolean colorToTheRight;

    public IndexEntryButton(int id, int x, int y, int width, int page, String label1, String label2, int color,
            boolean colorToTheRight) {
        super(id, x, y, width, 21, "");
        this.page = page;
        this.label1 = label1;
        this.label2 = label2;
        this.color = color;
        this.colorToTheRight = colorToTheRight;
    }

    public int getPage() {
        return page;
    }

    @Override
    public void playPressSound(SoundHandler soundHandlerIn) {

    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (visible) {
            hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;

            int textColor = GuiColors.INDEX_TEXT;
            if (hovered) {
                textColor = GuiColors.INDEX_TEXT_HIGHLIGHT;
            }

            drawHorizontalLine(x, x + width, y, GuiColors.INDEX_SEPARATOR);
            drawLabel(mc.fontRenderer, label1, x, y + 3, textColor);
            drawLabel(mc.fontRenderer, label2, x, y + 12, textColor);
            drawHorizontalLine(x, x + width, y + 21, GuiColors.INDEX_SEPARATOR);

            int colorBoxX = x + 2;
            int colorBoxY = y + 4;

            if (colorToTheRight) {
                colorBoxX = x + width - 8;
            }

            drawRect(colorBoxX, colorBoxY, colorBoxX + 6, colorBoxY + 14, GuiColors.COLOR_INDICATOR_BORDER);
            drawRect(colorBoxX + 1, colorBoxY + 1, colorBoxX + 5, colorBoxY + 13, 0xff000000 | color);
        } else {
            hovered = false;
        }
    }

    private void drawLabel(FontRenderer fontRenderer, String label, int x, int y, int textColor) {
        int labelWidth = fontRenderer.getStringWidth(label);
        x += width / 2 - labelWidth / 2;
        fontRenderer.drawString(label, x, y, textColor);
    }
}