package games.alejandrocoria.mapfrontiers.client.gui;

import javax.annotation.ParametersAreNonnullByDefault;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.gui.components.Button;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class GuiSliderSlice extends Button {
    private final ResourceLocation texture;
    private final int textureSize;
    private int slice = 0;

    public GuiSliderSlice(int x, int y, ResourceLocation texture, int textureSize, Button.OnPress pressedAction) {
        super(x, y, 13, 69, TextComponent.EMPTY, pressedAction);
        this.texture = texture;
        this.textureSize = textureSize;
    }

    public void changeSlice(int slice) {
        this.slice = slice;
    }

    public int getSlice() {
        return slice;
    }

    @Override
    public void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
        changeSlice(mouseX, mouseY);
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        changeSlice(mouseX, mouseY);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        changeSlice(mouseX, mouseY);
    }

    @Override
    public void playDownSound(SoundManager soundHandlerIn) {

    }

    @Override
    public void renderButton(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        RenderSystem.setShaderColor(1.f, 1.f, 1.f, 1f);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);

        blit(matrixStack, x, y, 312, 1, width, height, textureSize, textureSize);
        blit(matrixStack, x, y + height - slice * 4 - 5, 312, 71, width, 5, textureSize, textureSize);
    }

    private void changeSlice(double mouseX, double mouseY) {
        slice = (y + height - (int) mouseY) / 4;
        slice = Math.min(Math.max(slice, 0), 16);
        onPress();
    }
}
