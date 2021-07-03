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
public class GuiSliderSlice extends Button {
    private final ResourceLocation texture;
    private final int textureSize;
    private int slice = 0;

    public GuiSliderSlice(int x, int y, ResourceLocation texture, int textureSize, Button.IPressable pressedAction) {
        super(x, y, 13, 69, StringTextComponent.EMPTY, pressedAction);
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
    public void playDownSound(SoundHandler soundHandlerIn) {

    }

    @Override
    public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        RenderSystem.color3f(1.f, 1.f, 1.f);
        Minecraft mc = Minecraft.getInstance();
        mc.getTextureManager().bind(texture);

        blit(matrixStack, x, y, 312, 1, width, height, textureSize, textureSize);
        blit(matrixStack, x, y + height - slice * 4 - 5, 312, 71, width, 5, textureSize, textureSize);
    }

    private void changeSlice(double mouseX, double mouseY) {
        slice = (y + height - (int) mouseY) / 4;
        slice = Math.min(Math.max(slice, 0), 16);
        onPress();
    }
}
