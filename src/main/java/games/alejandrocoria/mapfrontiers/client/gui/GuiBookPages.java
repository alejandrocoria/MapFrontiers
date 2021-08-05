package games.alejandrocoria.mapfrontiers.client.gui;

import javax.annotation.ParametersAreNonnullByDefault;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class GuiBookPages extends AbstractWidget {
    private final ResourceLocation texture;
    private boolean doublePage = false;
    private final float uOffset;
    private final float vOffset;
    private final int bookWidth;
    private final int bookHeight;
    private final int textureWidth;
    private final int textureHeight;

    public GuiBookPages(int x, int y, float uOffset, float vOffset, int bookWidth, int bookHeight, int textureWidth,
            int textureHeight, ResourceLocation texture) {
        super(x, y, 0, 0, TextComponent.EMPTY);
        this.texture = texture;
        this.uOffset = uOffset;
        this.vOffset = vOffset;
        this.bookWidth = bookWidth;
        this.bookHeight = bookHeight;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
    }

    public void setDoublePage(boolean enable) {
        doublePage = enable;
    }

    @Override
    public boolean clicked(double mouseX, double mouseY) {
        return false;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return false;
    }

    @Override
    public void playDownSound(SoundManager soundHandlerIn) {

    }

    @Override
    public void renderButton(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        RenderSystem.setShaderColor(1.f, 1.f, 1.f, 1f);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);

        if (doublePage) {
            blit(matrixStack, x, y, uOffset, vOffset, bookWidth, bookHeight, textureWidth, textureHeight);
        } else {
            blit(matrixStack, x + bookWidth / 2, y, uOffset + bookWidth / 2, vOffset, bookWidth / 2, bookHeight, textureWidth,
                    textureHeight);
        }
    }

    @Override
    public void updateNarration(NarrationElementOutput p_169152_)
    {

    }
}
