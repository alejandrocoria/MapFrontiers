package games.alejandrocoria.mapfrontiers.client.gui;

import javax.annotation.ParametersAreNonnullByDefault;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class GuiBookPages extends Widget {
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
        super(x, y, 0, 0, StringTextComponent.EMPTY);
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
    public void playDownSound(SoundHandler soundHandlerIn) {

    }

    @Override
    public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        RenderSystem.color3f(1.f, 1.f, 1.f);
        Minecraft mc = Minecraft.getInstance();
        mc.getTextureManager().bind(texture);

        if (doublePage) {
            blit(matrixStack, x, y, uOffset, vOffset, bookWidth, bookHeight, textureWidth, textureHeight);
        } else {
            blit(matrixStack, x + bookWidth / 2, y, uOffset + bookWidth / 2, vOffset, bookWidth / 2, bookHeight, textureWidth,
                    textureHeight);
        }
    }
}
