package games.alejandrocoria.mapfrontiers.client.gui;

import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@ParametersAreNonnullByDefault
@SideOnly(Side.CLIENT)
public class GuiSliderSlice extends GuiButton {
    private final ResourceLocation texture;
    private final int textureSize;
    private int slice = 0;

    public GuiSliderSlice(int id, int x, int y, ResourceLocation texture, int textureSize) {
        super(id, x, y, 13, 69, "");
        this.texture = texture;
        this.textureSize = textureSize;
    }

    public void changeSlice(int slice) {
        this.slice = slice;
    }

    public int getSlice() {
        return slice;
    }

    public void mouseDragged(int mouseX, int mouseY) {
        changeSlice(mouseX, mouseY);
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY) {
        changeSlice(mouseX, mouseY);
    }

    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        if (enabled && visible && hovered) {
            changeSlice(mouseX, mouseY);
            return true;
        }

        return false;
    }

    @Override
    public void playPressSound(SoundHandler soundHandlerIn) {

    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (visible) {
            hovered = (mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height);
            GlStateManager.color(1.f, 1.f, 1.f);
            mc.getTextureManager().bindTexture(texture);

            drawModalRectWithCustomSizedTexture(x, y, 312, 1, width, height, textureSize, textureSize);
            drawModalRectWithCustomSizedTexture(x, y + height - slice * 4 - 5, 312, 71, width, 5, textureSize, textureSize);
        } else {
            hovered = false;
        }
    }

    private void changeSlice(int mouseX, int mouseY) {
        slice = (y + height - mouseY) / 4;
        slice = Math.min(Math.max(slice, 0), 16);
    }
}