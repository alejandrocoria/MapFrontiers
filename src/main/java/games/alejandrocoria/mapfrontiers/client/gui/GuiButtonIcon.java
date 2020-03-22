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
public class GuiButtonIcon extends GuiButton {
    private final int texX;
    private final int texY;
    private final int diffX;
    private final ResourceLocation texture;
    private final int textureSize;

    public GuiButtonIcon(int id, int x, int y, int width, int height, int texX, int texY, int diffX, ResourceLocation texture,
            int textureSize) {
        super(id, x, y, width, height, "");
        this.texX = texX;
        this.texY = texY;
        this.diffX = diffX;
        this.texture = texture;
        this.textureSize = textureSize;
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
            int textureX = texX;
            int textureY = texY;

            if (hovered) {
                textureX += diffX;
            }

            drawModalRectWithCustomSizedTexture(x, y, textureX, textureY, width, height, textureSize, textureSize);
        }
    }
}