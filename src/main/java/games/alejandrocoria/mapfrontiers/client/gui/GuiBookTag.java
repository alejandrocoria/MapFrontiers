package games.alejandrocoria.mapfrontiers.client.gui;

import javax.annotation.ParametersAreNonnullByDefault;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class GuiBookTag extends Button {
    private final ResourceLocation texture;
    private final int textureSize;
    private final boolean toLeft;

    public GuiBookTag(int x, int y, int width, boolean toLeft, Component text, ResourceLocation texture, int textureSize,
            Button.OnPress pressedAction) {
        super(x, y, Math.max(Math.min(width, 127), 67), 15, text, pressedAction);
        this.texture = texture;
        this.textureSize = textureSize;
        this.toLeft = toLeft;
    }

    @Override
    public void playDownSound(SoundManager soundHandlerIn) {

    }

    @Override
    public void renderButton(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        isHovered = isMouseOver(mouseX, mouseY);

        Minecraft mc = Minecraft.getInstance();
        RenderSystem.setShaderColor(1.f, 1.f, 1.f, 1f);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);
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

    private void drawLeftLabel(PoseStack matrixStack, Font font) {
        int labelWidth = font.width(getMessage().getString());
        font.draw(matrixStack, getMessage().getString(), x - labelWidth - 4, y + 4, GuiColors.BOOKTAG_TEXT);
    }

    private void drawRightLabel(PoseStack matrixStack, Font font) {
        font.draw(matrixStack, getMessage().getString(), x + 5, y + 4, GuiColors.BOOKTAG_TEXT);
    }
}
