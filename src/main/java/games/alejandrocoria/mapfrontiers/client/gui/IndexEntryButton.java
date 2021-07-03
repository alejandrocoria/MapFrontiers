package games.alejandrocoria.mapfrontiers.client.gui;

import javax.annotation.ParametersAreNonnullByDefault;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class IndexEntryButton extends Button {
    private final int page;
    private final String label1;
    private final String label2;
    private final int color;
    private final boolean colorToTheRight;

    public IndexEntryButton(int x, int y, int width, int page, String label1, String label2, int color, boolean colorToTheRight,
            Button.IPressable pressedAction) {
        super(x, y, width, 21, StringTextComponent.EMPTY, pressedAction);
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
    public void playDownSound(SoundHandler soundHandlerIn) {

    }

    @Override
    public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        int textColor = GuiColors.INDEX_TEXT;
        if (isHovered) {
            textColor = GuiColors.INDEX_TEXT_HIGHLIGHT;
        }

        Minecraft mc = Minecraft.getInstance();

        hLine(matrixStack, x, x + width, y, GuiColors.INDEX_SEPARATOR);
        drawLabel(matrixStack, mc.font, label1, x, y + 3, textColor);
        drawLabel(matrixStack, mc.font, label2, x, y + 12, textColor);
        hLine(matrixStack, x, x + width, y + 21, GuiColors.INDEX_SEPARATOR);

        int colorBoxX = x + 2;
        int colorBoxY = y + 4;

        if (colorToTheRight) {
            colorBoxX = x + width - 8;
        }

        fill(matrixStack, colorBoxX, colorBoxY, colorBoxX + 6, colorBoxY + 14, GuiColors.COLOR_INDICATOR_BORDER);
        fill(matrixStack, colorBoxX + 1, colorBoxY + 1, colorBoxX + 5, colorBoxY + 13, 0xff000000 | color);
    }

    private void drawLabel(MatrixStack matrixStack, FontRenderer font, String label, int x, int y, int textColor) {
        int labelWidth = font.width(label);
        x += width / 2 - labelWidth / 2;
        font.draw(matrixStack, label, x, y, textColor);
    }
}
