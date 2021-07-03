package games.alejandrocoria.mapfrontiers.client.gui;

import javax.annotation.ParametersAreNonnullByDefault;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class GuiBookTag extends Button {
    private final ResourceLocation texture;
    private final int textureSize;
    private final boolean toLeft;

    public GuiBookTag(int x, int y, int width, boolean toLeft, ITextComponent text, ResourceLocation texture, int textureSize,
            Button.IPressable pressedAction) {
        super(x, y, Math.max(Math.min(width, 127), 67), 15, text, pressedAction);
        this.texture = texture;
        this.textureSize = textureSize;
        this.toLeft = toLeft;
    }

    @Override
    public void playDownSound(SoundHandler soundHandlerIn) {

    }

    @Override
    public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        isHovered = isMouseOver(mouseX, mouseY);

        RenderSystem.color3f(1.f, 1.f, 1.f);
        Minecraft mc = Minecraft.getInstance();
        mc.getTextureManager().bind(texture);
        int textureX = 330;
        int textureY = 52;

        if (isHovered) {
            textureX += 68;
        }

        if (toLeft) {
            textureY += 16;
            blit(matrixStack, x - width, y, textureX, textureY, Math.min(67, width), height, textureSize, textureSize);
            blit(matrixStack, x - width + 67, y, textureX + 7, textureY, width - 67, height, textureSize, textureSize);
            drawLeftLabel(matrixStack, mc.font);
        } else {
            blit(matrixStack, x, y, textureX, textureY, Math.min(60, width), height, textureSize, textureSize);
            blit(matrixStack, x + Math.min(60, width), y, textureX + 67 - width + 60, textureY, width - 60, height, textureSize,
                    textureSize);
            drawRightLabel(matrixStack, mc.font);
        }
    }

    @Override
    public boolean clicked(double mouseX, double mouseY) {
        return isMouseOver(mouseX, mouseY);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (active && visible) {
            if (toLeft) {
                return mouseX >= x - width && mouseY >= y && mouseX < x && mouseY < y + height;
            } else {
                return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
            }
        }

        return false;
    }

    private void drawLeftLabel(MatrixStack matrixStack, FontRenderer font) {
        int labelWidth = font.width(getMessage().getString());
        font.draw(matrixStack, getMessage().getString(), x - labelWidth - 4, y + 4, GuiColors.BOOKTAG_TEXT);
    }

    private void drawRightLabel(MatrixStack matrixStack, FontRenderer font) {
        font.draw(matrixStack, getMessage().getString(), x + 5, y + 4, GuiColors.BOOKTAG_TEXT);
    }
}
