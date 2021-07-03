package games.alejandrocoria.mapfrontiers.client.gui;

import javax.annotation.ParametersAreNonnullByDefault;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
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
            int textureSize, Button.IPressable pressedAction) {
        super(x, y, width, height, StringTextComponent.EMPTY, pressedAction);
        this.texX = texX;
        this.texY = texY;
        this.diffX = diffX;
        this.texture = texture;
        this.textureSize = textureSize;
    }

    @Override
    public void playDownSound(SoundHandler soundHandlerIn) {

    }

    @Override
    public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        RenderSystem.color3f(1.f, 1.f, 1.f);
        Minecraft mc = Minecraft.getInstance();
        mc.getTextureManager().bind(texture);
        int textureX = texX;

        if (isHovered) {
            textureX += diffX;
        }

        blit(matrixStack, x, y, textureX, texY, width, height, textureSize, textureSize);
    }
}
