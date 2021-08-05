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
public class GuiButtonIcon extends Button {
    private final int texX;
    private final int texY;
    private final int diffX;
    private final ResourceLocation texture;
    private final int textureSize;

    public GuiButtonIcon(int x, int y, int width, int height, int texX, int texY, int diffX, ResourceLocation texture,
            int textureSize, Button.OnPress pressedAction) {
        super(x, y, width, height, TextComponent.EMPTY, pressedAction);
        this.texX = texX;
        this.texY = texY;
        this.diffX = diffX;
        this.texture = texture;
        this.textureSize = textureSize;
    }

    @Override
    public void playDownSound(SoundManager soundHandlerIn) {

    }

    @Override
    public void renderButton(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        RenderSystem.setShaderColor(1.f, 1.f, 1.f, 1f);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);
        int textureX = texX;

        if (isHovered) {
            textureX += diffX;
        }

        blit(matrixStack, x, y, textureX, texY, width, height, textureSize, textureSize);
    }
}
