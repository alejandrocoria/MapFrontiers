package games.alejandrocoria.mapfrontiers.client.gui;

import javax.annotation.ParametersAreNonnullByDefault;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import games.alejandrocoria.mapfrontiers.MapFrontiers;
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
    private static final ResourceLocation texture = new ResourceLocation(MapFrontiers.MODID + ":textures/gui/buttons.png");
    private static final int textureSizeX = 26;
    private static final int textureSizeY = 26;

    public enum Type {
        Add, Remove
    }

    private int texY;

    public GuiButtonIcon(int x, int y, Type type, Button.IPressable pressedAction) {
        super(x, y, 13, 13, StringTextComponent.EMPTY, pressedAction);

        switch (type) {
            case Add:
                texY = 0;
                break;
            case Remove:
                texY = 13;
                break;
        }
    }

    @Override
    public void playDownSound(SoundHandler soundHandlerIn) {

    }

    @Override
    public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        RenderSystem.color4f(1.f, 1.f, 1.f, 1f);
        Minecraft.getInstance().getTextureManager().bind(texture);

        int texX = 0;
        if (isHovered) {
            texX = 13;
        }

        blit(matrixStack, x, y, texX, texY, width, height, textureSizeX, textureSizeY);
    }
}
